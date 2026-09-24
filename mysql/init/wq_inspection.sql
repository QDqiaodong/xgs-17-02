-- ============================================================
-- 饮水机水质抽检与复检模块
-- ============================================================
USE xgs_water;

-- 生效阈值（后台维护，提交批次时快照固化到批次/不合格项，后续修改不影响已提交批次）
CREATE TABLE IF NOT EXISTS wq_threshold (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_code VARCHAR(32) NOT NULL COMMENT '指标编码：residual_chlorine-余氯, turbidity-浊度, temperature-温度, odor-气味',
    metric_name VARCHAR(32) NOT NULL COMMENT '指标名称',
    unit VARCHAR(16) DEFAULT '' COMMENT '计量单位',
    judge_type VARCHAR(16) NOT NULL DEFAULT 'range' COMMENT '判定方式：range-数值区间, enum-枚举结论',
    min_value DECIMAL(10,3) NULL COMMENT '数值下限（含）',
    max_value DECIMAL(10,3) NULL COMMENT '数值上限（含）',
    pass_values VARCHAR(255) DEFAULT '' COMMENT '枚举判定的合格结论，逗号分隔',
    display_order INT DEFAULT 0 COMMENT '展示顺序',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：1-是，0-否',
    update_operator VARCHAR(50) DEFAULT 'admin' COMMENT '最后维护人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_metric_code (metric_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水质指标生效阈值表';

-- 抽检批次
CREATE TABLE IF NOT EXISTS wq_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(40) NOT NULL COMMENT '批次编号',
    scope_group_id BIGINT COMMENT '发起抽检的区域节点ID（园区/楼栋/楼层）',
    scope_group_path VARCHAR(500) COMMENT '发起区域完整路径快照',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT-草稿, PENDING_REVIEW-待复核, RETURNED-退回, PENDING_RETEST-待复检, CLOSED-已关闭',
    submitter VARCHAR(50) COMMENT '提交人（采样人）',
    submit_time DATETIME COMMENT '最近一次提交时间（阈值在此刻固化）',
    reviewer VARCHAR(50) COMMENT '最近一次复核处理人',
    review_time DATETIME COMMENT '最近一次复核时间',
    return_reason VARCHAR(500) COMMENT '最近一次整批退回原因',
    threshold_snapshot TEXT COMMENT '提交时固化的全部指标阈值JSON，后续阈值调整不影响已提交批次',
    close_operator VARCHAR(50) COMMENT '关闭操作人',
    close_time DATETIME COMMENT '关闭时间',
    remark VARCHAR(500) COMMENT '备注',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（并发复核/复检防重）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_no (batch_no),
    INDEX idx_status (status),
    INDEX idx_submitter (submitter),
    INDEX idx_submit_time (submit_time),
    INDEX idx_scope_group (scope_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水质抽检批次表';

-- 批次内每台设备的采样记录（提交时固化设备编号/型号/区域路径快照）
CREATE TABLE IF NOT EXISTS wq_sample_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    device_id BIGINT NOT NULL COMMENT '饮水机ID',
    device_no_snapshot VARCHAR(50) NOT NULL COMMENT '提交时设备编号快照',
    model_snapshot VARCHAR(100) NOT NULL COMMENT '提交时型号快照',
    group_id_snapshot BIGINT COMMENT '提交时所属楼层ID快照',
    group_path_snapshot VARCHAR(500) COMMENT '提交时完整区域路径快照（历史采样位置）',
    sampler VARCHAR(50) COMMENT '采样人',
    sample_time DATETIME COMMENT '采样时间',
    residual_chlorine DECIMAL(10,3) COMMENT '余氯 mg/L',
    turbidity DECIMAL(10,3) COMMENT '浊度 NTU',
    temperature DECIMAL(10,3) COMMENT '温度 ℃',
    odor VARCHAR(100) COMMENT '气味结论',
    item_result VARCHAR(16) COMMENT 'PASS-合格, FAIL-不合格（按固化阈值判定）',
    fail_metrics VARCHAR(255) COMMENT '不合格指标编码，逗号分隔',
    review_result VARCHAR(16) COMMENT '复核结论：PASS-确认合格, RETEST-开启复检；退回后为空',
    review_comment VARCHAR(500) COMMENT '该设备复核意见',
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_batch_device (batch_id, device_id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次采样设备记录表';

-- 不合格项（按设备×指标维度，复检链路的源头）
CREATE TABLE IF NOT EXISTS wq_fail_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL COMMENT '来源批次ID',
    sample_item_id BIGINT NOT NULL COMMENT '来源采样记录ID',
    device_id BIGINT NOT NULL COMMENT '饮水机ID',
    device_no_snapshot VARCHAR(50) NOT NULL COMMENT '设备编号快照',
    metric_code VARCHAR(32) NOT NULL COMMENT '不合格指标编码',
    metric_name VARCHAR(32) NOT NULL COMMENT '不合格指标名称',
    metric_value VARCHAR(100) COMMENT '原始采样值',
    threshold_snapshot TEXT COMMENT '提交时该指标阈值快照JSON',
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN-待复检, CLOSED_PASS-复检合格关闭, CLOSED_VOID-退回作废',
    open_reviewer VARCHAR(50) COMMENT '开启复检的复核人',
    open_time DATETIME COMMENT '开启时间',
    close_operator VARCHAR(50) COMMENT '关闭操作人',
    close_time DATETIME COMMENT '关闭时间',
    latest_retest_id BIGINT COMMENT '最近一次复检记录ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_batch_id (batch_id),
    INDEX idx_device_status (device_id, status),
    INDEX idx_sample_item (sample_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水质抽检不合格项表';

-- 复检记录（同一不合格项保留历次结果）
CREATE TABLE IF NOT EXISTS wq_retest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fail_item_id BIGINT NOT NULL COMMENT '关联的不合格项ID',
    batch_id BIGINT NOT NULL COMMENT '来源批次ID',
    device_id BIGINT NOT NULL COMMENT '饮水机ID',
    device_no_snapshot VARCHAR(50) NOT NULL COMMENT '设备编号快照',
    metric_code VARCHAR(32) NOT NULL COMMENT '复检指标编码',
    metric_name VARCHAR(32) NOT NULL COMMENT '复检指标名称',
    metric_value VARCHAR(100) COMMENT '复检采样值',
    odor VARCHAR(100) COMMENT '气味复检结论（气味指标时）',
    retest_result VARCHAR(16) NOT NULL COMMENT 'PASS-复检合格, FAIL-复检仍不合格',
    threshold_snapshot TEXT COMMENT '判定所用阈值快照（沿用原不合格项）',
    retest_operator VARCHAR(50) COMMENT '复检人',
    retest_time DATETIME COMMENT '复检采样时间',
    comment VARCHAR(500) COMMENT '备注',
    request_id VARCHAR(64) NOT NULL COMMENT '幂等键：同一请求重试只生成一条记录',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_id (request_id),
    INDEX idx_fail_item (fail_item_id),
    INDEX idx_device_id (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不合格项复检记录表';

-- 照片引用（采样照片与复检照片统一登记，关系落库，防重复引用）
CREATE TABLE IF NOT EXISTS wq_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ref_type VARCHAR(16) NOT NULL COMMENT 'SAMPLE-采样照片, RETEST-复检照片',
    sample_item_id BIGINT NOT NULL DEFAULT 0 COMMENT '采样记录ID（ref_type=SAMPLE，否则0）',
    retest_id BIGINT NOT NULL DEFAULT 0 COMMENT '复检记录ID（ref_type=RETEST，否则0）',
    device_id BIGINT COMMENT '冗余设备ID便于检索',
    photo_url VARCHAR(500) NOT NULL COMMENT '文件URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ref (ref_type, sample_item_id, retest_id, photo_url),
    INDEX idx_sample_item (sample_item_id),
    INDEX idx_retest (retest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽检/复检照片引用表';

-- 批次状态变化流水（每次提交/退回/开启复检/复检流转/关闭留痕）
CREATE TABLE IF NOT EXISTS wq_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    from_status VARCHAR(16) COMMENT '变更前状态',
    to_status VARCHAR(16) NOT NULL COMMENT '变更后状态',
    action VARCHAR(32) NOT NULL COMMENT 'CREATE/SUBMIT/RETURN/REVIEW_RETEST/RETEST_RESULT/CLOSE',
    operator VARCHAR(50) COMMENT '操作人',
    detail VARCHAR(1000) COMMENT '变更说明（退回原因/复核意见/复检结论）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_batch_id (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽检批次状态流水表';

-- 写操作幂等表（防重复点击/网络重试产生重复状态变更）
CREATE TABLE IF NOT EXISTS wq_idempotent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL COMMENT '客户端生成的幂等键',
    action VARCHAR(32) NOT NULL COMMENT '操作类型',
    batch_id BIGINT COMMENT '关联批次',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽检写操作幂等记录表';

-- 设备档案增加“待复检”业务标记：与设备启用/停用状态正交，绝不覆盖停用状态
ALTER TABLE water_dispenser
    ADD COLUMN pending_retest TINYINT NOT NULL DEFAULT 0 COMMENT '待复检标记：1-待复检，0-正常；与status(启用/停用)正交';

-- 阈值默认数据（GB 直饮水常规参考值）
INSERT INTO wq_threshold (metric_code, metric_name, unit, judge_type, min_value, max_value, pass_values, display_order)
SELECT * FROM (
    SELECT 'residual_chlorine' AS metric_code, '余氯' AS metric_name, 'mg/L' AS unit, 'range' AS judge_type,
           0.030 AS min_value, 0.800 AS max_value, '' AS pass_values, 1 AS display_order
    UNION ALL
    SELECT 'turbidity', '浊度', 'NTU', 'range', 0.000, 1.000, '', 2
    UNION ALL
    SELECT 'temperature', '温度', '℃', 'range', 0.000, 40.000, '', 3
    UNION ALL
    SELECT 'odor', '气味', '', 'enum', NULL, NULL, '无异味,正常', 4
) t
WHERE NOT EXISTS (SELECT 1 FROM wq_threshold WHERE metric_code = t.metric_code);
