package com.xgs.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("group_transfer_log")
public class GroupTransferLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deviceId;
    private String deviceNo;
    private Long oldGroupId;
    private String oldGroupPath;
    private Long newGroupId;
    private String newGroupPath;
    private String operator;
    private String transferReason;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceNo() {
        return deviceNo;
    }

    public void setDeviceNo(String deviceNo) {
        this.deviceNo = deviceNo;
    }

    public Long getOldGroupId() {
        return oldGroupId;
    }

    public void setOldGroupId(Long oldGroupId) {
        this.oldGroupId = oldGroupId;
    }

    public String getOldGroupPath() {
        return oldGroupPath;
    }

    public void setOldGroupPath(String oldGroupPath) {
        this.oldGroupPath = oldGroupPath;
    }

    public Long getNewGroupId() {
        return newGroupId;
    }

    public void setNewGroupId(Long newGroupId) {
        this.newGroupId = newGroupId;
    }

    public String getNewGroupPath() {
        return newGroupPath;
    }

    public void setNewGroupPath(String newGroupPath) {
        this.newGroupPath = newGroupPath;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getTransferReason() {
        return transferReason;
    }

    public void setTransferReason(String transferReason) {
        this.transferReason = transferReason;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
