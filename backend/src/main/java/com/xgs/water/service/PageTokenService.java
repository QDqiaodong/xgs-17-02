package com.xgs.water.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xgs.water.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * 键集翻页游标。
 *
 * 游标自包含（payload + HMAC-SHA256 签名），不依赖 Redis 等有状态存储，
 * 因此缓存服务不可用时翻页不受影响。payload 携带：
 *  - 生成时间（用于过期判定）
 *  - 筛选+排序指纹（用于判定游标是否还匹配当前查询条件）
 *  - 上一页最后一行的排序值与唯一 id（用于键集续页）
 *
 * 任何篡改、过期或与当前筛选条件不一致的游标都以 410 明确拒绝，
 * 前端保留筛选条件并引导从结果起点重新检索。
 */
@Service
public class PageTokenService {

    public static final int CODE_STALE_CURSOR = 410;

    @Value("${device.search.cursor-secret:}")
    private String configuredSecret;

    @Value("${device.search.cursor-ttl-seconds:1800}")
    private long ttlSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CursorPayload {
        public long exp;
        public String fp;
        public String sortField;
        /** 上一页最后一行主排序字段值；createTime 为 epochMilli，installDate 为 yyyy-MM-dd */
        public Object val;
        public long id;
        /** val 是否为 null（JSON 序列化会丢失 null 语义，故显式记录） */
        public boolean valNull;
    }

    public String issue(String fingerprint, String sortField, Object value, boolean valueNull, long id) {
        try {
            CursorPayload payload = new CursorPayload();
            payload.exp = Instant.now().getEpochSecond() + ttlSeconds;
            payload.fp = fingerprint;
            payload.sortField = sortField;
            payload.val = value;
            payload.id = id;
            payload.valNull = valueNull;
            String body = URL_ENCODER.encodeToString(
                    objectMapper.writeValueAsBytes(payload));
            return body + "." + sign(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("游标生成失败", e);
        }
    }

    /**
     * 校验并解析游标。签名错误/损坏/过期/指纹不匹配统一抛 410，
     * 由页面提示“翻页标识已失效或筛选条件已变化”，保留筛选条件并回到起点。
     */
    public CursorPayload parseAndVerify(String token, String currentFingerprint) {
        String body;
        try {
            int dot = token.lastIndexOf('.');
            if (dot <= 0 || dot >= token.length() - 1) {
                throw new BusinessException(CODE_STALE_CURSOR, staleMessage());
            }
            body = token.substring(0, dot);
            String sig = token.substring(dot + 1);
            if (!constantTimeEquals(sig, sign(body))) {
                throw new BusinessException(CODE_STALE_CURSOR, staleMessage());
            }
            CursorPayload payload = objectMapper.readValue(URL_DECODER.decode(body), CursorPayload.class);
            if (Instant.now().getEpochSecond() > payload.exp) {
                throw new BusinessException(CODE_STALE_CURSOR,
                        "翻页标识已过期，请保留当前筛选条件，从第一页重新检索。");
            }
            if (payload.fp == null || !payload.fp.equals(currentFingerprint)) {
                throw new BusinessException(CODE_STALE_CURSOR,
                        "筛选条件或排序已变化，已为您从新结果的第一页开始。");
            }
            return payload;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CODE_STALE_CURSOR, staleMessage());
        }
    }

    private String staleMessage() {
        return "翻页标识无效或已损坏，请保留当前筛选条件，从第一页重新检索。";
    }

    private String sign(String body) {
        try {
            byte[] secretBytes = secret().getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("游标签名失败", e);
        }
    }

    /**
     * 未配置密钥时使用实例内随机密钥：重启后旧游标必然失效，返回 410 引导重新检索，
     * 属于明确可接受的降级；生产建议通过 DEVICE_SEARCH_CURSOR_SECRET 固定密钥。
     */
    private String secret() {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret;
        }
        return FALLBACK_SECRET;
    }

    private static final String FALLBACK_SECRET =
            "xgs-device-search-fallback-" + java.util.UUID.randomUUID();

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
