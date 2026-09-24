package com.xgs.water.service;

import com.xgs.water.constant.WqConstants;
import com.xgs.water.entity.WqBatch;
import com.xgs.water.entity.WqIdempotentRecord;
import com.xgs.water.entity.WqStatusLog;
import com.xgs.water.exception.BusinessException;
import com.xgs.water.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 抽检模块公共能力：编号生成、幂等抢占、状态流水、设备待复检标记重算。
 */
@Service
public class WqSupportService {

    @Autowired
    private WqIdempotentRecordMapper idempotentRecordMapper;
    @Autowired
    private WqStatusLogMapper statusLogMapper;
    @Autowired
    private WqFailItemMapper failItemMapper;
    @Autowired
    private WaterDispenserMapper waterDispenserMapper;
    @Autowired
    private SearchCacheClient searchCacheClient;

    private static final Map<String, String> STATUS_NAME = Map.of(
            WqConstants.BATCH_DRAFT, "草稿",
            WqConstants.BATCH_PENDING_REVIEW, "待复核",
            WqConstants.BATCH_RETURNED, "退回",
            WqConstants.BATCH_PENDING_RETEST, "待复检",
            WqConstants.BATCH_CLOSED, "已关闭"
    );

    public static String statusName(String status) {
        return STATUS_NAME.getOrDefault(status, status);
    }

    /**
     * 批次号：WQ + yyyyMMddHHmmss + 4位ID，保证同库唯一（uk_batch_no 兜底）。
     */
    public String generateBatchNo(Long id) {
        return "WQ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", id % 10000);
    }

    /**
     * 抢占幂等键。抢占失败说明同一 requestId 已被处理（重复点击/网络重试），
     * 直接抛 409，由前端提示并刷新。
     */
    public void acquireRequestId(String requestId, String action, Long batchId) {
        if (requestId == null || requestId.isBlank()) {
            throw new BusinessException("缺少幂等标识，禁止提交");
        }
        WqIdempotentRecord record = new WqIdempotentRecord();
        record.setRequestId(requestId);
        record.setAction(action);
        record.setBatchId(batchId);
        int rows = idempotentRecordMapper.tryAcquire(record);
        if (rows == 0) {
            throw new BusinessException(409, "该操作已处理完成，请勿重复提交（页面将刷新到最新状态）");
        }
    }

    public void log(Long batchId, String fromStatus, String toStatus,
                    String action, String operator, String detail) {
        WqStatusLog log = new WqStatusLog();
        log.setBatchId(batchId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperator(operator);
        log.setDetail(detail);
        statusLogMapper.insert(log);
    }

    /**
     * 依据全库未关闭不合格项重算某台设备的“待复检”标记。
     * 规则：同一设备所有未关闭不合格项全部复检合格（OPEN 数为 0）才解除；
     * 只动 pending_retest 标记，绝不修改 status（停用设备保持停用，不自动启用）。
     */
    public void refreshDevicePendingRetest(Long deviceId) {
        long openCount = failItemMapper.selectOpenByDeviceId(deviceId).size();
        applyPendingRetest(deviceId, openCount > 0 ? 1 : 0);
    }

    /**
     * 显式设置待复检标记（提交批次时，不合格项尚未经复核开启，需直接置位）。
     */
    public void markDevicePendingRetest(Long deviceId, boolean pending) {
        applyPendingRetest(deviceId, pending ? 1 : 0);
    }

    private void applyPendingRetest(Long deviceId, int target) {
        var device = waterDispenserMapper.selectById(deviceId);
        if (device == null) {
            return;
        }
        if ((device.getPendingRetest() == null ? 0 : device.getPendingRetest()) != target) {
            // 显式只更新待复检标记，不触碰 status 字段（停用/启用状态正交，绝不覆盖）
            com.xgs.water.entity.WaterDispenser patch = new com.xgs.water.entity.WaterDispenser();
            patch.setId(deviceId);
            patch.setPendingRetest(target);
            waterDispenserMapper.updateById(patch);
            // 待复检标记是检索筛选条件之一，变更后推进检索缓存版本
            searchCacheClient.bumpVersion();
        }
    }
}
