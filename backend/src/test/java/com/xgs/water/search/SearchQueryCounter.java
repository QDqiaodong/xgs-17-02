package com.xgs.water.search;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 统计设备检索相关 SQL 的执行次数，用于验收“不能逐行查树/不能全量读入内存”：
 * 一次检索页请求固定只允许 1 条设备检索 SQL（闭包子树解析另计 1 条以内）。
 *
 * 按 MappedStatement id 过滤，避免把抽检模块的语句算进来。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SearchQueryCounter implements Interceptor {

    private final AtomicLong deviceSearchQueries = new AtomicLong(0);
    private final AtomicLong closureQueries = new AtomicLong(0);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String id = ms.getId();
        if (id.endsWith("DeviceSearchMapper.search")) {
            deviceSearchQueries.incrementAndGet();
        }
        if (id.contains("BuildingGroupClosureMapper")) {
            closureQueries.incrementAndGet();
        }
        return invocation.proceed();
    }

    public long deviceSearchQueries() {
        return deviceSearchQueries.get();
    }

    public long closureQueries() {
        return closureQueries.get();
    }

    public void reset() {
        deviceSearchQueries.set(0);
        closureQueries.set(0);
    }
}
