package com.xgs.water.service;

import java.util.HashMap;
import java.util.Map;

/**
 * 同一次请求内的区域路径缓存（ThreadLocal）。
 * 老的分页接口逐行回填 groupPath 时，避免对同一楼层重复查祖先链；
 * 检索新接口直接由 SQL JOIN 产出路径，不依赖本缓存。
 *
 * 用法：
 *   PathCache.begin();
 *   try { ...业务... } finally { PathCache.end(); }
 */
public final class PathCache {

    private static final ThreadLocal<Map<Long, String>> HOLDER = new ThreadLocal<>();

    private PathCache() {
    }

    public static void begin() {
        HOLDER.set(new HashMap<>());
    }

    public static void end() {
        HOLDER.remove();
    }

    public static Map<Long, String> current() {
        Map<Long, String> map = HOLDER.get();
        if (map == null) {
            // 未显式 begin 时懒初始化并挂到当前线程，保证同一次请求内缓存有效
            map = new HashMap<>();
            HOLDER.set(map);
        }
        return map;
    }
}
