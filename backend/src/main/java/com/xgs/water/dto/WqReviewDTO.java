package com.xgs.water.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 复核操作：RETURN-整批退回（reason 必填）；RETEST-确认合格项并为不合格设备开启复检。
 * 复核人不能等于提交人。
 */
public class WqReviewDTO {
    @NotNull(message = "批次ID不能为空")
    private Long batchId;
    @NotBlank(message = "复核人不能为空")
    private String reviewer;
    @NotBlank(message = "缺少幂等标识")
    private String requestId;
    /** RETURN / RETEST */
    @NotBlank(message = "复核动作不能为空")
    private String action;
    private String reason;
    /** action=RETEST 时逐设备的复核意见：key=sampleItemId */
    private List<ItemReview> itemReviews;

    public static class ItemReview {
        private Long sampleItemId;
        /** PASS-确认合格, RETEST-开启复检 */
        private String reviewResult;
        private String comment;

        public Long getSampleItemId() { return sampleItemId; }
        public void setSampleItemId(Long sampleItemId) { this.sampleItemId = sampleItemId; }
        public String getReviewResult() { return reviewResult; }
        public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public List<ItemReview> getItemReviews() { return itemReviews; }
    public void setItemReviews(List<ItemReview> itemReviews) { this.itemReviews = itemReviews; }
}
