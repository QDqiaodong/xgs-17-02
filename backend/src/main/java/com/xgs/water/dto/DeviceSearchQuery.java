package com.xgs.water.dto;

/**
 * 全园设备快速检索条件。全部字段可空，空表示不限。
 *
 * 字段顺序即筛选维度：区域树范围 / 设备编号 / 型号 / 安装规格 /
 * 出水类型 / 启用状态 / 待复检标记。
 */
public class DeviceSearchQuery {

    /** 区域树选中节点：包含其全部下级区域（园区->楼栋->楼层） */
    private Long groupId;
    /** 设备编号：前缀匹配（右模糊），可走 idx_device_no */
    private String deviceNo;
    /** 型号：精确匹配 */
    private String model;
    /** 安装规格：精确匹配 */
    private String spec;
    /** 出水类型：在逗号分隔的 water_type 中做包含匹配（FIND_IN_SET） */
    private String waterType;
    /** 启用状态：1-正常 0-停用 */
    private Integer status;
    /** 待复检标记：1-仅看待复检 0-仅看正常 */
    private Integer pendingRetest;

    /** 排序字段白名单：deviceNo / installDate / model / createTime(默认) */
    private String sort = "createTime";
    /** asc / desc，默认 desc */
    private String order = "desc";

    /** 每页条数，上限 200，默认 20 */
    private Integer pageSize = 20;
    /** 上一页最后一条记录的游标令牌；为空表示第一页 */
    private String cursor;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getDeviceNo() { return deviceNo; }
    public void setDeviceNo(String deviceNo) { this.deviceNo = deviceNo; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public String getWaterType() { return waterType; }
    public void setWaterType(String waterType) { this.waterType = waterType; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getPendingRetest() { return pendingRetest; }
    public void setPendingRetest(Integer pendingRetest) { this.pendingRetest = pendingRetest; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public String getCursor() { return cursor; }
    public void setCursor(String cursor) { this.cursor = cursor; }
}
