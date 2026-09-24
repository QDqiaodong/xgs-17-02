package com.xgs.water.service;

import com.xgs.water.vo.DeviceFacetVO;
import com.xgs.water.vo.DeviceSearchResult;

/**
 * 设备检索缓存。
 * 任何实现都必须满足：缓存不可用时调用方透明退回数据库查询，
 * 返回的过滤规则、字段格式与排序不得因是否命中缓存而改变
 *（缓存内容本身就是数据库查询结果的原样序列化）。
 *
 * 键约定：调用方只传业务键（查询指纹 / 固定 facet 键），实现内部自行加前缀。
 */
public interface SearchCache {

    /** 筛选项缓存业务键（全局唯一，型号/规格/出水类型不随筛选条件变化） */
    String FACET_KEY = "all";

    /** 取首页检索结果；未命中或缓存故障返回 null（调用方查库） */
    DeviceSearchResult getSearchPage(String fingerprint);

    /** 写首页检索结果；缓存故障静默忽略 */
    void putSearchPage(String fingerprint, DeviceSearchResult result);

    /** 取筛选项；未命中或缓存故障返回 null */
    DeviceFacetVO getFacets(String key);

    /** 写筛选项；缓存故障静默忽略 */
    void putFacets(String key, DeviceFacetVO facets);

    /** 是否真实可用（测试可切换；Redis 连接异常时实现返回 false） */
    boolean isAvailable();
}
