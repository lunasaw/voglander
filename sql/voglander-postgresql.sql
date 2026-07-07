/*
 Navicat Premium Data Transfer

 Source Server         : luna-local
 Source Server Type    : MySQL
 Source Server Version : 80200 (8.2.0)
 Source Host           : localhost:3306
 Source Schema         : voglander

 Target Server Type    : MySQL
 Target Server Version : 80200 (8.2.0)
 File Encoding         : 65001

 Date: 29/01/2024 22:23:42
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_device
-- ----------------------------
DROP TABLE IF EXISTS tb_device;
CREATE TABLE tb_device
(
    id             BIGSERIAL,
    create_time    TIMESTAMP                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id      varchar(64)                        NOT NULL,
    type           int                                                    NOT NULL DEFAULT '1',
    status         int                                                    NOT NULL DEFAULT '0',
    name           varchar(255)          DEFAULT '',
    ip             varchar(64)  NOT NULL,
    port           int                                                    NOT NULL,
    register_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    keepalive_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    server_ip      varchar(255) NOT NULL,
    extend         text,
    PRIMARY KEY (id),
    UNIQUE (device_id),
    -- Phase 2a：状态/心跳查询与离线扫描的支撑索引（修 P8 全表扫）
);

-- ----------------------------
-- Table structure for tb_device_channel
-- ----------------------------
DROP TABLE IF EXISTS tb_device_channel;
CREATE TABLE tb_device_channel
(
    id          BIGSERIAL,
    create_time TIMESTAMP                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status      int                                                          NOT NULL DEFAULT '0',
    channel_id varchar(50) NOT NULL,
    device_id   varchar(64)        NOT NULL,
    name       varchar(255) DEFAULT NULL,
    extend      text,
    PRIMARY KEY (id),
    last_seen_time TIMESTAMP                                                              DEFAULT NULL,
    status_source  varchar(32)                DEFAULT NULL,
    missing_count  int                                                          NOT NULL DEFAULT '0',
    PRIMARY KEY (id),
    UNIQUE (channel_id, device_id),
    -- Phase 2a：复合 UNIQUE 最左前缀是 channel_id，无法支撑"按 device_id 查通道"，补单列索引
    -- 1.0.4：cascadeOffline + 列表过滤 + 单调查询共用复合索引
);

-- ----------------------------
-- Table structure for tb_device_config
-- ----------------------------
DROP TABLE IF EXISTS tb_device_config;
CREATE TABLE tb_device_config
(
    id           BIGSERIAL,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_id    bigint                                                 NOT NULL,
    config_key   varchar(64)  NOT NULL,
    config_value varchar(255) NOT NULL,
    extend       text,
    PRIMARY KEY (id),
    UNIQUE (device_id, config_key)
);

-- ----------------------------
-- Table structure for tb_export_task
-- ----------------------------
DROP TABLE IF EXISTS tb_export_task;
CREATE TABLE tb_export_task
(
    id          BIGSERIAL,
    gmt_create  TIMESTAMP                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_update  TIMESTAMP                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    biz_id      varchar(255)                                                 NOT NULL,
    member_cnt  bigint                                                       NOT NULL DEFAULT '0',
    format      varchar(10) NOT NULL,
    apply_time  TIMESTAMP                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    export_time TIMESTAMP                                                              DEFAULT NULL,
    url         varchar(2500)        DEFAULT NULL,
    status      int                                                          NOT NULL DEFAULT '0',
    deleted     int                                                          NOT NULL DEFAULT '0',
    param       text,
    name        varchar(500)         DEFAULT NULL,
    type        int                                                          NOT NULL DEFAULT '0',
    expired     int                                                          NOT NULL DEFAULT '0',
    extend      text,
    apply_user  varchar(256)         DEFAULT NULL,
    PRIMARY KEY (id),
);

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for tb_media_node
-- ----------------------------
DROP TABLE IF EXISTS tb_media_node;
CREATE TABLE tb_media_node
(
    id           BIGSERIAL,
    create_time  TIMESTAMP                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP                                               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    server_id    varchar(64)  NOT NULL,
    name         varchar(255)          DEFAULT '',
    host         varchar(255) NOT NULL,
    secret       varchar(255) NOT NULL,
    enabled      SMALLINT(1)                                             NOT NULL DEFAULT '1',
    hook_enabled SMALLINT(1)                                             NOT NULL DEFAULT '1',
    weight       int                                                    NOT NULL DEFAULT '100',
    keepalive    bigint                                                          DEFAULT '0',
    status       int                                                    NOT NULL DEFAULT '0',
    description  varchar(500)          DEFAULT '',
    extend       text,
    PRIMARY KEY (id),
    UNIQUE (server_id)
);

-- 部门表
CREATE TABLE IF NOT EXISTS tb_dept
(
    id          BIGSERIAL PRIMARY KEY,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parent_id   BIGINT                DEFAULT 0,
    dept_name   VARCHAR(100) NOT NULL,
    dept_code   VARCHAR(50),
    remark      VARCHAR(500),
    status      INT                   DEFAULT 1,
    sort_order  INT                   DEFAULT 0,
    leader      VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    extend      TEXT,
    INDEX       idx_parent_id(parent_id
) ,
    INDEX idx_dept_name (dept_name),
    INDEX idx_status (status)
);

-- 插入默认根部门
INSERT INTO tb_dept (parent_id, dept_name, dept_code, remark, status, sort_order, leader)
VALUES (0, '总公司', 'ROOT', '根部门', 1, 0, '系统管理员');

-- 用户表
CREATE TABLE tb_user
(
    id          BIGSERIAL,
    create_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username    VARCHAR(64)     NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    nickname    VARCHAR(255)             DEFAULT '',
    email       VARCHAR(255)             DEFAULT '',
    phone       VARCHAR(20)              DEFAULT '',
    avatar      VARCHAR(500)             DEFAULT '',
    status      SMALLINT         NOT NULL DEFAULT 1,
    last_login  TIMESTAMP                 DEFAULT NULL,
    extend      TEXT,
    PRIMARY KEY (id),
    UNIQUE (username)
);

-- 角色表
CREATE TABLE tb_role
(
    id          BIGSERIAL,
    create_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role_name   VARCHAR(255)    NOT NULL,
    description VARCHAR(500)             DEFAULT '',
    status      SMALLINT         NOT NULL DEFAULT 1,
    extend      TEXT,
    PRIMARY KEY (id)
);

-- 菜单表
CREATE TABLE tb_menu
(
    id          BIGSERIAL,
    create_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parent_id   BIGINT          NOT NULL DEFAULT 0,
    menu_code   VARCHAR(64)     NOT NULL,
    menu_name   VARCHAR(255)    NOT NULL,
    menu_type   SMALLINT         NOT NULL DEFAULT 1,
    path        VARCHAR(255)             DEFAULT '',
    component   VARCHAR(255)             DEFAULT '',
    icon        VARCHAR(255)             DEFAULT '',
    sort_order  INT             NOT NULL DEFAULT 0,
    status      SMALLINT         NOT NULL DEFAULT 1,
    permission  VARCHAR(255)             DEFAULT '',
    meta JSON,
    extend      TEXT,
    PRIMARY KEY (id),
    UNIQUE (menu_code),
)
    );

-- 用户角色关联表
CREATE TABLE tb_user_role
(
    id          BIGSERIAL,
    create_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id     BIGINT          NOT NULL,
    role_id     BIGINT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (user_id, role_id)
);

-- 角色菜单关联表
CREATE TABLE tb_role_menu
(
    id          BIGSERIAL,
    create_time TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role_id     BIGINT          NOT NULL,
    menu_id     BIGINT          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (role_id, menu_id)
);

-- 插入默认管理员用户 (密码: admin123)
-- 注意：这里的密码是使用PasswordUtils.encode("admin123")生成的
INSERT INTO tb_user (username, password, nickname, status)
VALUES ('admin', '$2a$10$salt123456789012345678901234567890123456789012345678901234567890', '管理员', 1);

-- 插入默认角色
INSERT INTO tb_role (role_name, description, status)
VALUES ('系统管理员', '系统管理员角色', 1),
       ('普通用户', '普通用户角色', 1);

-- 插入根级菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- Dashboard 仪表板目录
(1, 0, 'Dashboard', 'page.dashboard.title', 1, '/dashboard', '', 'carbon:dashboard', -1, 1, '',
 JSON_OBJECT('order', -1, 'title', 'page.dashboard.title', 'hideInMenu', false)),

-- System 系统管理目录
(2, 0, 'System', 'system.title', 1, '/system', '', 'carbon:settings', 9997, 1, '',
 JSON_OBJECT('icon', 'carbon:settings', 'order', 9997, 'title', 'system.title', 'badge', 'new', 'badgeType', 'normal',
             'badgeVariants', 'primary', 'hideInMenu', false)),

-- Media 媒体管理目录
(300, 0, 'Media', 'media.title', 1, '/media', '', 'mdi:server-network', 9996, 1, '',
 JSON_OBJECT('icon', 'mdi:server-network', 'order', 9996, 'title', 'media.title', 'hideInMenu', false)),

-- Project 项目管理目录
(9, 0, 'Project', 'demos.vben.title', 1, '/vben-admin', '', 'carbon:data-center', 9998, 1, '',
 JSON_OBJECT('badgeType', 'dot', 'order', 9998, 'title', 'demos.vben.title', 'icon', 'carbon:data-center', 'hideInMenu',
             false)),

-- About 关于页面
(10, 0, 'About', 'demos.vben.about', 2, '/about', '/about/index', 'lucide:copyright', 9999, 1, '',
 JSON_OBJECT('icon', 'lucide:copyright', 'order', 9999, 'title', 'demos.vben.about', 'hideInMenu', false));

-- 插入Dashboard子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- Analytics 分析页面
(101, 1, 'Analytics', 'page.dashboard.analytics', 2, '/analytics', '/dashboard/analytics/index', 'carbon:analytics', 1,
 1, '',
 JSON_OBJECT('affixTab', true, 'title', 'page.dashboard.analytics', 'hideInMenu', false)),

-- Workspace 工作台
(102, 1, 'Workspace', 'page.dashboard.workspace', 2, '/workspace', '/dashboard/workspace/index', 'carbon:workspace', 2,
 1, '',
 JSON_OBJECT('title', 'page.dashboard.workspace', 'hideInMenu', false));

-- 插入System子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 系统菜单管理
(201, 2, 'SystemMenu', 'system.menu.title', 2, '/system/menu', '/system/menu/list', 'mdi:menu', 1, 1,
 'System:Menu:List',
 JSON_OBJECT('icon', 'mdi:menu', 'title', 'system.menu.title', 'hideChildrenInMenu', true, 'hideInMenu', false)),

-- 系统角色管理
(202, 2, 'SystemRole', 'system.role.title', 2, '/system/role', '/system/role/list', 'mdi:account-group', 2, 1,
 'System:Role:List',
 JSON_OBJECT('icon', 'mdi:account-group', 'title', 'system.role.title', 'hideChildrenInMenu', true, 'hideInMenu',
             false)),

-- 系统用户管理
(203, 2, 'SystemUser', 'system.user.title', 2, '/system/user', '/system/user/list', 'mdi:account', 3, 1,
 'System:User:List',
 JSON_OBJECT('icon', 'mdi:account', 'title', 'system.user.title', 'hideChildrenInMenu', true, 'hideInMenu', false)),

-- 系统部门管理
(204, 2, 'SystemDept', 'system.dept.title', 2, '/system/dept', '/system/dept/list', 'charm:organisation', 4, 1,
 'System:Dept:List',
 JSON_OBJECT('icon', 'charm:organisation', 'title', 'system.dept.title', 'hideChildrenInMenu', true, 'hideInMenu',
             false));

-- 插入Media子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 流媒体节点管理
(301, 300, 'MediaNode', 'media.node.title', 2, '/media/node', '/media/node/list', 'mdi:server-network', 1, 1,
 'Media:Node:List',
 JSON_OBJECT('icon', 'mdi:server-network', 'title', 'media.node.title', 'hideInMenu', false)),

-- 流媒体节点详情 (隐藏在菜单中)
(302, 300, 'MediaNodeDetail', 'media.node.detail', 2, '/media/node/detail/:nodeKey', '/media/node/detail',
 'mdi:server-network', 2, 1,
 'Media:Node:List',
 JSON_OBJECT('hideInMenu', true, 'icon', 'mdi:server-network', 'title', 'media.node.detail')),

-- 流媒体列表
(303, 300, 'MediaList', 'media.list.title', 2, '/media/list', '/media/list/list', 'mdi:video-outline', 3, 1,
 'Media:List:Query',
 JSON_OBJECT('icon', 'mdi:video-outline', 'title', 'media.list.title', 'hideInMenu', false));

-- 插入System菜单的按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 菜单管理按钮
(20101, 201, 'SystemMenuCreate', 'system.menu.create', 3, null, null, '', 1, 1, 'System:Menu:Create',
 JSON_OBJECT('title', 'system.menu.create', 'hideInMenu', true)),
(20102, 201, 'SystemMenuEdit', 'system.menu.edit', 3, null, null, '', 2, 1, 'System:Menu:Edit',
 JSON_OBJECT('title', 'system.menu.edit', 'hideInMenu', true)),
(20103, 201, 'SystemMenuDelete', 'system.menu.delete', 3, null, null, '', 3, 1, 'System:Menu:Delete',
 JSON_OBJECT('title', 'system.menu.delete', 'hideInMenu', true)),

-- 角色管理按钮
(20201, 202, 'SystemRoleCreate', 'system.role.create', 3, null, null, '', 1, 1, 'System:Role:Create',
 JSON_OBJECT('title', 'system.role.create', 'hideInMenu', true)),
(20202, 202, 'SystemRoleEdit', 'system.role.edit', 3, null, null, '', 2, 1, 'System:Role:Edit',
 JSON_OBJECT('title', 'system.role.edit', 'hideInMenu', true)),
(20203, 202, 'SystemRoleDelete', 'system.role.delete', 3, null, null, '', 3, 1, 'System:Role:Delete',
 JSON_OBJECT('title', 'system.role.delete', 'hideInMenu', true)),

-- 用户管理按钮
(20301, 203, 'SystemUserCreate', 'system.user.create', 3, null, null, '', 1, 1, 'System:User:Create',
 JSON_OBJECT('title', 'system.user.create', 'hideInMenu', true)),
(20302, 203, 'SystemUserEdit', 'system.user.edit', 3, null, null, '', 2, 1, 'System:User:Edit',
 JSON_OBJECT('title', 'system.user.edit', 'hideInMenu', true)),
(20303, 203, 'SystemUserDelete', 'system.user.delete', 3, null, null, '', 3, 1, 'System:User:Delete',
 JSON_OBJECT('title', 'system.user.delete', 'hideInMenu', true)),

-- 部门管理按钮
(20401, 204, 'SystemDeptCreate', 'system.dept.create', 3, null, null, '', 1, 1, 'System:Dept:Create',
 JSON_OBJECT('title', 'system.dept.create', 'hideInMenu', true)),
(20402, 204, 'SystemDeptEdit', 'system.dept.edit', 3, null, null, '', 2, 1, 'System:Dept:Edit',
 JSON_OBJECT('title', 'system.dept.edit', 'hideInMenu', true)),
(20403, 204, 'SystemDeptDelete', 'system.dept.delete', 3, null, null, '', 3, 1, 'System:Dept:Delete',
 JSON_OBJECT('title', 'system.dept.delete', 'hideInMenu', true)),

-- 媒体节点管理按钮
(30101, 301, 'MediaNodeCreate', 'media.node.create', 3, null, null, '', 1, 1, 'Media:Node:Create',
 JSON_OBJECT('title', 'media.node.create', 'hideInMenu', true)),
(30102, 301, 'MediaNodeEdit', 'media.node.edit', 3, null, null, '', 2, 1, 'Media:Node:Edit',
 JSON_OBJECT('title', 'media.node.edit', 'hideInMenu', true)),
(30103, 301, 'MediaNodeDelete', 'media.node.delete', 3, null, null, '', 3, 1, 'Media:Node:Delete',
 JSON_OBJECT('title', 'media.node.delete', 'hideInMenu', true)),

-- 流媒体列表按钮
(30301, 303, 'MediaStreamClose', 'media.list.actions.close', 3, null, null, '', 1, 1, 'Media:Stream:Close',
 JSON_OBJECT('title', 'media.list.actions.close', 'hideInMenu', true)),
(30302, 303, 'MediaStreamExport', 'media.list.actions.export', 3, null, null, '', 2, 1, 'Media:List:Export',
 JSON_OBJECT('title', 'media.list.actions.export', 'hideInMenu', true));

-- 插入ProtocolLab(协议验证台)菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 协议验证台目录
(400, 0, 'ProtocolLab', 'protocolLab.category', 1, '/protocol-lab', '', 'mdi:lan-connect', 9995, 1, '',
 JSON_OBJECT('icon', 'mdi:lan-connect', 'order', 9995, 'title', 'protocolLab.category', 'hideInMenu', false)),

-- GB28181 协议验证台页面
(401, 400, 'ProtocolLabGb28181', 'protocolLab.menu', 2, '/protocol-lab/gb28181', '/protocol-lab/index',
 'mdi:lan-connect', 1, 1, 'ProtocolLab:Gb28181:Query',
 JSON_OBJECT('icon', 'mdi:lan-connect', 'title', 'protocolLab.menu', 'hideInMenu', false));

-- 插入Device(GB28181设备管理)菜单 + 列表
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 设备管理目录
(500, 0, 'Device', 'device.menu', 1, '/device', '', 'mdi:cctv', 9994, 1, '',
 JSON_OBJECT('icon', 'mdi:cctv', 'order', 9994, 'title', 'device.menu', 'hideInMenu', false)),

-- 设备列表
(501, 500, 'DeviceList', 'device.title', 2, '/device/list', '/device/list', 'mdi:cctv', 1, 1,
 'Device:Device:Query',
 JSON_OBJECT('icon', 'mdi:cctv', 'title', 'device.title', 'hideInMenu', false)),

-- 设备通道列表(S5 钻取页,隐藏于菜单,由设备列表「通道数」path 导航进入)
(502, 500, 'DeviceChannelList', 'device.channel.title', 2, '/device/channel/:deviceId', '/device/channel/list',
 'mdi:video-input-component', 2, 1, 'Device:Device:Query',
 JSON_OBJECT('icon', 'mdi:video-input-component', 'title', 'device.channel.title', 'hideInMenu', true));

-- 插入Device按钮权限(menu_type=3,隐藏,仅用于鉴权,与前端 hasAccessByCodes 引用对齐)
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
(50101, 501, 'DeviceDetail', 'device.action.detail', 3, null, null, '', 1, 1, 'Device:Device:Query',
 JSON_OBJECT('title', 'device.action.detail', 'hideInMenu', true)),
(50102, 501, 'DeviceLive', 'device.action.live', 3, null, null, '', 2, 1, 'Device:Cmd:Live',
 JSON_OBJECT('title', 'device.action.live', 'hideInMenu', true)),
(50103, 501, 'DevicePtz', 'device.section.ptz', 3, null, null, '', 3, 1, 'Device:Cmd:Ptz',
 JSON_OBJECT('title', 'device.section.ptz', 'hideInMenu', true)),
(50104, 501, 'DeviceQuery', 'device.section.query', 3, null, null, '', 4, 1, 'Device:Cmd:Query',
 JSON_OBJECT('title', 'device.section.query', 'hideInMenu', true)),
(50105, 501, 'DeviceConfig', 'device.section.config', 3, null, null, '', 5, 1, 'Device:Cmd:Config',
 JSON_OBJECT('title', 'device.section.config', 'hideInMenu', true)),
(50106, 501, 'DeviceRecord', 'device.section.record', 3, null, null, '', 6, 1, 'Device:Cmd:Record',
 JSON_OBJECT('title', 'device.section.record', 'hideInMenu', true)),
(50107, 501, 'DeviceAlarm', 'device.section.alarm', 3, null, null, '', 7, 1, 'Device:Cmd:Alarm',
 JSON_OBJECT('title', 'device.section.alarm', 'hideInMenu', true)),
(50108, 501, 'DeviceBroadcast', 'device.action.broadcast', 3, null, null, '', 8, 1, 'Device:Cmd:Broadcast',
 JSON_OBJECT('title', 'device.action.broadcast', 'hideInMenu', true)),
(50109, 501, 'DeviceEdit', 'device.action.edit', 3, null, null, '', 9, 1, 'Device:Device:Edit',
 JSON_OBJECT('title', 'device.action.edit', 'hideInMenu', true)),
(50110, 501, 'DeviceDelete', 'device.action.delete', 3, null, null, '', 10, 1, 'Device:Device:Delete',
 JSON_OBJECT('title', 'device.action.delete', 'hideInMenu', true)),
(50111, 501, 'DeviceSubscription', 'device.section.subscribe', 3, null, null, '', 11, 1, 'Device:Subscription:Edit',
 JSON_OBJECT('title', 'device.section.subscribe', 'hideInMenu', true)),

-- 设备通道列表（502）按钮权限：编辑 / 删除（含批量删除、清离线，共用 Delete 权限码）
(50201, 502, 'DeviceChannelEdit', 'device.action.edit', 3, null, null, '', 1, 1, 'Device:Channel:Edit',
 JSON_OBJECT('title', 'device.action.edit', 'hideInMenu', true)),
(50202, 502, 'DeviceChannelDelete', 'device.action.delete', 3, null, null, '', 2, 1, 'Device:Channel:Delete',
 JSON_OBJECT('title', 'device.action.delete', 'hideInMenu', true));

-- 插入级联管理目录（600）+ 平台列表(601) / 通道映射(602) 页面（前端视图 P7 落地，本轮占位+权限码）
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
(600, 0, 'Cascade', 'cascade.title', 1, '/cascade', '', 'mdi:transit-connection-variant', 9993, 1, '',
 JSON_OBJECT('icon', 'mdi:transit-connection-variant', 'order', 9993, 'title', 'cascade.title', 'hideInMenu', false)),
(601, 600, 'CascadePlatform', 'cascade.platform.title', 2, '/cascade/platform', '/cascade/platform/list',
 'mdi:server-network-outline', 1, 1, 'Cascade:Platform:List',
 JSON_OBJECT('icon', 'mdi:server-network-outline', 'title', 'cascade.platform.title', 'hideInMenu', false)),
(602, 600, 'CascadeChannel', 'cascade.channel.title', 2, '/cascade/channel', '/cascade/channel/list',
 'mdi:swap-horizontal', 2, 1, 'Cascade:Channel:List',
 JSON_OBJECT('icon', 'mdi:swap-horizontal', 'title', 'cascade.channel.title', 'hideInMenu', false));

-- 级联平台列表（601）/ 通道映射（602）按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
(60101, 601, 'CascadePlatformCreate', 'cascade.platform.create', 3, null, null, '', 1, 1, 'Cascade:Platform:Create',
 JSON_OBJECT('title', 'cascade.platform.create', 'hideInMenu', true)),
(60102, 601, 'CascadePlatformEdit', 'cascade.platform.edit', 3, null, null, '', 2, 1, 'Cascade:Platform:Edit',
 JSON_OBJECT('title', 'cascade.platform.edit', 'hideInMenu', true)),
(60103, 601, 'CascadePlatformDelete', 'cascade.platform.delete', 3, null, null, '', 3, 1, 'Cascade:Platform:Delete',
 JSON_OBJECT('title', 'cascade.platform.delete', 'hideInMenu', true)),
(60104, 601, 'CascadePlatformStatus', 'cascade.platform.status', 3, null, null, '', 4, 1, 'Cascade:Platform:Status',
 JSON_OBJECT('title', 'cascade.platform.status', 'hideInMenu', true)),
(60201, 602, 'CascadeChannelCreate', 'cascade.channel.create', 3, null, null, '', 1, 1, 'Cascade:Channel:Create',
 JSON_OBJECT('title', 'cascade.channel.create', 'hideInMenu', true)),
(60202, 602, 'CascadeChannelEdit', 'cascade.channel.edit', 3, null, null, '', 2, 1, 'Cascade:Channel:Edit',
 JSON_OBJECT('title', 'cascade.channel.edit', 'hideInMenu', true)),
(60203, 602, 'CascadeChannelDelete', 'cascade.channel.delete', 3, null, null, '', 3, 1, 'Cascade:Channel:Delete',
 JSON_OBJECT('title', 'cascade.channel.delete', 'hideInMenu', true));

-- 插入Project子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- VbenDocument 文档
(901, 9, 'VbenDocument', 'demos.vben.document', 2, '/vben-admin/document', 'IFrameView', 'carbon:book', 1, 1, '',
 JSON_OBJECT('icon', 'carbon:book', 'iframeSrc', 'https://doc.vben.pro', 'title', 'demos.vben.document', 'hideInMenu',
             false)),

-- VbenGithub Github链接
(902, 9, 'VbenGithub', 'Github', 2, '/vben-admin/github', 'IFrameView', 'carbon:logo-github', 2, 1, '',
 JSON_OBJECT('icon', 'carbon:logo-github', 'link', 'https://github.com/vbenjs/vue-vben-admin', 'title', 'Github',
             'hideInMenu', false)),

-- VbenAntdv Antdv链接 (状态为禁用)
(903, 9, 'VbenAntdv', 'demos.vben.antdv', 2, '/vben-admin/antdv', 'IFrameView', 'carbon:hexagon-vertical-solid', 3,
 0, '',
 JSON_OBJECT('icon', 'carbon:hexagon-vertical-solid', 'badgeType', 'dot', 'link', 'https://ant.vben.pro', 'title',
             'demos.vben.antdv', 'hideInMenu', false));

-- 给管理员用户分配管理员角色
INSERT INTO tb_user_role (user_id, role_id)
VALUES (1, 1);

-- 给管理员角色分配所有菜单权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE status = 1;

-- ----------------------------
-- Table structure for tb_stream_proxy
-- ----------------------------
DROP TABLE IF EXISTS tb_stream_proxy;
CREATE TABLE tb_stream_proxy
(
    id            BIGSERIAL,
    create_time   TIMESTAMP                                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP                                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    app           VARCHAR(255)  NOT NULL,
    stream        VARCHAR(255)  NOT NULL,
    url           VARCHAR(1000) NOT NULL,
    status        INT                                                     NOT NULL DEFAULT 1,
    online_status INT                                                     NOT NULL DEFAULT 0,
    proxy_key     VARCHAR(255)           DEFAULT NULL,
    server_id VARCHAR(64) DEFAULT NULL,
    enabled       SMALLINT(1)                                              NOT NULL DEFAULT 1,
    description   VARCHAR(500)           DEFAULT '',
    extend        TEXT,
    PRIMARY KEY (id),
    UNIQUE (app, stream),
);

-- ----------------------------
-- Table structure for tb_push_proxy
-- ----------------------------
DROP TABLE IF EXISTS tb_push_proxy;
CREATE TABLE tb_push_proxy
(
    id            BIGSERIAL,
    create_time   TIMESTAMP                                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP                                                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    app           VARCHAR(255)  NOT NULL,
    stream        VARCHAR(255)  NOT NULL,
    dst_url       VARCHAR(1000) NOT NULL,
    schema        VARCHAR(50)   NOT NULL DEFAULT 'rtmp',
    status        INT                                                     NOT NULL DEFAULT 1,
    online_status INT                                                     NOT NULL DEFAULT 0,
    proxy_key     VARCHAR(255)           DEFAULT NULL,
    server_id     VARCHAR(64)            DEFAULT NULL,
    enabled       SMALLINT(1)                                              NOT NULL DEFAULT 1,
    description   VARCHAR(500)           DEFAULT '',
    extend TEXT,
    PRIMARY KEY (id),
    UNIQUE (app, stream),
);

-- ----------------------------
-- Table structure for tb_media_session
-- ----------------------------
DROP TABLE IF EXISTS tb_media_session;
CREATE TABLE tb_media_session
(
    id           BIGSERIAL,
    create_time  TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    call_id      VARCHAR(255) NOT NULL,
    device_id    VARCHAR(64)           DEFAULT NULL,
    channel_id   VARCHAR(64)           DEFAULT NULL,
    ssrc         VARCHAR(32)           DEFAULT NULL,
    stream       VARCHAR(255)          DEFAULT NULL,
    status       INT                                                   NOT NULL DEFAULT 2,
    session_type VARCHAR(32)           DEFAULT NULL,
    extend       TEXT,
    stream_id      VARCHAR(128)         DEFAULT NULL,
    node_server_id VARCHAR(64)          DEFAULT NULL,
    ref_count      INT                                                   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE (call_id),
    UNIQUE (stream_id),
);

-- ----------------------------
-- Table structure for tb_alarm
-- ----------------------------
DROP TABLE IF EXISTS tb_alarm;
CREATE TABLE tb_alarm
(
    id          BIGSERIAL,
    create_time TIMESTAMP                                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP                                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id   VARCHAR(64) NOT NULL,
    channel_id  VARCHAR(64)          DEFAULT NULL,
    alarm_type  INT                                                           DEFAULT NULL,
    alarm_level INT                                                           DEFAULT NULL,
    alarm_time  TIMESTAMP                                                      DEFAULT NULL,
    description VARCHAR(512)         DEFAULT NULL,
    ack_status  INT                                                  NOT NULL DEFAULT 0,
    extend      TEXT,
    PRIMARY KEY (id),
);


-- ----------------------------
-- Table structure for tb_device_subscription  (GB28181-2022 订阅状态)
-- ----------------------------
DROP TABLE IF EXISTS tb_device_subscription;
CREATE TABLE tb_device_subscription
(
    id               BIGSERIAL,
    create_time      TIMESTAMP                                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP                                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id        VARCHAR(64) NOT NULL,
    sub_type         VARCHAR(32) NOT NULL,
    enabled          SMALLINT                                              NOT NULL DEFAULT 0,
    status           SMALLINT                                              NOT NULL DEFAULT 0,
    call_id          VARCHAR(255)         DEFAULT NULL,
    expires          INT                                                           DEFAULT NULL,
    interval_sec     INT                                                           DEFAULT NULL,
    expire_time      TIMESTAMP                                                      DEFAULT NULL,
    last_notify_time TIMESTAMP                                                      DEFAULT NULL,
    extend           TEXT,
    PRIMARY KEY (id),
    UNIQUE (device_id, sub_type),
);


-- ----------------------------
-- Table structure for tb_device_position  (��动位置落库)
-- ----------------------------
DROP TABLE IF EXISTS tb_device_position;
CREATE TABLE tb_device_position
(
    id            BIGSERIAL,
    create_time   TIMESTAMP                                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP                                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id     VARCHAR(64) NOT NULL,
    channel_id    VARCHAR(64)          DEFAULT NULL,
    longitude     VARCHAR(32)          DEFAULT NULL,
    latitude      VARCHAR(32)          DEFAULT NULL,
    speed         VARCHAR(32)          DEFAULT NULL,
    direction     VARCHAR(32)          DEFAULT NULL,
    altitude      VARCHAR(32)          DEFAULT NULL,
    position_time TIMESTAMP                                                      DEFAULT NULL,
    extend        TEXT,
    PRIMARY KEY (id),
);


-- ----------------------------
-- Table structure for tb_cascade_platform  (级联上级平台)
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_platform;
CREATE TABLE tb_cascade_platform
(
    id                 BIGSERIAL,
    create_time        TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    platform_id        VARCHAR(64) NOT NULL,
    platform_ip        VARCHAR(64) NOT NULL,
    platform_port      INT                                                   NOT NULL,
    platform_domain    VARCHAR(64) NOT NULL,
    username           VARCHAR(64) NOT NULL DEFAULT '',
    password           VARCHAR(128) NOT NULL DEFAULT '',
    local_client_id    VARCHAR(64) NOT NULL,
    local_ip           VARCHAR(64)          DEFAULT NULL,
    local_port         INT                                                   NOT NULL DEFAULT 5070,
    enabled            SMALLINT                                               NOT NULL DEFAULT 1,
    register_status    SMALLINT                                               NOT NULL DEFAULT 0,
    keepalive_interval INT                                                   NOT NULL DEFAULT 60,
    register_expires   INT                                                   NOT NULL DEFAULT 3600,
    charset            VARCHAR(10) NOT NULL DEFAULT 'GB2312',
    transport          VARCHAR(10) NOT NULL DEFAULT 'UDP',
    extend             TEXT,
    PRIMARY KEY (id),
    UNIQUE (platform_id)
);


-- ----------------------------
-- Table structure for tb_cascade_channel  (级联上报通道)
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_channel;
CREATE TABLE tb_cascade_channel
(
    id                 BIGSERIAL,
    create_time        TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    platform_id        VARCHAR(64) NOT NULL,
    local_device_id    VARCHAR(64) NOT NULL,
    local_channel_id   VARCHAR(64) NOT NULL,
    cascade_channel_id VARCHAR(64) NOT NULL,
    cascade_name       VARCHAR(255)         DEFAULT NULL,
    enabled            SMALLINT                                               NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE (platform_id, local_channel_id)
);


-- ----------------------------
-- Table structure for tb_cascade_subscribe  (上级订本平台 → 据此主动推送)
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_subscribe;
CREATE TABLE tb_cascade_subscribe
(
    id           BIGSERIAL,
    create_time  TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    platform_id  VARCHAR(64) NOT NULL,
    sub_type     VARCHAR(32) NOT NULL,
    call_id      VARCHAR(255)         DEFAULT NULL,
    sn           VARCHAR(64)          DEFAULT NULL,
    expires      INT                                                   NOT NULL DEFAULT 3600,
    interval_sec INT                                                            DEFAULT NULL,
    expire_time  TIMESTAMP                                                       DEFAULT NULL,
    status       SMALLINT                                               NOT NULL DEFAULT 1,
    extend       TEXT,
    PRIMARY KEY (id),
    UNIQUE (platform_id, sub_type),
);


-- ----------------------------
-- Table structure for tb_cascade_record_request  (上级查录像 → 转查真实设备 → 异步聚合回包)
-- ----------------------------
DROP TABLE IF EXISTS tb_cascade_record_request;
CREATE TABLE tb_cascade_record_request
(
    id                 BIGSERIAL,
    create_time        TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    platform_id        VARCHAR(64) NOT NULL,
    superior_sn        VARCHAR(64) NOT NULL,
    cascade_channel_id VARCHAR(64) NOT NULL,
    local_device_id    VARCHAR(64) NOT NULL,
    local_channel_id   VARCHAR(64) NOT NULL,
    local_sn           VARCHAR(64)          DEFAULT NULL,
    start_time         VARCHAR(32)          DEFAULT NULL,
    end_time           VARCHAR(32)          DEFAULT NULL,
    status             SMALLINT                                               NOT NULL DEFAULT 0,
    extend             TEXT,
    PRIMARY KEY (id),
);


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 插入拉流代理管理菜单
-- ----------------------------
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon,
                       sort_order, status, permission, meta)
VALUES
-- 拉流代理管理主菜单
(304, 300, 'MediaStreamProxy', 'media.streamProxy.title', 2, '/media/stream-proxy', '/media/stream-proxy/list',
 'mdi:video-switch', 4, 1, 'Media:StreamProxy:List',
 JSON_OBJECT('icon', 'mdi:video-switch', 'title', 'media.streamProxy.title', 'hideInMenu', false));

-- ----------------------------
-- 插入拉流代理管理按钮权限
-- ----------------------------
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon,
                       sort_order, status, permission, meta)
VALUES
-- 新增拉流代理按钮
(30401, 304, 'MediaStreamProxyCreate', 'media.streamProxy.create', 3, null, null, '', 1, 1, 'Media:StreamProxy:Create',
 JSON_OBJECT('title', 'media.streamProxy.create', 'hideInMenu', true)),

-- 编辑拉流代理按钮
(30402, 304, 'MediaStreamProxyEdit', 'media.streamProxy.edit', 3, null, null, '', 2, 1, 'Media:StreamProxy:Edit',
 JSON_OBJECT('title', 'media.streamProxy.edit', 'hideInMenu', true)),

-- 删除拉流代理按钮
(30403, 304, 'MediaStreamProxyDelete', 'media.streamProxy.delete', 3, null, null, '', 3, 1, 'Media:StreamProxy:Delete',
 JSON_OBJECT('title', 'media.streamProxy.delete', 'hideInMenu', true)),

-- 查看拉流代理详情按钮
(30404, 304, 'MediaStreamProxyView', 'media.streamProxy.view', 3, null, null, '', 4, 1, 'Media:StreamProxy:View',
 JSON_OBJECT('title', 'media.streamProxy.view', 'hideInMenu', true)),

-- 启用/禁用拉流代理按钮
(30405, 304, 'MediaStreamProxyStatus', 'media.streamProxy.status', 3, null, null, '', 5, 1, 'Media:StreamProxy:Status',
 JSON_OBJECT('title', 'media.streamProxy.status', 'hideInMenu', true)),

-- 播放拉流代理按钮
(30406, 304, 'MediaStreamProxyPlay', 'media.streamProxy.play', 3, null, null, '', 6, 1, 'Media:StreamProxy:Play',
 '{
   "title": "media.streamProxy.play",
   "hideInMenu": true
 }');

-- ----------------------------
-- 给管理员角色分配新菜单权限
-- ----------------------------
INSERT INTO tb_role_menu (role_id, menu_id)
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
    (305, 300, 'MediaPushProxy', 'media.pushProxy.title', 2, '/media/push-proxy', '/media/push-proxy/list', 'mdi:video-switch-outline', 5, 1, 'Media:PushProxy:List', '{"icon": "mdi:video-switch-outline", "title": "media.pushProxy.title", "hideInMenu": false}');

-- ----------------------------
-- 插入推流代理管理按钮权限（简化版）
-- ----------------------------
INSERT OR
REPLACE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                      permission, meta)
VALUES
-- 查看推流代理权限（包含列表查看、详情查看、播放等只读操作）
    (30501, 305, 'MediaPushProxyView', 'media.pushProxy.view', 3, null, null, '', 1, 1, 'Media:PushProxy:View', '{"title": "media.pushProxy.view", "hideInMenu": true}'),

-- 修改推流代理权限（包含新增、编辑、删除、状态切换、启动、停止等所有操作）
    (30502, 305, 'MediaPushProxyEdit', 'media.pushProxy.edit', 3, null, null, '', 2, 1, 'Media:PushProxy:Edit', '{"title": "media.pushProxy.edit", "hideInMenu": true}');

-- ----------------------------
-- 给管理员角色分配推流代理菜单权限
-- ----------------------------
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (305, 30501, 30502);

-- ----------------------------
-- 验证插入结果
-- ----------------------------
SELECT m.id,
       m.parent_id,
       m.menu_code,
       m.menu_name,
       m.menu_type,
       m.path,
       m.component,
       m.icon,
       m.sort_order,
       m.status,
       m.permission,
       JSON_EXTRACT(m.meta, '$.title') as meta_title
FROM tb_menu m
WHERE m.id IN (304, 30401, 30402, 30403, 30404, 30405)
ORDER BY m.id;

-- ----------------------------
-- 验证角色权限分配
-- ----------------------------
SELECT rm.role_id,
       rm.menu_id,
       m.menu_name,
       m.permission
FROM tb_role_menu rm
         JOIN tb_menu m ON rm.menu_id = m.id
WHERE rm.menu_id IN (304, 30401, 30402, 30403, 30404, 30405)
ORDER BY rm.menu_id;

SET FOREIGN_KEY_CHECKS = 1;