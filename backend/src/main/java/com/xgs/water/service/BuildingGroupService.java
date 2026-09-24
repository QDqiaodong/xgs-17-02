package com.xgs.water.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xgs.water.entity.BuildingGroup;
import com.xgs.water.mapper.BuildingGroupMapper;
import com.xgs.water.mapper.WaterDispenserMapper;
import com.xgs.water.vo.BuildingGroupTreeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuildingGroupService {

    @Autowired
    private BuildingGroupMapper buildingGroupMapper;
    @Autowired
    private WaterDispenserMapper waterDispenserMapper;
    @Autowired
    private SearchCacheClient searchCacheClient;

    public List<BuildingGroupTreeVO> getTree() {
        List<BuildingGroup> allGroups = buildingGroupMapper.selectList(
                new LambdaQueryWrapper<BuildingGroup>()
                        .orderByAsc(BuildingGroup::getSortOrder, BuildingGroup::getId)
        );

        Map<Long, BuildingGroupTreeVO> voMap = new HashMap<>();
        for (BuildingGroup group : allGroups) {
            BuildingGroupTreeVO vo = convertToVO(group);
            vo.setDeviceCount(waterDispenserMapper.countByGroupId(group.getId()));
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
        searchCacheClient.bumpVersion();
    }

    @Transactional
    public void update(BuildingGroup group) {
        buildingGroupMapper.updateById(group);
        searchCacheClient.bumpVersion();
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
        searchCacheClient.bumpVersion();
    }

    public String getGroupPath(Long groupId) {
        List<String> pathNames = new ArrayList<>();
        Long currentId = groupId;
        while (currentId != null && currentId > 0) {
            BuildingGroup group = buildingGroupMapper.selectById(currentId);
            if (group == null) break;
            pathNames.add(0, group.getName());
            currentId = group.getParentId();
        }
        return String.join(" / ", pathNames);
    }

    public List<Long> getAllChildGroupIds(Long groupId) {
        List<Long> result = new ArrayList<>();
        result.add(groupId);
        collectChildIds(groupId, result);
        return result;
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
