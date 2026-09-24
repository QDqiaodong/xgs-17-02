package com.xgs.water.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xgs.water.dto.WaterDispenserDTO;
import com.xgs.water.entity.GroupTransferLog;
import com.xgs.water.service.WaterDispenserService;
import com.xgs.water.vo.WaterDispenserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/water-dispenser")
public class WaterDispenserController {

    @Autowired
    private WaterDispenserService waterDispenserService;

    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(required = false) Long groupId,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Integer status) {
        Map<String, Object> result = new HashMap<>();
        IPage<WaterDispenserVO> page = waterDispenserService.page(pageNum, pageSize, groupId, keyword, status);
        result.put("code", 200);
        result.put("data", page);
        result.put("message", "success");
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        WaterDispenserVO vo = waterDispenserService.getById(id);
        if (vo == null) {
            result.put("code", 404);
            result.put("message", "设备不存在");
        } else {
            result.put("code", 200);
            result.put("data", vo);
            result.put("message", "success");
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> save(@RequestBody WaterDispenserDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            waterDispenserService.save(dto);
            result.put("code", 200);
            result.put("message", "新增成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody WaterDispenserDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            waterDispenserService.update(dto);
            result.put("code", 200);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            waterDispenserService.delete(id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/transfer-log/page")
    public Map<String, Object> transferLogPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        IPage<GroupTransferLog> page = waterDispenserService.transferLogPage(pageNum, pageSize, deviceId);
        result.put("code", 200);
        result.put("data", page);
        result.put("message", "success");
        return result;
    }
}
