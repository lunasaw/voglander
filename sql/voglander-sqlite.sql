/*
 SQLite Schema for Voglander
 Converted from MySQL schema
 Date: 2025-01-08
*/

PRAGMA foreign_keys = OFF;

-- ----------------------------
-- Table structure for tb_device
-- ----------------------------
DROP TABLE IF EXISTS tb_device;
CREATE TABLE tb_device
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    device_id      VARCHAR(64)                            NOT NULL,
    type           INTEGER      DEFAULT 1                 NOT NULL,
    status         INTEGER      DEFAULT 0                 NOT NULL,
    name           VARCHAR(255) DEFAULT '',
    ip             VARCHAR(64)                            NOT NULL,
    port           INTEGER                                NOT NULL,
    register_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    keepalive_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    server_ip      VARCHAR(255)                           NOT NULL,
    extend         TEXT
);

CREATE UNIQUE INDEX uk_device ON tb_device (device_id);

-- ----------------------------
-- Table structure for tb_device_channel
-- ----------------------------
DROP TABLE IF EXISTS tb_device_channel;
CREATE TABLE tb_device_channel
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status      INTEGER      DEFAULT 0                 NOT NULL,
    channel_id      VARCHAR(50)  NOT NULL,
    device_id       VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) DEFAULT NULL,
    last_seen_time  DATETIME     DEFAULT NULL,
    status_source   VARCHAR(32)  DEFAULT NULL,
    missing_count   INTEGER      NOT NULL DEFAULT 0,
    extend          TEXT
);

CREATE UNIQUE INDEX idx_channel_device ON tb_device_channel (channel_id, device_id);
CREATE INDEX idx_device_id ON tb_device_channel (device_id);
CREATE INDEX idx_device_status ON tb_device_channel (device_id, status);

-- ----------------------------
-- Table structure for tb_device_config
-- ----------------------------
DROP TABLE IF EXISTS tb_device_config;
CREATE TABLE tb_device_config
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    device_id    INTEGER      NOT NULL,
    config_key   VARCHAR(64)  NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    extend       TEXT
);

CREATE UNIQUE INDEX idx_device_config ON tb_device_config (device_id, config_key);

-- ----------------------------
-- Sequence table for manual sequence management
-- ----------------------------
DROP TABLE IF EXISTS sequence;
CREATE TABLE sequence
(
    seq_name      VARCHAR(50) PRIMARY KEY NOT NULL,
    current_val   INTEGER                 NOT NULL,
    increment_val INTEGER DEFAULT 1       NOT NULL
);

INSERT INTO sequence
VALUES ('seq_test1_num1', 0, 1);
INSERT INTO sequence
VALUES ('seq_test1_num2', 0, 2);

-- Note: SQLite doesn't support stored functions like MySQL
-- The currval() and nextval() functions would need to be implemented in application code

-- ----------------------------
-- Table structure for tb_media_node
-- ----------------------------
DROP TABLE IF EXISTS tb_media_node;
CREATE TABLE tb_media_node
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    server_id    VARCHAR(64)                            NOT NULL,
    name         VARCHAR(255) DEFAULT '',
    host         VARCHAR(255)                           NOT NULL,
    secret       VARCHAR(255)                           NOT NULL,
    enabled      INTEGER      DEFAULT 1                 NOT NULL,
    hook_enabled INTEGER      DEFAULT 1                 NOT NULL,
    weight       INTEGER      DEFAULT 100               NOT NULL,
    keepalive    INTEGER      DEFAULT 0,
    status       INTEGER      DEFAULT 0                 NOT NULL,
    description  VARCHAR(500) DEFAULT '',
    extend       TEXT
);

CREATE UNIQUE INDEX uk_server_id ON tb_media_node (server_id);

-- ----------------------------
-- Table structure for tb_media_session
-- ----------------------------
DROP TABLE IF EXISTS tb_media_session;
CREATE TABLE tb_media_session
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    call_id      VARCHAR(255) NOT NULL,
    device_id    VARCHAR(64)  DEFAULT NULL,
    channel_id   VARCHAR(64)  DEFAULT NULL,
    ssrc         VARCHAR(32)  DEFAULT NULL,
    stream       VARCHAR(255) DEFAULT NULL,
    status       INTEGER      NOT NULL DEFAULT 2,
    session_type VARCHAR(32)  DEFAULT NULL,
    extend       TEXT,
    stream_id      VARCHAR(128) DEFAULT NULL,
    node_server_id VARCHAR(64)  DEFAULT NULL,
    ref_count      INTEGER      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_call_id ON tb_media_session (call_id);
CREATE UNIQUE INDEX uk_media_session_stream_id ON tb_media_session (stream_id);
CREATE INDEX idx_media_session_device_id ON tb_media_session (device_id);
CREATE INDEX idx_media_session_status ON tb_media_session (status);
CREATE INDEX idx_media_session_status_node ON tb_media_session (status, node_server_id);
CREATE INDEX idx_media_session_device_channel ON tb_media_session (device_id, channel_id, status);

-- ----------------------------
-- Table structure for tb_alarm
-- ----------------------------
DROP TABLE IF EXISTS tb_alarm;
CREATE TABLE tb_alarm
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    device_id   VARCHAR(64) NOT NULL,
    channel_id  VARCHAR(64) DEFAULT NULL,
    alarm_type  INTEGER     DEFAULT NULL,
    alarm_level INTEGER     DEFAULT NULL,
    alarm_time  DATETIME    DEFAULT NULL,
    description VARCHAR(512) DEFAULT NULL,
    ack_status  INTEGER     NOT NULL DEFAULT 0,
    extend      TEXT
);

CREATE INDEX idx_alarm_device_time ON tb_alarm (device_id, alarm_time);
CREATE INDEX idx_alarm_level_status ON tb_alarm (alarm_level, ack_status);

-- ----------------------------
-- 部门表
-- ----------------------------
DROP TABLE IF EXISTS tb_dept;
CREATE TABLE tb_dept
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    parent_id   INTEGER  DEFAULT 0,
    dept_name   VARCHAR(100)                       NOT NULL,
    dept_code   VARCHAR(50),
    remark      VARCHAR(500),
    status      INTEGER  DEFAULT 1,
    sort_order  INTEGER  DEFAULT 0,
    leader      VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    extend      TEXT
);

CREATE INDEX idx_parent_id ON tb_dept (parent_id);
CREATE INDEX idx_dept_name ON tb_dept (dept_name);
CREATE INDEX idx_dept_status ON tb_dept (status);

-- 插入默认根部门
INSERT OR IGNORE INTO tb_dept (parent_id, dept_name, dept_code, remark, status, sort_order, leader)
VALUES (0, '总公司', 'ROOT', '根部门', 1, 0, '系统管理员');

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    username    VARCHAR(64)                            NOT NULL,
    password    VARCHAR(255)                           NOT NULL,
    nickname    VARCHAR(255) DEFAULT '',
    email       VARCHAR(255) DEFAULT '',
    phone       VARCHAR(20)  DEFAULT '',
    avatar      VARCHAR(500) DEFAULT '',
    status      INTEGER      DEFAULT 1                 NOT NULL,
    last_login  DATETIME     DEFAULT NULL,
    extend      TEXT
);

CREATE UNIQUE INDEX uk_username ON tb_user (username);

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS tb_role;
CREATE TABLE tb_role
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    role_name   VARCHAR(255)                           NOT NULL,
    description VARCHAR(500) DEFAULT '',
    status      INTEGER      DEFAULT 1                 NOT NULL,
    extend      TEXT
);

-- ----------------------------
-- 菜单表
-- ----------------------------
DROP TABLE IF EXISTS tb_menu;
CREATE TABLE tb_menu
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    parent_id   INTEGER      DEFAULT 0                 NOT NULL,
    menu_code   VARCHAR(64)                            NOT NULL,
    menu_name   VARCHAR(255)                           NOT NULL,
    menu_type   INTEGER      DEFAULT 1                 NOT NULL,
    path        VARCHAR(255) DEFAULT '',
    component   VARCHAR(255) DEFAULT '',
    icon        VARCHAR(255) DEFAULT '',
    sort_order  INTEGER      DEFAULT 0                 NOT NULL,
    status      INTEGER      DEFAULT 1                 NOT NULL,
    permission  VARCHAR(255) DEFAULT '',
    meta        TEXT, -- JSON stored as TEXT in SQLite
    extend      TEXT
);

CREATE UNIQUE INDEX uk_menu_code ON tb_menu (menu_code);
CREATE INDEX idx_menu_parent_id ON tb_menu (parent_id);

-- ----------------------------
-- 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS tb_user_role;
CREATE TABLE tb_user_role
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id     INTEGER                            NOT NULL,
    role_id     INTEGER                            NOT NULL
);

CREATE UNIQUE INDEX uk_user_role ON tb_user_role (user_id, role_id);

-- ----------------------------
-- 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS tb_role_menu;
CREATE TABLE tb_role_menu
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    role_id     INTEGER                            NOT NULL,
    menu_id     INTEGER                            NOT NULL
);

CREATE UNIQUE INDEX uk_role_menu ON tb_role_menu (role_id, menu_id);

-- 插入默认管理员用户 (密码: admin123)
-- 注意：这里的密码是使用PasswordUtils.encode("admin123")生成的
INSERT OR IGNORE INTO tb_user (username, password, nickname, status)
VALUES ('admin', '$2a$10$7p2jxt/ieuNtK+fDVB0xJDQoiHx5DKhCm8SgqaPx29e8ZYA++OunAMGbVTWeYCY5', '管理员', 1);

-- 插入默认角色
INSERT OR IGNORE INTO tb_role (role_name, description, status)
VALUES ('系统管理员', '系统管理员角色', 1),
       ('普通用户', '普通用户角色', 1);

-- 插入根级菜单
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- Dashboard 仪表板目录
(1, 0, 'Dashboard', 'page.dashboard.title', 1, '/dashboard', '', 'carbon:dashboard', -1, 1, '',
 '{"order": -1, "title": "page.dashboard.title", "hideInMenu": false}'),

-- System 系统管理目录
(2, 0, 'System', 'system.title', 1, '/system', '', 'carbon:settings', 9997, 1, '',
 '{"icon": "carbon:settings", "order": 9997, "title": "system.title", "badge": "new", "badgeType": "normal", "badgeVariants": "primary", "hideInMenu": false}'),

-- Media 媒体管理目录
(300, 0, 'Media', 'media.title', 1, '/media', '', 'mdi:server-network', 9996, 1, '',
 '{"icon": "mdi:server-network", "order": 9996, "title": "media.title", "hideInMenu": false}'),

-- Project 项目管理目录
(9, 0, 'Project', 'demos.vben.title', 1, '/vben-admin', '', 'carbon:data-center', 9998, 1, '',
 '{"badgeType": "dot", "order": 9998, "title": "demos.vben.title", "icon": "carbon:data-center", "hideInMenu": false}'),

-- About 关于页面
(10, 0, 'About', 'demos.vben.about', 2, '/about', '/about/index', 'lucide:copyright', 9999, 1, '',
 '{"icon": "lucide:copyright", "order": 9999, "title": "demos.vben.about", "hideInMenu": false}');

-- 插入Dashboard子菜单
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- Analytics 分析页面
(101, 1, 'Analytics', 'page.dashboard.analytics', 2, '/analytics', '/dashboard/analytics/index', 'carbon:analytics', 1,
 1, '',
 '{"affixTab": true, "title": "page.dashboard.analytics", "hideInMenu": false}'),

-- Workspace 工作台
(102, 1, 'Workspace', 'page.dashboard.workspace', 2, '/workspace', '/dashboard/workspace/index', 'carbon:workspace', 2,
 1, '',
 '{"title": "page.dashboard.workspace", "hideInMenu": false}');

-- 插入System子菜单
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- 系统菜单管理
(201, 2, 'SystemMenu', 'system.menu.title', 2, '/system/menu', '/system/menu/list', 'mdi:menu', 1, 1,
 'System:Menu:List',
 '{"icon": "mdi:menu", "title": "system.menu.title", "hideChildrenInMenu": true, "hideInMenu": false}'),

-- 系统角色管理
(202, 2, 'SystemRole', 'system.role.title', 2, '/system/role', '/system/role/list', 'mdi:account-group', 2, 1,
 'System:Role:List',
 '{"icon": "mdi:account-group", "title": "system.role.title", "hideChildrenInMenu": true, "hideInMenu": false}'),

-- 系统用户管理
(203, 2, 'SystemUser', 'system.user.title', 2, '/system/user', '/system/user/list', 'mdi:account', 3, 1,
 'System:User:List',
 '{"icon": "mdi:account", "title": "system.user.title", "hideChildrenInMenu": true, "hideInMenu": false}'),

-- 系统部门管理
(204, 2, 'SystemDept', 'system.dept.title', 2, '/system/dept', '/system/dept/list', 'charm:organisation', 4, 1,
 'System:Dept:List',
 '{"icon": "charm:organisation", "title": "system.dept.title", "hideChildrenInMenu": true, "hideInMenu": false}');

-- 插入Media子菜单
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- 流媒体节点管理
(301, 300, 'MediaNode', 'media.node.title', 2, '/media/node', '/media/node/list', 'mdi:server-network', 1, 1,
 'Media:Node:List',
 '{"icon": "mdi:server-network", "title": "media.node.title", "hideInMenu": false}'),

-- 流媒体节点详情 (隐藏在菜单中)
(302, 300, 'MediaNodeDetail', 'media.node.detail', 2, '/media/node/detail/:nodeKey', '/media/node/detail',
 'mdi:server-network', 2, 1, 'Media:Node:List',
 '{"hideInMenu": true, "icon": "mdi:server-network", "title": "media.node.detail"}'),

-- 流媒体列表
(303, 300, 'MediaList', 'media.list.title', 2, '/media/list', '/media/list/list', 'mdi:video-outline', 3, 1,
 'Media:List:Query',
 '{"icon": "mdi:video-outline", "title": "media.list.title", "hideInMenu": false}');

-- 插入System菜单的按钮权限
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- 菜单管理按钮
(20101, 201, 'SystemMenuCreate', 'system.menu.create', 3, null, null, '', 1, 1, 'System:Menu:Create',
 '{"title": "system.menu.create", "hideInMenu": true}'),
(20102, 201, 'SystemMenuEdit', 'system.menu.edit', 3, null, null, '', 2, 1, 'System:Menu:Edit',
 '{"title": "system.menu.edit", "hideInMenu": true}'),
(20103, 201, 'SystemMenuDelete', 'system.menu.delete', 3, null, null, '', 3, 1, 'System:Menu:Delete',
 '{"title": "system.menu.delete", "hideInMenu": true}'),

-- 角色管理按钮
(20201, 202, 'SystemRoleCreate', 'system.role.create', 3, null, null, '', 1, 1, 'System:Role:Create',
 '{"title": "system.role.create", "hideInMenu": true}'),
(20202, 202, 'SystemRoleEdit', 'system.role.edit', 3, null, null, '', 2, 1, 'System:Role:Edit',
 '{"title": "system.role.edit", "hideInMenu": true}'),
(20203, 202, 'SystemRoleDelete', 'system.role.delete', 3, null, null, '', 3, 1, 'System:Role:Delete',
 '{"title": "system.role.delete", "hideInMenu": true}'),

-- 用户管理按钮
(20301, 203, 'SystemUserCreate', 'system.user.create', 3, null, null, '', 1, 1, 'System:User:Create',
 '{"title": "system.user.create", "hideInMenu": true}'),
(20302, 203, 'SystemUserEdit', 'system.user.edit', 3, null, null, '', 2, 1, 'System:User:Edit',
 '{"title": "system.user.edit", "hideInMenu": true}'),
(20303, 203, 'SystemUserDelete', 'system.user.delete', 3, null, null, '', 3, 1, 'System:User:Delete',
 '{"title": "system.user.delete", "hideInMenu": true}'),

-- 部门管理按钮
(20401, 204, 'SystemDeptCreate', 'system.dept.create', 3, null, null, '', 1, 1, 'System:Dept:Create',
 '{"title": "system.dept.create", "hideInMenu": true}'),
(20402, 204, 'SystemDeptEdit', 'system.dept.edit', 3, null, null, '', 2, 1, 'System:Dept:Edit',
 '{"title": "system.dept.edit", "hideInMenu": true}'),
(20403, 204, 'SystemDeptDelete', 'system.dept.delete', 3, null, null, '', 3, 1, 'System:Dept:Delete',
 '{"title": "system.dept.delete", "hideInMenu": true}'),

-- 媒体节点管理按钮
(30101, 301, 'MediaNodeCreate', 'media.node.create', 3, null, null, '', 1, 1, 'Media:Node:Create',
 '{"title": "media.node.create", "hideInMenu": true}'),
(30102, 301, 'MediaNodeEdit', 'media.node.edit', 3, null, null, '', 2, 1, 'Media:Node:Edit',
 '{"title": "media.node.edit", "hideInMenu": true}'),
(30103, 301, 'MediaNodeDelete', 'media.node.delete', 3, null, null, '', 3, 1, 'Media:Node:Delete',
 '{"title": "media.node.delete", "hideInMenu": true}'),

-- 流媒体列表按钮
(30301, 303, 'MediaStreamClose', 'media.list.actions.close', 3, null, null, '', 1, 1, 'Media:Stream:Close',
 '{"title": "media.list.actions.close", "hideInMenu": true}'),
(30302, 303, 'MediaStreamExport', 'media.list.actions.export', 3, null, null, '', 2, 1, 'Media:List:Export',
 '{"title": "media.list.actions.export", "hideInMenu": true}');

-- 插入ProtocolLab(协议验证台)菜单
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- 协议验证台目录
(400, 0, 'ProtocolLab', 'protocolLab.category', 1, '/protocol-lab', '', 'mdi:lan-connect', 9995, 1, '',
 '{"icon": "mdi:lan-connect", "order": 9995, "title": "protocolLab.category", "hideInMenu": false}'),

-- GB28181 协议验证台页面
(401, 400, 'ProtocolLabGb28181', 'protocolLab.menu', 2, '/protocol-lab/gb28181', '/protocol-lab/index',
 'mdi:lan-connect', 1, 1, 'ProtocolLab:Gb28181:Query',
 '{"icon": "mdi:lan-connect", "title": "protocolLab.menu", "hideInMenu": false}');

-- 插入Device(GB28181设备管理)菜单 + 列表
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- 设备管理目录
(500, 0, 'Device', 'device.menu', 1, '/device', '', 'mdi:cctv', 9994, 1, '',
 '{"icon": "mdi:cctv", "order": 9994, "title": "device.menu", "hideInMenu": false}'),

-- 设备列表
(501, 500, 'DeviceList', 'device.title', 2, '/device/list', '/device/list', 'mdi:cctv', 1, 1,
 'Device:Device:Query',
 '{"icon": "mdi:cctv", "title": "device.title", "hideInMenu": false}'),

-- 设备通道列表(S5 钻取页,隐藏于菜单,由设备列表「通道数」path 导航进入)
(502, 500, 'DeviceChannelList', 'device.channel.title', 2, '/device/channel/:deviceId', '/device/channel/list',
 'mdi:video-input-component', 2, 1, 'Device:Device:Query',
 '{"icon": "mdi:video-input-component", "title": "device.channel.title", "hideInMenu": true}');

-- 插入Device按钮权限(menu_type=3,隐藏,仅用于鉴权,与前端 hasAccessByCodes 引用对齐)
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
(50101, 501, 'DeviceDetail', 'device.action.detail', 3, null, null, '', 1, 1, 'Device:Device:Query',
 '{"title": "device.action.detail", "hideInMenu": true}'),
(50102, 501, 'DeviceLive', 'device.action.live', 3, null, null, '', 2, 1, 'Device:Cmd:Live',
 '{"title": "device.action.live", "hideInMenu": true}'),
(50103, 501, 'DevicePtz', 'device.section.ptz', 3, null, null, '', 3, 1, 'Device:Cmd:Ptz',
 '{"title": "device.section.ptz", "hideInMenu": true}'),
(50104, 501, 'DeviceQuery', 'device.section.query', 3, null, null, '', 4, 1, 'Device:Cmd:Query',
 '{"title": "device.section.query", "hideInMenu": true}'),
(50105, 501, 'DeviceConfig', 'device.section.config', 3, null, null, '', 5, 1, 'Device:Cmd:Config',
 '{"title": "device.section.config", "hideInMenu": true}'),
(50106, 501, 'DeviceRecord', 'device.section.record', 3, null, null, '', 6, 1, 'Device:Cmd:Record',
 '{"title": "device.section.record", "hideInMenu": true}'),
(50107, 501, 'DeviceAlarm', 'device.section.alarm', 3, null, null, '', 7, 1, 'Device:Cmd:Alarm',
 '{"title": "device.section.alarm", "hideInMenu": true}'),
(50108, 501, 'DeviceBroadcast', 'device.action.broadcast', 3, null, null, '', 8, 1, 'Device:Cmd:Broadcast',
 '{"title": "device.action.broadcast", "hideInMenu": true}'),
(50109, 501, 'DeviceEdit', 'device.action.edit', 3, null, null, '', 9, 1, 'Device:Device:Edit',
 '{"title": "device.action.edit", "hideInMenu": true}'),
(50110, 501, 'DeviceDelete', 'device.action.delete', 3, null, null, '', 10, 1, 'Device:Device:Delete',
 '{"title": "device.action.delete", "hideInMenu": true}'),
(50111, 501, 'DeviceSubscription', 'device.section.subscribe', 3, null, null, '', 11, 1, 'Device:Subscription:Edit',
 '{"title": "device.section.subscribe", "hideInMenu": true}'),

-- 设备通道列表（502）按钮权限：编辑 / 删除（含批量删除、清离线，共用 Delete 权限码）
(50201, 502, 'DeviceChannelEdit', 'device.action.edit', 3, null, null, '', 1, 1, 'Device:Channel:Edit',
 '{"title": "device.action.edit", "hideInMenu": true}'),
(50202, 502, 'DeviceChannelDelete', 'device.action.delete', 3, null, null, '', 2, 1, 'Device:Channel:Delete',
 '{"title": "device.action.delete", "hideInMenu": true}');

-- 插入级联管理目录（600）+ 平台列表(601) / 通道映射(602) 页面（前端视图 P7 落地，本轮占位+权限码）
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
(600, 0, 'Cascade', 'cascade.title', 1, '/cascade', '', 'mdi:transit-connection-variant', 9993, 1, '',
 '{"icon": "mdi:transit-connection-variant", "order": 9993, "title": "cascade.title", "hideInMenu": false}'),
(601, 600, 'CascadePlatform', 'cascade.platform.title', 2, '/cascade/platform', '/cascade/platform/list',
 'mdi:server-network-outline', 1, 1, 'Cascade:Platform:List',
 '{"icon": "mdi:server-network-outline", "title": "cascade.platform.title", "hideInMenu": false}'),
(602, 600, 'CascadeChannel', 'cascade.channel.title', 2, '/cascade/channel', '/cascade/channel/list',
 'mdi:swap-horizontal', 2, 1, 'Cascade:Channel:List',
 '{"icon": "mdi:swap-horizontal", "title": "cascade.channel.title", "hideInMenu": false}');

-- 级联平台列表（601）/ 通道映射（602）按钮权限
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
(60101, 601, 'CascadePlatformCreate', 'cascade.platform.create', 3, null, null, '', 1, 1, 'Cascade:Platform:Create',
 '{"title": "cascade.platform.create", "hideInMenu": true}'),
(60102, 601, 'CascadePlatformEdit', 'cascade.platform.edit', 3, null, null, '', 2, 1, 'Cascade:Platform:Edit',
 '{"title": "cascade.platform.edit", "hideInMenu": true}'),
(60103, 601, 'CascadePlatformDelete', 'cascade.platform.delete', 3, null, null, '', 3, 1, 'Cascade:Platform:Delete',
 '{"title": "cascade.platform.delete", "hideInMenu": true}'),
(60104, 601, 'CascadePlatformStatus', 'cascade.platform.status', 3, null, null, '', 4, 1, 'Cascade:Platform:Status',
 '{"title": "cascade.platform.status", "hideInMenu": true}'),
(60201, 602, 'CascadeChannelCreate', 'cascade.channel.create', 3, null, null, '', 1, 1, 'Cascade:Channel:Create',
 '{"title": "cascade.channel.create", "hideInMenu": true}'),
(60202, 602, 'CascadeChannelEdit', 'cascade.channel.edit', 3, null, null, '', 2, 1, 'Cascade:Channel:Edit',
 '{"title": "cascade.channel.edit", "hideInMenu": true}'),
(60203, 602, 'CascadeChannelDelete', 'cascade.channel.delete', 3, null, null, '', 3, 1, 'Cascade:Channel:Delete',
 '{"title": "cascade.channel.delete", "hideInMenu": true}');

-- 插入Project子菜单
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                               status, permission, meta)
VALUES
-- VbenDocument 文档
(901, 9, 'VbenDocument', 'demos.vben.document', 2, '/vben-admin/document', 'IFrameView', 'carbon:book', 1, 1, '',
 '{"icon": "carbon:book", "iframeSrc": "https://doc.vben.pro", "title": "demos.vben.document", "hideInMenu": false}'),

-- VbenGithub Github链接
(902, 9, 'VbenGithub', 'Github', 2, '/vben-admin/github', 'IFrameView', 'carbon:logo-github', 2, 1, '',
 '{"icon": "carbon:logo-github", "link": "https://github.com/vbenjs/vue-vben-admin", "title": "Github", "hideInMenu": false}'),

-- VbenAntdv Antdv链接 (状态为禁用)
(903, 9, 'VbenAntdv', 'demos.vben.antdv', 2, '/vben-admin/antdv', 'IFrameView', 'carbon:hexagon-vertical-solid', 3, 0,
 '',
 '{"icon": "carbon:hexagon-vertical-solid", "badgeType": "dot", "link": "https://ant.vben.pro", "title": "demos.vben.antdv", "hideInMenu": false}');

-- 给管理员用户分配管理员角色 (如果用户和角色存在)
INSERT OR IGNORE INTO tb_user_role (user_id, role_id)
SELECT u.id, r.id
FROM tb_user u,
     tb_role r
WHERE u.username = 'admin'
  AND r.role_name = '系统管理员';

-- 给管理员角色分配所有菜单权限
INSERT OR IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM tb_role r,
     tb_menu m
WHERE r.role_name = '系统管理员'
  AND m.status = 1;

-- ----------------------------
-- Table structure for tb_stream_proxy
-- ----------------------------
DROP TABLE IF EXISTS tb_stream_proxy;
CREATE TABLE tb_stream_proxy
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    app           VARCHAR(255)                           NOT NULL,
    stream        VARCHAR(255)                           NOT NULL,
    url           VARCHAR(1000)                          NOT NULL,
    status        INTEGER      DEFAULT 1                 NOT NULL,
    online_status INTEGER      DEFAULT 0                 NOT NULL,
    proxy_key     VARCHAR(255) DEFAULT NULL,
    server_id     VARCHAR(64)  DEFAULT NULL,
    enabled       INTEGER      DEFAULT 1                 NOT NULL,
    description   VARCHAR(500) DEFAULT '',
    extend        TEXT
);

CREATE UNIQUE INDEX uk_app_stream ON tb_stream_proxy (app, stream);
CREATE INDEX idx_stream_proxy_key ON tb_stream_proxy (proxy_key);
CREATE INDEX idx_stream_proxy_status ON tb_stream_proxy (status);
CREATE INDEX idx_stream_proxy_online_status ON tb_stream_proxy (online_status);
CREATE INDEX idx_stream_proxy_server_id ON tb_stream_proxy (server_id);

-- ----------------------------
-- Table structure for tb_push_proxy
-- ----------------------------
DROP TABLE IF EXISTS tb_push_proxy;
CREATE TABLE tb_push_proxy
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    app           VARCHAR(255)                          NOT NULL,
    stream        VARCHAR(255)                          NOT NULL,
    dst_url       VARCHAR(1000)                         NOT NULL,
    schema        VARCHAR(50) DEFAULT 'rtmp'            NOT NULL,
    status        INTEGER     DEFAULT 1                 NOT NULL,
    online_status INTEGER     DEFAULT 0                 NOT NULL,
    proxy_key     VARCHAR(255) DEFAULT NULL,
    server_id     VARCHAR(64)  DEFAULT NULL,
    enabled       INTEGER     DEFAULT 1                 NOT NULL,
    description   VARCHAR(500) DEFAULT '',
    extend        TEXT -- 扩展字段，包含vhost、retryCount、rtpType、timeoutSec等ZLM特定参数
);

CREATE UNIQUE INDEX uk_push_app_stream ON tb_push_proxy (app, stream);
CREATE INDEX idx_push_proxy_key ON tb_push_proxy (proxy_key);
CREATE INDEX idx_push_proxy_status ON tb_push_proxy (status);
CREATE INDEX idx_push_proxy_online_status ON tb_push_proxy (online_status);
CREATE INDEX idx_push_proxy_server_id ON tb_push_proxy (server_id);
CREATE INDEX idx_push_proxy_schema ON tb_push_proxy (schema);


/*
 SQLite Schema for Stream Proxy Menu Data
 Converted from MySQL schema
 Date: 12/08/2025 Stream Proxy Menu Creation
*/

-- ----------------------------
-- 插入拉流代理管理菜单
-- ----------------------------
INSERT OR
REPLACE
INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
              permission, meta)
VALUES
-- 拉流代理管理主菜单
(304, 300, 'MediaStreamProxy', 'media.streamProxy.title', 2, '/media/stream-proxy', '/media/stream-proxy/list',
 'mdi:video-switch', 4, 1, 'Media:StreamProxy:List',
 '{"icon": "mdi:video-switch", "title": "media.streamProxy.title", "hideInMenu": false}');

-- ----------------------------
-- 插入拉流代理管理按钮权限
-- ----------------------------
INSERT OR
REPLACE
INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
              permission, meta)
VALUES
-- 新增拉流代理按钮
(30401, 304, 'MediaStreamProxyCreate', 'media.streamProxy.create', 3, null, null, '', 1, 1, 'Media:StreamProxy:Create',
 '{"title": "media.streamProxy.create", "hideInMenu": true}'),

-- 编辑拉流代理按钮
(30402, 304, 'MediaStreamProxyEdit', 'media.streamProxy.edit', 3, null, null, '', 2, 1, 'Media:StreamProxy:Edit',
 '{"title": "media.streamProxy.edit", "hideInMenu": true}'),

-- 删除拉流代理按钮
(30403, 304, 'MediaStreamProxyDelete', 'media.streamProxy.delete', 3, null, null, '', 3, 1, 'Media:StreamProxy:Delete',
 '{"title": "media.streamProxy.delete", "hideInMenu": true}'),

-- 查看拉流代理详情按钮
(30404, 304, 'MediaStreamProxyView', 'media.streamProxy.view', 3, null, null, '', 4, 1, 'Media:StreamProxy:View',
 '{"title": "media.streamProxy.view", "hideInMenu": true}'),

-- 启用/禁用拉流代理按钮
(30405, 304, 'MediaStreamProxyStatus', 'media.streamProxy.status', 3, null, null, '', 5, 1, 'Media:StreamProxy:Status',
 '{"title": "media.streamProxy.status", "hideInMenu": true}'),

-- 播放拉流代理按钮
(30406, 304, 'MediaStreamProxyPlay', 'media.streamProxy.play', 3, null, null, '', 6, 1, 'Media:StreamProxy:Play',
 '{"title": "media.streamProxy.play", "hideInMenu": true}');


-- ----------------------------
-- 给管理员角色分配新菜单权限
-- ----------------------------
INSERT OR
REPLACE
INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (304, 30401, 30402, 30403, 30404, 30405);

-- ----------------------------
-- 插入推流代理管理菜单
-- ----------------------------
INSERT OR
REPLACE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                      permission, meta)
VALUES
-- 推流代理管理主菜单
(305, 300, 'MediaPushProxy', 'media.pushProxy.title', 2, '/media/push-proxy', '/media/push-proxy/list',
 'mdi:video-switch-outline', 5, 1, 'Media:PushProxy:List',
 '{"icon": "mdi:video-switch-outline", "title": "media.pushProxy.title", "hideInMenu": false}');

-- ----------------------------
-- 插入推流代理管理按钮权限（简化版）
-- ----------------------------
INSERT OR
REPLACE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                      permission, meta)
VALUES
-- 查看推流代理权限（包含列表查看、详情查看、播放等只读操作）
(30501, 305, 'MediaPushProxyView', 'media.pushProxy.view', 3, null, null, '', 1, 1, 'Media:PushProxy:View',
 '{"title": "media.pushProxy.view", "hideInMenu": true}'),

-- 修改推流代理权限（包含新增、编辑、删除、状态切换、启动、停止等所有操作）
(30502, 305, 'MediaPushProxyEdit', 'media.pushProxy.edit', 3, null, null, '', 2, 1, 'Media:PushProxy:Edit',
 '{"title": "media.pushProxy.edit", "hideInMenu": true}');

-- ----------------------------
-- 给管理员角色分配推流代理菜单权限
-- ----------------------------
INSERT OR
REPLACE INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (305, 30501, 30502);

-- ----------------------------
-- 级联上级平台表
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_platform;
CREATE TABLE tb_cascade_platform
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    platform_id        VARCHAR(64)                            NOT NULL,
    platform_ip        VARCHAR(64)                            NOT NULL,
    platform_port      INTEGER                                NOT NULL,
    platform_domain    VARCHAR(64)                            NOT NULL,
    username           VARCHAR(64)  DEFAULT ''                NOT NULL,
    password           VARCHAR(128) DEFAULT ''                NOT NULL,
    local_client_id    VARCHAR(64)                            NOT NULL,
    local_ip           VARCHAR(64)  DEFAULT NULL,
    local_port         INTEGER      DEFAULT 5070              NOT NULL,
    enabled            INTEGER      DEFAULT 1                 NOT NULL,
    register_status    INTEGER      DEFAULT 0                 NOT NULL,
    keepalive_interval INTEGER      DEFAULT 60                NOT NULL,
    register_expires   INTEGER      DEFAULT 3600              NOT NULL,
    charset            VARCHAR(10)  DEFAULT 'GB2312'          NOT NULL,
    transport          VARCHAR(10)  DEFAULT 'UDP'             NOT NULL,
    extend             TEXT
);
CREATE UNIQUE INDEX uk_cascade_platform_id ON tb_cascade_platform (platform_id);

-- ----------------------------
-- 级联上报通道表
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_channel;
CREATE TABLE tb_cascade_channel
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    platform_id        VARCHAR(64)                            NOT NULL,
    local_device_id    VARCHAR(64)                            NOT NULL,
    local_channel_id   VARCHAR(64)                            NOT NULL,
    cascade_channel_id VARCHAR(64)                            NOT NULL,
    cascade_name       VARCHAR(255) DEFAULT NULL,
    enabled            INTEGER      DEFAULT 1                 NOT NULL
);
CREATE UNIQUE INDEX uk_cascade_platform_local ON tb_cascade_channel (platform_id, local_channel_id);

-- ----------------------------
-- 级联上级订阅表（上级订本平台 → 本平台据此主动推送）
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_subscribe;
CREATE TABLE tb_cascade_subscribe
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    platform_id  VARCHAR(64)                            NOT NULL,
    sub_type     VARCHAR(32)                            NOT NULL,
    call_id      VARCHAR(255) DEFAULT NULL,
    sn           VARCHAR(64)  DEFAULT NULL,
    expires      INTEGER      DEFAULT 3600              NOT NULL,
    interval_sec INTEGER      DEFAULT NULL,
    expire_time  DATETIME     DEFAULT NULL,
    status       INTEGER      DEFAULT 1                 NOT NULL,
    extend       TEXT
);
CREATE UNIQUE INDEX uk_cascade_subscribe ON tb_cascade_subscribe (platform_id, sub_type);
CREATE INDEX idx_cascade_subscribe_expire ON tb_cascade_subscribe (status, expire_time);

-- ----------------------------
-- 级联录像查询请求上下文表（上级查录像 → 转查真实设备 → 异步聚合回包）
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_record_request;
CREATE TABLE tb_cascade_record_request
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    platform_id        VARCHAR(64)                            NOT NULL,
    superior_sn        VARCHAR(64)                            NOT NULL,
    cascade_channel_id VARCHAR(64)                            NOT NULL,
    local_device_id    VARCHAR(64)                            NOT NULL,
    local_channel_id   VARCHAR(64)                            NOT NULL,
    local_sn           VARCHAR(64)  DEFAULT NULL,
    start_time         VARCHAR(32)  DEFAULT NULL,
    end_time           VARCHAR(32)  DEFAULT NULL,
    status             INTEGER      DEFAULT 0                 NOT NULL,
    extend             TEXT
);
CREATE INDEX idx_cascade_record_local ON tb_cascade_record_request (local_device_id, status);
CREATE INDEX idx_cascade_record_created ON tb_cascade_record_request (create_time);


-- GB28181-2022 设备订阅状态表
DROP TABLE IF EXISTS tb_device_subscription;
CREATE TABLE tb_device_subscription
(
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    device_id        VARCHAR(64)                            NOT NULL,
    sub_type         VARCHAR(32)                            NOT NULL,
    enabled          INTEGER      DEFAULT 0                 NOT NULL,
    status           INTEGER      DEFAULT 0                 NOT NULL,
    call_id          VARCHAR(255) DEFAULT NULL,
    expires          INTEGER      DEFAULT NULL,
    interval_sec     INTEGER      DEFAULT NULL,
    expire_time      DATETIME     DEFAULT NULL,
    last_notify_time DATETIME     DEFAULT NULL,
    extend           TEXT
);
CREATE UNIQUE INDEX uk_device_subscription ON tb_device_subscription (device_id, sub_type);
CREATE INDEX idx_device_subscription_expire ON tb_device_subscription (status, expire_time);

-- 设备移动位置表
DROP TABLE IF EXISTS tb_device_position;
CREATE TABLE tb_device_position
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    device_id     VARCHAR(64)                           NOT NULL,
    channel_id    VARCHAR(64) DEFAULT NULL,
    longitude     VARCHAR(32) DEFAULT NULL,
    latitude      VARCHAR(32) DEFAULT NULL,
    speed         VARCHAR(32) DEFAULT NULL,
    direction     VARCHAR(32) DEFAULT NULL,
    altitude      VARCHAR(32) DEFAULT NULL,
    position_time DATETIME    DEFAULT NULL,
    extend        TEXT
);
CREATE INDEX idx_device_position_device ON tb_device_position (device_id, position_time);

-- 1.0.9 通用业务任务内核
DROP TABLE IF EXISTS tb_biz_task_event;
CREATE TABLE tb_biz_task_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_id VARCHAR(64) NOT NULL, task_id VARCHAR(64) NOT NULL, execution_id VARCHAR(64),
    event_type VARCHAR(64) NOT NULL, from_state VARCHAR(32), to_state VARCHAR(32), attempt_no INTEGER,
    progress_current INTEGER, progress_total INTEGER, progress_message VARCHAR(512),
    failure_code VARCHAR(64), failure_message VARCHAR(512), actor_type VARCHAR(32), actor_id VARCHAR(64),
    worker_node VARCHAR(128), trace_id VARCHAR(128), dedupe_key VARCHAR(128), event_data TEXT,
    occurred_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX uk_biz_task_event_id ON tb_biz_task_event(event_id);
CREATE UNIQUE INDEX uk_biz_task_event_dedupe ON tb_biz_task_event(task_id,dedupe_key);
CREATE INDEX idx_biz_task_event_task ON tb_biz_task_event(task_id,occurred_at);
CREATE INDEX idx_biz_task_event_execution ON tb_biz_task_event(execution_id,occurred_at);
CREATE INDEX idx_biz_task_event_type ON tb_biz_task_event(event_type,occurred_at);

DROP TABLE IF EXISTS tb_biz_task_execution;
CREATE TABLE tb_biz_task_execution (
    id INTEGER PRIMARY KEY AUTOINCREMENT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, execution_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) NOT NULL, schedule_version INTEGER NOT NULL DEFAULT 1, planned_at DATETIME NOT NULL,
    deadline_at DATETIME, state VARCHAR(32) NOT NULL, attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 1, next_attempt_time DATETIME, started_at DATETIME,
    heartbeat_at DATETIME, finished_at DATETIME, claim_token VARCHAR(128), worker_node VARCHAR(128),
    lease_until DATETIME, progress_current INTEGER NOT NULL DEFAULT 0, progress_total INTEGER NOT NULL DEFAULT 0,
    progress_message VARCHAR(512), progress_revision INTEGER NOT NULL DEFAULT 0,
    result_ref_type VARCHAR(64), result_ref_id VARCHAR(128), result_summary TEXT,
    failure_code VARCHAR(64), failure_message VARCHAR(512), retryable INTEGER NOT NULL DEFAULT 0,
    retry_origin_execution_id VARCHAR(64), version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_biz_task_execution_id ON tb_biz_task_execution(execution_id);
CREATE UNIQUE INDEX uk_biz_task_execution_plan ON tb_biz_task_execution(task_id,schedule_version,planned_at);
CREATE INDEX idx_biz_task_execution_task ON tb_biz_task_execution(task_id,planned_at);
CREATE INDEX idx_biz_task_execution_pending ON tb_biz_task_execution(state,next_attempt_time);
CREATE INDEX idx_biz_task_execution_lease ON tb_biz_task_execution(state,lease_until);
CREATE INDEX idx_biz_task_execution_retry_origin ON tb_biz_task_execution(retry_origin_execution_id);

DROP TABLE IF EXISTS tb_biz_task;
CREATE TABLE tb_biz_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, task_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(64) NOT NULL, task_name VARCHAR(255) NOT NULL, description VARCHAR(512),
    task_mode VARCHAR(32) NOT NULL, schedule_start_time DATETIME, schedule_end_time DATETIME,
    interval_seconds INTEGER, next_plan_time DATETIME, schedule_version INTEGER NOT NULL DEFAULT 1,
    state VARCHAR(32) NOT NULL, priority INTEGER NOT NULL DEFAULT 0, last_execution_id VARCHAR(64),
    last_execute_time DATETIME, completed_time DATETIME, planned_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0, failed_count INTEGER NOT NULL DEFAULT 0,
    missed_count INTEGER NOT NULL DEFAULT 0, cancelled_count INTEGER NOT NULL DEFAULT 0,
    progress_current INTEGER NOT NULL DEFAULT 0, progress_total INTEGER NOT NULL DEFAULT 0,
    progress_message VARCHAR(512), progress_revision INTEGER NOT NULL DEFAULT 0,
    biz_key VARCHAR(128), subject_type VARCHAR(64), subject_id VARCHAR(128), payload TEXT NOT NULL,
    payload_version INTEGER NOT NULL, result_ref_type VARCHAR(64), result_ref_id VARCHAR(128),
    result_summary TEXT, last_failure_code VARCHAR(64), last_failure_message VARCHAR(512),
    origin_task_id VARCHAR(64), origin_execution_id VARCHAR(64), owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(64) NOT NULL, organization_id VARCHAR(64), idempotency_key VARCHAR(128),
    version INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_biz_task_task_id ON tb_biz_task(task_id);
CREATE UNIQUE INDEX uk_biz_task_idempotency ON tb_biz_task(owner_type,owner_id,task_type,idempotency_key);
CREATE INDEX idx_biz_task_due ON tb_biz_task(state,next_plan_time);
CREATE INDEX idx_biz_task_type_state ON tb_biz_task(task_type,state,create_time);
CREATE INDEX idx_biz_task_owner ON tb_biz_task(owner_type,owner_id,create_time);
CREATE INDEX idx_biz_task_biz_key ON tb_biz_task(biz_key,task_type);
CREATE INDEX idx_biz_task_subject ON tb_biz_task(subject_type,subject_id,create_time);

INSERT OR REPLACE INTO tb_menu(id,parent_id,menu_code,menu_name,menu_type,path,component,icon,sort_order,status,permission,meta) VALUES
(600,0,'TaskManagement','task.management.title',1,'/task','BasicLayout','lucide:list-checks',6,1,'','{"icon":"lucide:list-checks","title":"task.management.title","order":6}'),
(601,600,'TaskCenter','task.center.title',2,'/task/center','/task/center/list','lucide:activity',1,1,'Task:Query','{"icon":"lucide:activity","title":"task.center.title"}'),
(60101,601,'TaskQuery','task.action.query',3,NULL,NULL,'',1,1,'Task:Query','{"title":"task.action.query","hideInMenu":true}'),
(60102,601,'TaskControl','task.action.control',3,NULL,NULL,'',2,1,'Task:Control','{"title":"task.action.control","hideInMenu":true}');
INSERT OR IGNORE INTO tb_role_menu(role_id,menu_id)
SELECT 1,id FROM tb_menu WHERE id IN (600,601,60101,60102);

-- 1.0.9 图像资产
DROP TABLE IF EXISTS tb_image_asset;
CREATE TABLE tb_image_asset
(
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time        DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    asset_id           VARCHAR(64)                            NOT NULL,
    asset_name         VARCHAR(255)                           NOT NULL,
    status             VARCHAR(32)  DEFAULT 'AVAILABLE'       NOT NULL,
    storage_provider   VARCHAR(32)                            NOT NULL,
    storage_bucket     VARCHAR(128) DEFAULT NULL,
    storage_key        VARCHAR(512)                           NOT NULL,
    storage_node_id    VARCHAR(128) DEFAULT NULL,
    content_type       VARCHAR(64)                            NOT NULL,
    image_format       VARCHAR(32)                            NOT NULL,
    file_size          INTEGER                                NOT NULL,
    width              INTEGER                                NOT NULL,
    height             INTEGER                                NOT NULL,
    checksum_algorithm VARCHAR(32)  DEFAULT 'SHA256'           NOT NULL,
    checksum           VARCHAR(128)                           NOT NULL,
    captured_at        DATETIME                               NOT NULL,
    ingested_at        DATETIME                               NOT NULL,
    owner_type         VARCHAR(32)                            NOT NULL,
    owner_id           VARCHAR(64)                            NOT NULL,
    organization_id    VARCHAR(64)  DEFAULT NULL,
    idempotency_key    VARCHAR(128) DEFAULT NULL,
    retention_policy   VARCHAR(64)  DEFAULT 'PERMANENT'        NOT NULL,
    expires_at         DATETIME     DEFAULT NULL,
    deleted_at         DATETIME     DEFAULT NULL,
    delete_reason      VARCHAR(255) DEFAULT NULL,
    failure_code       VARCHAR(64)  DEFAULT NULL,
    failure_message    VARCHAR(512) DEFAULT NULL,
    version            INTEGER      DEFAULT 0                 NOT NULL
);
CREATE UNIQUE INDEX uk_image_asset_asset_id ON tb_image_asset (asset_id);
CREATE UNIQUE INDEX uk_image_asset_idempotency ON tb_image_asset (owner_type, owner_id, idempotency_key);
CREATE INDEX idx_image_asset_status_created ON tb_image_asset (status, create_time);
CREATE INDEX idx_image_asset_captured ON tb_image_asset (captured_at, asset_id);
CREATE INDEX idx_image_asset_owner ON tb_image_asset (owner_type, owner_id, create_time);
CREATE INDEX idx_image_asset_checksum ON tb_image_asset (checksum);

DROP TABLE IF EXISTS tb_image_asset_source;
CREATE TABLE tb_image_asset_source
(
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    asset_id            VARCHAR(64)                         NOT NULL,
    source_type         VARCHAR(32)                         NOT NULL,
    source_system       VARCHAR(64)                         NOT NULL,
    source_entity_type  VARCHAR(32)                         NOT NULL,
    source_entity_id    VARCHAR(128)                        NOT NULL,
    source_task_id      VARCHAR(64)  DEFAULT NULL,
    source_execution_id VARCHAR(64)  DEFAULT NULL,
    original_filename   VARCHAR(255) DEFAULT NULL,
    source_metadata     TEXT
);
CREATE UNIQUE INDEX uk_image_asset_source_asset ON tb_image_asset_source (asset_id);
CREATE UNIQUE INDEX uk_image_asset_source_execution ON tb_image_asset_source (source_execution_id);
CREATE INDEX idx_image_asset_source_type_created ON tb_image_asset_source (source_type, create_time);
CREATE INDEX idx_image_asset_source_entity_created ON tb_image_asset_source (source_entity_type, source_entity_id, create_time);
CREATE INDEX idx_image_asset_source_task ON tb_image_asset_source (source_task_id);

DROP TABLE IF EXISTS tb_image_collection_config;
CREATE TABLE tb_image_collection_config
(
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time           DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time           DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    task_id               VARCHAR(64)                            NOT NULL,
    device_id             VARCHAR(64)                            NOT NULL,
    channel_id            VARCHAR(64)                            NOT NULL,
    device_name_snapshot  VARCHAR(255) DEFAULT NULL,
    channel_name_snapshot VARCHAR(255) DEFAULT NULL,
    retention_policy      VARCHAR(64)  DEFAULT 'PERMANENT'        NOT NULL,
    capture_options       TEXT,
    version               INTEGER      DEFAULT 0                 NOT NULL
);
CREATE UNIQUE INDEX uk_image_collection_config_task ON tb_image_collection_config (task_id);
CREATE INDEX idx_image_collection_config_camera ON tb_image_collection_config (device_id, channel_id, create_time);

-- 图像管理菜单、页面与按钮权限
INSERT OR REPLACE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                                status, permission, meta)
VALUES (700, 0, 'ImageManagement', 'image.management.title', 1, '/image', 'BasicLayout', 'lucide:images', 7, 1, '',
        '{"icon":"lucide:images","title":"image.management.title","order":7}'),
       (701, 700, 'ImageAssets', 'image.asset.title', 2, '/image/assets', '/image/assets/list', 'lucide:image', 1, 1,
        'Image:Asset:Query', '{"icon":"lucide:image","title":"image.asset.title"}'),
       (702, 700, 'ImageCollection', 'image.collection.title', 2, '/image/collection', '/image/collection/list',
        'lucide:camera', 2, 1, 'Image:Collection:Query',
        '{"icon":"lucide:camera","title":"image.collection.title"}');

INSERT OR REPLACE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order,
                                status, permission, meta)
VALUES (70101, 701, 'ImageAssetQuery', 'image.asset.query', 3, NULL, NULL, '', 1, 1, 'Image:Asset:Query',
        '{"title":"image.asset.query","hideInMenu":true}'),
       (70102, 701, 'ImageAssetView', 'image.asset.view', 3, NULL, NULL, '', 2, 1, 'Image:Asset:View',
        '{"title":"image.asset.view","hideInMenu":true}'),
       (70103, 701, 'ImageAssetUpload', 'image.asset.upload', 3, NULL, NULL, '', 3, 1, 'Image:Asset:Upload',
        '{"title":"image.asset.upload","hideInMenu":true}'),
       (70104, 701, 'ImageAssetDownload', 'image.asset.download', 3, NULL, NULL, '', 4, 1, 'Image:Asset:Download',
        '{"title":"image.asset.download","hideInMenu":true}'),
       (70105, 701, 'ImageAssetDelete', 'image.asset.delete', 3, NULL, NULL, '', 5, 1, 'Image:Asset:Delete',
        '{"title":"image.asset.delete","hideInMenu":true}'),
       (70201, 702, 'ImageCollectionQuery', 'image.collection.query', 3, NULL, NULL, '', 1, 1,
        'Image:Collection:Query', '{"title":"image.collection.query","hideInMenu":true}'),
       (70202, 702, 'ImageCollectionCreate', 'image.collection.create', 3, NULL, NULL, '', 2, 1,
        'Image:Collection:Create', '{"title":"image.collection.create","hideInMenu":true}'),
       (70203, 702, 'ImageCollectionControl', 'image.collection.control', 3, NULL, NULL, '', 3, 1,
        'Image:Collection:Control', '{"title":"image.collection.control","hideInMenu":true}');

INSERT OR IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (700, 701, 702, 70101, 70102, 70103, 70104, 70105, 70201, 70202, 70203);

PRAGMA foreign_keys = ON;
