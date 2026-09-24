package com.xgs.water.dto;

/**
 * 翻页游标承载的全部信息（编码进令牌，无服务端状态）。
 *
 * 主键值 + id 决胜值：即使多条记录主排序值相同，id 仍保证全局确定序，
 * 连续翻页不会重复出现设备，也不会跳过设备。
 */
public class CursorToken {

    /** 上一页最后一条的主排序值（device_no/model/install_date/create_time），统一以字符串承载 */
    private String sortValue;
    /** 上一页最后一条的设备 id（唯一决胜键） */
    private Long id;
    /** 签发时间（epoch 毫秒） */
    private long ts;
    /** 生成游标时的筛选条件+排序+pageSize 指纹，防止游标串用到别的查询 */
    private String fingerprint;

    public CursorToken() {
    }

    public CursorToken(String sortValue, Long id, long ts, String fingerprint) {
        this.sortValue = sortValue;
        this.id = id;
        this.ts = ts;
        this.fingerprint = fingerprint;
    }

    public String getSortValue() { return sortValue; }
    public void setSortValue(String sortValue) { this.sortValue = sortValue; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public long getTs() { return ts; }
    public void setTs(long ts) { this.ts = ts; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
}
