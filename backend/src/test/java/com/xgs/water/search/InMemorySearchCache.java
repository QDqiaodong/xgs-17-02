package com.xgs.water.search;

import com.xgs.water.service.SearchCache;
import com.xgs.water.vo.DeviceFacetVO;
import com.xgs.water.vo.DeviceSearchResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 检索缓存的内存测试替身：
 * - 默认行为与 Redis 实现一致（命中/回填）；
 * - {@link #setOutage(boolean)} 可模拟“缓存服务不可用”，验证自动降级直查 DB；
 * - 可随时 {@link #clear()} 模拟缓存恢复后的冷启动。
 */
public class InMemorySearchCache implements SearchCache {

    private final Map<String, DeviceSearchResult> pages = new ConcurrentHashMap<>();
    private final Map<String, DeviceFacetVO> facets = new ConcurrentHashMap<>();

    /** true 时模拟 Redis 宕机：所有读写表现为故障（get 返回 null、put 静默丢弃） */
    private volatile boolean outage = false;

    public void setOutage(boolean outage) {
        this.outage = outage;
    }

    public void clear() {
        pages.clear();
        facets.clear();
    }

    public int cachedPageCount() {
        return pages.size();
    }

    @Override
    public DeviceSearchResult getSearchPage(String key) {
        if (outage) return null;
        return pages.get(key);
    }

    @Override
    public void putSearchPage(String key, DeviceSearchResult result) {
        if (outage) return;
        pages.put(key, result);
    }

    @Override
    public DeviceFacetVO getFacets(String key) {
        if (outage) return null;
        return facets.get(key);
    }

    @Override
    public void putFacets(String key, DeviceFacetVO value) {
        if (outage) return;
        facets.put(key, value);
    }

    @Override
    public boolean isAvailable() {
        return !outage;
    }
}
