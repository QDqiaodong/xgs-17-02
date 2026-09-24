package com.xgs.water.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class WaterModelCacheService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WaterModelCacheService.class);

    private static final String MODEL_PARAMS_KEY = "water:dispenser:model:params";
    private static final String MODEL_SPEC_KEY = "water:dispenser:model:spec:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Map<String, Map<String, Object>> DEFAULT_MODELS = new LinkedHashMap<>();

    static {
        DEFAULT_MODELS.put("美的YR-5", Map.of(
                "brand", "美的",
                "power", "550W",
                "capacity", "2L",
                "weight", "8kg",
                "defaultSpec", "立式冷热型",
                "defaultWaterType", "冷水,热水,温水",
                "score", 95
        ));
        DEFAULT_MODELS.put("沁园YR-10", Map.of(
                "brand", "沁园",
                "power", "600W",
                "capacity", "2.5L",
                "weight", "10kg",
                "defaultSpec", "立式冰热型",
                "defaultWaterType", "冷水,热水,冰水",
                "score", 90
        ));
        DEFAULT_MODELS.put("安吉尔J26", Map.of(
                "brand", "安吉尔",
                "power", "420W",
                "capacity", "1.5L",
                "weight", "5kg",
                "defaultSpec", "台式温热型",
                "defaultWaterType", "温水,热水",
                "score", 88
        ));
        DEFAULT_MODELS.put("海尔HRO50", Map.of(
                "brand", "海尔",
                "power", "700W",
                "capacity", "3L",
                "weight", "12kg",
                "defaultSpec", "立式冰热型",
                "defaultWaterType", "冷水,热水,冰水,温水",
                "score", 92
        ));
        DEFAULT_MODELS.put("九阳JYW-RO", Map.of(
                "brand", "九阳",
                "power", "480W",
                "capacity", "1.8L",
                "weight", "6kg",
                "defaultSpec", "台式冷热型",
                "defaultWaterType", "冷水,温水,热水",
                "score", 85
        ));
    }

    @PostConstruct
    public void initCache() {
        try {
            Boolean hasKey = redisTemplate.hasKey(MODEL_PARAMS_KEY);
            if (Boolean.FALSE.equals(hasKey)) {
                log.info("初始化饮水机型号参数缓存到Redis有序集合");
                ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
                for (Map.Entry<String, Map<String, Object>> entry : DEFAULT_MODELS.entrySet()) {
                    String modelName = entry.getKey();
                    Map<String, Object> params = entry.getValue();
                    Double score = ((Number) params.get("score")).doubleValue();
                    zSetOps.add(MODEL_PARAMS_KEY, modelName, score);
                    String specKey = MODEL_SPEC_KEY + modelName;
                    redisTemplate.opsForHash().putAll(specKey, params);
                    redisTemplate.expire(specKey, 24, TimeUnit.HOURS);
                }
                redisTemplate.expire(MODEL_PARAMS_KEY, 7, TimeUnit.DAYS);
                log.info("饮水机型号缓存初始化完成，共{}个型号", DEFAULT_MODELS.size());
            }
        } catch (Exception e) {
            log.warn("Redis缓存初始化失败，可能Redis未启动: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> getAllModels() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            Set<Object> modelNames = redisTemplate.opsForZSet().reverseRange(MODEL_PARAMS_KEY, 0, -1);
            if (modelNames != null && !modelNames.isEmpty()) {
                for (Object modelName : modelNames) {
                    Map<String, Object> modelInfo = getModelParams(modelName.toString());
                    modelInfo.put("name", modelName.toString());
                    result.add(modelInfo);
                }
            }
        } catch (Exception e) {
            log.warn("从Redis读取型号列表失败，使用本地数据", e);
            for (Map.Entry<String, Map<String, Object>> entry : DEFAULT_MODELS.entrySet()) {
                Map<String, Object> modelInfo = new HashMap<>(entry.getValue());
                modelInfo.put("name", entry.getKey());
                result.add(modelInfo);
            }
        }
        return result;
    }

    public Map<String, Object> getModelParams(String modelName) {
        Map<String, Object> result = new HashMap<>();
        try {
            String specKey = MODEL_SPEC_KEY + modelName;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(specKey);
            if (entries != null && !entries.isEmpty()) {
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
        } catch (Exception e) {
            log.warn("从Redis读取型号参数失败: {}", modelName, e);
        }
        if (result.isEmpty() && DEFAULT_MODELS.containsKey(modelName)) {
            result.putAll(DEFAULT_MODELS.get(modelName));
        }
        return result;
    }

    public void refreshCache() {
        try {
            redisTemplate.delete(MODEL_PARAMS_KEY);
            Set<String> keys = redisTemplate.keys(MODEL_SPEC_KEY + "*");
            if (keys != null) {
                redisTemplate.delete(keys);
            }
            initCache();
        } catch (Exception e) {
            log.warn("刷新缓存失败", e);
        }
    }
}
