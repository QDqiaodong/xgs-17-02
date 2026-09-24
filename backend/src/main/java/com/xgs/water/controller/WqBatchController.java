package com.xgs.water.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xgs.water.dto.WqBatchSaveDTO;
import com.xgs.water.dto.WqBatchSubmitDTO;
import com.xgs.water.dto.WqReviewDTO;
import com.xgs.water.dto.WqRetestSubmitDTO;
import com.xgs.water.service.WqInspectionService;
import com.xgs.water.vo.WqBatchDetailVO;
import com.xgs.water.vo.WqBatchListVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽检批次：草稿 / 提交 / 复核（退回、确认+复检）/ 复检结果 / 详情 / 列表
 */
@RestController
@RequestMapping("/wq/batch")
public class WqBatchController {

    @Autowired
    private WqInspectionService inspectionService;

    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(required = false) Long groupId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestParam(required = false) String deviceNo) {
        Map<String, Object> result = new HashMap<>();
        IPage<WqBatchListVO> page = inspectionService.page(
                pageNum, pageSize, groupId, status, startDate, endDate, deviceNo);
        result.put("code", 200);
        result.put("data", page);
        result.put("message", "success");
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        WqBatchDetailVO vo = inspectionService.detail(id);
        if (vo == null) {
            result.put("code", 404);
            result.put("message", "批次不存在");
        } else {
            result.put("code", 200);
            result.put("data", vo);
            result.put("message", "success");
        }
        return result;
    }

    /** 新建草稿 / 保存草稿（草稿与退回态可增删设备、修改结果） */
    @PostMapping("/draft")
    public Map<String, Object> saveDraft(@RequestBody WqBatchSaveDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long id = inspectionService.saveDraft(dto);
            result.put("code", 200);
            result.put("data", id);
            result.put("message", "草稿已保存");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}/draft")
    public Map<String, Object> deleteDraft(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            inspectionService.deleteDraft(id);
            result.put("code", 200);
            result.put("message", "草稿已删除");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 提交：固化阈值、自动判定、不合格设备标记待复检 */
    @PostMapping("/submit")
    public Map<String, Object> submit(@Valid @RequestBody WqBatchSubmitDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            inspectionService.submit(dto);
            result.put("code", 200);
            result.put("message", "提交成功，已进入待复核");
        } catch (com.xgs.water.exception.BusinessException e) {
            result.put("code", e.getCode());
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 复核：整批退回 或 确认合格项并开启复检 */
    @PostMapping("/review")
    public Map<String, Object> review(@Valid @RequestBody WqReviewDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            inspectionService.review(dto);
            result.put("code", 200);
            result.put("message", "复核处理成功");
        } catch (com.xgs.water.exception.BusinessException e) {
            result.put("code", e.getCode());
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 复检结果提交 */
    @PostMapping("/retest")
    public Map<String, Object> retest(@Valid @RequestBody WqRetestSubmitDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            inspectionService.submitRetest(dto);
            result.put("code", 200);
            result.put("message", "复检结果已保存");
        } catch (com.xgs.water.exception.BusinessException e) {
            result.put("code", e.getCode());
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
