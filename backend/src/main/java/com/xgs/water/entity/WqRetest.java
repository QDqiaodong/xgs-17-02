package com.xgs.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("wq_retest")
public class WqRetest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long failItemId;
    private Long batchId;
    private Long deviceId;
    private String deviceNoSnapshot;
    private String metricCode;
    private String metricName;
    private String metricValue;
    private String odor;
    private String retestResult;
    private String thresholdSnapshot;
    private String retestOperator;
    private LocalDateTime retestTime;
    private String comment;
    private String requestId;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFailItemId() { return failItemId; }
    public void setFailItemId(Long failItemId) { this.failItemId = failItemId; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
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
    public String getOdor() { return odor; }
    public void setOdor(String odor) { this.odor = odor; }
    public String getRetestResult() { return retestResult; }
    public void setRetestResult(String retestResult) { this.retestResult = retestResult; }
    public String getThresholdSnapshot() { return thresholdSnapshot; }
    public void setThresholdSnapshot(String thresholdSnapshot) { this.thresholdSnapshot = thresholdSnapshot; }
    public String getRetestOperator() { return retestOperator; }
    public void setRetestOperator(String retestOperator) { this.retestOperator = retestOperator; }
    public LocalDateTime getRetestTime() { return retestTime; }
    public void setRetestTime(LocalDateTime retestTime) { this.retestTime = retestTime; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
