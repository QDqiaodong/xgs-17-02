package com.xgs.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("wq_sample_item")
public class WqSampleItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long deviceId;
    private String deviceNoSnapshot;
    private String modelSnapshot;
    private Long groupIdSnapshot;
    private String groupPathSnapshot;
    private String sampler;
    private LocalDateTime sampleTime;
    private BigDecimal residualChlorine;
    private BigDecimal turbidity;
    private BigDecimal temperature;
    private String odor;
    private String itemResult;
    private String failMetrics;
    private String reviewResult;
    private String reviewComment;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getDeviceNoSnapshot() { return deviceNoSnapshot; }
    public void setDeviceNoSnapshot(String deviceNoSnapshot) { this.deviceNoSnapshot = deviceNoSnapshot; }
    public String getModelSnapshot() { return modelSnapshot; }
    public void setModelSnapshot(String modelSnapshot) { this.modelSnapshot = modelSnapshot; }
    public Long getGroupIdSnapshot() { return groupIdSnapshot; }
    public void setGroupIdSnapshot(Long groupIdSnapshot) { this.groupIdSnapshot = groupIdSnapshot; }
    public String getGroupPathSnapshot() { return groupPathSnapshot; }
    public void setGroupPathSnapshot(String groupPathSnapshot) { this.groupPathSnapshot = groupPathSnapshot; }
    public String getSampler() { return sampler; }
    public void setSampler(String sampler) { this.sampler = sampler; }
    public LocalDateTime getSampleTime() { return sampleTime; }
    public void setSampleTime(LocalDateTime sampleTime) { this.sampleTime = sampleTime; }
    public BigDecimal getResidualChlorine() { return residualChlorine; }
    public void setResidualChlorine(BigDecimal residualChlorine) { this.residualChlorine = residualChlorine; }
    public BigDecimal getTurbidity() { return turbidity; }
    public void setTurbidity(BigDecimal turbidity) { this.turbidity = turbidity; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public String getOdor() { return odor; }
    public void setOdor(String odor) { this.odor = odor; }
    public String getItemResult() { return itemResult; }
    public void setItemResult(String itemResult) { this.itemResult = itemResult; }
    public String getFailMetrics() { return failMetrics; }
    public void setFailMetrics(String failMetrics) { this.failMetrics = failMetrics; }
    public String getReviewResult() { return reviewResult; }
    public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
