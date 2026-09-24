package com.xgs.water.dto;

import java.util.List;

/**
 * 检索 SQL 的归一化参数（Service 层构造，Mapper 直接使用）。
 * sortExpr 只能来自 {@link com.xgs.water.service.DeviceSearchService} 内的白名单映射。
 */
public class DeviceSearchParams {

    private List<Long> groupIds;
    private String deviceNo;
    private String model;
    private String spec;
    private String waterType;
    private Integer status;
    private Integer pendingRetest;

    /** 实际排序/游标比较/返回值共用的表达式（白名单）；时间列统一秒精度 */
    private String sortExpr;
    /** true=ASC，false=DESC；id 决胜键跟随同一方向 */
    private boolean asc;

    /** 是否带键集游标（第一页为 false） */
    private boolean hasCursor;
    /** 游标主排序值（类型随排序列：String/LocalDate/LocalDateTime）；null 时用 cursorSortNull 分支 */
    private Object cursorValue;
    private boolean cursorSortNull;
    private Long cursorId;

    /** 取 pageSize+1 条判断是否有下一页 */
    private int limit;

    public List<Long> getGroupIds() { return groupIds; }
    public void setGroupIds(List<Long> groupIds) { this.groupIds = groupIds; }

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

    public String getSortExpr() { return sortExpr; }
    public void setSortExpr(String sortExpr) { this.sortExpr = sortExpr; }

    public boolean isAsc() { return asc; }
    public void setAsc(boolean asc) { this.asc = asc; }

    public boolean isHasCursor() { return hasCursor; }
    public void setHasCursor(boolean hasCursor) { this.hasCursor = hasCursor; }

    public Object getCursorValue() { return cursorValue; }
    public void setCursorValue(Object cursorValue) { this.cursorValue = cursorValue; }

    public boolean isCursorSortNull() { return cursorSortNull; }
    public void setCursorSortNull(boolean cursorSortNull) { this.cursorSortNull = cursorSortNull; }

    public Long getCursorId() { return cursorId; }
    public void setCursorId(Long cursorId) { this.cursorId = cursorId; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
