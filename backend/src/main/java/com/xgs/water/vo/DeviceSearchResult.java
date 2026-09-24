package com.xgs.water.vo;

import java.util.List;

/**
 * 全园设备快速检索结果。
 * 不使用 OFFSET 页码，nextCursor 是下一页唯一、稳定的翻页标识；
 * 第一页没有游标，连续向后翻页靠上一页返回的 nextCursor。
 */
public class DeviceSearchResult {

    private List<WaterDispenserVO> records;
    private long total;
    private int pageSize;
    private boolean hasMore;
    private String nextCursor;
    private String fingerprint;
    private String sortField;
    private String sortDir;
    /** db / cache：本次结果的数据来源，便于验收缓存降级一致性 */
    private String source;

    public List<WaterDispenserVO> getRecords() { return records; }
    public void setRecords(List<WaterDispenserVO> records) { this.records = records; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getSortField() { return sortField; }
    public void setSortField(String sortField) { this.sortField = sortField; }

    public String getSortDir() { return sortDir; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
