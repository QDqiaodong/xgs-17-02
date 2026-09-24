package com.xgs.water.vo;

import java.time.LocalDateTime;

/**
 * 批次列表行
 */
public class WqBatchListVO {
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
    private Integer deviceCount;
    private Integer failCount;
    private Integer openFailCount;
    private LocalDateTime createTime;

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
    public Integer getDeviceCount() { return deviceCount; }
    public void setDeviceCount(Integer deviceCount) { this.deviceCount = deviceCount; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public Integer getOpenFailCount() { return openFailCount; }
    public void setOpenFailCount(Integer openFailCount) { this.openFailCount = openFailCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
