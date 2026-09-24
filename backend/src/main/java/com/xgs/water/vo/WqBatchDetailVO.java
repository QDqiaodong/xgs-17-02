package com.xgs.water.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次详情：原始采样（含提交时位置快照 + 设备当前区域对照）、复核意见、不合格项复检链路、状态流水
 */
public class WqBatchDetailVO {

    private Long id;
    private String batchNo;
    private Long scopeGroupId;
    private String scopeGroupPath;
    private String status;
    private String statusName;
    private String submitter;
    private LocalDateTime submitTime;
    private String reviewer;
    private LocalDateTime reviewTime;
    private String returnReason;
    private String thresholdSnapshot;
    private String closeOperator;
    private LocalDateTime closeTime;
    private String remark;
    private Integer version;
    private LocalDateTime createTime;
    private List<SampleItemVO> items;
    private List<FailItemVO> failItems;
    private List<StatusLogVO> statusLogs;

    public static class SampleItemVO {
        private Long id;
        private Long deviceId;
        private String deviceNoSnapshot;
        private String modelSnapshot;
        private Long groupIdSnapshot;
        private String groupPathSnapshot;
        /** 设备当前所属区域（设备调组/改名后，详情可对照，历史快照本身不被改动） */
        private String currentGroupPath;
        private String currentDeviceNo;
        private Integer deviceStatus;
        private Integer devicePendingRetest;
        private String sampler;
        private LocalDateTime sampleTime;
        private BigDecimal residualChlorine;
        private BigDecimal turbidity;
        private BigDecimal temperature;
        private String odor;
        private String itemResult;
        private String itemResultName;
        private List<String> failMetrics;
        private String reviewResult;
        private String reviewComment;
        private List<String> photos;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
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
        public String getCurrentGroupPath() { return currentGroupPath; }
        public void setCurrentGroupPath(String currentGroupPath) { this.currentGroupPath = currentGroupPath; }
        public String getCurrentDeviceNo() { return currentDeviceNo; }
        public void setCurrentDeviceNo(String currentDeviceNo) { this.currentDeviceNo = currentDeviceNo; }
        public Integer getDeviceStatus() { return deviceStatus; }
        public void setDeviceStatus(Integer deviceStatus) { this.deviceStatus = deviceStatus; }
        public Integer getDevicePendingRetest() { return devicePendingRetest; }
        public void setDevicePendingRetest(Integer devicePendingRetest) { this.devicePendingRetest = devicePendingRetest; }
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
        public String getItemResultName() { return itemResultName; }
        public void setItemResultName(String itemResultName) { this.itemResultName = itemResultName; }
        public List<String> getFailMetrics() { return failMetrics; }
        public void setFailMetrics(List<String> failMetrics) { this.failMetrics = failMetrics; }
        public String getReviewResult() { return reviewResult; }
        public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }
        public String getReviewComment() { return reviewComment; }
        public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
        public List<String> getPhotos() { return photos; }
        public void setPhotos(List<String> photos) { this.photos = photos; }
    }

    public static class FailItemVO {
        private Long id;
        private Long sampleItemId;
        private Long deviceId;
        private String deviceNoSnapshot;
        private String metricCode;
        private String metricName;
        private String metricValue;
        private String thresholdSnapshot;
        private String status;
        private String statusName;
        private String openReviewer;
        private LocalDateTime openTime;
        private String closeOperator;
        private LocalDateTime closeTime;
        private List<RetestVO> retests;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
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
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public String getOpenReviewer() { return openReviewer; }
        public void setOpenReviewer(String openReviewer) { this.openReviewer = openReviewer; }
        public LocalDateTime getOpenTime() { return openTime; }
        public void setOpenTime(LocalDateTime openTime) { this.openTime = openTime; }
        public String getCloseOperator() { return closeOperator; }
        public void setCloseOperator(String closeOperator) { this.closeOperator = closeOperator; }
        public LocalDateTime getCloseTime() { return closeTime; }
        public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }
        public List<RetestVO> getRetests() { return retests; }
        public void setRetests(List<RetestVO> retests) { this.retests = retests; }
    }

    public static class RetestVO {
        private Long id;
        private Long failItemId;
        private String metricCode;
        private String metricName;
        private String metricValue;
        private String odor;
        private String retestResult;
        private String retestResultName;
        private String thresholdSnapshot;
        private String retestOperator;
        private LocalDateTime retestTime;
        private String comment;
        private LocalDateTime createTime;
        private List<String> photos;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getFailItemId() { return failItemId; }
        public void setFailItemId(Long failItemId) { this.failItemId = failItemId; }
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
        public String getRetestResultName() { return retestResultName; }
        public void setRetestResultName(String retestResultName) { this.retestResultName = retestResultName; }
        public String getThresholdSnapshot() { return thresholdSnapshot; }
        public void setThresholdSnapshot(String thresholdSnapshot) { this.thresholdSnapshot = thresholdSnapshot; }
        public String getRetestOperator() { return retestOperator; }
        public void setRetestOperator(String retestOperator) { this.retestOperator = retestOperator; }
        public LocalDateTime getRetestTime() { return retestTime; }
        public void setRetestTime(LocalDateTime retestTime) { this.retestTime = retestTime; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public List<String> getPhotos() { return photos; }
        public void setPhotos(List<String> photos) { this.photos = photos; }
    }

    public static class StatusLogVO {
        private Long id;
        private String fromStatus;
        private String toStatus;
        private String action;
        private String operator;
        private String detail;
        private LocalDateTime createTime;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFromStatus() { return fromStatus; }
        public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
        public String getToStatus() { return toStatus; }
        public void setToStatus(String toStatus) { this.toStatus = toStatus; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getScopeGroupId() { return scopeGroupId; }
    public void setScopeGroupId(Long scopeGroupId) { this.scopeGroupId = scopeGroupId; }
    public String getScopeGroupPath() { return scopeGroupPath; }
    public void setScopeGroupPath(String scopeGroupPath) { this.scopeGroupPath = scopeGroupPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getSubmitter() { return submitter; }
    public void setSubmitter(String submitter) { this.submitter = submitter; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public String getThresholdSnapshot() { return thresholdSnapshot; }
    public void setThresholdSnapshot(String thresholdSnapshot) { this.thresholdSnapshot = thresholdSnapshot; }
    public String getCloseOperator() { return closeOperator; }
    public void setCloseOperator(String closeOperator) { this.closeOperator = closeOperator; }
    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<SampleItemVO> getItems() { return items; }
    public void setItems(List<SampleItemVO> items) { this.items = items; }
    public List<FailItemVO> getFailItems() { return failItems; }
    public void setFailItems(List<FailItemVO> failItems) { this.failItems = failItems; }
    public List<StatusLogVO> getStatusLogs() { return statusLogs; }
    public void setStatusLogs(List<StatusLogVO> statusLogs) { this.statusLogs = statusLogs; }
}
