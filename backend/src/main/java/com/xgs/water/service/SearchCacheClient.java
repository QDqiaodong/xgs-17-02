package com.xgs.water.service;

/**
 * 设备检索缓存客户端抽象。
 *
 * 生产实现走 Redis；测试可替换为可控制故障的内存实现，用于验证“缓存降级结果一致”。
 * 任何缓存操作都不允许向上抛出：缓存服务不可用时调用方直接回退数据库。
 */
public interface SearchCacheClient {

    String get(String key);

    void set(String key, String value, long ttlSeconds);

    /**
     * 设备/区域数据发生写操作后递增版本号，使旧缓存键自然失效。
     * 返回递增后的版本号；缓存不可用时返回 0，调用方据此直接走库。
     */
    long bumpVersion();

    long currentVersion();

    /** 测试/排障用：模拟缓存服务不可用 */
    default void setAvailable(boolean available) {
        // no-op
    }
}
