package com.xgs.water.vo;

/**
 * 全园设备快速检索的内部查询参数。
 * Controller 入参经 DeviceSearchService 规范化、白名单校验后填充本对象，
 * 游标解析出的(排序值, id)也在此携带进 Mapper。
 */
public class DeviceSearchQuery {

    /** 排序字段 -> 列名白名单，列名只允许来自这里，杜绝 SQL 注入 */
    public static final java.util.Map<String, String> SORT_COLUMNS = java.util.Map.of(
            "createTime", "create_time",
            "deviceNo", "device_no",
            "installDate", "install_date",
            "model", "model"
    );

    private Long groupId;
    private String deviceNo;
    private String model;
    private String spec;
    private String waterType;
    private Integer status;
    private Integer pendingRetest;

    /** 规范化后的排序字段 key，如 createTime */
    private String sortField;
    /** Mapper 中使用的排序列名，如 create_time */
    private String sortColumn;
    /** ASC / DESC */
    private String sortDir;

    private int pageSize;
    private int limit;

    /** 规范化筛选+排序指纹，游标必须与当前指纹一致，否则视为条件已变更 */
    private String fingerprint;

    private boolean cursorPresent;
    private Long cursorId;
    private Object cursorValue;
    private boolean cursorValueNull;

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

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public String getSortColumn() { return sortColumn; }
    public void setSortColumn(String sortColumn) { this.sortColumn = sortColumn; }

    public String getSortDir() { return sortDir; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public boolean isCursorPresent() { return cursorPresent; }
    public void setCursorPresent(boolean cursorPresent) { this.cursorPresent = cursorPresent; }

    public Long getCursorId() { return cursorId; }
    public void setCursorId(Long cursorId) { this.cursorId = cursorId; }

    public Object getCursorValue() { return cursorValue; }
    public void setCursorValue(Object cursorValue) { this.cursorValue = cursorValue; }

    public boolean isCursorValueNull() { return cursorValueNull; }
    public void setCursorValueNull(boolean cursorValueNull) { this.cursorValueNull = cursorValueNull; }
}
