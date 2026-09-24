package com.xgs.water.dto;

import java.math.BigDecimal;

/**
 * 后台维护生效阈值。已提交批次不受影响（其判定依据批次表中的快照）。
 */
public class WqThresholdDTO {
    private Long id;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String passValues;
    private Integer enabled;
    private String operator;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    public String getPassValues() { return passValues; }
    public void setPassValues(String passValues) { this.passValues = passValues; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
