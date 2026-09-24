package com.xgs.water.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建批次或保存草稿。草稿可增删设备、修改结果；不做指标完整性校验。
 */
public class WqBatchSaveDTO {
    /** 为空表示新建草稿 */
    private Long id;
    private Long scopeGroupId;
    private String remark;
    private String operator;
    private List<WqSampleItemDTO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScopeGroupId() { return scopeGroupId; }
    public void setScopeGroupId(Long scopeGroupId) { this.scopeGroupId = scopeGroupId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public List<WqSampleItemDTO> getItems() { return items; }
    public void setItems(List<WqSampleItemDTO> items) { this.items = items; }
}
