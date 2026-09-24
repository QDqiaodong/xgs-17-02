package com.xgs.water.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 批次提交（草稿/退回 -> 待复核）。
 * 一次请求原子完成：设备快照、阈值固化、结果判定、设备待复检标记、状态流水、照片关系。
 * requestId 由前端生成，重复点击/网络重试不会产生重复提交。
 */
public class WqBatchSubmitDTO {
    @NotNull(message = "批次ID不能为空")
    private Long id;
    @NotBlank(message = "提交人不能为空")
    private String submitter;
    @NotBlank(message = "缺少幂等标识")
    private String requestId;
    private String remark;
    private List<WqSampleItemDTO> items;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<WqSampleItemDTO> getItems() { return items; }
    public void setItems(List<WqSampleItemDTO> items) { this.items = items; }
}
