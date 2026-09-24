package com.xgs.water.controller;

import com.xgs.water.service.WaterModelCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Autowired
    private WaterModelCacheService waterModelCacheService;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";

    @GetMapping("/models")
    public Map<String, Object> getModels() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", waterModelCacheService.getAllModels());
        result.put("message", "success");
        return result;
    }

    @GetMapping("/models/{name}")
    public Map<String, Object> getModelParams(@PathVariable String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", waterModelCacheService.getModelParams(name));
        result.put("message", "success");
        return result;
    }

    @PostMapping("/models/refresh")
    public Map<String, Object> refreshModelCache() {
        Map<String, Object> result = new HashMap<>();
        waterModelCacheService.refreshCache();
        result.put("code", 200);
        result.put("message", "缓存刷新成功");
        return result;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file.isEmpty()) {
                result.put("code", 500);
                result.put("message", "文件为空");
                return result;
            }
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                    + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());
            result.put("code", 200);
            result.put("data", "/uploads/" + newFilename);
            result.put("message", "上传成功");
        } catch (IOException e) {
            result.put("code", 500);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }
}
