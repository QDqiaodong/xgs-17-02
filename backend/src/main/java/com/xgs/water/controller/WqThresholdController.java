package com.xgs.water.controller;

import com.xgs.water.dto.WqThresholdDTO;
import com.xgs.water.entity.WqThreshold;
import com.xgs.water.service.WqThresholdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 生效阈值后台维护。修改仅影响之后新提交的批次。
 */
@RestController
@RequestMapping("/wq/threshold")
public class WqThresholdController {

    @Autowired
    private WqThresholdService thresholdService;

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        List<WqThreshold> list = thresholdService.list();
        result.put("code", 200);
        result.put("data", list);
        result.put("message", "success");
        return result;
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody WqThresholdDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            thresholdService.update(dto);
            result.put("code", 200);
            result.put("message", "阈值已更新（仅影响之后提交的批次）");
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
