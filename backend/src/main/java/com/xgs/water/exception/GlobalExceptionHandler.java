package com.xgs.water.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一异常响应：保持与现有接口一致的 {code, message, data} 信封。
 * 任何写操作失败都由事务回滚保证一致性，前端凭 code 区分并发冲突(409)。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusiness(BusinessException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", e.getCode());
        result.put("message", e.getMessage());
        return result;
    }

    /**
     * 翻页游标过期（签名合法但超过 TTL）。
     * 前端保留全部筛选条件，清空 cursor 并引导用户从第一页重新检索。
     */
    @ExceptionHandler(CursorExpiredException.class)
    public Map<String, Object> handleCursorExpired(CursorExpiredException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 410);
        result.put("message", e.getMessage());
        result.put("data", Map.of("cursorExpired", true));
        return result;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", message);
        return result;
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Map<String, Object> handleDuplicateKey(DuplicateKeyException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 409);
        result.put("message", "数据已被处理，请勿重复提交（已刷新到最新状态）");
        return result;
    }

    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", e.getMessage() != null ? e.getMessage() : "系统异常");
        return result;
    }
}
