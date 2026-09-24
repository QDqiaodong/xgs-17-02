package com.xgs.water.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批次内单台设备的采样数据（草稿保存与提交共用）。
 * 提交时后端会重新从设备档案快照编号/型号/区域路径，前端无法伪造历史位置。
 */
public class WqSampleItemDTO {
    private Long id;
    private Long deviceId;
    private String sampler;
    private LocalDateTime sampleTime;
    private BigDecimal residualChlorine;
    private BigDecimal turbidity;
    private BigDecimal temperature;
    private String odor;
    private List<String> photos;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
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
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
}
