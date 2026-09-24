-- ============================================================
-- 全园设备快速检索：支撑十万级设备的稳定键集分页组合索引
-- 说明：
--  1. 区域子树展开与完整区域路径均由递归 CTE 在 SQL 内完成，不对每行单独查树。
--  2. 翻页采用“排序字段 + 唯一 id”全序键集游标，下列复合索引覆盖
--     常见筛选条件的等值前缀 + 排序尾列，保证大结果集深翻页成本恒定。
--  3. device_no 的唯一索引已存在（uk / 建表 UNIQUE），不再重复建前缀索引。
-- ============================================================
USE xgs_water;

-- 待复检筛选 + 建档时间排序（后勤排查高频路径）
CREATE INDEX idx_pending_retest_ctime ON water_dispenser (pending_retest, create_time, id);

-- 启用状态筛选 + 建档时间排序
CREATE INDEX idx_status_ctime ON water_dispenser (status, create_time, id);

-- 型号/安装规格精确筛选 + 排序
CREATE INDEX idx_model_ctime ON water_dispenser (model, create_time, id);
CREATE INDEX idx_spec_ctime ON water_dispenser (spec, create_time, id);

-- 所属区域是任何“按树范围检索”的必经过滤；前缀 region 索引
CREATE INDEX idx_group_ctime ON water_dispenser (group_id, create_time, id);

-- 其余可选排序键的 id 并列兜底
CREATE INDEX idx_install_date_id ON water_dispenser (install_date, id);
