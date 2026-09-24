package com.xgs.water.exception;

import com.xgs.water.dto.CursorToken;

/**
 * 翻页游标已过期（签名合法但超过 TTL）。
 * 前端据此保留筛选条件、清空页码并引导从起点重新检索。
 */
public class CursorExpiredException extends RuntimeException {

    private final transient CursorToken cursor;

    public CursorExpiredException(CursorToken cursor) {
        super("翻页位置已过期，请从第一页重新检索");
        this.cursor = cursor;
    }

    public CursorToken getCursor() {
        return cursor;
    }
}
