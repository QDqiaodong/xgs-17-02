package com.xgs.water.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xgs.water.vo.DeviceFacetVO;
import com.xgs.water.vo.DeviceSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 检索缓存实现。
 *
 * 降级原则：Redis 的任何连接/序列化异常都被吞掉并按“未命中”处理，
 * 业务查询自动回到数据库；缓存内容是 DB 结果的原样 JSON，
 * 因此缓存恢复后同一查询的字段、过滤规则、排序与直查完全一致。
 */
@Component
public class RedisSearchCache implements SearchCache {

    private static final Logger log = LoggerFactory.getLogger(RedisSearchCache.class);

    private static final String SEARCH_PREFIX = "water:device:search:";
    private static final String FACET_PREFIX = "water:device:facet:";

    /** 首页短缓存：只缓存不带游标的第一页，避免深翻页缓存膨胀与数据漂移放大 */
    private static final Duration SEARCH_TTL = Duration.ofSeconds(30);
    private static final Duration FACET_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 运行时一旦探测到 Redis 故障则短时直接走库，避免每次请求都等待连接超时 */
    private volatile boolean degraded = false;
    private volatile long degradedUntil = 0L;
    private final long degradeWindowMs;

    @Autowired
    public RedisSearchCache(StringRedisTemplate redisTemplate,
                            @Value("${device.search.cache-degrade-ms:5000}") long degradeWindowMs) {
        this.redisTemplate = redisTemplate;
        this.degradeWindowMs = degradeWindowMs;
        // 从 classpath 自动发现 JSR-310 时间模块，LocalDate/LocalDateTime 正确序列化
        this.objectMapper = ObjectMapper.findAndRegisterModules();
    }

    @Override
    public boolean isAvailable() {
        if (degraded && System.currentTimeMillis() < degradedUntil) {
            return false;
        }
        if (degraded && System.currentTimeMillis() >= degradedUntil) {
            degraded = false;
        }
        return true;
    }

    @Override
    public DeviceSearchResult getSearchPage(String key) {
        if (!isAvailable()) return null;
        try {
            String json = redisTemplate.opsForValue().get(SEARCH_PREFIX + key);
            if (json == null) return null;
            return objectMapper.readValue(json, DeviceSearchResult.class);
        } catch (Exception e) {
            markDegraded(e);
            return null;
        }
    }

    @Override
    public void putSearchPage(String key, DeviceSearchResult result) {
        if (!isAvailable()) return;
        try {
            redisTemplate.opsForValue().set(SEARCH_PREFIX + key,
                    objectMapper.writeValueAsString(result), SEARCH_TTL);
        } catch (Exception e) {
            markDegraded(e);
        }
    }

    @Override
    public DeviceFacetVO getFacets(String key) {
        if (!isAvailable()) return null;
        try {
            String json = redisTemplate.opsForValue().get(FACET_PREFIX + key);
            if (json == null) return null;
            return objectMapper.readValue(json, DeviceFacetVO.class);
        } catch (Exception e) {
            markDegraded(e);
            return null;
        }
    }

    @Override
    public void putFacets(String key, DeviceFacetVO facets) {
        if (!isAvailable()) return;
        try {
            redisTemplate.opsForValue().set(FACET_PREFIX + key,
                    objectMapper.writeValueAsString(facets), FACET_TTL);
        } catch (Exception e) {
            markDegraded(e);
        }
    }

    private void markDegraded(Exception e) {
        if (!degraded) {
            log.warn("检索缓存不可用，降级直查数据库（{}ms 后自动重试）：{}", degradeWindowMs, e.getMessage());
        }
        degraded = true;
        degradedUntil = System.currentTimeMillis() + degradeWindowMs;
    }
}
