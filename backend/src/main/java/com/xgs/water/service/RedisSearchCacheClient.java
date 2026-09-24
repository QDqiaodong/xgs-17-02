package com.xgs.water.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的检索缓存。
 *
 * 缓存只保存“最终结果”（过滤后的行、格式化字段、排序顺序、总数），
 * 命中与未命中返回的内容由同一套组装逻辑产生，缓存恢复后不会因数据来源不同
 * 而改变字段或顺序。所有 Redis 异常被吞掉并返回空值，由业务层回退数据库查询。
 */
@Component
@ConditionalOnMissingBean(SearchCacheClient.class)
public class RedisSearchCacheClient implements SearchCacheClient {

    private static final Logger log = LoggerFactory.getLogger(RedisSearchCacheClient.class);

    static final String VERSION_KEY = "water:device:search:ver";
    static final String KEY_PREFIX = "water:device:search:v";

    private final StringRedisTemplate redis;

    public RedisSearchCacheClient(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key) {
        try {
            return redis.opsForValue().get(KEY_PREFIX + key);
        } catch (Exception e) {
            log.warn("检索缓存读取失败，回退数据库查询: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        try {
            redis.opsForValue().set(KEY_PREFIX + key, value, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("检索缓存写入失败，本次结果仍正常返回: {}", e.getMessage());
        }
    }

    @Override
    public long bumpVersion() {
        try {
            Long v = redis.opsForValue().increment(VERSION_KEY);
            return v == null ? 0 : v;
        } catch (Exception e) {
            log.warn("检索缓存版本递增失败，本次直接走数据库: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long currentVersion() {
        try {
            String v = redis.opsForValue().get(VERSION_KEY);
            return v == null ? 0 : Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }
}
