package com.xgs.water.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("wq_photo")
public class WqPhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String refType;
    private Long sampleItemId;
    private Long retestId;
    private Long deviceId;
    private String photoUrl;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public Long getSampleItemId() { return sampleItemId; }
    public void setSampleItemId(Long sampleItemId) { this.sampleItemId = sampleItemId; }
    public Long getRetestId() { return retestId; }
    public void setRetestId(Long retestId) { this.retestId = retestId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
