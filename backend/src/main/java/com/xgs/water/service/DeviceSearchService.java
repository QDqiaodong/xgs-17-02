package com.xgs.water.service;

import com.xgs.water.dto.CursorToken;
import com.xgs.water.dto.DeviceSearchParams;
import com.xgs.water.dto.DeviceSearchQuery;
import com.xgs.water.exception.BusinessException;
import com.xgs.water.exception.CursorExpiredException;
import com.xgs.water.mapper.DeviceSearchMapper;
import com.xgs.water.vo.DeviceFacetVO;
import com.xgs.water.vo.DeviceSearchItem;
import com.xgs.water.vo.DeviceSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 全园设备快速检索服务。
 *
 * 稳定性要点：
 * 1. 排序永远是 (主排序值, id) 二元键，id 唯一 => 任何主排序值相同都不会页间抖动；
 * 2. 键集（seek）分页不用 OFFSET，连续向后翻页不重不漏，十万级数据深翻页性能恒定；
 * 3. 游标 HMAC 签名 + TTL + 查询指纹：篡改无效、过期明确报错、换条件必回起点；
 * 4. 区域路径由 SQL JOIN 直接产出，零逐行查树；
 * 5. Redis 故障透明降级直查 DB，缓存数据是 DB 结果原样序列化，来源不同不改变结果。
 */
@Service
public class DeviceSearchService {

    /** 出水类型固定枚举（逗号分隔存储中的原子值），不依赖全表扫描去重 */
    public static final List<String> WATER_TYPES = List.of("冷水", "热水", "温水", "冰水");

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 排序字段白名单：前端值 -> 实际排序表达式（时间统一截到秒，保证游标值可精确比较） */
    private static final Map<String, String> SORT_EXPR = Map.of(
            "createTime", "CAST(wd.create_time AS DATETIME)",
            "deviceNo", "wd.device_no",
            "model", "wd.model",
            "installDate", "wd.install_date"
    );

    @Autowired
    private DeviceSearchMapper searchMapper;
    @Autowired
    private BuildingGroupService buildingGroupService;
    @Autowired
    private CursorCodec cursorCodec;
    @Autowired
    private SearchCache searchCache;

    public DeviceSearchResult search(DeviceSearchQuery query) {
        String sortKey = normalizeSort(query.getSort());
        boolean asc = "asc".equalsIgnoreCase(query.getOrder());
        int pageSize = normalizePageSize(query.getPageSize());
        String trimmedDeviceNo = trim(query.getDeviceNo());
        String model = trim(query.getModel());
        String spec = trim(query.getSpec());
        String waterType = normalizeWaterType(query.getWaterType());
        Integer status = normalizeStatus(query.getStatus());
        Integer pendingRetest = normalizePendingRetest(query.getPendingRetest());

        // 区域范围：选中节点及其全部下级（闭包表单次查询）
        List<Long> groupIds = query.getGroupId() == null ? null
                : buildingGroupService.getAllChildGroupIds(query.getGroupId());

        // 条件指纹：游标签入同样的指纹；任何条件/排序/pageSize 变化都使旧游标失效
        String fingerprint = fingerprint(query.getGroupId(), groupIds, trimmedDeviceNo, model, spec,
                waterType, status, pendingRetest, sortKey, asc, pageSize);

        boolean firstPage = query.getCursor() == null || query.getCursor().isBlank();
        String cacheKey = fingerprint;

        if (firstPage) {
            DeviceSearchResult cached = searchCache.getSearchPage(cacheKey);
            if (cached != null) {
                cached.setSource("cache");
                return cached;
            }
        }

        DeviceSearchParams params = new DeviceSearchParams();
        params.setGroupIds(groupIds);
        params.setDeviceNo(trimmedDeviceNo);
        params.setModel(model);
        params.setSpec(spec);
        params.setWaterType(waterType);
        params.setStatus(status);
        params.setPendingRetest(pendingRetest);
        params.setSortExpr(SORT_EXPR.get(sortKey));
        params.setAsc(asc);
        params.setLimit(pageSize + 1);

        if (!firstPage) {
            CursorToken cursor = decodeAndValidateCursor(query.getCursor(), fingerprint);
            params.setHasCursor(true);
            params.setCursorId(cursor.getId());
            if (cursor.getSortValue() == null) {
                params.setCursorSortNull(true);
            } else {
                params.setCursorSortNull(false);
                params.setCursorValue(parseCursorValue(cursor.getSortValue(), sortKey));
            }
        }

        List<DeviceSearchItem> rows = searchMapper.search(params);
        DeviceSearchResult result = assemble(rows, pageSize, fingerprint, sortKey);
        result.setSource("db");

        if (firstPage) {
            // 只缓存首页：存的就是 DB 结果对象（剥离 source 标记后原样存取）
            searchCache.putSearchPage(cacheKey, cloneForCache(result));
        }
        return result;
    }

    private DeviceSearchResult assemble(List<DeviceSearchItem> rows, int pageSize,
                                        String fingerprint, String sortKey) {
        DeviceSearchResult result = new DeviceSearchResult();
        result.setPageSize(pageSize);
        boolean hasMore = rows.size() > pageSize;
        if (hasMore) {
            rows = rows.subList(0, pageSize);
            DeviceSearchItem last = rows.get(pageSize - 1);
            String sortValue = formatSortValue(last, sortKey);
            CursorToken token = new CursorToken(sortValue, last.getId(),
                    System.currentTimeMillis(), fingerprint);
            result.setNextCursor(cursorCodec.encode(token));
        }
        result.setRecords(rows);
        result.setHasMore(hasMore);
        return result;
    }

    /** 缓存对象剥离仅用于观测的来源标记，保证命中缓存后再由统一出口标注 source=cache */
    private DeviceSearchResult cloneForCache(DeviceSearchResult dbResult) {
        DeviceSearchResult copy = new DeviceSearchResult();
        copy.setRecords(dbResult.getRecords());
        copy.setNextCursor(dbResult.getNextCursor());
        copy.setHasMore(dbResult.isHasMore());
        copy.setPageSize(dbResult.getPageSize());
        copy.setSource(null);
        return copy;
    }

    private CursorToken decodeAndValidateCursor(String raw, String fingerprint) {
        CursorToken cursor;
        try {
            cursor = cursorCodec.decode(raw, true);
        } catch (CursorExpiredException e) {
            throw e;
        }
        if (cursor == null || cursor.getId() == null) {
            // 签名非法/格式损坏：等同失效，前端按“从起点重查”处理
            throw new BusinessException(410, "翻页位置已失效，可能链接已损坏，请从第一页重新检索");
        }
        if (!fingerprint.equals(cursor.getFingerprint())) {
            // 筛选条件/排序/页大小已变化：必须从新结果起点开始，不能沿用旧翻页位置
            throw new BusinessException(410, "筛选条件或排序已变化，请从第一页重新检索");
        }
        return cursor;
    }

    /** 筛选项：型号、规格库内去重；出水类型为固定枚举。缓存故障自动直查，规则不变。 */
    public DeviceFacetVO facets() {
        DeviceFacetVO cached = searchCache.getFacets(SearchCache.FACET_KEY);
        if (cached != null) {
            cached.setSource("cache");
            return cached;
        }
        DeviceFacetVO vo = new DeviceFacetVO();
        vo.setModels(searchMapper.distinctModels());
        vo.setSpecs(searchMapper.distinctSpecs());
        vo.setWaterTypes(WATER_TYPES);
        vo.setSource("db");
        DeviceFacetVO forCache = new DeviceFacetVO();
        forCache.setModels(vo.getModels());
        forCache.setSpecs(vo.getSpecs());
        forCache.setWaterTypes(vo.getWaterTypes());
        searchCache.putFacets(SearchCache.FACET_KEY, forCache);
        return vo;
    }

    // ---------------- 归一化与指纹 ----------------

    private String normalizeSort(String sort) {
        return sort != null && SORT_EXPR.containsKey(sort) ? sort : "createTime";
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String normalizeWaterType(String waterType) {
        String t = trim(waterType);
        return (t != null && WATER_TYPES.contains(t)) ? t : null;
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) return null;
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "启用状态只允许 1(正常) 或 0(停用)");
        }
        return status;
    }

    private Integer normalizePendingRetest(Integer pendingRetest) {
        if (pendingRetest == null) return null;
        if (pendingRetest != 0 && pendingRetest != 1) {
            throw new BusinessException(400, "待复检标记只允许 1 或 0");
        }
        return pendingRetest;
    }

    private static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 指纹纳入“实际解析出的区域 id 集合”，而不仅是选中节点：
     * 树结构变化（虽然本系统不允许移动节点）或选中不同层级都能正确区分查询。
     */
    private String fingerprint(Long groupId, List<Long> groupIds, String deviceNo, String model,
                               String spec, String waterType, Integer status, Integer pendingRetest,
                               String sortKey, boolean asc, int pageSize) {
        String groups = groupIds == null ? "*" : groupIds.stream().sorted().map(String::valueOf)
                .reduce((a, b) -> a + "," + b).orElse("");
        String raw = String.join("|",
                "g=" + groups,
                "dn=" + nv(deviceNo),
                "m=" + nv(model),
                "s=" + nv(spec),
                "w=" + nv(waterType),
                "st=" + (status == null ? "*" : status),
                "pr=" + (pendingRetest == null ? "*" : pendingRetest),
                "sort=" + sortKey + ":" + (asc ? "asc" : "desc"),
                "size=" + pageSize,
                "root=" + (groupId == null ? "0" : groupId));
        return sha256(raw);
    }

    private static String nv(String s) {
        return s == null ? "*" : s;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("指纹计算失败", e);
        }
    }

    private Object parseCursorValue(String value, String sortKey) {
        return switch (sortKey) {
            case "installDate" -> LocalDate.parse(value);
            case "createTime" -> LocalDateTime.parse(value, DT_FMT);
            default -> value; // device_no / model 字符串
        };
    }

    private String formatSortValue(DeviceSearchItem item, String sortKey) {
        return switch (sortKey) {
            case "deviceNo" -> item.getDeviceNo();
            case "model" -> item.getModel() == null ? null : item.getModel();
            case "installDate" -> item.getInstallDate() == null ? null : item.getInstallDate().toString();
            default -> item.getCreateTime() == null ? null : item.getCreateTime().format(DT_FMT);
        };
    }
}
