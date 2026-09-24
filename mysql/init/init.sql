CREATE DATABASE IF NOT EXISTS xgs_water DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xgs_water;

CREATE TABLE IF NOT EXISTS building_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0 COMMENT '父节点ID，0表示根节点',
    name VARCHAR(100) NOT NULL COMMENT '分组名称',
    type TINYINT NOT NULL COMMENT '类型：1-园区，2-楼栋，3-楼层',
    sort_order INT DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋分组树形表';

CREATE TABLE IF NOT EXISTS water_dispenser (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_no VARCHAR(50) NOT NULL UNIQUE COMMENT '设备编号',
    model VARCHAR(100) NOT NULL COMMENT '饮水机型号',
    spec VARCHAR(200) COMMENT '安装规格',
    water_type VARCHAR(50) COMMENT '出水类型：冷水/热水/温水/冰水',
    image_url VARCHAR(500) COMMENT '设备图片URL',
    group_id BIGINT NOT NULL COMMENT '所属分组ID（楼层节点）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-停用',
    install_date DATE COMMENT '安装日期',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_group_id (group_id),
    INDEX idx_device_no (device_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饮水机设备档案表';

CREATE TABLE IF NOT EXISTS group_transfer_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_id BIGINT NOT NULL COMMENT '设备ID',
    device_no VARCHAR(50) NOT NULL COMMENT '设备编号',
    old_group_id BIGINT COMMENT '原分组ID',
    old_group_path VARCHAR(500) COMMENT '原分组完整路径',
    new_group_id BIGINT NOT NULL COMMENT '新分组ID',
    new_group_path VARCHAR(500) NOT NULL COMMENT '新分组完整路径',
    operator VARCHAR(50) DEFAULT 'admin' COMMENT '操作人',
    transfer_reason VARCHAR(500) COMMENT '调整原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分组调整流水表';

INSERT INTO building_group (parent_id, name, type, sort_order, description) VALUES
(0, '创新产业园A区', 1, 1, '主园区办公区域'),
(0, '创新产业园B区', 1, 2, '研发中心区域'),
(1, '1号楼', 2, 1, '行政办公楼'),
(1, '2号楼', 2, 2, '研发楼'),
(2, '3号楼', 2, 1, '研发中心A栋'),
(3, '1层', 3, 1, '1楼大厅'),
(3, '2层', 3, 2, '2楼办公区'),
(3, '3层', 3, 3, '3楼会议室区'),
(4, '1层', 3, 1, '1楼前台'),
(4, '2层', 3, 2, '2楼研发区'),
(5, '1层', 3, 1, '1楼大厅'),
(5, '2层', 3, 2, '2楼实验室');

INSERT INTO water_dispenser (device_no, model, spec, water_type, image_url, group_id, status, install_date, remark) VALUES
('WD20240001', '美的YR-5', '立式冷热型', '冷水,热水,温水', '', 6, 1, '2024-01-15', '大厅饮水机'),
('WD20240002', '沁园YR-10', '立式冰热型', '冷水,热水,冰水', '', 6, 1, '2024-01-20', '备用饮水机'),
('WD20240003', '安吉尔J26', '台式温热型', '温水,热水', '', 7, 1, '2024-02-10', '办公区饮水机'),
('WD20240004', '美的YR-5', '立式冷热型', '冷水,热水,温水', '', 8, 1, '2024-02-15', '会议室专用'),
('WD20240005', '海尔HRO50', '立式冰热型', '冷水,热水,冰水,温水', '', 9, 1, '2024-03-01', '前台迎宾区'),
('WD20240006', '沁园YR-10', '立式冰热型', '冷水,热水,冰水', '', 10, 1, '2024-03-10', '研发区A'),
('WD20240007', '安吉尔J26', '台式温热型', '温水,热水', '', 11, 1, '2024-04-01', '大厅东侧'),
('WD20240008', '美的YR-5', '立式冷热型', '冷水,热水,温水', '', 12, 1, '2024-04-15', '实验室门口');
