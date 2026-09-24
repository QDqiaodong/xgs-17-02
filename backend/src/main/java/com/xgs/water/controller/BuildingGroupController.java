package com.xgs.water.controller;

import com.xgs.water.dto.BuildingGroupDTO;
import com.xgs.water.entity.BuildingGroup;
import com.xgs.water.service.BuildingGroupService;
import com.xgs.water.vo.BuildingGroupTreeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/building-group")
public class BuildingGroupController {

    @Autowired
    private BuildingGroupService buildingGroupService;

    @GetMapping("/tree")
    public Map<String, Object> getTree() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", buildingGroupService.getTree());
        result.put("message", "success");
        return result;
    }

    @GetMapping("/leaf-list")
    public Map<String, Object> getLeafList() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", buildingGroupService.getFlatLeafNodes());
        result.put("message", "success");
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        BuildingGroup group = buildingGroupService.getById(id);
        if (group == null) {
            result.put("code", 404);
            result.put("message", "分组不存在");
        } else {
            result.put("code", 200);
            result.put("data", group);
            result.put("message", "success");
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> save(@RequestBody BuildingGroupDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            BuildingGroup group = new BuildingGroup();
            BeanUtils.copyProperties(dto, group);
            buildingGroupService.save(group);
            result.put("code", 200);
            result.put("data", group.getId());
            result.put("message", "新增成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody BuildingGroupDTO dto) {
        Map<String, Object> result = new HashMap<>();
        try {
            BuildingGroup group = new BuildingGroup();
            BeanUtils.copyProperties(dto, group);
            buildingGroupService.update(group);
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
            buildingGroupService.delete(id);
            result.put("code", 200);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
