package com.xgs.water.vo;

import java.util.List;

/**
 * 游标分页结果。不返回总数：
 * 十万级数据下深翻页不做 COUNT，是否有下一页由「多取 1 条」判断。
 */
public class DeviceSearchResult {

    /** 当前页记录（pageSize 条；最后一页可能更少） */
    private List<DeviceSearchItem> records;
    /** 下一页游标令牌；无更多数据时为 null */
    private String nextCursor;
    /** 是否还有下一页 */
    private boolean hasMore;
    /** 本次实际生效的每页条数 */
    private int pageSize;
    /** 数据来源：db-直查数据库，cache-缓存；缓存降级时前端无需变化，仅用于观测 */
    private String source;

    public List<DeviceSearchItem> getRecords() { return records; }
    public void setRecords(List<DeviceSearchItem> records) { this.records = records; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
