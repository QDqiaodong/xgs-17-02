-- ============================================================
-- 全园设备快速检索 —— 旧库升级脚本
-- 文件名以 zz_ 开头，确保在 init.sql、wq_inspection.sql 之后执行。
-- 全新数据卷会自动执行；已存在数据的旧库也可手工执行本文件，全部幂等。
-- ============================================================
USE xgs_water;

-- 1) 区域闭包表
CREATE TABLE IF NOT EXISTS building_group_closure (
    ancestor_id   BIGINT NOT NULL COMMENT '祖先区域ID',
    descendant_id BIGINT NOT NULL COMMENT '后代区域ID（含祖先自身）',
    distance      INT    NOT NULL COMMENT '层级距离：0=自身，1=直接子级，依次递增',
    PRIMARY KEY (ancestor_id, descendant_id),
    INDEX idx_descendant (descendant_id, ancestor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域树祖先-后代闭包表';

-- 2) 缺失的闭包关系按递归 CTE 幂等补齐（应用启动时也会做一次同样的回填）
INSERT INTO building_group_closure (ancestor_id, descendant_id, distance)
WITH RECURSIVE rcte AS (
    SELECT id AS ancestor_id, id AS descendant_id, 0 AS distance
    FROM building_group
    UNION ALL
    SELECT r.ancestor_id, g.id AS descendant_id, r.distance + 1
    FROM rcte r
    JOIN building_group g ON g.parent_id = r.descendant_id
)
SELECT r.ancestor_id, r.descendant_id, r.distance
FROM rcte r
WHERE NOT EXISTS (
    SELECT 1 FROM building_group_closure c
    WHERE c.ancestor_id = r.ancestor_id AND c.descendant_id = r.descendant_id
);

-- 3) 检索索引（按名判断，缺失才加）
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'water_dispenser' AND INDEX_NAME = 'idx_status_pending');
SET @ddl := IF(@idx_exists = 0, 'ALTER TABLE water_dispenser ADD INDEX idx_status_pending (status, pending_retest)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'water_dispenser' AND INDEX_NAME = 'idx_model');
SET @ddl := IF(@idx_exists = 0, 'ALTER TABLE water_dispenser ADD INDEX idx_model (model)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'water_dispenser' AND INDEX_NAME = 'idx_install_date');
SET @ddl := IF(@idx_exists = 0, 'ALTER TABLE water_dispenser ADD INDEX idx_install_date (install_date)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'water_dispenser' AND INDEX_NAME = 'idx_create_id');
SET @ddl := IF(@idx_exists = 0, 'ALTER TABLE water_dispenser ADD INDEX idx_create_id (create_time, id)', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
