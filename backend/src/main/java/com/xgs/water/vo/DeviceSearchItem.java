package com.xgs.water.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检索结果行：区域路径由 SQL 固定层级 JOIN 直接产出，后端不再逐行查树。
 */
public class DeviceSearchItem {
    private Long id;
    private String deviceNo;
    private String model;
    private String spec;
    private String waterType;
    private Long groupId;
    /** 完整区域路径，如：创新产业园A区 / 1号楼 / 1层 */
    private String groupPath;
    private Integer status;
    private String statusName;
    private Integer pendingRetest;
    private LocalDate installDate;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceNo() { return deviceNo; }
    public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getWaterType() { return waterType; }
    public void setWaterType(String waterType) { this.waterType = waterType; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getGroupPath() { return groupPath; }
    public void setGroupPath(String groupPath) { this.groupPath = groupPath; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public Integer getPendingRetest() { return pendingRetest; }
    public void setPendingRetest(Integer pendingRetest) { this.pendingRetest = pendingRetest; }

    public LocalDate getInstallDate() { return installDate; }
    public void setInstallDate(LocalDate installDate) { this.installDate = installDate; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
