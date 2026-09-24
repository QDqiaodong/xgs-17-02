package com.xgs.water.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgs.water.dto.WaterDispenserDTO;
import com.xgs.water.entity.GroupTransferLog;
import com.xgs.water.entity.WaterDispenser;
import com.xgs.water.mapper.GroupTransferLogMapper;
import com.xgs.water.mapper.WaterDispenserMapper;
import com.xgs.water.vo.WaterDispenserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WaterDispenserService {

    @Autowired
    private WaterDispenserMapper waterDispenserMapper;
    @Autowired
    private GroupTransferLogMapper transferLogMapper;
    @Autowired
    private BuildingGroupService buildingGroupService;

    public IPage<WaterDispenserVO> page(Integer pageNum, Integer pageSize,
                                         Long groupId, String keyword, Integer status) {
        Page<WaterDispenserVO> page = new Page<>(pageNum, pageSize);
        List<Long> groupIds = null;
        if (groupId != null) {
            groupIds = buildingGroupService.getAllChildGroupIds(groupId);
        }
        PathCache.begin();
        try {
            IPage<WaterDispenserVO> result = waterDispenserMapper.selectPageList(page, groupId, groupIds, keyword, status);
            for (WaterDispenserVO vo : result.getRecords()) {
                vo.setGroupPath(buildingGroupService.getGroupPath(vo.getGroupId()));
            }
            return result;
        } finally {
            PathCache.end();
        }
    }

    public WaterDispenserVO getById(Long id) {
        WaterDispenser entity = waterDispenserMapper.selectById(id);
        if (entity == null) return null;
        WaterDispenserVO vo = new WaterDispenserVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setStatusName(entity.getStatus() == 1 ? "正常" : "停用");
        vo.setGroupPath(buildingGroupService.getGroupPath(entity.getGroupId()));
        return vo;
    }

    @Transactional
    public void save(WaterDispenserDTO dto) {
        Long existCount = waterDispenserMapper.selectCount(
                new LambdaQueryWrapper<WaterDispenser>().eq(WaterDispenser::getDeviceNo, dto.getDeviceNo())
        );
        if (existCount > 0) {
            throw new RuntimeException("设备编号已存在");
        }
        WaterDispenser entity = new WaterDispenser();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        waterDispenserMapper.insert(entity);

        GroupTransferLog log = new GroupTransferLog();
        log.setDeviceId(entity.getId());
        log.setDeviceNo(entity.getDeviceNo());
        log.setNewGroupId(entity.getGroupId());
        log.setNewGroupPath(buildingGroupService.getGroupPath(entity.getGroupId()));
        log.setTransferReason(dto.getTransferReason() != null ? dto.getTransferReason() : "初始建档");
        transferLogMapper.insert(log);
    }

    @Transactional
    public void update(WaterDispenserDTO dto) {
        WaterDispenser old = waterDispenserMapper.selectById(dto.getId());
        if (old == null) {
            throw new RuntimeException("设备不存在");
        }
        if (!old.getDeviceNo().equals(dto.getDeviceNo())) {
            Long existCount = waterDispenserMapper.selectCount(
                    new LambdaQueryWrapper<WaterDispenser>()
                            .eq(WaterDispenser::getDeviceNo, dto.getDeviceNo())
                            .ne(WaterDispenser::getId, dto.getId())
            );
            if (existCount > 0) {
                throw new RuntimeException("设备编号已存在");
            }
        }

        boolean groupChanged = !old.getGroupId().equals(dto.getGroupId());

        WaterDispenser entity = new WaterDispenser();
        BeanUtils.copyProperties(dto, entity);
        // 待复检标记只能由抽检复检流程维护，设备档案编辑接口不得覆盖
        entity.setPendingRetest(old.getPendingRetest());
        waterDispenserMapper.updateById(entity);

        if (groupChanged) {
            GroupTransferLog log = new GroupTransferLog();
            log.setDeviceId(entity.getId());
            log.setDeviceNo(dto.getDeviceNo());
            log.setOldGroupId(old.getGroupId());
            log.setOldGroupPath(buildingGroupService.getGroupPath(old.getGroupId()));
            log.setNewGroupId(dto.getGroupId());
            log.setNewGroupPath(buildingGroupService.getGroupPath(dto.getGroupId()));
            log.setTransferReason(dto.getTransferReason() != null ? dto.getTransferReason() : "分组调整");
            transferLogMapper.insert(log);
        }
    }

    @Transactional
    public void delete(Long id) {
        waterDispenserMapper.deleteById(id);
    }

    public IPage<GroupTransferLog> transferLogPage(Integer pageNum, Integer pageSize, Long deviceId) {
        Page<GroupTransferLog> page = new Page<>(pageNum, pageSize);
        return transferLogMapper.selectPage(page,
                new LambdaQueryWrapper<GroupTransferLog>()
                        .eq(deviceId != null, GroupTransferLog::getDeviceId, deviceId)
                        .orderByDesc(GroupTransferLog::getCreateTime)
        );
    }
}
