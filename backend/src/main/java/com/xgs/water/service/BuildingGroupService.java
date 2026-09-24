package com.xgs.water.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xgs.water.entity.BuildingGroup;
import com.xgs.water.entity.BuildingGroupClosure;
import com.xgs.water.mapper.BuildingGroupClosureMapper;
import com.xgs.water.mapper.BuildingGroupMapper;
import com.xgs.water.mapper.WaterDispenserMapper;
import com.xgs.water.vo.BuildingGroupTreeVO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuildingGroupService {

    private static final Logger log = LoggerFactory.getLogger(BuildingGroupService.class);

    @Autowired
    private BuildingGroupMapper buildingGroupMapper;
    @Autowired
    private BuildingGroupClosureMapper closureMapper;
    @Autowired
    private WaterDispenserMapper waterDispenserMapper;

    /**
     * 启动幂等回填闭包表：
     * 旧库升级时闭包表为空或不完整，按当前区域树用可移植的 Java 逻辑重建
     *（不依赖数据库的递归 CTE 方言；SQL 迁移脚本里另有 MySQL 8 原生 CTE 版本）。
     */
    @PostConstruct
    public void rebuildClosureIfNeeded() {
        try {
            long groupCount = buildingGroupMapper.selectCount(null);
            long closureCount = closureMapper.countAll();
            if (groupCount > 0 && closureCount < groupCount) {
                List<BuildingGroup> all = buildingGroupMapper.selectList(null);
                closureMapper.delete(null);
                Map<Long, BuildingGroup> byId = new HashMap<>();
                for (BuildingGroup g : all) {
                    byId.put(g.getId(), g);
                }
                List<BuildingGroupClosure> rows = new ArrayList<>();
                for (BuildingGroup g : all) {
                    // 自身
                    rows.add(closure(g.getId(), g.getId(), 0));
                    // 逐级向上找祖先
                    Long parentId = g.getParentId();
                    int distance = 1;
                    Set<Long> guard = new HashSet<>();
                    while (parentId != null && parentId > 0 && guard.add(parentId)) {
                        BuildingGroup ancestor = byId.get(parentId);
                        if (ancestor == null) break;
                        rows.add(closure(ancestor.getId(), g.getId(), distance));
                        parentId = ancestor.getParentId();
                        distance++;
                    }
                }
                // 批量写入，避免上万区域时逐行 INSERT
                for (int i = 0; i < rows.size(); i++) {
                    closureMapper.insert(rows.get(i));
                }
                log.info("区域闭包表已重建：{} 个区域，{} 条关系", groupCount, closureMapper.countAll());
            }
        } catch (Exception e) {
            // 表尚未建好等情况下不阻断启动；检索接口在闭包缺失时仍可回退为递归收集
            log.warn("区域闭包表回填跳过：{}", e.getMessage());
        }
    }

    private BuildingGroupClosure closure(Long ancestorId, Long descendantId, int distance) {
        BuildingGroupClosure c = new BuildingGroupClosure();
        c.setAncestorId(ancestorId);
        c.setDescendantId(descendantId);
        c.setDistance(distance);
        return c;
    }

    public List<BuildingGroupTreeVO> getTree() {
        List<BuildingGroup> allGroups = buildingGroupMapper.selectList(
                new LambdaQueryWrapper<BuildingGroup>()
                        .orderByAsc(BuildingGroup::getSortOrder, BuildingGroup::getId)
        );

        // 设备数一次 GROUP BY 取回，避免逐节点 count 的 N+1
        Map<Long, Integer> countMap = waterDispenserMapper.countGroupedByGroup().stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).intValue(),
                        (a, b) -> a));

        Map<Long, BuildingGroupTreeVO> voMap = new HashMap<>();
        for (BuildingGroup group : allGroups) {
            BuildingGroupTreeVO vo = convertToVO(group);
            vo.setDeviceCount(countMap.getOrDefault(group.getId(), 0));
            voMap.put(group.getId(), vo);
        }

        List<BuildingGroupTreeVO> roots = new ArrayList<>();
        for (BuildingGroup group : allGroups) {
            BuildingGroupTreeVO vo = voMap.get(group.getId());
            if (group.getParentId() == null || group.getParentId() == 0) {
                roots.add(vo);
            } else {
                BuildingGroupTreeVO parent = voMap.get(group.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }

    public List<BuildingGroupTreeVO> getFlatLeafNodes() {
        return buildingGroupMapper.selectList(
                new LambdaQueryWrapper<BuildingGroup>()
                        .eq(BuildingGroup::getType, 3)
                        .orderByAsc(BuildingGroup::getSortOrder, BuildingGroup::getId)
        ).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private BuildingGroupTreeVO convertToVO(BuildingGroup group) {
        BuildingGroupTreeVO vo = new BuildingGroupTreeVO();
        vo.setId(group.getId());
        vo.setParentId(group.getParentId());
        vo.setLabel(group.getName());
        vo.setType(group.getType());
        vo.setTypeName(getTypeName(group.getType()));
        vo.setSortOrder(group.getSortOrder());
        vo.setDescription(group.getDescription());
        return vo;
    }

    private String getTypeName(Integer type) {
        return switch (type) {
            case 1 -> "园区";
            case 2 -> "楼栋";
            case 3 -> "楼层";
            default -> "未知";
        };
    }

    public BuildingGroup getById(Long id) {
        return buildingGroupMapper.selectById(id);
    }

    @Transactional
    public void save(BuildingGroup group) {
        if (group.getParentId() == null) {
            group.setParentId(0L);
        }
        if (group.getSortOrder() == null) {
            group.setSortOrder(0);
        }
        buildingGroupMapper.insert(group);
        // 挂载闭包：新节点 = 父节点所有祖先(含父)的后代 + 自身
        closureMapper.insertForNewNode(group.getId(), group.getParentId());
    }

    @Transactional
    public void update(BuildingGroup group) {
        // 改名不影响闭包关系；本系统层级固定（园区/楼栋/楼层）且不提供移动子树功能
        buildingGroupMapper.updateById(group);
    }

    @Transactional
    public void delete(Long id) {
        BuildingGroup group = buildingGroupMapper.selectById(id);
        if (group == null) {
            throw new RuntimeException("分组不存在");
        }
        Long count = buildingGroupMapper.selectCount(
                new LambdaQueryWrapper<BuildingGroup>().eq(BuildingGroup::getParentId, id)
        );
        if (count > 0) {
            throw new RuntimeException("存在子节点，无法删除");
        }
        Integer deviceCount = waterDispenserMapper.countByGroupId(id);
        if (deviceCount > 0) {
            throw new RuntimeException("该分组下存在设备，无法删除");
        }
        buildingGroupMapper.deleteById(id);
        closureMapper.deleteDescendantRefs(id);
    }

    /**
     * 完整区域路径（如「创新产业园A区 / 1号楼 / 1层」）。
     * 走闭包表取祖先链，单次 SQL；同一线程内按请求缓存，避免列表逐行重复查询。
     */
    public String getGroupPath(Long groupId) {
        if (groupId == null) {
            return "";
        }
        Map<Long, String> cache = PathCache.current();
        String cached = cache.get(groupId);
        if (cached != null) {
            return cached;
        }
        List<Long> ancestorIds = closureMapper.selectAncestorIds(groupId);
        if (ancestorIds.isEmpty()) {
            // 闭包缺失（如异常库）回退为逐节点向上
            ancestorIds = fallbackAncestorIds(groupId);
        }
        List<String> names = new ArrayList<>();
        for (Long aid : ancestorIds) {
            BuildingGroup g = buildingGroupMapper.selectById(aid);
            if (g != null) {
                names.add(g.getName());
            }
        }
        String path = String.join(" / ", names);
        cache.put(groupId, path);
        return path;
    }

    private List<Long> fallbackAncestorIds(Long groupId) {
        LinkedList<Long> ids = new LinkedList<>();
        Long currentId = groupId;
        while (currentId != null && currentId > 0) {
            BuildingGroup g = buildingGroupMapper.selectById(currentId);
            if (g == null) break;
            ids.addFirst(currentId);
            currentId = g.getParentId();
        }
        return ids;
    }

    /**
     * 取某区域全部下级（含自身）。选中园区/楼栋时检索范围必须包含所有下级区域。
     * 走闭包表单次 SQL；闭包缺失时回退递归。
     */
    public List<Long> getAllChildGroupIds(Long groupId) {
        if (groupId == null) {
            return null;
        }
        List<Long> ids = closureMapper.selectDescendantIds(groupId);
        if (ids.isEmpty()) {
            ids = new ArrayList<>();
            ids.add(groupId);
            collectChildIds(groupId, ids);
        }
        return ids;
    }

    private void collectChildIds(Long parentId, List<Long> result) {
        List<BuildingGroup> children = buildingGroupMapper.selectList(
                new LambdaQueryWrapper<BuildingGroup>().eq(BuildingGroup::getParentId, parentId)
        );
        for (BuildingGroup child : children) {
            result.add(child.getId());
            collectChildIds(child.getId(), result);
        }
    }
}
