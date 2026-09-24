-- H2 (MySQL MODE) 下的测试库结构，与 mysql/init/*.sql 保持同构
CREATE TABLE IF NOT EXISTS building_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    type TINYINT NOT NULL,
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS water_dispenser (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_no VARCHAR(50) NOT NULL UNIQUE,
    model VARCHAR(100) NOT NULL,
    spec VARCHAR(200),
    water_type VARCHAR(50),
    image_url VARCHAR(500),
    group_id BIGINT NOT NULL,
    status TINYINT DEFAULT 1,
    pending_retest TINYINT NOT NULL DEFAULT 0,
    install_date DATE,
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 快速检索组合索引（与 mysql/init/zz_device_search.sql 对齐，用于验证十万级数据访问路径）
CREATE INDEX IF NOT EXISTS idx_pending_retest_ctime ON water_dispenser (pending_retest, create_time, id);
CREATE INDEX IF NOT EXISTS idx_status_ctime ON water_dispenser (status, create_time, id);
CREATE INDEX IF NOT EXISTS idx_model_ctime ON water_dispenser (model, create_time, id);
CREATE INDEX IF NOT EXISTS idx_spec_ctime ON water_dispenser (spec, create_time, id);
CREATE INDEX IF NOT EXISTS idx_group_ctime ON water_dispenser (group_id, create_time, id);
CREATE INDEX IF NOT EXISTS idx_install_date_id ON water_dispenser (install_date, id);

CREATE TABLE IF NOT EXISTS group_transfer_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL,
    device_no VARCHAR(50) NOT NULL,
    old_group_id BIGINT,
    old_group_path VARCHAR(500),
    new_group_id BIGINT NOT NULL,
    new_group_path VARCHAR(500) NOT NULL,
    operator VARCHAR(50) DEFAULT 'admin',
    transfer_reason VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wq_threshold (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_code VARCHAR(32) NOT NULL,
    metric_name VARCHAR(32) NOT NULL,
    unit VARCHAR(16) DEFAULT '',
    judge_type VARCHAR(16) NOT NULL DEFAULT 'range',
    min_value DECIMAL(10,3),
    max_value DECIMAL(10,3),
    pass_values VARCHAR(255) DEFAULT '',
    display_order INT DEFAULT 0,
    enabled TINYINT DEFAULT 1,
    update_operator VARCHAR(50) DEFAULT 'admin',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_th_metric UNIQUE (metric_code)
);

CREATE TABLE IF NOT EXISTS wq_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_no VARCHAR(40) NOT NULL,
    scope_group_id BIGINT,
    scope_group_path VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    submitter VARCHAR(50),
    submit_time DATETIME,
    reviewer VARCHAR(50),
    review_time DATETIME,
    return_reason VARCHAR(500),
    threshold_snapshot CLOB,
    close_operator VARCHAR(50),
    close_time DATETIME,
    remark VARCHAR(500),
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_no UNIQUE (batch_no)
);

CREATE TABLE IF NOT EXISTS wq_sample_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    device_no_snapshot VARCHAR(50) NOT NULL,
    model_snapshot VARCHAR(100) NOT NULL,
    group_id_snapshot BIGINT,
    group_path_snapshot VARCHAR(500),
    sampler VARCHAR(50),
    sample_time DATETIME,
    residual_chlorine DECIMAL(10,3),
    turbidity DECIMAL(10,3),
    temperature DECIMAL(10,3),
    odor VARCHAR(100),
    item_result VARCHAR(16),
    fail_metrics VARCHAR(255),
    review_result VARCHAR(16),
    review_comment VARCHAR(500),
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_device UNIQUE (batch_id, device_id)
);

CREATE TABLE IF NOT EXISTS wq_fail_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    sample_item_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    device_no_snapshot VARCHAR(50) NOT NULL,
    metric_code VARCHAR(32) NOT NULL,
    metric_name VARCHAR(32) NOT NULL,
    metric_value VARCHAR(100),
    threshold_snapshot CLOB,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    open_reviewer VARCHAR(50),
    open_time DATETIME,
    close_operator VARCHAR(50),
    close_time DATETIME,
    latest_retest_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wq_retest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fail_item_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    device_no_snapshot VARCHAR(50) NOT NULL,
    metric_code VARCHAR(32) NOT NULL,
    metric_name VARCHAR(32) NOT NULL,
    metric_value VARCHAR(100),
    odor VARCHAR(100),
    retest_result VARCHAR(16) NOT NULL,
    threshold_snapshot CLOB,
    retest_operator VARCHAR(50),
    retest_time DATETIME,
    comment VARCHAR(500),
    request_id VARCHAR(64) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_retest_req UNIQUE (request_id)
);

CREATE TABLE IF NOT EXISTS wq_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ref_type VARCHAR(16) NOT NULL,
    sample_item_id BIGINT NOT NULL DEFAULT 0,
    retest_id BIGINT NOT NULL DEFAULT 0,
    device_id BIGINT,
    photo_url VARCHAR(500) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_photo_ref UNIQUE (ref_type, sample_item_id, retest_id, photo_url)
);

CREATE TABLE IF NOT EXISTS wq_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id BIGINT NOT NULL,
    from_status VARCHAR(16),
    to_status VARCHAR(16) NOT NULL,
    action VARCHAR(32) NOT NULL,
    operator VARCHAR(50),
    detail VARCHAR(1000),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wq_idempotent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    batch_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idem_req UNIQUE (request_id)
);
