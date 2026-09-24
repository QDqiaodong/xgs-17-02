package com.xgs.water.search;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.xgs.water.service.SearchCache;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 检索模块测试配置：
 * 1. 用可控的内存缓存替换 Redis 缓存（测试环境没有 Redis 也能跑），
 *    通过 outage 开关验证“缓存不可用 -> 自动直查 DB -> 恢复后一致”；
 * 2. 注册 SQL 计数拦截器，验收单次检索的查询条数。
 *
 * Redis 缓存实现由各测试类通过 @MockBean(RedisSearchCache.class) 屏蔽，
 * 避免测试环境尝试连接 Redis。
 */
@TestConfiguration
public class SearchTestConfig {

    @Bean
    @Primary
    public SearchCache inMemorySearchCache() {
        return new InMemorySearchCache();
    }

    @Bean
    public SearchQueryCounter searchQueryCounter() {
        return new SearchQueryCounter();
    }

    @Bean
    public ConfigurationCustomizer searchCounterCustomizer(SearchQueryCounter counter) {
        return configuration -> configuration.addInterceptor(counter);
    }
}
