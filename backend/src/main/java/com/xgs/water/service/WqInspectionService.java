package com.xgs.water.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xgs.water.constant.WqConstants;
import com.xgs.water.dto.*;
import com.xgs.water.entity.*;
import com.xgs.water.exception.BusinessException;
import com.xgs.water.mapper.*;
import com.xgs.water.vo.WqBatchDetailVO;
import com.xgs.water.vo.WqBatchListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 水质抽检与复检核心服务。
 *
 * 闭环要点：
 * 1. 提交时固化阈值快照 + 设备编号/型号/区域路径快照；
 * 2. 状态机：DRAFT/RETURNED -> PENDING_REVIEW -> (RETURNED | PENDING_RETEST) -> CLOSED；
 * 3. 复核人不能复核本人提交；不合格设备开启复检，复检按不合格项保留历次结果；
 * 4. 同一设备全部未关闭不合格项复检合格才解除“待复检”，停用设备不自动启用；
 * 5. 行锁 + 乐观锁 + 幂等键三重防并发重复；
 * 6. 每个写方法整体事务，任一设备/流水/照片写入失败全部回滚。
 */
@Service
public class WqInspectionService {

    @Autowired private WqBatchMapper batchMapper;
    @Autowired private WqSampleItemMapper sampleItemMapper;
    @Autowired private WqFailItemMapper failItemMapper;
    @Autowired private WqRetestMapper retestMapper;
    @Autowired private WqPhotoMapper photoMapper;
    @Autowired private WqStatusLogMapper statusLogMapper;
    @Autowired private WaterDispenserMapper waterDispenserMapper;
    @Autowired private BuildingGroupService buildingGroupService;
    @Autowired private WqThresholdService thresholdService;
    @Autowired private WqSupportService supportService;

    private static final List<String> METRIC_CODES = List.of(
            WqConstants.METRIC_RESIDUAL_CHLORINE,
            WqConstants.METRIC_TURBIDITY,
            WqConstants.METRIC_TEMPERATURE,
            WqConstants.METRIC_ODOR
    );

    // ========================= 列表与详情 =========================

    public IPage<WqBatchListVO> page(Integer pageNum, Integer pageSize,
                                     Long groupId, String status,
                                     String startDate, String endDate, String deviceNo) {
        Page<WqBatchListVO> page = new Page<>(pageNum, pageSize);
        List<Long> groupIds = null;
        if (groupId != null) {
            groupIds = buildingGroupService.getAllChildGroupIds(groupId);
        }
        IPage<WqBatchListVO> result = batchMapper.selectBatchPage(
                page, groupIds, status, startDate, endDate, deviceNo);
        for (WqBatchListVO vo : result.getRecords()) {
            vo.setStatusName(WqSupportService.statusName(vo.getStatus()));
        }
        return result;
    }

    public WqBatchDetailVO detail(Long id) {
        WqBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            return null;
        }
        WqBatchDetailVO vo = new WqBatchDetailVO();
        BeanUtils.copyProperties(batch, vo);
        vo.setStatusName(WqSupportService.statusName(batch.getStatus()));

        List<WqSampleItem> items = sampleItemMapper.selectByBatchId(id);
        List<Long> itemIds = items.stream().map(WqSampleItem::getId).collect(Collectors.toList());
        Map<Long, List<String>> samplePhotoMap = loadSamplePhotos(itemIds);

        List<WqFailItem> failItems = failItemMapper.selectByBatchId(id);
        List<Long> failIds = failItems.stream().map(WqFailItem::getId).collect(Collectors.toList());
        List<WqRetest> retests = failIds.isEmpty() ? Collections.emptyList()
                : retestMapper.selectByFailItemIds(failIds);
        Map<Long, List<WqRetest>> retestGroup = retests.stream()
                .collect(Collectors.groupingBy(WqRetest::getFailItemId));
        List<Long> retestIds = retests.stream().map(WqRetest::getId).collect(Collectors.toList());
        Map<Long, List<String>> retestPhotoMap = loadRetestPhotos(retestIds);

        // 批量取设备当前档案（当前区域/编号只用于对照展示，历史快照永不被覆盖）
        Map<Long, WaterDispenser> deviceMap = new HashMap<>();
        for (Long deviceId : items.stream().map(WqSampleItem::getDeviceId).collect(Collectors.toSet())) {
            WaterDispenser d = waterDispenserMapper.selectById(deviceId);
            if (d != null) {
                deviceMap.put(deviceId, d);
            }
        }

        List<WqBatchDetailVO.SampleItemVO> itemVOs = new ArrayList<>();
        for (WqSampleItem item : items) {
            WqBatchDetailVO.SampleItemVO ivo = new WqBatchDetailVO.SampleItemVO();
            BeanUtils.copyProperties(item, ivo);
            ivo.setItemResultName(WqConstants.RESULT_PASS.equals(item.getItemResult()) ? "合格"
                    : WqConstants.RESULT_FAIL.equals(item.getItemResult()) ? "不合格" : "未判定");
            ivo.setFailMetrics(splitCsv(item.getFailMetrics()));
            ivo.setPhotos(samplePhotoMap.getOrDefault(item.getId(), new ArrayList<>()));
            WaterDispenser current = deviceMap.get(item.getDeviceId());
            if (current != null) {
                ivo.setCurrentDeviceNo(current.getDeviceNo());
                ivo.setCurrentGroupPath(buildingGroupService.getGroupPath(current.getGroupId()));
                ivo.setDeviceStatus(current.getStatus());
                ivo.setDevicePendingRetest(current.getPendingRetest());
            } else {
                ivo.setCurrentGroupPath("（设备档案已删除）");
            }
            itemVOs.add(ivo);
        }
        vo.setItems(itemVOs);

        List<WqBatchDetailVO.FailItemVO> failVOs = new ArrayList<>();
        for (WqFailItem f : failItems) {
            WqBatchDetailVO.FailItemVO fvo = new WqBatchDetailVO.FailItemVO();
            BeanUtils.copyProperties(f, fvo);
            fvo.setStatusName(failStatusName(f.getStatus()));
            List<WqBatchDetailVO.RetestVO> retestVOs = new ArrayList<>();
            for (WqRetest r : retestGroup.getOrDefault(f.getId(), Collections.emptyList())) {
                WqBatchDetailVO.RetestVO rvo = new WqBatchDetailVO.RetestVO();
                BeanUtils.copyProperties(r, rvo);
                rvo.setRetestResultName(WqConstants.RESULT_PASS.equals(r.getRetestResult())
                        ? "复检合格" : "复检不合格");
                rvo.setPhotos(retestPhotoMap.getOrDefault(r.getId(), new ArrayList<>()));
                retestVOs.add(rvo);
            }
            fvo.setRetests(retestVOs);
            failVOs.add(fvo);
        }
        vo.setFailItems(failVOs);

        List<WqBatchDetailVO.StatusLogVO> logVOs = new ArrayList<>();
        for (WqStatusLog log : batchStatusLogs(id)) {
            WqBatchDetailVO.StatusLogVO lvo = new WqBatchDetailVO.StatusLogVO();
            BeanUtils.copyProperties(log, lvo);
            logVOs.add(lvo);
        }
        vo.setStatusLogs(logVOs);
        return vo;
    }

    private Map<Long, List<String>> loadSamplePhotos(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WqPhoto> photos = photoMapper.selectByRefIds(itemIds, List.of(-1L));
        Map<Long, List<String>> map = new HashMap<>();
        for (WqPhoto p : photos) {
            map.computeIfAbsent(p.getSampleItemId(), k -> new ArrayList<>()).add(p.getPhotoUrl());
        }
        return map;
    }

    private Map<Long, List<String>> loadRetestPhotos(List<Long> retestIds) {
        if (retestIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WqPhoto> photos = photoMapper.selectByRefIds(List.of(-1L), retestIds);
        Map<Long, List<String>> map = new HashMap<>();
        for (WqPhoto p : photos) {
            map.computeIfAbsent(p.getRetestId(), k -> new ArrayList<>()).add(p.getPhotoUrl());
        }
        return map;
    }

    private List<WqStatusLog> batchStatusLogs(Long batchId) {
        return statusLogMapper.selectByBatchId(batchId);
    }

    private static String failStatusName(String status) {
        return switch (status) {
            case WqConstants.FAIL_OPEN -> "待复检";
            case WqConstants.FAIL_CLOSED_PASS -> "复检合格关闭";
            case WqConstants.FAIL_CLOSED_VOID -> "退回作废";
            default -> status;
        };
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ========================= 草稿：建批/保存/删除 =========================

    /**
     * 新建草稿或保存草稿。仅 DRAFT/RETURNED 可编辑，可增删设备、修改采样结果。
     * 草稿阶段不固化阈值、不判定、不生成不合格项、不动设备状态。
     */
    @Transactional
    public Long saveDraft(WqBatchSaveDTO dto) {
        WqBatch batch;
        boolean isNew = dto.getId() == null;
        String operator = defaultOperator(dto.getOperator());
        if (isNew) {
            if (dto.getScopeGroupId() == null) {
                throw new BusinessException("请选择抽检区域（园区/楼栋/楼层）");
            }
            if (dto.getItems() == null || dto.getItems().isEmpty()) {
                throw new BusinessException("请至少选择一台饮水机");
            }
            batch = new WqBatch();
            batch.setScopeGroupId(dto.getScopeGroupId());
            batch.setScopeGroupPath(buildingGroupService.getGroupPath(dto.getScopeGroupId()));
            batch.setStatus(WqConstants.BATCH_DRAFT);
            batch.setRemark(dto.getRemark());
            // batch_no 非空：先放临时唯一号，拿到自增 ID 后更新为正式批次号
            batch.setBatchNo("WQ-TMP-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            batchMapper.insert(batch);
            batch.setBatchNo(supportService.generateBatchNo(batch.getId()));
            batchMapper.updateById(batch);
            supportService.log(batch.getId(), null, WqConstants.BATCH_DRAFT,
                    WqConstants.ACTION_CREATE, operator, "创建抽检草稿");
        } else {
            batch = batchMapper.selectById(dto.getId());
            if (batch == null) {
                throw new BusinessException("批次不存在");
            }
            if (!WqConstants.BATCH_DRAFT.equals(batch.getStatus())
                    && !WqConstants.BATCH_RETURNED.equals(batch.getStatus())) {
                throw new BusinessException(409, "批次已" + WqSupportService.statusName(batch.getStatus())
                        + "，原始采样数据不可直接编辑");
            }
            if (dto.getScopeGroupId() != null) {
                batch.setScopeGroupId(dto.getScopeGroupId());
                batch.setScopeGroupPath(buildingGroupService.getGroupPath(dto.getScopeGroupId()));
            }
            batch.setRemark(dto.getRemark());
            batchMapper.updateById(batch);
        }

        List<WqSampleItemDTO> itemDTOs = dto.getItems() == null ? Collections.emptyList() : dto.getItems();
        syncItems(batch.getId(), itemDTOs, null);
        return batch.getId();
    }

    /**
     * 同步批次内采样设备与照片关系。
     * 保存时即以设备档案当前值写入快照；submit() 提交时刻会再统一刷新一次，保证以提交时刻为准。
     *
     * @param defaultSampler 提交模式下作为采样人默认值；草稿模式为 null
     */
    private void syncItems(Long batchId, List<WqSampleItemDTO> itemDTOs, String defaultSampler) {
        List<WqSampleItem> exists = sampleItemMapper.selectByBatchId(batchId);
        Map<Long, WqSampleItem> existsByDevice = exists.stream()
                .collect(Collectors.toMap(WqSampleItem::getDeviceId, i -> i, (a, b) -> a));
        Set<Long> keepDeviceIds = new HashSet<>();

        int order = 0;
        for (WqSampleItemDTO dto : itemDTOs) {
            if (dto.getDeviceId() == null) {
                throw new BusinessException("存在未选择设备的采样行");
            }
            WaterDispenser device = waterDispenserMapper.selectById(dto.getDeviceId());
            if (device == null) {
                throw new BusinessException("设备不存在或已删除：ID=" + dto.getDeviceId());
            }
            keepDeviceIds.add(device.getId());

            WqSampleItem item = existsByDevice.get(device.getId());
            if (item == null) {
                item = new WqSampleItem();
                item.setBatchId(batchId);
                item.setDeviceId(device.getId());
            }
            // 任何保存都以设备档案当前值写入快照；提交时会再次刷新，保证“提交当时”语义
            item.setDeviceNoSnapshot(device.getDeviceNo());
            item.setModelSnapshot(device.getModel());
            item.setGroupIdSnapshot(device.getGroupId());
            item.setGroupPathSnapshot(buildingGroupService.getGroupPath(device.getGroupId()));
            item.setSampler(dto.getSampler() != null && !dto.getSampler().isBlank()
                    ? dto.getSampler() : defaultSampler);
            item.setSampleTime(dto.getSampleTime());
            item.setResidualChlorine(dto.getResidualChlorine());
            item.setTurbidity(dto.getTurbidity());
            item.setTemperature(dto.getTemperature());
            item.setOdor(dto.getOdor());
            item.setSortOrder(order++);
            if (item.getId() == null) {
                sampleItemMapper.insert(item);
            } else {
                sampleItemMapper.updateById(item);
            }
            syncPhotos(item, dto.getPhotos());
        }

        // 删除被移除的设备行；关联 wq_retest 的行（不可能在草稿阶段）保留，这里草稿/退回批次只会有作废不合格项
        for (WqSampleItem old : exists) {
            if (!keepDeviceIds.contains(old.getDeviceId())) {
                sampleItemMapper.deleteById(old.getId());
                photoMapper.delete(new LambdaQueryWrapper<WqPhoto>()
                        .eq(WqPhoto::getRefType, WqConstants.PHOTO_SAMPLE)
                        .eq(WqPhoto::getSampleItemId, old.getId()));
            }
        }
    }

    /**
     * 照片关系以本次提交的 URL 集合为准做差量同步；重复 URL 由唯一键兜底不重复。
     */
    private void syncPhotos(WqSampleItem item, List<String> urls) {
        Set<String> want = urls == null ? Set.of()
                : urls.stream().filter(u -> u != null && !u.isBlank()).collect(Collectors.toSet());
        List<WqPhoto> oldPhotos = photoMapper.selectByRefIds(List.of(item.getId()), List.of(-1L));
        for (WqPhoto p : oldPhotos) {
            if (!want.contains(p.getPhotoUrl())) {
                photoMapper.deleteById(p.getId());
            }
        }
        for (String url : want) {
            WqPhoto photo = new WqPhoto();
            photo.setRefType(WqConstants.PHOTO_SAMPLE);
            photo.setSampleItemId(item.getId());
            photo.setRetestId(0L);
            photo.setDeviceId(item.getDeviceId());
            photo.setPhotoUrl(url);
            photoMapper.insertIgnore(photo);
        }
    }

    @Transactional
    public void deleteDraft(Long id) {
        WqBatch batch = batchMapper.selectById(id);
        if (batch == null) {
            throw new BusinessException("批次不存在");
        }
        if (!WqConstants.BATCH_DRAFT.equals(batch.getStatus())) {
            throw new BusinessException(409, "只有草稿批次可以删除");
        }
        List<WqSampleItem> items = sampleItemMapper.selectByBatchId(id);
        for (WqSampleItem item : items) {
            photoMapper.delete(new LambdaQueryWrapper<WqPhoto>()
                    .eq(WqPhoto::getRefType, WqConstants.PHOTO_SAMPLE)
                    .eq(WqPhoto::getSampleItemId, item.getId()));
        }
        sampleItemMapper.delete(new LambdaQueryWrapper<WqSampleItem>().eq(WqSampleItem::getBatchId, id));
        batchMapper.deleteById(id);
    }

    // ========================= 提交：阈值固化 + 自动判定 + 待复检标记 =========================

    /**
     * 提交批次。一个事务内原子完成：
     * 幂等抢占 -> 锁批次 -> 校验可提交状态 -> 保存设备/数据/照片 -> 重刷提交时刻快照
     * -> 按生效阈值固化并逐设备判定 -> 设备待复检标记 -> 状态/流水落库。
     * 任一步骤异常整体回滚，绝不出现“批次已流转但部分设备/照片未落库”。
     */
    @Transactional
    public void submit(WqBatchSubmitDTO dto) {
        // 先抢占幂等键：与业务在同一事务，回滚则键释放；提交成功后键固化，重试返回 409
        supportService.acquireRequestId(dto.getRequestId(), WqConstants.ACTION_SUBMIT, dto.getId());

        WqBatch batch = batchMapper.selectByIdForUpdate(dto.getId());
        if (batch == null) {
            throw new BusinessException("批次不存在");
        }
        if (!WqConstants.BATCH_DRAFT.equals(batch.getStatus())
                && !WqConstants.BATCH_RETURNED.equals(batch.getStatus())) {
            throw new BusinessException(409, "批次当前为「" + WqSupportService.statusName(batch.getStatus())
                    + "」，不能重复提交");
        }

        // 退回后再次提交：释放旧的作废不合格项（无复检引用，可物理删除，保持链路干净）
        List<WqFailItem> oldFails = failItemMapper.selectByBatchId(batch.getId());
        for (WqFailItem old : oldFails) {
            if (WqConstants.FAIL_CLOSED_VOID.equals(old.getStatus())) {
                failItemMapper.deleteById(old.getId());
            } else if (WqConstants.FAIL_OPEN.equals(old.getStatus())) {
                throw new BusinessException(409, "批次存在未关闭的不合格项，不能重新提交");
            }
        }

        // 保存本次提交的设备与采样数据（提交模式做完整校验）
        List<WqSampleItemDTO> itemDTOs = dto.getItems() == null ? Collections.emptyList() : dto.getItems();
        if (itemDTOs.isEmpty()) {
            throw new BusinessException("请至少选择一台饮水机再提交");
        }
        syncItems(batch.getId(), itemDTOs, dto.getSubmitter());

        // 阈值在提交时刻固化：之后后台怎么改，都不影响本批次判定与复检
        String snapshotJson = thresholdService.buildSnapshotJson();
        Map<String, Map<String, Object>> snapshot = thresholdService.parseSnapshot(snapshotJson);

        List<WqSampleItem> items = sampleItemMapper.selectByBatchId(batch.getId());
        Set<Long> failDeviceIds = new HashSet<>();
        for (WqSampleItem item : items) {
            // 提交当时再次刷新设备快照，防止 syncItems 之后设备刚好调组/改名
            WaterDispenser device = waterDispenserMapper.selectById(item.getDeviceId());
            if (device == null) {
                throw new BusinessException("设备不存在或已删除：ID=" + item.getDeviceId());
            }
            item.setDeviceNoSnapshot(device.getDeviceNo());
            item.setModelSnapshot(device.getModel());
            item.setGroupIdSnapshot(device.getGroupId());
            item.setGroupPathSnapshot(buildingGroupService.getGroupPath(device.getGroupId()));

            List<String> failMetrics = judgeItem(item, snapshot);
            boolean pass = failMetrics.isEmpty();
            item.setItemResult(pass ? WqConstants.RESULT_PASS : WqConstants.RESULT_FAIL);
            item.setFailMetrics(pass ? null : String.join(",", failMetrics));
            item.setReviewResult(null);
            item.setReviewComment(null);
            sampleItemMapper.updateById(item);
            if (!pass) {
                failDeviceIds.add(item.getDeviceId());
            }
        }

        String fromStatus = batch.getStatus();
        batch.setStatus(WqConstants.BATCH_PENDING_REVIEW);
        batch.setSubmitter(dto.getSubmitter());
        batch.setSubmitTime(LocalDateTime.now());
        batch.setReviewer(null);
        batch.setReviewTime(null);
        batch.setReturnReason(null);
        batch.setThresholdSnapshot(snapshotJson);
        if (dto.getRemark() != null) {
            batch.setRemark(dto.getRemark());
        }
        // version 由乐观锁插件自增；FOR UPDATE 已保证并发提交串行
        batchMapper.updateById(batch);

        // 只要有一台不合格，提交后即标记设备“待复检”（此时不合格项尚未经复核开启，
        // 不能按 open 不合格项计数，需要显式置位）；只动 pending_retest，绝不覆盖停用状态
        for (Long deviceId : failDeviceIds) {
            supportService.markDevicePendingRetest(deviceId, true);
        }
        // 已不在本批次不合格集合、但此前因其它批次问题遗留的标记，仍按全库 open 项规则重算
        for (WqSampleItem item : items) {
            if (!failDeviceIds.contains(item.getDeviceId())) {
                supportService.refreshDevicePendingRetest(item.getDeviceId());
            }
        }

        supportService.log(batch.getId(), fromStatus, WqConstants.BATCH_PENDING_REVIEW,
                WqConstants.ACTION_SUBMIT, dto.getSubmitter(),
                "提交抽检，共" + items.size() + "台，不合格" + failDeviceIds.size() + "台，阈值已固化");
    }

    /**
     * 按固化阈值逐指标判定，返回不合格指标编码列表。
     * 提交时四项指标必须完整填写，缺项视为数据不完整直接拒绝（而不是悄悄判合格）。
     */
    private List<String> judgeItem(WqSampleItem item, Map<String, Map<String, Object>> snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(WqConstants.METRIC_RESIDUAL_CHLORINE,
                item.getResidualChlorine() == null ? null : item.getResidualChlorine().toPlainString());
        values.put(WqConstants.METRIC_TURBIDITY,
                item.getTurbidity() == null ? null : item.getTurbidity().toPlainString());
        values.put(WqConstants.METRIC_TEMPERATURE,
                item.getTemperature() == null ? null : item.getTemperature().toPlainString());
        values.put(WqConstants.METRIC_ODOR, item.getOdor());

        List<String> failMetrics = new ArrayList<>();
        for (String code : METRIC_CODES) {
            Map<String, Object> metricSnapshot = snapshot.get(code);
            if (metricSnapshot == null) {
                throw new BusinessException("指标「" + thresholdService.metricName(snapshot, code)
                        + "」未配置生效阈值，无法提交，请先在阈值管理中维护");
            }
            String value = values.get(code);
            if (value == null || value.isBlank()) {
                throw new BusinessException("设备「" + item.getDeviceNoSnapshot()
                        + "」的「" + thresholdService.metricName(snapshot, code) + "」采样数据不完整");
            }
            if (item.getSampleTime() == null) {
                throw new BusinessException("设备「" + item.getDeviceNoSnapshot() + "」缺少采样时间");
            }
            if (!thresholdService.judgeWithSnapshot(metricSnapshot, value)) {
                failMetrics.add(code);
            }
        }
        return failMetrics;
    }

    // ========================= 复核：整批退回 / 确认合格 + 开启复检 =========================

    /**
     * 复核处理。行锁 + 幂等键保证两名复核人同时操作只有一人成功。
     */
    @Transactional
    public void review(WqReviewDTO dto) {
        supportService.acquireRequestId(dto.getRequestId(),
                "REVIEW_" + dto.getAction(), dto.getBatchId());

        WqBatch batch = batchMapper.selectByIdForUpdate(dto.getBatchId());
        if (batch == null) {
            throw new BusinessException("批次不存在");
        }
        if (!WqConstants.BATCH_PENDING_REVIEW.equals(batch.getStatus())) {
            throw new BusinessException(409, "批次当前为「" + WqSupportService.statusName(batch.getStatus())
                    + "」，已被他人处理，请刷新查看最新状态");
        }
        if (batch.getSubmitter() != null && batch.getSubmitter().equals(dto.getReviewer())) {
            throw new BusinessException("复核人不能复核自己提交的批次，请更换账号");
        }

        if ("RETURN".equals(dto.getAction())) {
            doReturn(batch, dto);
        } else if ("RETEST".equals(dto.getAction())) {
            doReviewConfirm(batch, dto);
        } else {
            throw new BusinessException("未知复核动作：" + dto.getAction());
        }
    }

    /** 整批退回：批次回到退回态，采样数据可由提交人修改后重新提交；不合格项作废并解除设备待复检 */
    private void doReturn(WqBatch batch, WqReviewDTO dto) {
        if (dto.getReason() == null || dto.getReason().isBlank()) {
            throw new BusinessException("整批退回必须填写退回原因");
        }
        String fromStatus = batch.getStatus();
        batch.setStatus(WqConstants.BATCH_RETURNED);
        batch.setReviewer(dto.getReviewer());
        batch.setReviewTime(LocalDateTime.now());
        batch.setReturnReason(dto.getReason());
        batchMapper.updateById(batch);

        List<WqSampleItem> items = sampleItemMapper.selectByBatchId(batch.getId());
        Set<Long> deviceIds = new HashSet<>();
        for (WqSampleItem item : items) {
            item.setReviewResult(null);
            item.setReviewComment(null);
            sampleItemMapper.updateById(item);
            deviceIds.add(item.getDeviceId());
        }
        // 尚未开启不合格项（复核阶段），仅按设备重算：别的批次仍有 open 项则保持待复检
        for (Long deviceId : deviceIds) {
            supportService.refreshDevicePendingRetest(deviceId);
        }

        supportService.log(batch.getId(), fromStatus, WqConstants.BATCH_RETURNED,
                WqConstants.ACTION_RETURN, dto.getReviewer(), "整批退回：" + dto.getReason());
    }

    /**
     * 确认复核：合格项确认合格；不合格设备逐指标开启复检（生成 OPEN 不合格项）。
     * 存在不合格项 -> 批次进入待复检；全部合格 -> 批次关闭。
     */
    private void doReviewConfirm(WqBatch batch, WqReviewDTO dto) {
        Map<Long, WqReviewDTO.ItemReview> reviewMap = dto.getItemReviews() == null
                ? Collections.emptyMap()
                : dto.getItemReviews().stream()
                .collect(Collectors.toMap(WqReviewDTO.ItemReview::getSampleItemId, r -> r, (a, b) -> a));

        List<WqSampleItem> items = sampleItemMapper.selectByBatchId(batch.getId());
        Set<Long> failDeviceIds = new HashSet<>();
        int openCount = 0;
        LocalDateTime now = LocalDateTime.now();
        Map<String, Map<String, Object>> snapshot = thresholdService.parseSnapshot(batch.getThresholdSnapshot());

        for (WqSampleItem item : items) {
            WqReviewDTO.ItemReview review = reviewMap.get(item.getId());
            boolean judgedFail = WqConstants.RESULT_FAIL.equals(item.getItemResult());
            String comment = review != null ? review.getComment() : null;

            if (judgedFail) {
                // 不合格设备：为每个不合格指标开启复检项
                item.setReviewResult(WqConstants.RESULT_FAIL);
                item.setReviewComment(comment);
                sampleItemMapper.updateById(item);
                failDeviceIds.add(item.getDeviceId());

                for (String code : splitCsv(item.getFailMetrics())) {
                    WqFailItem fail = new WqFailItem();
                    fail.setBatchId(batch.getId());
                    fail.setSampleItemId(item.getId());
                    fail.setDeviceId(item.getDeviceId());
                    fail.setDeviceNoSnapshot(item.getDeviceNoSnapshot());
                    fail.setMetricCode(code);
                    fail.setMetricName(thresholdService.metricName(snapshot, code));
                    fail.setMetricValue(originalMetricValue(item, code));
                    Map<String, Object> metricSnapshot = snapshot.get(code);
                    fail.setThresholdSnapshot(thresholdService.writeMetricSnapshot(
                            metricSnapshot != null ? metricSnapshot : Map.of()));
                    fail.setStatus(WqConstants.FAIL_OPEN);
                    fail.setOpenReviewer(dto.getReviewer());
                    fail.setOpenTime(now);
                    failItemMapper.insert(fail);
                    openCount++;
                }
            } else {
                item.setReviewResult(WqConstants.RESULT_PASS);
                item.setReviewComment(comment);
                sampleItemMapper.updateById(item);
            }
        }

        String fromStatus = batch.getStatus();
        String toStatus;
        String action;
        String detail;
        if (openCount > 0) {
            toStatus = WqConstants.BATCH_PENDING_RETEST;
            action = WqConstants.ACTION_REVIEW_RETEST;
            detail = "复核完成：合格项已确认，开启复检不合格项" + openCount + "个";
            for (Long deviceId : failDeviceIds) {
                supportService.refreshDevicePendingRetest(deviceId);
            }
        } else {
            toStatus = WqConstants.BATCH_CLOSED;
            action = WqConstants.ACTION_CLOSE;
            batch.setCloseOperator(dto.getReviewer());
            batch.setCloseTime(now);
            detail = "复核完成：全部设备合格，批次关闭";
        }
        batch.setStatus(toStatus);
        batch.setReviewer(dto.getReviewer());
        batch.setReviewTime(now);
        batch.setReturnReason(null);
        batchMapper.updateById(batch);

        supportService.log(batch.getId(), fromStatus, toStatus, action, dto.getReviewer(), detail);
    }

    private String originalMetricValue(WqSampleItem item, String code) {
        return switch (code) {
            case WqConstants.METRIC_RESIDUAL_CHLORINE ->
                    item.getResidualChlorine() == null ? null : item.getResidualChlorine().toPlainString();
            case WqConstants.METRIC_TURBIDITY ->
                    item.getTurbidity() == null ? null : item.getTurbidity().toPlainString();
            case WqConstants.METRIC_TEMPERATURE ->
                    item.getTemperature() == null ? null : item.getTemperature().toPlainString();
            case WqConstants.METRIC_ODOR -> item.getOdor();
            default -> null;
        };
    }

    // ========================= 复检：关联原不合格项，保留历次结果 =========================

    /**
     * 提交一次复检结果。
     * 幂等键 uk_request_id 防止网络重试生成重复复检记录；
     * 不合格项行锁保证并发只有一个请求能写结果；
     * PASS 关闭该项并按“同一设备全部 open 项清零”规则决定是否解除待复检；
     * FAIL 保留 OPEN，历次记录都保留，批次维持待复检。
     * 设备停用时只关闭抽检问题，不自动启用设备（只更新 pending_retest）。
     */
    @Transactional
    public void submitRetest(WqRetestSubmitDTO dto) {
        // wq_retest.uk_request_id 为最终防线；先显式抢占给出友好提示
        supportService.acquireRequestId(dto.getRequestId(), WqConstants.ACTION_RETEST_RESULT, null);

        WqFailItem fail = failItemMapper.selectByIdForUpdate(dto.getFailItemId());
        if (fail == null) {
            throw new BusinessException("不合格项不存在");
        }
        if (!WqConstants.FAIL_OPEN.equals(fail.getStatus())) {
            throw new BusinessException(409, "该不合格项已关闭（" + failStatusName(fail.getStatus())
                    + "），数据已被他人处理，请刷新查看最新状态");
        }
        WqBatch batch = batchMapper.selectByIdForUpdate(fail.getBatchId());
        if (batch == null) {
            throw new BusinessException("来源批次不存在");
        }
        if (!WqConstants.BATCH_PENDING_RETEST.equals(batch.getStatus())) {
            throw new BusinessException(409, "批次当前为「" + WqSupportService.statusName(batch.getStatus())
                    + "」，不能提交复检，请刷新");
        }

        Map<String, Object> metricSnapshot = thresholdService.readMetricSnapshot(fail.getThresholdSnapshot());
        boolean isOdor = WqConstants.METRIC_ODOR.equals(fail.getMetricCode());
        String value;
        String odor = null;
        if (isOdor) {
            odor = dto.getOdor();
            value = dto.getOdor();
        } else {
            if (dto.getMetricValue() == null) {
                throw new BusinessException("请填写复检数值");
            }
            value = dto.getMetricValue().toPlainString();
        }
        if (dto.getRetestTime() == null) {
            throw new BusinessException("请选择复检采样时间");
        }
        boolean pass = thresholdService.judgeWithSnapshot(metricSnapshot, value);

        WqRetest retest = new WqRetest();
        retest.setFailItemId(fail.getId());
        retest.setBatchId(batch.getId());
        retest.setDeviceId(fail.getDeviceId());
        retest.setDeviceNoSnapshot(fail.getDeviceNoSnapshot());
        retest.setMetricCode(fail.getMetricCode());
        retest.setMetricName(fail.getMetricName());
        retest.setMetricValue(isOdor ? null : value);
        retest.setOdor(odor);
        retest.setRetestResult(pass ? WqConstants.RESULT_PASS : WqConstants.RESULT_FAIL);
        retest.setThresholdSnapshot(fail.getThresholdSnapshot());
        retest.setRetestOperator(dto.getRetestOperator());
        retest.setRetestTime(dto.getRetestTime());
        retest.setComment(dto.getComment());
        retest.setRequestId(dto.getRequestId());
        retestMapper.insert(retest);

        // 复检照片关系落库，唯一键保证重试不产生重复引用
        if (dto.getPhotos() != null) {
            for (String url : dto.getPhotos()) {
                if (url == null || url.isBlank()) {
                    continue;
                }
                WqPhoto photo = new WqPhoto();
                photo.setRefType(WqConstants.PHOTO_RETEST);
                photo.setSampleItemId(0L);
                photo.setRetestId(retest.getId());
                photo.setDeviceId(fail.getDeviceId());
                photo.setPhotoUrl(url);
                photoMapper.insertIgnore(photo);
            }
        }

        String detail;
        if (pass) {
            fail.setStatus(WqConstants.FAIL_CLOSED_PASS);
            fail.setCloseOperator(dto.getRetestOperator());
            fail.setCloseTime(LocalDateTime.now());
            detail = "设备「" + fail.getDeviceNoSnapshot() + "」指标「" + fail.getMetricName()
                    + "」复检合格，不合格项关闭";
        } else {
            detail = "设备「" + fail.getDeviceNoSnapshot() + "」指标「" + fail.getMetricName()
                    + "」复检仍不合格（值=" + value + "），保持待复检";
        }
        fail.setLatestRetestId(retest.getId());
        failItemMapper.updateById(fail);

        // 解除规则：同一设备全部未关闭不合格项均复检合格后才解除“待复检”；停用设备不自动启用
        supportService.refreshDevicePendingRetest(fail.getDeviceId());

        // 若批次内所有不合格项均已关闭，则批次关闭
        long remainOpen = failItemMapper.selectByBatchId(batch.getId()).stream()
                .filter(f -> WqConstants.FAIL_OPEN.equals(f.getStatus()))
                .count();
        String fromStatus = batch.getStatus();
        if (remainOpen == 0) {
            batch.setStatus(WqConstants.BATCH_CLOSED);
            batch.setCloseOperator(dto.getRetestOperator());
            batch.setCloseTime(LocalDateTime.now());
            batchMapper.updateById(batch);
            supportService.log(batch.getId(), fromStatus, WqConstants.BATCH_CLOSED,
                    WqConstants.ACTION_CLOSE, dto.getRetestOperator(),
                    "全部不合格项复检合格，批次关闭");
        }

        supportService.log(batch.getId(), fromStatus, batch.getStatus(),
                WqConstants.ACTION_RETEST_RESULT, dto.getRetestOperator(), detail);
    }

    // ========================= 其它 =========================

    private String defaultOperator(String operator) {
        return operator == null || operator.isBlank() ? "admin" : operator;
    }
}
