package com.xgs.water.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 区域树祖先-后代闭包关系。
 * 每对 (ancestorId, descendantId) 一行，distance=0 表示自身。
 * 由 BuildingGroupService 在新增节点时维护，应用启动幂等全量回填。
 */
@TableName("building_group_closure")
public class BuildingGroupClosure {
    private Long ancestorId;
    private Long descendantId;
    private Integer distance;

    public Long getAncestorId() {
        return ancestorId;
    }

    public void setAncestorId(Long ancestorId) {
        this.ancestorId = ancestorId;
    }

    public Long getDescendantId() {
        return descendantId;
    }

    public void setDescendantId(Long descendantId) {
        this.descendantId = descendantId;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }
}
