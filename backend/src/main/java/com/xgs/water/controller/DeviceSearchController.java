package com.xgs.water.controller;

import com.xgs.water.dto.DeviceSearchQuery;
import com.xgs.water.service.DeviceSearchService;
import com.xgs.water.vo.DeviceFacetVO;
import com.xgs.water.vo.DeviceSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 全园设备快速检索。
 * 与旧 /water-dispenser/page（抽检批次选择设备仍在使用）并存，互不影响。
 */
@RestController
@RequestMapping("/water-dispenser")
public class DeviceSearchController {

    @Autowired
    private DeviceSearchService deviceSearchService;

    /**
     * 组合检索（游标分页）。
     * 游标过期/失效由全局异常处理返回 code=410，前端保留筛选并引导从起点重查。
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false) Long groupId,
                                      @RequestParam(required = false) String deviceNo,
                                      @RequestParam(required = false) String model,
                                      @RequestParam(required = false) String spec,
                                      @RequestParam(required = false) String waterType,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) Integer pendingRetest,
                                      @RequestParam(required = false, defaultValue = "createTime") String sort,
                                      @RequestParam(required = false, defaultValue = "desc") String order,
                                      @RequestParam(required = false, defaultValue = "20") Integer pageSize,
                                      @RequestParam(required = false) String cursor) {
        DeviceSearchQuery query = new DeviceSearchQuery();
        query.setGroupId(groupId);
        query.setDeviceNo(deviceNo);
        query.setModel(model);
        query.setSpec(spec);
        query.setWaterType(waterType);
        query.setStatus(status);
        query.setPendingRetest(pendingRetest);
        query.setSort(sort);
        query.setOrder(order);
        query.setPageSize(pageSize);
        query.setCursor(cursor);

        DeviceSearchResult data = deviceSearchService.search(query);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return result;
    }

    /** 筛选面板可选项：型号、安装规格、出水类型（区域树用既有 /building-group/tree） */
    @GetMapping("/search/facets")
    public Map<String, Object> facets() {
        DeviceFacetVO data = deviceSearchService.facets();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("message", "success");
        return result;
    }
}
