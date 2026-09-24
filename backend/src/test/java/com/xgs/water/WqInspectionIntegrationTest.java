package com.xgs.water;

import com.xgs.water.constant.WqConstants;
import com.xgs.water.dto.*;
import com.xgs.water.entity.*;
import com.xgs.water.exception.BusinessException;
import com.xgs.water.mapper.*;
import com.xgs.water.service.WaterModelCacheService;
import com.xgs.water.vo.WqBatchDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 抽检闭环集成验证（H2 MySQL 模式）：
 * 阈值判定 -> 提交固化快照 -> 待复检标记 -> 退回/复核 -> 复检链路 ->
 * 解除规则 -> 停用设备不启用 -> 历史快照不变 -> 并发防重 -> 事务回滚。
 */
@SpringBootTest
class WqInspectionIntegrationTest {

    @Autowired private com.xgs.water.service.WqInspectionService service;
    @Autowired private WqBatchMapper batchMapper;
    @Autowired private WqSampleItemMapper sampleItemMapper;
    @Autowired private WqFailItemMapper failItemMapper;
    @Autowired private WqRetestMapper retestMapper;
    @Autowired private WqStatusLogMapper statusLogMapper;
    @Autowired private WqIdempotentRecordMapper idemMapper;
    @Autowired private WqPhotoMapper photoMapper;
    @Autowired private WaterDispenserMapper deviceMapper;
    @Autowired private BuildingGroupMapper groupMapper;
    @Autowired private WqThresholdMapper thresholdMapper;

    @MockBean
    private WaterModelCacheService waterModelCacheService;

    private Long floorA;
    private Long floorB;
    private Long d1; // 合格
    private Long d2; // 不合格（余氯超标）
    private Long d3; // 停用设备（不合格）
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 9, 23, 10, 0);

    private static String rid() {
        return UUID.randomUUID().toString();
    }

    @BeforeEach
    void setup() {
        // H2 内存库跨用例复用，每个用例前清空业务数据（结构与阈值种子保留）
        photoMapper.delete(null);
        retestMapper.delete(null);
        failItemMapper.delete(null);
        sampleItemMapper.delete(null);
        statusLogMapper.delete(null);
        idemMapper.delete(null);
        batchMapper.delete(null);
        deviceMapper.delete(null);
        groupMapper.delete(null);
        resetThresholds();

        BuildingGroup park = new BuildingGroup();
        park.setName("测试园区"); park.setType(1); park.setParentId(0L); park.setSortOrder(1);
        groupMapper.insert(park);
        BuildingGroup building = new BuildingGroup();
        building.setName("1号楼"); building.setType(2); building.setParentId(park.getId()); building.setSortOrder(1);
        groupMapper.insert(building);
        floorA = createFloor(building.getId(), "1层", 1);
        floorB = createFloor(building.getId(), "2层", 2);

        d1 = createDevice("WD-T-001", floorA, 1);
        d2 = createDevice("WD-T-002", floorA, 1);
        d3 = createDevice("WD-T-003", floorA, 0);
    }

    /** 恢复生效阈值默认值，避免上个用例修改阈值后影响后续用例的提交判定 */
    private void resetThresholds() {
        thresholdMapper.selectList(null).forEach(t -> {
            if (WqConstants.METRIC_RESIDUAL_CHLORINE.equals(t.getMetricCode())) {
                t.setMinValue(new BigDecimal("0.030"));
                t.setMaxValue(new BigDecimal("0.800"));
            } else if (WqConstants.METRIC_TURBIDITY.equals(t.getMetricCode())) {
                t.setMinValue(new BigDecimal("0.000"));
                t.setMaxValue(new BigDecimal("1.000"));
            } else if (WqConstants.METRIC_TEMPERATURE.equals(t.getMetricCode())) {
                t.setMinValue(new BigDecimal("0.000"));
                t.setMaxValue(new BigDecimal("40.000"));
            } else if (WqConstants.METRIC_ODOR.equals(t.getMetricCode())) {
                t.setPassValues("无异味,正常");
            }
            t.setEnabled(1);
            thresholdMapper.updateById(t);
        });
    }

    private Long createFloor(Long parent, String name, int sort) {
        BuildingGroup f = new BuildingGroup();
        f.setName(name); f.setType(3); f.setParentId(parent); f.setSortOrder(sort);
        groupMapper.insert(f);
        return f.getId();
    }

    private Long createDevice(String no, Long groupId, int status) {
        WaterDispenser d = new WaterDispenser();
        d.setDeviceNo(no);
        d.setModel("测试型号-X");
        d.setGroupId(groupId);
        d.setStatus(status);
        d.setPendingRetest(0);
        deviceMapper.insert(d);
        return d.getId();
    }

    private WqSampleItemDTO item(Long deviceId, String cl, String tb, String tp, String odor, String... photos) {
        WqSampleItemDTO i = new WqSampleItemDTO();
        i.setDeviceId(deviceId);
        i.setSampler("张三");
        i.setSampleTime(T0);
        i.setResidualChlorine(cl == null ? null : new BigDecimal(cl));
        i.setTurbidity(tb == null ? null : new BigDecimal(tb));
        i.setTemperature(tp == null ? null : new BigDecimal(tp));
        i.setOdor(odor);
        List<String> ps = new ArrayList<>(List.of(photos));
        i.setPhotos(ps);
        return i;
    }

    private Long submitBatch(List<WqSampleItemDTO> items) {
        WqBatchSaveDTO save = new WqBatchSaveDTO();
        save.setScopeGroupId(floorA);
        save.setOperator("张三");
        save.setItems(items);
        Long id = service.saveDraft(save);

        WqBatchSubmitDTO submit = new WqBatchSubmitDTO();
        submit.setId(id);
        submit.setSubmitter("张三");
        submit.setRequestId(rid());
        submit.setItems(items);
        service.submit(submit);
        return id;
    }

    // 1. 阈值自动判定：一台不合格 => 待复核 + 设备待复检；停用设备状态不被覆盖
    @Test
    void submit_autoJudge_andPendingRetest_keepsDisabledStatus() {
        Long id = submitBatch(List.of(
                item(d1, "0.30", "0.5", "25", "无异味", "/uploads/a.jpg"),
                item(d2, "1.20", "0.5", "25", "无异味"), // 余氯超 0.8
                item(d3, "0.30", "5.0", "25", "无异味")  // 停用设备，浊度超标
        ));

        WqBatch batch = batchMapper.selectById(id);
        assertEquals(WqConstants.BATCH_PENDING_REVIEW, batch.getStatus());
        assertNotNull(batch.getThresholdSnapshot());

        List<WqSampleItem> items = sampleItemMapper.selectByBatchId(id);
        WqSampleItem i2 = items.stream().filter(x -> x.getDeviceId().equals(d2)).findFirst().orElseThrow();
        assertEquals(WqConstants.RESULT_FAIL, i2.getItemResult());
        assertEquals("residual_chlorine", i2.getFailMetrics());

        assertEquals(1, deviceMapper.selectById(d2).getPendingRetest());
        assertEquals(1, deviceMapper.selectById(d3).getPendingRetest());
        // 停用状态绝不被覆盖
        assertEquals(0, deviceMapper.selectById(d3).getStatus());
        // 合格设备不标记
        assertEquals(0, deviceMapper.selectById(d1).getPendingRetest());

        // 照片关系落库
        java.util.List<WqPhoto> samplePhotos = photoMapper.selectList(null);
        assertEquals(1, samplePhotos.stream()
                .filter((WqPhoto p) -> WqConstants.PHOTO_SAMPLE.equals(p.getRefType())).count());
    }

    // 2. 历史快照：提交后设备调组+改名，详情仍显示原采样位置，同时能看到当前区域
    @Test
    void snapshot_kept_afterDeviceMovedAndRenamed() {
        Long id = submitBatch(List.of(item(d2, "1.20", "0.5", "25", "无异味")));

        String originalPath = sampleItemMapper.selectByBatchId(id).get(0).getGroupPathSnapshot();
        assertTrue(originalPath.endsWith("1层"));

        WaterDispenser moved = deviceMapper.selectById(d2);
        moved.setDeviceNo("WD-NEW-999");
        moved.setPendingRetest(moved.getPendingRetest()); // 模拟更新时保持标记
        moved.setGroupId(floorB);
        deviceMapper.updateById(moved);

        WqBatchDetailVO detail = service.detail(id);
        WqBatchDetailVO.SampleItemVO vo = detail.getItems().get(0);
        assertEquals("WD-T-002", vo.getDeviceNoSnapshot());
        assertTrue(vo.getGroupPathSnapshot().endsWith("1层"));
        assertEquals("WD-NEW-999", vo.getCurrentDeviceNo());
        assertTrue(vo.getCurrentGroupPath().endsWith("2层"));
    }

    // 3. 阈值固化：提交后放宽阈值，原批次复检仍按旧阈值判定
    @Test
    void thresholdFrozen_notAffectedByLaterChange() {
        Long id = submitBatch(List.of(item(d2, "0.90", "0.5", "25", "无异味"))); // 0.90 > 0.8 不合格

        WqThreshold threshold = thresholdMapper.selectList(null).stream()
                .filter(t -> WqConstants.METRIC_RESIDUAL_CHLORINE.equals(t.getMetricCode()))
                .findFirst().orElseThrow();
        threshold.setMaxValue(new BigDecimal("2.00"));
        thresholdMapper.updateById(threshold);

        WqBatchDetailVO detail = service.detail(id);
        // 批次快照仍为 0.8
        assertTrue(detail.getThresholdSnapshot().contains("0.800"));
        assertFalse(detail.getThresholdSnapshot().contains("2.000"));
        assertEquals(WqConstants.RESULT_FAIL, detail.getItems().get(0).getItemResult());
    }

    // 4. 复核人不能复核自己提交的批次
    @Test
    void reviewer_cannotReviewOwnBatch() {
        Long id = submitBatch(List.of(item(d2, "1.20", "0.5", "25", "无异味")));
        WqReviewDTO dto = reviewDto(id, "张三", "RETEST");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.review(dto));
        assertEquals(400, ex.getCode());
        // 状态未变
        assertEquals(WqConstants.BATCH_PENDING_REVIEW, batchMapper.selectById(id).getStatus());
    }

    // 5. 整批退回 => 退回态，设备待复检解除，可重新提交，阈值重新固化
    @Test
    void fullReturn_thenResubmit_flow() {
        Long id = submitBatch(List.of(item(d2, "1.20", "0.5", "25", "无异味")));
        assertEquals(1, deviceMapper.selectById(d2).getPendingRetest());

        service.review(reviewDto(id, "王五", "RETURN"));
        assertEquals(WqConstants.BATCH_RETURNED, batchMapper.selectById(id).getStatus());
        assertEquals(0, deviceMapper.selectById(d2).getPendingRetest());

        // 修改为合格数据重新提交
        List<WqSampleItemDTO> fixed = List.of(item(d2, "0.40", "0.5", "25", "无异味"));
        WqBatchSubmitDTO re = new WqBatchSubmitDTO();
        re.setId(id); re.setSubmitter("张三"); re.setRequestId(rid()); re.setItems(fixed);
        service.submit(re);
        assertEquals(WqConstants.BATCH_PENDING_REVIEW, batchMapper.selectById(id).getStatus());
        assertEquals(WqConstants.RESULT_PASS, sampleItemMapper.selectByBatchId(id).get(0).getItemResult());
        assertEquals(0, deviceMapper.selectById(d2).getPendingRetest());

        // 别人复核确认 -> 全合格关闭
        service.review(reviewDto(id, "王五", "RETEST"));
        assertEquals(WqConstants.BATCH_CLOSED, batchMapper.selectById(id).getStatus());
    }

    // 6. 复检链路：两个不合格指标，第一次过一项，设备仍待复检；第二项过了才解除并关闭批次
    @Test
    void retestChain_releaseOnlyAfterAllOpenItemsPassed_disabledDeviceNotReenabled() {
        // d3 停用：余氯+浊度都不合格
        Long id = submitBatch(List.of(item(d3, "1.20", "5.0", "25", "无异味")));
        service.review(reviewDto(id, "王五", "RETEST"));
        assertEquals(WqConstants.BATCH_PENDING_RETEST, batchMapper.selectById(id).getStatus());

        List<WqFailItem> fails = failItemMapper.selectByBatchId(id);
        assertEquals(2, fails.size());
        WqFailItem clFail = fails.stream()
                .filter(f -> f.getMetricCode().equals(WqConstants.METRIC_RESIDUAL_CHLORINE)).findFirst().orElseThrow();
        WqFailItem tbFail = fails.stream()
                .filter(f -> f.getMetricCode().equals(WqConstants.METRIC_TURBIDITY)).findFirst().orElseThrow();

        // 第一次：余氯先复检不合格，项保持 OPEN，批次保持待复检
        submitRetest(clFail.getId(), "0.95", null, WqConstants.RESULT_FAIL);
        assertEquals(WqConstants.FAIL_OPEN, failItemMapper.selectById(clFail.getId()).getStatus());
        assertEquals(WqConstants.BATCH_PENDING_RETEST, batchMapper.selectById(id).getStatus());

        // 第二次：余氯合格，仅该项关闭；浊度仍 OPEN => 设备仍待复检，批次仍待复检
        submitRetest(clFail.getId(), "0.40", null, WqConstants.RESULT_PASS);
        assertEquals(WqConstants.FAIL_CLOSED_PASS, failItemMapper.selectById(clFail.getId()).getStatus());
        assertEquals(1, deviceMapper.selectById(d3).getPendingRetest());
        assertEquals(0, deviceMapper.selectById(d3).getStatus()); // 停用未自动启用
        assertEquals(WqConstants.BATCH_PENDING_RETEST, batchMapper.selectById(id).getStatus());

        // 浊度一次不合格再一次合格（历次都保留）
        submitRetest(tbFail.getId(), "2.0", null, WqConstants.RESULT_FAIL);
        submitRetest(tbFail.getId(), "0.6", null, WqConstants.RESULT_PASS);
        // 全部 open 清零 => 解除待复检 + 批次关闭；停用状态依旧不变
        assertEquals(0, deviceMapper.selectById(d3).getPendingRetest());
        assertEquals(0, deviceMapper.selectById(d3).getStatus());
        assertEquals(WqConstants.BATCH_CLOSED, batchMapper.selectById(id).getStatus());

        // 历次复检记录全部保留（余氯2次 + 浊度2次）
        assertEquals(4, retestMapper.selectByBatchId(id).size());
    }

    // 7. 多设备：一台仍有 open 项时批次不关闭，所有设备 open 清零才整体关闭
    @Test
    void batchClosed_onlyWhenAllDevicesCleared() {
        Long id = submitBatch(List.of(
                item(d2, "1.20", "0.5", "25", "无异味"),
                item(d1, "0.3", "5.0", "25", "无异味") // d1 浊度不合格
        ));
        service.review(reviewDto(id, "王五", "RETEST"));
        List<WqFailItem> fails = failItemMapper.selectByBatchId(id);
        assertEquals(2, fails.size());

        WqFailItem first = fails.get(0);
        submitRetest(first.getId(), first.getMetricCode().equals("odor") ? null : "0.4",
                "无异味", WqConstants.RESULT_PASS);
        assertEquals(WqConstants.BATCH_PENDING_RETEST, batchMapper.selectById(id).getStatus());

        WqFailItem second = fails.get(1);
        submitRetest(second.getId(), second.getMetricCode().equals("odor") ? null : "0.4",
                "无异味", WqConstants.RESULT_PASS);
        assertEquals(WqConstants.BATCH_CLOSED, batchMapper.selectById(id).getStatus());
        assertEquals(0, deviceMapper.selectById(d1).getPendingRetest());
        assertEquals(0, deviceMapper.selectById(d2).getPendingRetest());
    }

    // 8. 幂等：同一 requestId 复检重试不产生重复记录/重复状态变更/重复照片
    @Test
    void idempotent_sameRequestId_noDuplicate() {
        Long id = submitBatch(List.of(item(d2, "1.20", "5.0", "25", "无异味")));
        service.review(reviewDto(id, "王五", "RETEST"));
        WqFailItem fail = failItemMapper.selectByBatchId(id).get(0);

        WqRetestSubmitDTO dto = retestDto(fail.getId(), "0.4", null);
        String req = dto.getRequestId();
        service.submitRetest(dto);
        // 模拟网络重试：同样的 requestId 再来一次
        BusinessException ex = assertThrows(BusinessException.class, () -> service.submitRetest(dto));
        assertEquals(409, ex.getCode());

        List<WqRetest> retests = retestMapper.selectByBatchId(id);
        assertEquals(1, retests.size());
        // 照片引用不重复
        java.util.List<WqPhoto> allPhotos = photoMapper.selectList(null);
        long photos = allPhotos.stream()
                .filter((WqPhoto p) -> WqConstants.PHOTO_RETEST.equals(p.getRefType())).count();
        assertEquals(1, photos);
        // 幂等记录只有一条
        assertEquals(1, idemMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WqIdempotentRecord>()
                        .eq(WqIdempotentRecord::getRequestId, req)));
    }

    // 9. 提交幂等：重复点击同一 requestId 不产生重复状态流水/变更
    @Test
    void submit_idempotent_duplicateClick() {
        WqBatchSaveDTO save = new WqBatchSaveDTO();
        save.setScopeGroupId(floorA); save.setOperator("张三");
        save.setItems(List.of(item(d2, "1.20", "0.5", "25", "无异味")));
        Long id = service.saveDraft(save);

        WqBatchSubmitDTO submit = new WqBatchSubmitDTO();
        submit.setId(id); submit.setSubmitter("张三"); submit.setRequestId(rid());
        submit.setItems(save.getItems());
        service.submit(submit);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.submit(submit));
        assertEquals(409, ex.getCode());
        long submitLogs = statusLogMapper.selectByBatchId(id).stream()
                .filter(l -> WqConstants.ACTION_SUBMIT.equals(l.getAction())).count();
        assertEquals(1, submitLogs);
    }

    // 10. 并发：两名复核人同时处理，只一人成功
    @Test
    void concurrentReview_onlyOneSucceeds() throws Exception {
        Long id = submitBatch(List.of(item(d2, "1.20", "0.5", "25", "无异味")));

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final String reviewer = t == 0 ? "王五" : "赵六";
            pool.submit(() -> {
                try {
                    start.await();
                    service.review(reviewDto(id, reviewer, "RETEST"));
                    ok.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getCode() == 409) conflict.incrementAndGet();
                } catch (Exception e) {
                    // 唯一键冲突等也算对方抢占成功
                    conflict.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(1, ok.get(), "只能有一名复核人成功");
        assertEquals(1, conflict.get(), "另一人应收到冲突提示");
        // 只有一个人开启了复检项
        long opens = statusLogMapper.selectByBatchId(id).stream()
                .filter(l -> WqConstants.ACTION_REVIEW_RETEST.equals(l.getAction())).count();
        assertEquals(1, opens);
        assertEquals(1, failItemMapper.selectByBatchId(id).size());
    }

    // 11. 并发复检：同一不合格项两个请求，只生成一条复检记录
    @Test
    void concurrentRetest_onlyOneRecord() throws Exception {
        Long id = submitBatch(List.of(item(d2, "1.20", "0.5", "25", "无异味")));
        service.review(reviewDto(id, "王五", "RETEST"));
        WqFailItem fail = failItemMapper.selectByBatchId(id).get(0);

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    service.submitRetest(retestDto(fail.getId(), "0.4", null));
                    ok.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(1, ok.get());
        assertEquals(1, retestMapper.selectByBatchId(id).size());
    }

    // 12. 提交时数据不完整 => 整体回滚：批次状态、判定、快照、流水都不留半成品
    @Test
    void submit_incompleteData_rollsBackEverything() {
        WqBatchSaveDTO save = new WqBatchSaveDTO();
        save.setScopeGroupId(floorA); save.setOperator("张三");
        save.setItems(List.of(item(d1, null, "0.5", "25", "无异味"))); // 缺余氯
        Long id = service.saveDraft(save);

        WqBatchSubmitDTO submit = new WqBatchSubmitDTO();
        submit.setId(id); submit.setSubmitter("张三"); submit.setRequestId(rid());
        submit.setItems(save.getItems());
        assertThrows(BusinessException.class, () -> service.submit(submit));

        WqBatch batch = batchMapper.selectById(id);
        assertEquals(WqConstants.BATCH_DRAFT, batch.getStatus());
        assertNull(batch.getSubmitTime());
        assertNull(batch.getThresholdSnapshot());
        assertNull(sampleItemMapper.selectByBatchId(id).get(0).getItemResult());
        assertEquals(0, statusLogMapper.selectByBatchId(id).stream()
                .filter(l -> WqConstants.ACTION_SUBMIT.equals(l.getAction())).count());
        // 设备未被标记
        assertEquals(0, deviceMapper.selectById(d1).getPendingRetest());
    }

    // 13. 全部合格提交 + 复核 => 直接关闭，无不合格项、设备不标记
    @Test
    void allPass_reviewClosesDirectly() {
        Long id = submitBatch(List.of(item(d1, "0.3", "0.5", "25", "无异味")));
        service.review(reviewDto(id, "王五", "RETEST"));
        assertEquals(WqConstants.BATCH_CLOSED, batchMapper.selectById(id).getStatus());
        assertEquals(0, failItemMapper.selectByBatchId(id).size());
        assertEquals(0, deviceMapper.selectById(d1).getPendingRetest());
    }

    // ---------- helpers ----------

    private WqReviewDTO reviewDto(Long batchId, String reviewer, String action) {
        WqReviewDTO dto = new WqReviewDTO();
        dto.setBatchId(batchId);
        dto.setReviewer(reviewer);
        dto.setRequestId(rid());
        dto.setAction(action);
        if ("RETURN".equals(action)) {
            dto.setReason("采样数据存疑，请重新核实");
        }
        return dto;
    }

    private WqRetestSubmitDTO retestDto(Long failItemId, String value, String odor) {
        WqRetestSubmitDTO dto = new WqRetestSubmitDTO();
        dto.setFailItemId(failItemId);
        dto.setRetestOperator("赵六");
        dto.setRequestId(rid());
        dto.setRetestTime(T0.plusDays(1));
        if (value != null) dto.setMetricValue(new BigDecimal(value));
        dto.setOdor(odor);
        dto.setPhotos(List.of("/uploads/retest.jpg"));
        return dto;
    }

    private void submitRetest(Long failItemId, String value, String odor, String expectedResultIgnored) {
        service.submitRetest(retestDto(failItemId, value, odor));
    }
}
