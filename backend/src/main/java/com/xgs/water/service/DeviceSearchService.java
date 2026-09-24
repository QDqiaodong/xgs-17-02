package com.xgs.water.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xgs.water.exception.BusinessException;
import com.xgs.water.mapper.WaterDispenserMapper;
import com.xgs.water.vo.DeviceSearchQuery;
import com.xgs.water.vo.DeviceSearchResult;
import com.xgs.water.vo.WaterDispenserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * 全园设备快速检索服务。
 *
 * 设计要点：
 * 1. 区域树的子树展开与完整路径都在 SQL 内以递归 CTE + 等值 JOIN 完成，
 *    每次检索固定为少量 SQL，绝不对每行单独查树，也不把设备整体读入内存。
 * 2. 排序为“排序字段 + 唯一 id”的全序键集分页，连续翻页不重复、不跳过、不抖动。
 * 3. 游标携带筛选+排序指纹，更换任一筛选条件或排序时旧游标必然失效，从起点开始。
 * 4. Redis 不可用时整体退化为直接查库；缓存只缓存最终结果（行/字段/顺序/总数），
 *    缓存恢复后相同条件返回内容不变。
 */
@Service
public class DeviceSearchService {

    private static final Logger log = LoggerFactory.getLogger(DeviceSearchService.class);

    /** 出水类型取值集合，与建档时的枚举保持一致 */
    private static final Set<String> WATER_TYPES = Set.of("冷水", "热水", "温水", "冰水");
    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final long CACHE_TTL_SECONDS = 60;

    private final WaterDispenserMapper deviceMapper;
    private final PageTokenService pageTokenService;
    private final SearchCacheClient cacheClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            // 注册 JSR-310 以支持 LocalDateTime/LocalDate；缓存内部格式与外部响应解耦，
            // 无论命中与否，最终都由 Spring MVC 用同一配置序列化给前端
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public DeviceSearchService(WaterDispenserMapper deviceMapper,
                               PageTokenService pageTokenService,
                               SearchCacheClient cacheClient) {
        this.deviceMapper = deviceMapper;
        this.pageTokenService = pageTokenService;
        this.cacheClient = cacheClient;
    }

    public DeviceSearchResult search(Long groupId, String deviceNo, String model, String spec,
                                     String waterType, Integer status, Integer pendingRetest,
                                     String sortField, String sortDir,
                                     Integer pageSize, String cursor) {
        DeviceSearchQuery q = buildQuery(groupId, deviceNo, model, spec, waterType,
                status, pendingRetest, sortField, sortDir, pageSize);

        if (cursor != null && !cursor.isBlank()) {
            // 指纹不一致/过期/签名错误都会得到明确的 410，由前端保留条件、回到起点
            PageTokenService.CursorPayload payload =
                    pageTokenService.parseAndVerify(cursor, q.getFingerprint());
            if (!q.getSortField().equals(payload.sortField)) {
                throw new BusinessException(PageTokenService.CODE_STALE_CURSOR,
                        "排序方式已变化，已为您从新结果的第一页开始。");
            }
            q.setCursorPresent(true);
            q.setCursorId(payload.id);
            q.setCursorValueNull(payload.valNull);
            q.setCursorValue(convertCursorValue(q.getSortField(), payload.val));
        }

        long cacheVersion = cacheClient.currentVersion();
        String cacheKey = buildCacheKey(q, cursor, cacheVersion);

        String cached = cacheVersion > 0 ? cacheClient.get(cacheKey) : null;
        if (cached != null) {
            DeviceSearchResult hit = deserialize(cached);
            if (hit != null) {
                hit.setSource("cache");
                return hit;
            }
        }

        DeviceSearchResult result = queryFromDb(q);
        String json = serialize(result);
        if (json != null) {
            cacheClient.set(cacheKey, json, CACHE_TTL_SECONDS);
        }
        return result;
    }

    private DeviceSearchResult queryFromDb(DeviceSearchQuery q) {
        List<WaterDispenserVO> rows = deviceMapper.searchPage(q);
        long total = deviceMapper.searchCount(q);

        boolean hasMore = rows.size() > q.getPageSize();
        if (hasMore) {
            rows = new ArrayList<>(rows.subList(0, q.getPageSize()));
        }

        DeviceSearchResult result = new DeviceSearchResult();
        result.setRecords(rows);
        result.setTotal(total);
        result.setPageSize(q.getPageSize());
        result.setHasMore(hasMore);
        result.setSortField(q.getSortField());
        result.setSortDir(q.getSortDir());
        result.setFingerprint(q.getFingerprint());
        result.setSource("db");
        if (hasMore) {
            WaterDispenserVO last = rows.get(rows.size() - 1);
            result.setNextCursor(issueCursor(q, last));
        }
        return result;
    }

    private String issueCursor(DeviceSearchQuery q, WaterDispenserVO last) {
        Object value = extractSortValue(q.getSortField(), last);
        boolean valueNull = value == null;
        return pageTokenService.issue(
                q.getFingerprint(), q.getSortField(), value, valueNull, last.getId());
    }

    private Object extractSortValue(String sortField, WaterDispenserVO vo) {
        return switch (sortField) {
            case "deviceNo" -> vo.getDeviceNo();
            case "model" -> vo.getModel();
            // LocalDate/LocalDateTime 以字符串写入游标，保证自包含且可签名
            case "installDate" -> vo.getInstallDate() == null
                    ? null : vo.getInstallDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            default -> vo.getCreateTime() == null
                    ? null : vo.getCreateTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        };
    }

    private Object convertCursorValue(String sortField, Object val) {
        if (val == null) {
            return null;
        }
        if ("installDate".equals(sortField)) {
            return LocalDate.parse(val.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if ("createTime".equals(sortField)) {
            return Long.parseLong(val.toString());
        }
        return val.toString();
    }

    private DeviceSearchQuery buildQuery(Long groupId, String deviceNo, String model, String spec,
                                         String waterType, Integer status, Integer pendingRetest,
                                         String sortField, String sortDir, Integer pageSize) {
        DeviceSearchQuery q = new DeviceSearchQuery();
        q.setGroupId(groupId);
        q.setDeviceNo(trimToNull(deviceNo));
        q.setModel(trimToNull(model));
        q.setSpec(trimToNull(spec));
        if (waterType != null && !waterType.isBlank()) {
            String wt = waterType.trim();
            if (!WATER_TYPES.contains(wt)) {
                throw new BusinessException("出水类型取值不合法");
            }
            q.setWaterType(wt);
        }
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException("启用状态取值不合法");
        }
        q.setStatus(status);
        if (pendingRetest != null && pendingRetest != 0 && pendingRetest != 1) {
            throw new BusinessException("待复检标记取值不合法");
        }
        q.setPendingRetest(pendingRetest);

        String field = (sortField == null || sortField.isBlank()) ? "createTime" : sortField.trim();
        if (!DeviceSearchQuery.SORT_COLUMNS.containsKey(field)) {
            throw new BusinessException("排序字段不合法");
        }
        String dir = (sortDir == null || sortDir.isBlank()) ? "DESC" : sortDir.trim().toUpperCase();
        if (!"ASC".equals(dir) && !"DESC".equals(dir)) {
            throw new BusinessException("排序方向不合法");
        }
        q.setSortField(field);
        q.setSortColumn(DeviceSearchQuery.SORT_COLUMNS.get(field));
        q.setSortDir(dir);

        int size = (pageSize == null || pageSize <= 0) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        q.setPageSize(size);
        // 多取一行判定 hasMore
        q.setLimit(size + 1);

        q.setFingerprint(buildFingerprint(q));
        return q;
    }

    /**
     * 筛选条件+排序的规范化指纹。顺序、空值处理固定，
     * 不含页码/游标/页大小：相同条件不同页共享指纹，任一条件变化指纹即变。
     */
    private String buildFingerprint(DeviceSearchQuery q) {
        StringBuilder sb = new StringBuilder("g=").append(nz(q.getGroupId()));
        sb.append("|dn=").append(nz(q.getDeviceNo()));
        sb.append("|m=").append(nz(q.getModel()));
        sb.append("|s=").append(nz(q.getSpec()));
        sb.append("|wt=").append(nz(q.getWaterType()));
        sb.append("|st=").append(nz(q.getStatus()));
        sb.append("|pr=").append(nz(q.getPendingRetest()));
        sb.append("|sf=").append(q.getSortField());
        sb.append("|sd=").append(q.getSortDir());
        return sb.toString();
    }

    private String buildCacheKey(DeviceSearchQuery q, String cursor, long version) {
        // 指纹已规范化，直接 SHA-256 定长化，避免键过长；版本号前缀负责写后失效
        String raw = q.getFingerprint() + "|ps=" + q.getPageSize() + "|c=" + (cursor == null ? "" : cursor);
        String hash = sha256(raw);
        return version + ":" + hash;
    }

    private String sha256(String raw) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String serialize(DeviceSearchResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("检索结果序列化失败，跳过本次缓存: {}", e.getMessage());
            return null;
        }
    }

    private DeviceSearchResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<DeviceSearchResult>() {
            });
        } catch (Exception e) {
            log.warn("检索缓存内容解析失败，回退数据库: {}", e.getMessage());
            return null;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String nz(Object o) {
        return o == null ? "" : o.toString();
    }
}
