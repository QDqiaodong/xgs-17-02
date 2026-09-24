package com.xgs.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("wq_fail_item")
public class WqFailItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long sampleItemId;
    private Long deviceId;
    private String deviceNoSnapshot;
    private String metricCode;
    private String metricName;
    private String metricValue;
    private String thresholdSnapshot;
    private String status;
    private String openReviewer;
    private LocalDateTime openTime;
    private String closeOperator;
    private LocalDateTime closeTime;
    private Long latestRetestId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Long getSampleItemId() { return sampleItemId; }
    public void setSampleItemId(Long sampleItemId) { this.sampleItemId = sampleItemId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getDeviceNoSnapshot() { return deviceNoSnapshot; }
    public void setDeviceNoSnapshot(String deviceNoSnapshot) { this.deviceNoSnapshot = deviceNoSnapshot; }
    public String getMetricCode() { return metricCode; }
    public void setMetricCode(String metricCode) { this.metricCode = metricCode; }
    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public String getMetricValue() { return metricValue; }
    public void setMetricValue(String metricValue) { this.metricValue = metricValue; }
    public String getThresholdSnapshot() { return thresholdSnapshot; }
    public void setThresholdSnapshot(String thresholdSnapshot) { this.thresholdSnapshot = thresholdSnapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOpenReviewer() { return openReviewer; }
    public void setOpenReviewer(String openReviewer) { this.openReviewer = openReviewer; }
    public LocalDateTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalDateTime openTime) { this.openTime = openTime; }
    public String getCloseOperator() { return closeOperator; }
    public void setCloseOperator(String closeOperator) { this.closeOperator = closeOperator; }
    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }
    public Long getLatestRetestId() { return latestRetestId; }
    public void setLatestRetestId(Long latestRetestId) { this.latestRetestId = latestRetestId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
