package com.xgs.water.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xgs.water.constant.WqConstants;
import com.xgs.water.dto.WqThresholdDTO;
import com.xgs.water.entity.WqThreshold;
import com.xgs.water.exception.BusinessException;
import com.xgs.water.mapper.WqThresholdMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阈值维护与判定。
 * 关键点：批次提交时把当时的生效阈值序列化为 JSON 快照存入 wq_batch.threshold_snapshot，
 * 判定与后续复检一律读快照，后台改阈值不会影响任何已提交批次。
 */
@Service
public class WqThresholdService {

    @Autowired
    private WqThresholdMapper thresholdMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public List<WqThreshold> list() {
        return thresholdMapper.selectList(
                new LambdaQueryWrapper<WqThreshold>().orderByAsc(WqThreshold::getDisplayOrder, WqThreshold::getId));
    }

    public List<WqThreshold> listEnabled() {
        return thresholdMapper.selectList(
                new LambdaQueryWrapper<WqThreshold>()
                        .eq(WqThreshold::getEnabled, 1)
                        .orderByAsc(WqThreshold::getDisplayOrder, WqThreshold::getId));
    }

    @Transactional
    public void update(WqThresholdDTO dto) {
        WqThreshold entity = thresholdMapper.selectById(dto.getId());
        if (entity == null) {
            throw new BusinessException("阈值不存在");
        }
        if (WqConstants.JUDGE_RANGE.equals(entity.getJudgeType())) {
            if (dto.getMinValue() == null || dto.getMaxValue() == null) {
                throw new BusinessException(entity.getMetricName() + "的上下限不能为空");
            }
            if (dto.getMinValue().compareTo(dto.getMaxValue()) > 0) {
                throw new BusinessException(entity.getMetricName() + "的下限不能大于上限");
            }
            entity.setMinValue(dto.getMinValue());
            entity.setMaxValue(dto.getMaxValue());
        } else {
            if (dto.getPassValues() == null || dto.getPassValues().isBlank()) {
                throw new BusinessException(entity.getMetricName() + "的合格结论不能为空");
            }
            entity.setPassValues(dto.getPassValues());
        }
        entity.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        entity.setUpdateOperator(dto.getOperator() != null ? dto.getOperator() : "admin");
        thresholdMapper.updateById(entity);
    }

    /**
     * 固化当前全部生效阈值，序列化为 JSON（提交时刻调用）。
     */
    public String buildSnapshotJson() {
        Map<String, Map<String, Object>> snapshot = new LinkedHashMap<>();
        for (WqThreshold t : listEnabled()) {
            snapshot.put(t.getMetricCode(), toSnapshotMap(t));
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("阈值快照生成失败");
        }
    }

    private Map<String, Object> toSnapshotMap(WqThreshold t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("metricCode", t.getMetricCode());
        map.put("metricName", t.getMetricName());
        map.put("unit", t.getUnit());
        map.put("judgeType", t.getJudgeType());
        if (WqConstants.JUDGE_RANGE.equals(t.getJudgeType())) {
            map.put("minValue", t.getMinValue());
            map.put("maxValue", t.getMaxValue());
        } else {
            map.put("passValues", List.of(t.getPassValues().split(",")));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException("批次缺少固化阈值，无法判定");
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("批次阈值快照解析失败");
        }
    }

    /**
     * 用快照阈值判定单个指标。
     *
     * @param value range 指标传数值（字符串）；enum 指标传结论
     * @return true 合格
     */
    public boolean judgeWithSnapshot(Map<String, Object> metricSnapshot, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String judgeType = String.valueOf(metricSnapshot.get("judgeType"));
        if (WqConstants.JUDGE_ENUM.equals(judgeType)) {
            Object passValues = metricSnapshot.get("passValues");
            if (passValues instanceof List<?> list) {
                for (Object pass : list) {
                    if (value.trim().equalsIgnoreCase(String.valueOf(pass).trim())) {
                        return true;
                    }
                }
            }
            return false;
        }
        // range
        try {
            BigDecimal v = new BigDecimal(value.trim());
            BigDecimal min = new BigDecimal(String.valueOf(metricSnapshot.get("minValue")));
            BigDecimal max = new BigDecimal(String.valueOf(metricSnapshot.get("maxValue")));
            return v.compareTo(min) >= 0 && v.compareTo(max) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 取指标展示名称，优先用快照中的名称 */
    public String metricName(Map<String, Map<String, Object>> snapshot, String code) {
        Map<String, Object> m = snapshot.get(code);
        if (m != null && m.get("metricName") != null) {
            return String.valueOf(m.get("metricName"));
        }
        WqThreshold t = thresholdMapper.selectOne(
                new LambdaQueryWrapper<WqThreshold>().eq(WqThreshold::getMetricCode, code));
        return t != null ? t.getMetricName() : code;
    }

    /** 单个指标阈值快照再序列化（存入不合格项，复检沿用同一阈值） */
    public String writeMetricSnapshot(Map<String, Object> metricSnapshot) {
        try {
            return objectMapper.writeValueAsString(metricSnapshot);
        } catch (JsonProcessingException e) {
            throw new BusinessException("阈值快照生成失败");
        }
    }

    public Map<String, Object> readMetricSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
