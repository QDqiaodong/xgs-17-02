package com.xgs.water.search;

import com.xgs.water.dto.DeviceSearchQuery;
import com.xgs.water.entity.BuildingGroup;
import com.xgs.water.exception.BusinessException;
import com.xgs.water.exception.CursorExpiredException;
import com.xgs.water.mapper.BuildingGroupClosureMapper;
import com.xgs.water.service.BuildingGroupService;
import com.xgs.water.service.CursorCodec;
import com.xgs.water.service.DeviceSearchService;
import com.xgs.water.service.RedisSearchCache;
import com.xgs.water.service.SearchCache;
import com.xgs.water.vo.DeviceFacetVO;
import com.xgs.water.vo.DeviceSearchItem;
import com.xgs.water.vo.DeviceSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全园设备快速检索验收（H2 MySQL 模式）：
 * 1. 十万级设备全量键集翻页：不重复、不跳过、顺序稳定（主排序值大量相同也不抖动）；
 * 2. 组合筛选：区域含全部下级、编号前缀、型号、规格、出水类型、状态、待复检；
 * 3. 游标：过期 410、篡改拒绝、跨查询指纹隔离；
 * 4. 缓存：宕机自动降级直查 DB，规则/字段/顺序不变；恢复后同源一致；
 * 5. 查询次数：每页固定 1 条检索 SQL，区域解析最多 1 条闭包 SQL，零逐行查树。
 */
@SpringBootTest
@Import(SearchTestConfig.class)
class DeviceSearchIntegrationTest {

    private static final int TOTAL = 100_000;
    /** 额外构造的空安装日期设备数（install_date 可空，验证 NULL 排序与键集谓词不丢行） */
    private static final int NULL_DATE_EXTRA = 5;
    private static final int TOTAL_WITH_EXTRA = TOTAL + NULL_DATE_EXTRA;
    private static final int PAGE = 200;

    private static final String[] MODELS = {"美的YR-5", "沁园YR-10", "安吉尔J26", "海尔HRO50", "九阳JYW-RO"};
    private static final String[] SPECS = {"立式冷热型", "立式冰热型", "台式温热型", "台式冷热型"};
    private static final String[] WATERS = {"冷水,热水,温水", "冷水,热水,冰水", "温水,热水", "冷水,热水,冰水,温水"};

    /** create_time 只有 20 个不同值 => 平均每个排序值 5000 台，强制检验 id 决胜键 */
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 8, 0, 0);

    /** 全部楼层 id（设备均匀挂在这些楼层上） */
    private static final List<Long> FLOOR_IDS = new ArrayList<>();
    private static Long parkAId;
    private static Long buildingA1Id;
    private static boolean seeded = false;

    @Autowired private DeviceSearchService searchService;
    @Autowired private BuildingGroupService groupService;
    @Autowired private BuildingGroupClosureMapper closureMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SearchCache searchCache;
    @Autowired private SearchQueryCounter counter;
    @Autowired private CursorCodec cursorCodec;

    @MockBean
    private RedisSearchCache redisSearchCache;

    @BeforeEach
    void setup() {
        if (!seeded) {
            seedTree();
            seedDevices();
            seeded = true;
        }
        if (searchCache instanceof InMemorySearchCache mem) {
            mem.clear();
            mem.setOutage(false);
        }
        counter.reset();
    }

    // ---------------- 数据构造 ----------------

    private void seedTree() {
        // 2 个园区，每个 3 栋楼，每栋 4 层 = 24 个楼层
        for (int p = 1; p <= 2; p++) {
            Long parkId = createGroup(0L, "园区" + p, 1);
            if (p == 1) parkAId = parkId;
            for (int b = 1; b <= 3; b++) {
                Long buildingId = createGroup(parkId, p + "号园区-" + b + "号楼", 2);
                if (p == 1 && b == 1) buildingA1Id = buildingId;
                for (int f = 1; f <= 4; f++) {
                    FLOOR_IDS.add(createGroup(buildingId, f + "层", 3));
                }
            }
        }
        assertEquals(24, FLOOR_IDS.size());
        // 每个区域有自身关系 => 闭包行数应远多于区域数
        assertTrue(closureMapper.countAll() >= 2 + 6 + 24);
    }

    private Long createGroup(Long parentId, String name, int type) {
        BuildingGroup g = new BuildingGroup();
        g.setParentId(parentId);
        g.setName(name);
        g.setType(type);
        g.setSortOrder(0);
        groupService.save(g); // 内部维护闭包关系
        return g.getId();
    }

    private void seedDevices() {
        // 分批参数化批量插入，10 万行控制在秒级
        String sql = "INSERT INTO water_dispenser " +
                "(device_no, model, spec, water_type, group_id, status, pending_retest, install_date, remark, create_time, update_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        int batch = 1000;
        List<Object[]> rows = new ArrayList<>(batch);
        for (int i = 1; i <= TOTAL; i++) {
            long floorId = FLOOR_IDS.get(i % FLOOR_IDS.size());
            String deviceNo = String.format("WD%08d", i);
            String model = MODELS[i % MODELS.length];
            String spec = SPECS[i % SPECS.length];
            String water = WATERS[i % WATERS.length];
            int status = (i % 10 == 0) ? 0 : 1;                 // 10% 停用
            int pending = (i % 7 == 0) ? 1 : 0;                 // 约 14% 待复检
            java.sql.Date installDate = java.sql.Date.valueOf(LocalDate.of(2024, 1, 1).plusDays(i % 400));
            LocalDateTime createTime = BASE_TIME.plusMinutes(i % 20); // 仅 20 个不同时间
            java.sql.Timestamp ts = java.sql.Timestamp.valueOf(createTime);
            rows.add(new Object[]{deviceNo, model, spec, water, floorId, status, pending,
                    installDate, null, ts, ts});
            if (rows.size() == batch) {
                jdbcTemplate.batchUpdate(sql, rows);
                rows.clear();
            }
        }
        if (!rows.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, rows);
        }

        // 少量安装日期为 NULL 的设备：验证可空排序列的键集翻页不丢 NULL 行
        List<Object[]> extra = new ArrayList<>();
        for (int i = 1; i <= NULL_DATE_EXTRA; i++) {
            long floorId = FLOOR_IDS.get(i);
            LocalDateTime createTime = BASE_TIME.plusMinutes(25 + i); // 时间也不同于主数据
            java.sql.Timestamp ts = java.sql.Timestamp.valueOf(createTime);
            extra.add(new Object[]{String.format("WD9%07d", i), MODELS[i % MODELS.length],
                    SPECS[i % SPECS.length], WATERS[i % WATERS.length], floorId, 1, 0,
                    null, null, ts, ts});
        }
        jdbcTemplate.batchUpdate(sql, extra);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM water_dispenser", Integer.class);
        assertEquals(TOTAL_WITH_EXTRA, count.intValue());
    }

    private DeviceSearchQuery query() {
        DeviceSearchQuery q = new DeviceSearchQuery();
        q.setPageSize(PAGE);
        return q;
    }

    /** 按游标连续翻完整个结果集，返回有序 id 列表 */
    private List<Long> walkAll(DeviceSearchQuery template) {
        List<Long> ids = new ArrayList<>();
        String cursor = null;
        int guard = 0;
        while (true) {
            template.setCursor(cursor);
            DeviceSearchResult r = searchService.search(template);
            for (DeviceSearchItem item : r.getRecords()) {
                ids.add(item.getId());
            }
            if (!r.isHasMore()) {
                assertNull(r.getNextCursor());
                break;
            }
            assertNotNull(r.getNextCursor());
            cursor = r.getNextCursor();
            if (++guard > TOTAL_WITH_EXTRA / PAGE + 10) {
                fail("翻页超过预期上限，可能出现重复死循环");
            }
        }
        return ids;
    }

    // ---------------- 1. 翻页稳定性（核心验收） ----------------

    @Test
    void fullPagination_noDuplicateNoSkipStableOrder() {
        DeviceSearchQuery q = query(); // 默认 createTime DESC, id DESC
        counter.reset();
        List<Long> ids = walkAll(q);

        assertEquals(TOTAL_WITH_EXTRA, ids.size(), "必须遍历全部设备（含空安装日期设备）");
        assertEquals(TOTAL_WITH_EXTRA, new HashSet<>(ids).size(), "连续翻页同一台设备不得重复出现");

        // 用独立的一次性全量查询拿到“数据库认定的确定序”，逐行核对，不得跳过/乱序
        List<Long> expected = jdbcTemplate.queryForList(
                "SELECT id FROM water_dispenser ORDER BY CAST(create_time AS DATETIME) DESC, id DESC",
                Long.class);
        assertEquals(expected, ids, "键集翻页顺序必须与确定序完全一致");

        // 每页只有 1 条设备检索 SQL：禁止逐行查树、禁止全量读入内存
        long pages = (TOTAL_WITH_EXTRA + PAGE - 1) / PAGE;
        assertEquals(pages, counter.deviceSearchQueries(),
                "每次翻页恰好 1 条检索 SQL，共 " + pages + " 页");
    }

    @Test
    void tieOnPrimarySort_idBreaksTieConsistently() {
        // 第一页 200 台共享同一 create_time 的概率极高（每个时间值约 5000 台）
        DeviceSearchQuery q = query();
        DeviceSearchResult first = searchService.search(q);
        LocalDateTime firstTime = first.getRecords().get(0).getCreateTime();

        long sameTimeCount = first.getRecords().stream()
                .filter(d -> d.getCreateTime().equals(firstTime)).count();
        assertTrue(sameTimeCount > 1, "测试数据必须构造出主排序值相同的多行");

        // 同页内 id 必须严格降序跟随
        List<Long> pageIds = first.getRecords().stream().map(DeviceSearchItem::getId).toList();
        for (int i = 1; i < pageIds.size(); i++) {
            assertTrue(pageIds.get(i - 1) > pageIds.get(i),
                    "主排序值相同时 id 必须稳定决胜，页内不得抖动");
        }
    }

    @Test
    void nullInstallDate_rowsAreNeverSkipped() {
        Integer nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM water_dispenser WHERE install_date IS NULL", Integer.class);
        assertEquals(NULL_DATE_EXTRA, nullCount.intValue());

        // installDate 升降序都必须把 NULL 行完整翻出来（总数已由 everySortDirection 覆盖，这里核对尾部）
        DeviceSearchQuery qDesc = query();
        qDesc.setSort("installDate");
        qDesc.setOrder("desc");
        List<Long> descIds = walkAll(qDesc);
        assertEquals(TOTAL_WITH_EXTRA, descIds.size());

        // NULL 在降序最后：连续翻完后 NULL 设备必须确实出现
        DeviceSearchQuery onlyNull = query();
        // 通过编号前缀隔离空日期设备（WD9xxxxxx），再按安装日期降序
        onlyNull.setDeviceNo("WD9");
        DeviceSearchResult r = searchService.search(onlyNull);
        assertEquals(NULL_DATE_EXTRA, r.getRecords().size());
        assertTrue(r.getRecords().stream().allMatch(d -> d.getInstallDate() == null));
    }

    @Test
    void everySortDirection_isDeterministic() {
        for (String sort : List.of("createTime", "deviceNo", "model", "installDate")) {
            for (String order : List.of("desc", "asc")) {
                DeviceSearchQuery q = query();
                q.setSort(sort);
                q.setOrder(order);
                counter.reset();
                List<Long> ids = walkAll(q);
                assertEquals(TOTAL_WITH_EXTRA, ids.size(), sort + "/" + order + " 应覆盖全部设备");
                assertEquals(TOTAL_WITH_EXTRA, new HashSet<>(ids).size(),
                        sort + "/" + order + " 不得重复");
            }
        }
    }

    // ---------------- 2. 组合筛选 ----------------

    @Test
    void closure_ancestorsAndDescendants_areComplete() {
        // 园区A：自身 + 3 楼栋 + 12 楼层 = 16 个后代
        List<Long> descendants = closureMapper.selectDescendantIds(parkAId);
        assertEquals(16, descendants.size());
        assertTrue(descendants.contains(parkAId));
        assertTrue(descendants.contains(buildingA1Id));

        // 任意楼层的祖先链：楼层 -> 楼栋 -> 园区，共 3 个
        Long aFloorId = FLOOR_IDS.get(0);
        List<Long> ancestors = closureMapper.selectAncestorIds(aFloorId);
        assertEquals(3, ancestors.size());
        assertEquals(parkAId, ancestors.get(0));
        assertEquals(aFloorId, ancestors.get(2));
    }

    @Test
    void groupFilter_includesAllDescendants_andPathRenderedInSql() {
        // 选园区A：1 园区 * 3 栋 * 4 层 = 12 个楼层，占一半设备
        DeviceSearchQuery q = query();
        q.setGroupId(parkAId);

        counter.reset();
        DeviceSearchResult page = searchService.search(q);
        // 区域解析只允许 1 条闭包 SQL（一次性取全部下级）
        assertTrue(counter.closureQueries() <= 1L, "选中区域只允许一次闭包子树查询");
        assertEquals(1L, counter.deviceSearchQueries());

        List<Long> ids = walkAll(q);
        Integer parkCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM water_dispenser WHERE group_id IN " +
                        "(SELECT descendant_id FROM building_group_closure WHERE ancestor_id = ?)",
                Integer.class, parkAId);
        assertEquals(parkCount.intValue(), ids.size(), "选园区必须包含其全部下级区域内的设备");

        // 路径由 SQL JOIN 产出，每行都完整；不允许出现后端逐行补路径
        for (DeviceSearchItem d : page.getRecords()) {
            assertNotNull(d.getGroupPath());
            assertTrue(d.getGroupPath().startsWith("园区1"),
                    "结果必须显示完整区域路径，实际：" + d.getGroupPath());
            assertTrue(d.getGroupPath().contains("号楼"), "路径需含楼栋层级");
            assertTrue(d.getGroupPath().endsWith("层"), "路径需含楼层层级");
        }

        // 选单栋楼：与闭包子树设备数一致
        DeviceSearchQuery buildingQ = query();
        buildingQ.setGroupId(buildingA1Id);
        Integer buildingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM water_dispenser WHERE group_id IN " +
                        "(SELECT descendant_id FROM building_group_closure WHERE ancestor_id = ?)",
                Integer.class, buildingA1Id);
        assertEquals(buildingCount.intValue(), walkAll(buildingQ).size());
    }

    @Test
    void combinedFilters_matchDatabaseSemantics() {
        DeviceSearchQuery q = query();
        q.setGroupId(parkAId);
        q.setDeviceNo("WD00001"); // 前缀匹配：命中编号 WD00001xx（100 台）与精确的 00000001
        q.setModel("美的YR-5");
        q.setSpec("立式冷热型");
        q.setWaterType("温水");
        q.setStatus(1);
        q.setPendingRetest(0);

        DeviceSearchResult page = searchService.search(q);
        for (DeviceSearchItem d : page.getRecords()) {
            assertTrue(d.getDeviceNo().startsWith("WD00001"));
            assertEquals("美的YR-5", d.getModel());
            assertEquals("立式冷热型", d.getSpec());
            assertTrue(Arrays.asList(d.getWaterType().split(",")).contains("温水"));
            assertEquals(1, d.getStatus());
            assertEquals(0, d.getPendingRetest());
            assertEquals("正常", d.getStatusName());
        }

        // 与数据库同语义统计一致（用相同条件独立核对）
        Long expected = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM water_dispenser wd " +
                        "WHERE wd.group_id IN (SELECT descendant_id FROM building_group_closure WHERE ancestor_id = ?) " +
                        "AND wd.device_no LIKE CONCAT(?,'%') AND wd.model = ? AND wd.spec = ? " +
                        "AND CONCAT(',', COALESCE(wd.water_type,''), ',') LIKE CONCAT('%,',?,'%') " +
                        "AND wd.status = 1 AND wd.pending_retest = 0",
                Long.class, parkAId, "WD00001", "美的YR-5", "立式冷热型", "温水");
        List<Long> ids = walkAll(q);
        assertEquals(expected.intValue(), ids.size());
    }

    @Test
    void pendingRetestFilter_includesDisabledDevices_orthogonalStatus() {
        DeviceSearchQuery q = query();
        q.setPendingRetest(1);
        List<Long> ids = walkAll(q);
        Long expectedPending = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM water_dispenser WHERE pending_retest = 1", Long.class);
        assertEquals(expectedPending.intValue(), ids.size());
        assertTrue(ids.size() > 0);
    }

    @Test
    void invalidFilterValues_rejected() {
        DeviceSearchQuery q = query();
        q.setStatus(9);
        assertThrows(BusinessException.class, () -> searchService.search(q));
        DeviceSearchQuery q2 = query();
        q2.setWaterType("毒水");
        // 非法出水类型归一化为“不限”，不报错也不拼 SQL
        DeviceSearchResult r = searchService.search(q2);
        assertEquals(PAGE, r.getRecords().size());
    }

    // ---------------- 3. 游标安全 ----------------

    @Test
    void expiredCursor_keepsFiltersAndSignalsRestart() {
        DeviceSearchQuery q = query();
        q.setModel("美的YR-5");
        DeviceSearchResult first = searchService.search(q);
        DeviceSearchItem last = first.getRecords().get(PAGE - 1);

        // 从有效游标取出指纹，再把签发时间改到 TTL 之前，重新签名 => 合法但过期
        com.xgs.water.dto.CursorToken valid = cursorCodec.decode(first.getNextCursor(), false);
        com.xgs.water.dto.CursorToken old = new com.xgs.water.dto.CursorToken(
                last.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                last.getId(),
                System.currentTimeMillis() - cursorCodec.getTtlMs() - 1000,
                valid.getFingerprint());
        String expired = cursorCodec.encode(old);

        // 筛选条件保持不变（仍是 美的YR-5），仅游标过期
        DeviceSearchQuery again = query();
        again.setModel("美的YR-5");
        again.setCursor(expired);
        CursorExpiredException ex = assertThrows(CursorExpiredException.class,
                () -> searchService.search(again));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void tamperedCursor_rejected() {
        DeviceSearchResult first = searchService.search(query());
        String cursor = first.getNextCursor();
        String tampered = cursor.substring(0, cursor.length() - 2)
                + (cursor.endsWith("a") ? "b" : "a");

        DeviceSearchQuery q = query();
        q.setCursor(tampered);
        BusinessException ex = assertThrows(BusinessException.class, () -> searchService.search(q));
        assertEquals(410, ex.getCode());
    }

    @Test
    void changedFilter_oldCursorRejected_mustRestartFromBeginning() {
        DeviceSearchQuery q1 = query();
        q1.setModel("美的YR-5");
        String cursorFromModelFilter = searchService.search(q1).getNextCursor();

        // 更换型号后沿用旧游标 => 指纹不一致，必须拒绝并从起点开始
        DeviceSearchQuery q2 = query();
        q2.setModel("沁园YR-10");
        q2.setCursor(cursorFromModelFilter);
        BusinessException ex = assertThrows(BusinessException.class, () -> searchService.search(q2));
        assertEquals(410, ex.getCode());

        // 不带游标从起点检索正常
        DeviceSearchQuery q3 = query();
        q3.setModel("沁园YR-10");
        DeviceSearchResult restart = searchService.search(q3);
        assertEquals(PAGE, restart.getRecords().size());
        assertEquals("沁园YR-10", restart.getRecords().get(0).getModel());
    }

    @Test
    void sortingChange_resetsToStart() {
        DeviceSearchQuery qDesc = query();
        DeviceSearchResult descPage = searchService.search(qDesc);
        DeviceSearchQuery qAsc = query();
        qAsc.setOrder("asc");
        // 旧游标来自 desc；asc 的指纹不同
        qAsc.setCursor(descPage.getNextCursor());
        assertThrows(BusinessException.class, () -> searchService.search(qAsc));
    }

    // ---------------- 4. 缓存降级与恢复 ----------------

    @Test
    void cacheOutage_fallsBackToDb_sameRulesOrderAndFields() {
        InMemorySearchCache mem = (InMemorySearchCache) searchCache;

        // 用小的确定性结果集，避免十万级全表首页在两次查询间隙受数据变化影响
        DeviceSearchQuery q = query();
        q.setDeviceNo("WD0000005");
        DeviceSearchResult db1 = searchService.search(q);
        assertEquals("db", db1.getSource());
        assertTrue(db1.getRecords().size() > 1);
        DeviceSearchResult cached = searchService.search(q);
        assertEquals("cache", cached.getSource());
        assertRowsEqual(db1.getRecords(), cached.getRecords(), "命中缓存的字段与顺序必须与直查一致");

        // 缓存服务不可用：自动直查 DB
        mem.setOutage(true);
        DeviceSearchResult down = searchService.search(q);
        assertEquals("db", down.getSource());
        assertRowsEqual(db1.getRecords(), down.getRecords(),
                "缓存降级后过滤规则、字段、排序不得变化");

        // 恢复后：冷启动先直查回填，再查命中缓存，仍与最初 DB 结果一致
        mem.setOutage(false);
        DeviceSearchResult db2 = searchService.search(q);
        assertEquals("db", db2.getSource());
        assertRowsEqual(db1.getRecords(), db2.getRecords(), "缓存恢复后来源不同结果也必须一致");
        DeviceSearchResult cached2 = searchService.search(q);
        assertEquals("cache", cached2.getSource());
        assertRowsEqual(db1.getRecords(), cached2.getRecords());
    }

    @Test
    void facets_areStableAcrossCacheToggle() {
        InMemorySearchCache mem = (InMemorySearchCache) searchCache;
        DeviceFacetVO db = searchService.facets();
        assertEquals("db", db.getSource());
        assertEquals(MODELS.length, db.getModels().size());
        assertTrue(db.getSpecs().containsAll(Arrays.asList(SPECS)));
        assertEquals(List.of("冷水", "热水", "温水", "冰水"), db.getWaterTypes());

        DeviceFacetVO cached = searchService.facets();
        assertEquals("cache", cached.getSource());
        assertEquals(db.getModels(), cached.getModels());
        assertEquals(db.getSpecs(), cached.getSpecs());

        mem.setOutage(true);
        DeviceFacetVO down = searchService.facets();
        assertEquals("db", down.getSource());
        assertEquals(db.getModels(), down.getModels());
        assertEquals(db.getSpecs(), down.getSpecs());
    }

    private void assertRowsEqual(List<DeviceSearchItem> a, List<DeviceSearchItem> b, String msg) {
        assertEquals(a.size(), b.size(), msg + "（行数不同）");
        for (int i = 0; i < a.size(); i++) {
            DeviceSearchItem x = a.get(i);
            DeviceSearchItem y = b.get(i);
            assertEquals(x.getId(), y.getId(), msg + "（第" + i + "行设备不同）");
            assertEquals(x.getDeviceNo(), y.getDeviceNo());
            assertEquals(x.getModel(), y.getModel());
            assertEquals(x.getSpec(), y.getSpec());
            assertEquals(x.getWaterType(), y.getWaterType());
            assertEquals(x.getGroupPath(), y.getGroupPath(), msg + "（区域路径不同）");
            assertEquals(x.getStatusName(), y.getStatusName());
            assertEquals(x.getPendingRetest(), y.getPendingRetest());
            assertEquals(x.getInstallDate(), y.getInstallDate());
        }
    }

    // ---------------- 5. 结果集边界 ----------------

    @Test
    void lastPage_sizesAndHasMoreCorrect() {
        // 编号前缀精确到唯一条目：尾页仅 1 行、无下一页、无游标
        DeviceSearchQuery q = query();
        q.setDeviceNo("WD00099999");
        DeviceSearchResult r = searchService.search(q);
        assertEquals(1, r.getRecords().size());
        assertFalse(r.isHasMore());
        assertNull(r.getNextCursor());
        assertEquals("WD00099999", r.getRecords().get(0).getDeviceNo());
    }

    @Test
    void pageSize_cappedAt200_neverLoadsWholeTable() {
        // 不允许通过超大 pageSize 把全部设备一次读入内存：服务端钳制为 200（LIMIT 201 判尾）
        DeviceSearchQuery q = new DeviceSearchQuery();
        q.setPageSize(10_000);
        DeviceSearchResult r = searchService.search(q);
        assertEquals(200, r.getPageSize());
        assertEquals(200, r.getRecords().size());
        assertTrue(r.isHasMore());
    }
}
