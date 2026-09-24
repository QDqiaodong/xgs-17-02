package com.xgs.water.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提交一次复检结果。必须关联一个未关闭的不合格项，历次结果均保留在 wq_retest。
 * 判定沿用该不合格项在批次提交时固化的阈值。
 */
public class WqRetestSubmitDTO {
    @NotNull(message = "不合格项ID不能为空")
    private Long failItemId;
    @NotBlank(message = "复检人不能为空")
    private String retestOperator;
    @NotBlank(message = "缺少幂等标识")
    private String requestId;
    /** range 指标用数值 */
    private BigDecimal metricValue;
    /** enum 指标（气味）用结论 */
    private String odor;
    private LocalDateTime retestTime;
    private String comment;
    private List<String> photos;

    public Long getFailItemId() { return failItemId; }
    public void setFailItemId(Long failItemId) { this.failItemId = failItemId; }
    public String getRetestOperator() { return retestOperator; }
    public void setRetestOperator(String retestOperator) { this.retestOperator = retestOperator; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public BigDecimal getMetricValue() { return metricValue; }
    public void setMetricValue(BigDecimal metricValue) { this.metricValue = metricValue; }
    public String getOdor() { return odor; }
    public void setOdor(String odor) { this.odor = odor; }
    public LocalDateTime getRetestTime() { return retestTime; }
    public void setRetestTime(LocalDateTime retestTime) { this.retestTime = retestTime; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
}
