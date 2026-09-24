package com.xgs.water.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xgs.water.dto.WaterDispenserDTO;
import com.xgs.water.entity.GroupTransferLog;
import com.xgs.water.mapper.WaterDispenserMapper;
import com.xgs.water.service.DeviceSearchService;
import com.xgs.water.service.WaterDispenserService;
import com.xgs.water.vo.DeviceSearchResult;
import com.xgs.water.vo.WaterDispenserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/water-dispenser")
public class WaterDispenserController {

    /** 出水类型为固定字典，型号/安装规格来自设备数据去重 */
    private static final List<String> WATER_TYPES = List.of("冷水", "热水", "温水", "冰水");

    @Autowired
    private WaterDispenserService waterDispenserService;
    @Autowired
    private DeviceSearchService deviceSearchService;
    @Autowired
    private WaterDispenserMapper waterDispenserMapper;

    /**
     * 全园设备快速检索：区域树范围(含全部下级)、设备编号、型号、安装规格、
     * 出水类型、启用状态、待复检标记可任意组合；游标键集分页 + 稳定全序排序。
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false) Long groupId,
                                      @RequestParam(required = false) String deviceNo,
                                      @RequestParam(required = false) String model,
                                      @RequestParam(required = false) String spec,
                                      @RequestParam(required = false) String waterType,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) Integer pendingRetest,
                                      @RequestParam(required = false) String sortField,
                                      @RequestParam(required = false) String sortDir,
                                      @RequestParam(required = false) Integer pageSize,
                                      @RequestParam(required = false) String cursor) {
        Map<String, Object> result = new HashMap<>();
        DeviceSearchResult data = deviceSearchService.search(groupId, deviceNo, model, spec,
                waterType, status, pendingRetest, sortField, sortDir, pageSize, cursor);
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return result;
    }

    /** 筛选下拉候选：型号/安装规格直接对设备列去重，结果集小且走覆盖扫描 */
    @GetMapping("/filter-options")
    public Map<String, Object> filterOptions() {
        Map<String, Object> data = new HashMap<>();
        data.put("models", waterDispenserMapper.distinctModel());
        data.put("specs", waterDispenserMapper.distinctSpec());
        data.put("waterTypes", WATER_TYPES);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return result;
    }

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
