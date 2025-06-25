-- SQLite版本的数据库表结构

-- 设备表
DROP TABLE IF EXISTS tb_device;
CREATE TABLE tb_device
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    device_id      VARCHAR(64)  NOT NULL UNIQUE,
    type           INTEGER      NOT NULL DEFAULT 1,
    status         INTEGER      NOT NULL DEFAULT 0,
    name           VARCHAR(255)          DEFAULT '',
    ip             VARCHAR(64)  NOT NULL,
    port           INTEGER      NOT NULL,
    register_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    keepalive_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    server_ip      VARCHAR(255) NOT NULL,
    extend         TEXT
);

-- 设备通道表
DROP TABLE IF EXISTS tb_device_channel;
CREATE TABLE tb_device_channel
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME             DEFAULT CURRENT_TIMESTAMP,
    status      INTEGER     NOT NULL DEFAULT 0,
    channel_id  VARCHAR(50) NOT NULL,
    device_id   VARCHAR(64) NOT NULL,
    name        VARCHAR(255),
    extend      TEXT,
    UNIQUE (channel_id, device_id)
);

-- 设备配置表
DROP TABLE IF EXISTS tb_device_config;
CREATE TABLE tb_device_config
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    device_id    INTEGER      NOT NULL,
    config_key   VARCHAR(64)  NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    extend       TEXT,
    UNIQUE (device_id, config_key)
);

-- 导出任务表
DROP TABLE IF EXISTS tb_export_task;
CREATE TABLE tb_export_task
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    gmt_create  DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_update  DATETIME DEFAULT CURRENT_TIMESTAMP,
    biz_id      VARCHAR(255) NOT NULL,
    member_cnt  INTEGER  DEFAULT 0,
    format      VARCHAR(10)  NOT NULL,
    apply_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    export_time DATETIME,
    url         VARCHAR(2500),
    status      INTEGER  DEFAULT 0,
    deleted     INTEGER  DEFAULT 0,
    param       TEXT,
    name        VARCHAR(500),
    type        INTEGER  DEFAULT 0,
    expired     INTEGER  DEFAULT 0,
    extend      TEXT,
    apply_user  VARCHAR(256)
);

-- 流媒体节点管理表
DROP TABLE IF EXISTS tb_media_node;
CREATE TABLE tb_media_node
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    server_id    VARCHAR(64)  NOT NULL UNIQUE,
    name         VARCHAR(255)          DEFAULT '',
    host         VARCHAR(255) NOT NULL,
    secret       VARCHAR(255) NOT NULL,
    enabled      INTEGER      NOT NULL DEFAULT 1,
    hook_enabled INTEGER      NOT NULL DEFAULT 1,
    weight       INTEGER      NOT NULL DEFAULT 100,
    keepalive    INTEGER               DEFAULT 0,
    status       INTEGER      NOT NULL DEFAULT 0,
    description  VARCHAR(500)          DEFAULT '',
    extend       TEXT
);

-- 用户表
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(255)          DEFAULT '',
    email       VARCHAR(255)          DEFAULT '',
    phone       VARCHAR(20)           DEFAULT '',
    avatar      VARCHAR(500)          DEFAULT '',
    status      INTEGER      NOT NULL DEFAULT 1,
    last_login  DATETIME              DEFAULT NULL,
    extend      TEXT
);

-- 角色表
DROP TABLE IF EXISTS tb_role;
CREATE TABLE tb_role
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    role_code   VARCHAR(64)  NOT NULL UNIQUE,
    role_name   VARCHAR(255) NOT NULL,
    description VARCHAR(500)          DEFAULT '',
    status      INTEGER      NOT NULL DEFAULT 1,
    extend      TEXT
);

-- 菜单表
DROP TABLE IF EXISTS tb_menu;
CREATE TABLE tb_menu
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    parent_id   INTEGER      NOT NULL DEFAULT 0,
    menu_code   VARCHAR(64)  NOT NULL UNIQUE,
    menu_name   VARCHAR(255) NOT NULL,
    menu_type   INTEGER      NOT NULL DEFAULT 1,
    path        VARCHAR(255)          DEFAULT '',
    component   VARCHAR(255)          DEFAULT '',
    icon        VARCHAR(255)          DEFAULT '',
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    visible     INTEGER      NOT NULL DEFAULT 1,
    status      INTEGER      NOT NULL DEFAULT 1,
    permission  VARCHAR(255)          DEFAULT '',
    extend      TEXT
);

-- 创建菜单表的索引
CREATE INDEX idx_menu_parent_id ON tb_menu (parent_id);

-- 用户角色关联表
DROP TABLE IF EXISTS tb_user_role;
CREATE TABLE tb_user_role
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id     INTEGER NOT NULL,
    role_id     INTEGER NOT NULL,
    UNIQUE (user_id, role_id)
);

-- 角色菜单关联表
DROP TABLE IF EXISTS tb_role_menu;
CREATE TABLE tb_role_menu
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    role_id     INTEGER NOT NULL,
    menu_id     INTEGER NOT NULL,
    UNIQUE (role_id, menu_id)
);

-- 插入默认管理员用户 (密码: admin123)
-- 注意：这里的密码是使用PasswordUtils.encode("admin123")生成的
INSERT INTO tb_user (username, password, nickname, status)
VALUES ('admin', '$2a$10$Pcn9CQ6CyNOJNX3P629yUq91BFhguQM9/ddA72m0+Mo7d9AlOIkOikRCxRL2G/Fu', '管理员', 1);

-- 插入默认角色
INSERT INTO tb_role (role_code, role_name, description, status)
VALUES ('ADMIN', '系统管理员', '系统管理员角色', 1),
       ('USER', '普通用户', '普通用户角色', 1);

-- 插入默认菜单
INSERT INTO tb_menu (parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission)
VALUES (0, 'DASHBOARD', '仪表盘', 1, '/dashboard', '', 'dashboard', 1, 1, 1, ''),
       (0, 'DEVICE', '设备管理', 1, '/device', '', 'device', 2, 1, 1, ''),
       (2, 'DEVICE_LIST', '设备列表', 2, '/device/list', 'device/DeviceList', '', 1, 1, 1, 'device:list'),
       (2, 'DEVICE_CHANNEL', '设备通道', 2, '/device/channel', 'device/DeviceChannel', '', 2, 1, 1, 'device:channel'),
       (0, 'MEDIA', '流媒体管理', 1, '/media', '', 'media', 3, 1, 1, ''),
       (5, 'MEDIA_NODE', '节点管理', 2, '/media/node', 'media/MediaNode', '', 1, 1, 1, 'media:node'),
       (0, 'SYSTEM', '系统管理', 1, '/system', '', 'system', 4, 1, 1, ''),
       (7, 'SYSTEM_USER', '用户管理', 2, '/system/user', 'system/User', '', 1, 1, 1, 'system:user'),
       (7, 'SYSTEM_ROLE', '角色管理', 2, '/system/role', 'system/Role', '', 2, 1, 1, 'system:role'),
       (7, 'SYSTEM_MENU', '菜单管理', 2, '/system/menu', 'system/Menu', '', 3, 1, 1, 'system:menu');

-- 给管理员用户分配管理员角色
INSERT INTO tb_user_role (user_id, role_id)
VALUES (1, 1);

-- 给管理员角色分配所有菜单权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE status = 1;