package com.xgs.water.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xgs.water.dto.CursorToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 游标令牌编解码 + HMAC 签名。
 *
 * 令牌 = base64url(JSON 游标内容) + "." + base64url(HMAC-SHA256(内容))
 * - 签名防止伪造/篡改排序值与 id；
 * - 内容里含签发时间，过期即拒绝（expired 区分于非法）；
 * - 内容里含查询指纹，更换筛选/排序后旧游标立即不可用。
 * 无服务端会话，水平扩容和重启都不影响游标有效性（除自然过期）。
 */
@Component
public class CursorCodec {

    /** 默认 30 分钟：游标只用于连续浏览，过期后引导从第一页重新检索 */
    public static final long DEFAULT_TTL_MS = 30L * 60 * 1000;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] signingKey;
    private final long ttlMs;

    public CursorCodec(@Value("${device.search.cursor-key:xgs-device-search-cursor-key-2026}") String cursorKey,
                       @Value("${device.search.cursor-ttl-ms:1800000}") long ttlMs) {
        this.signingKey = cursorKey.getBytes(StandardCharsets.UTF_8);
        this.ttlMs = ttlMs > 0 ? ttlMs : DEFAULT_TTL_MS;
    }

    public long getTtlMs() {
        return ttlMs;
    }

    public String encode(CursorToken token) {
        try {
            String payload = base64Url(objectMapper.writeValueAsBytes(token));
            return payload + "." + sign(payload);
        } catch (Exception e) {
            throw new IllegalStateException("游标编码失败", e);
        }
    }

    /**
     * 解码并校验签名与默认 TTL。
     * @see #decode(String, long)
     */
    public CursorToken decode(String token, boolean requireFresh) {
        return decode(token, requireFresh ? ttlMs : 0L);
    }

    /**
     * 解码并校验签名。
     * @param maxAgeMs 允许的最大游标年龄；&lt;=0 表示只验签名不校验时间
     * @return 游标内容；签名非法/格式错误返回 null；过期抛 {@link CursorExpiredException}
     */
    public CursorToken decode(String token, long maxAgeMs) {
        if (token == null || token.indexOf('.') < 0) {
            return null;
        }
        int dot = token.indexOf('.');
        String payload = token.substring(0, dot);
        String sig = token.substring(dot + 1);
        String expected = sign(payload);
        if (!constantTimeEquals(expected, sig)) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(payload);
            CursorToken cursor = objectMapper.readValue(json, CursorToken.class);
            if (maxAgeMs > 0 && System.currentTimeMillis() - cursor.getTs() > maxAgeMs) {
                throw new CursorExpiredException(cursor);
            }
            return cursor;
        } catch (CursorExpiredException e) {
            throw e;
        } catch (Exception e) {
            return null;
        }
    }

    /** 游标签名合法但超过 TTL：上层保留筛选条件并提示从起点重查 */
    public boolean isExpiredSignature(String token) {
        try {
            int dot = token.indexOf('.');
            if (dot <= 0) return false;
            String payload = token.substring(0, dot);
            if (!constantTimeEquals(sign(payload), token.substring(dot + 1))) {
                return false;
            }
            byte[] json = Base64.getUrlDecoder().decode(payload);
            CursorToken cursor = objectMapper.readValue(json, CursorToken.class);
            return System.currentTimeMillis() - cursor.getTs() > ttlMs;
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return base64Url(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("游标签名失败", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }
}
