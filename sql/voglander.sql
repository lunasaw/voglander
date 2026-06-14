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
DROP TABLE IF EXISTS `tb_device`;
CREATE TABLE `tb_device`
(
    `id`             bigint unsigned                                        NOT NULL AUTO_INCREMENT,
    `create_time`    datetime                                               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime                                               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `device_id`      varchar(64) COLLATE utf8mb4_bin                        NOT NULL COMMENT '设备ID',
    `type`           int                                                    NOT NULL DEFAULT '1' COMMENT '设备协议类型 1:GB28181',
    `status`         int                                                    NOT NULL DEFAULT '0' COMMENT '状态 1在线 0离线',
    `name`           varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT '' COMMENT '自定义名称',
    `ip`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT 'IP',
    `port`           int                                                    NOT NULL COMMENT '端口',
    `register_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `keepalive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '心跳时间',
    `server_ip`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '注册节点',
    `extend`         text COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device` (`device_id`) USING BTREE,
    -- Phase 2a：状态/心跳查询与离线扫描的支撑索引（修 P8 全表扫）
    KEY `idx_status_keepalive` (`status`, `keepalive_time`) USING BTREE,
    KEY `idx_server_ip` (`server_ip`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 202
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

-- ----------------------------
-- Table structure for tb_device_channel
-- ----------------------------
DROP TABLE IF EXISTS `tb_device_channel`;
CREATE TABLE `tb_device_channel`
(
    `id`          bigint                                                       NOT NULL AUTO_INCREMENT,
    `create_time` datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `status`      int                                                          NOT NULL DEFAULT '0' COMMENT '状态 1在线 0离线',
    `channel_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '通道Id',
    `device_id`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin        NOT NULL COMMENT '设备ID',
    `name`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '通道名称',
    `extend`      text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    `last_seen_time` datetime                                                              DEFAULT NULL COMMENT '通道最近一次被目录/会话感知的时间',
    `status_source`  varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin                DEFAULT NULL COMMENT '当前 status 的来源：CATALOG / OFFLINE_CASCADE / SESSION / MANUAL / MISSING',
    `missing_count`  int                                                          NOT NULL DEFAULT '0' COMMENT '连续目录响应中未出现的次数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `channel_id` (`channel_id`, `device_id`) USING BTREE,
    -- Phase 2a：复合 UNIQUE 最左前缀是 channel_id，无法支撑"按 device_id 查通道"，补单列索引
    KEY `idx_device_id` (`device_id`) USING BTREE,
    -- 1.0.4：cascadeOffline + 列表过滤 + 单调查询共用复合索引
    KEY `idx_device_status` (`device_id`, `status`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 23
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

-- ----------------------------
-- Table structure for tb_device_config
-- ----------------------------
DROP TABLE IF EXISTS `tb_device_config`;
CREATE TABLE `tb_device_config`
(
    `id`           bigint unsigned                                        NOT NULL AUTO_INCREMENT,
    `create_time`  datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `device_id`    bigint                                                 NOT NULL COMMENT '设备ID',
    `config_key`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT '键',
    `config_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '值',
    `extend`       text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `device_id` (`device_id`, `config_key`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

-- ----------------------------
-- Table structure for tb_export_task
-- ----------------------------
DROP TABLE IF EXISTS `tb_export_task`;
CREATE TABLE `tb_export_task`
(
    `id`          bigint                                                       NOT NULL AUTO_INCREMENT COMMENT 'ID自增',
    `gmt_create`  datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_update`  datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `biz_id`      varchar(255)                                                 NOT NULL COMMENT '业务ID',
    `member_cnt`  bigint                                                       NOT NULL DEFAULT '0' COMMENT '导出的记录总数',
    `format`      varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '文件格式',
    `apply_time`  datetime                                                     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `export_time` datetime                                                              DEFAULT NULL COMMENT '导出报表时间',
    `url`         varchar(2500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci        DEFAULT NULL COMMENT '文件下载地址, 多个url用、隔开',
    `status`      int                                                          NOT NULL DEFAULT '0' COMMENT '是否完成，0->处理中, 1 -> 完成',
    `deleted`     int                                                          NOT NULL DEFAULT '0' COMMENT '是否删除，1 -> 删除, 0 -> 未删除',
    `param`       text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '搜索条件序列化',
    `name`        varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci         DEFAULT NULL COMMENT '导出名称',
    `type`        int                                                          NOT NULL DEFAULT '0' COMMENT '导出类型',
    `expired`     int                                                          NOT NULL DEFAULT '0' COMMENT '是否过期，1 -> 过期，0 -> 未过期',
    `extend`      text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci COMMENT '扩展信息',
    `apply_user`  varchar(256) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci         DEFAULT NULL COMMENT '申请人Id',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_shop_id` (`biz_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 38
  DEFAULT CHARSET = utf8mb3 COMMENT ='报表导出';

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------
--  Sequence structure for seq_test1_num1
-- ---------------------------
drop table if exists sequence;
create table sequence
(
    seq_name      VARCHAR(50) NOT NULL,           -- 序列名称
    current_val   INT         NOT NULL,           -- 当前值
    increment_val INT         NOT NULL DEFAULT 1, -- 步长(跨度)
    PRIMARY KEY (seq_name)
);

INSERT INTO sequence
VALUES ('seq_test1_num1', '0', '1');
INSERT INTO sequence
VALUES ('seq_test1_num2', '0', '2');


create function currval(v_seq_name VARCHAR(50))
    returns integer(11)
    READS SQL DATA
begin
    declare value integer;
    set value = 0;
    select current_val into value from sequence where seq_name = v_seq_name;
    return value;
end;

create function nextval(v_seq_name VARCHAR(50))
    returns integer(11)
    READS SQL DATA
begin
    update sequence set current_val = current_val + increment_val where seq_name = v_seq_name;
    return currval(v_seq_name);
end;


select nextval('seq_test1_num1');

-- ----------------------------
-- Table structure for tb_media_node
-- ----------------------------
DROP TABLE IF EXISTS `tb_media_node`;
CREATE TABLE `tb_media_node`
(
    `id`           bigint unsigned                                        NOT NULL AUTO_INCREMENT,
    `create_time`  datetime                                               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime                                               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `server_id`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT '节点ID',
    `name`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT '' COMMENT '节点名称',
    `host`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '节点地址',
    `secret`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'API密钥',
    `enabled`      tinyint(1)                                             NOT NULL DEFAULT '1' COMMENT '是否启用 1启用 0禁用',
    `hook_enabled` tinyint(1)                                             NOT NULL DEFAULT '1' COMMENT '是否启用Hook 1启用 0禁用',
    `weight`       int                                                    NOT NULL DEFAULT '100' COMMENT '节点权重',
    `keepalive`    bigint                                                          DEFAULT '0' COMMENT '心跳时间戳',
    `status`       int                                                    NOT NULL DEFAULT '0' COMMENT '节点状态 1在线 0离线',
    `description`  varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT '' COMMENT '节点描述',
    `extend`       text COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_server_id` (`server_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '流媒体节点管理表';

-- 部门表
CREATE TABLE IF NOT EXISTS tb_dept
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    parent_id   BIGINT                DEFAULT 0 COMMENT '父部门ID，0表示根部门',
    dept_name   VARCHAR(100) NOT NULL COMMENT '部门名称',
    dept_code   VARCHAR(50) COMMENT '部门编码',
    remark      VARCHAR(500) COMMENT '部门描述',
    status      INT                   DEFAULT 1 COMMENT '状态：1启用，0禁用',
    sort_order  INT                   DEFAULT 0 COMMENT '排序',
    leader      VARCHAR(50) COMMENT '部门负责人',
    phone       VARCHAR(20) COMMENT '联系电话',
    email       VARCHAR(100) COMMENT '邮箱',
    extend      TEXT COMMENT '扩展字段',
    INDEX       idx_parent_id(parent_id
) ,
    INDEX idx_dept_name (dept_name),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 插入默认根部门
INSERT INTO tb_dept (parent_id, dept_name, dept_code, remark, status, sort_order, leader)
VALUES (0, '总公司', 'ROOT', '根部门', 1, 0, '系统管理员')
ON DUPLICATE KEY UPDATE dept_name = dept_name;

-- 用户表
CREATE TABLE tb_user
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    username    VARCHAR(64)     NOT NULL COMMENT '用户名',
    password    VARCHAR(255)    NOT NULL COMMENT '密码',
    nickname    VARCHAR(255)             DEFAULT '' COMMENT '昵称',
    email       VARCHAR(255)             DEFAULT '' COMMENT '邮箱',
    phone       VARCHAR(20)              DEFAULT '' COMMENT '手机号',
    avatar      VARCHAR(500)             DEFAULT '' COMMENT '头像URL',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    last_login  DATETIME                 DEFAULT NULL COMMENT '最后登录时间',
    extend      TEXT COMMENT '扩展字段',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '用户表';

-- 角色表
CREATE TABLE tb_role
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    role_name   VARCHAR(255)    NOT NULL COMMENT '角色名称',
    description VARCHAR(500)             DEFAULT '' COMMENT '角色描述',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    extend      TEXT COMMENT '扩展字段',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '角色表';

-- 菜单表
CREATE TABLE tb_menu
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    parent_id   BIGINT          NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_code   VARCHAR(64)     NOT NULL COMMENT '菜单编码',
    menu_name   VARCHAR(255)    NOT NULL COMMENT '菜单名称',
    menu_type   TINYINT         NOT NULL DEFAULT 1 COMMENT '菜单类型 1目录 2菜单 3按钮',
    path        VARCHAR(255)             DEFAULT '' COMMENT '路由路径',
    component   VARCHAR(255)             DEFAULT '' COMMENT '组件路径',
    icon        VARCHAR(255)             DEFAULT '' COMMENT '菜单图标',
    sort_order  INT             NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    permission  VARCHAR(255)             DEFAULT '' COMMENT '权限标识',
    meta JSON COMMENT '菜单元数据(JSON格式)',
    extend      TEXT COMMENT '扩展字段',
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_code (menu_code),
    KEY         idx_parent_id(parent_id
)
    ) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '菜单表';

-- 用户角色关联表
CREATE TABLE tb_user_role
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    user_id     BIGINT          NOT NULL COMMENT '用户ID',
    role_id     BIGINT          NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '用户角色关联表';

-- 角色菜单关联表
CREATE TABLE tb_role_menu
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    role_id     BIGINT          NOT NULL COMMENT '角色ID',
    menu_id     BIGINT          NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '角色菜单关联表';

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
WHERE status = 1
ON DUPLICATE KEY UPDATE role_id = role_id;

-- ----------------------------
-- Table structure for tb_stream_proxy
-- ----------------------------
DROP TABLE IF EXISTS `tb_stream_proxy`;
CREATE TABLE `tb_stream_proxy`
(
    `id`            BIGINT UNSIGNED                                         NOT NULL AUTO_INCREMENT,
    `create_time`   DATETIME                                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME                                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `app`           VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT '应用名称',
    `stream`        VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT '流名称',
    `url`           VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '流地址',
    `status`        INT                                                     NOT NULL DEFAULT 1 COMMENT '状态 1正常 0异常',
    `online_status` INT                                                     NOT NULL DEFAULT 0 COMMENT '在线状态 1在线 0离线',
    `proxy_key`     VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT NULL COMMENT '代理密钥',
    `server_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '节点ID，保存当前添加拉流代理的节点',
    `enabled`       TINYINT(1)                                              NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0禁用',
    `description`   VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT '' COMMENT '描述',
    `extend`        TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_stream` (`app`, `stream`) USING BTREE,
    KEY `idx_stream_proxy_key` (`proxy_key`) USING BTREE,
    KEY `idx_stream_proxy_status` (`status`) USING BTREE,
    KEY `idx_stream_proxy_online_status` (`online_status`) USING BTREE,
    KEY `idx_stream_proxy_server_id` (`server_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '流代理管理表';

-- ----------------------------
-- Table structure for tb_push_proxy
-- ----------------------------
DROP TABLE IF EXISTS `tb_push_proxy`;
CREATE TABLE `tb_push_proxy`
(
    `id`            BIGINT UNSIGNED                                         NOT NULL AUTO_INCREMENT,
    `create_time`   DATETIME                                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME                                                NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `app`           VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT '应用名称',
    `stream`        VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT '流名称',
    `dst_url`       VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '推流目标地址',
    `schema`        VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin   NOT NULL DEFAULT 'rtmp' COMMENT '推流协议 rtmp/rtsp',
    `status`        INT                                                     NOT NULL DEFAULT 1 COMMENT '状态 1正常 0异常',
    `online_status` INT                                                     NOT NULL DEFAULT 0 COMMENT '在线状态 1在线 0离线',
    `proxy_key`     VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT NULL COMMENT '代理密钥',
    `server_id`     VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin            DEFAULT NULL COMMENT '节点ID，保存当前添加推流代理的节点',
    `enabled`       TINYINT(1)                                              NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0禁用',
    `description`   VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT '' COMMENT '描述',
    `extend` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段，包含vhost、retryCount、rtpType、timeoutSec等ZLM特定参数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_stream` (`app`, `stream`) USING BTREE,
    KEY `idx_push_proxy_key` (`proxy_key`) USING BTREE,
    KEY `idx_push_proxy_status` (`status`) USING BTREE,
    KEY `idx_push_proxy_online_status` (`online_status`) USING BTREE,
    KEY `idx_push_proxy_server_id` (`server_id`) USING BTREE,
    KEY `idx_push_proxy_schema` (`schema`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='推流代理管理表';

-- ----------------------------
-- Table structure for tb_media_session
-- ----------------------------
DROP TABLE IF EXISTS `tb_media_session`;
CREATE TABLE `tb_media_session`
(
    `id`           BIGINT UNSIGNED                                       NOT NULL AUTO_INCREMENT,
    `create_time`  DATETIME                                              NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME                                              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `call_id`      VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT 'SIP Call-ID，会话唯一标识',
    `device_id`    VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT NULL COMMENT '设备GB28181编码',
    `channel_id`   VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT NULL COMMENT '通道GB28181编码',
    `ssrc`         VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT NULL COMMENT '媒体流SSRC',
    `stream`       VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '媒体流标识',
    `status`       INT                                                   NOT NULL DEFAULT 2 COMMENT '会话状态 1活跃 0关闭 2邀请中 3失败',
    `session_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin           DEFAULT NULL COMMENT '会话类型 PLAY/PLAYBACK/DOWNLOAD/TALK',
    `extend`       TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    `stream_id`      VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin         DEFAULT NULL COMMENT '前端稳定主键 gb_live_{deviceId}_{channelId}',
    `node_server_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '媒体节点 serverId，亲和路由',
    `ref_count`      INT                                                   NOT NULL DEFAULT 0 COMMENT '观看者计数，权威值以 Redis 为准，DB 为快照',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_call_id` (`call_id`) USING BTREE,
    UNIQUE KEY `uk_stream_id` (`stream_id`) USING BTREE,
    KEY `idx_media_session_device_id` (`device_id`) USING BTREE,
    KEY `idx_media_session_status` (`status`) USING BTREE,
    KEY `idx_status_node` (`status`, `node_server_id`) USING BTREE,
    KEY `idx_device_channel` (`device_id`, `channel_id`, `status`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='媒体会话表';

-- ----------------------------
-- Table structure for tb_alarm
-- ----------------------------
DROP TABLE IF EXISTS `tb_alarm`;
CREATE TABLE `tb_alarm`
(
    `id`          BIGINT UNSIGNED                                      NOT NULL AUTO_INCREMENT,
    `create_time` DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `device_id`   VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '设备GB28181编码',
    `channel_id`  VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '通道GB28181编码',
    `alarm_type`  INT                                                           DEFAULT NULL COMMENT '1移动侦测 2设备告警 3故障 4视频丢失',
    `alarm_level` INT                                                           DEFAULT NULL COMMENT '1-4，4最高',
    `alarm_time`  DATETIME                                                      DEFAULT NULL COMMENT '告警时间',
    `description` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin         DEFAULT NULL COMMENT '告警描述',
    `ack_status`  INT                                                  NOT NULL DEFAULT 0 COMMENT '0未确认 1已确认',
    `extend`      TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    KEY `idx_device_time` (`device_id`, `alarm_time`) USING BTREE,
    KEY `idx_level_status` (`alarm_level`, `ack_status`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='告警表';


-- ----------------------------
-- Table structure for tb_device_subscription  (GB28181-2022 订阅状态)
-- ----------------------------
DROP TABLE IF EXISTS `tb_device_subscription`;
CREATE TABLE `tb_device_subscription`
(
    `id`               BIGINT UNSIGNED                                      NOT NULL AUTO_INCREMENT,
    `create_time`      DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `device_id`        VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '设备GB28181编码',
    `sub_type`         VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '订阅类型 CATALOG/MOBILE_POSITION/ALARM',
    `enabled`          TINYINT                                              NOT NULL DEFAULT 0 COMMENT '意图：1开启 0关闭(前端开关)',
    `status`           TINYINT                                              NOT NULL DEFAULT 0 COMMENT '运行态：0INACTIVE 1ACTIVE 2PENDING 3FAILED',
    `call_id`          VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin         DEFAULT NULL COMMENT 'SUBSCRIBE dialog callId',
    `expires`          INT                                                           DEFAULT NULL COMMENT '订阅有效期(秒)',
    `interval_sec`     INT                                                           DEFAULT NULL COMMENT '位置上报间隔(秒),仅 MOBILE_POSITION',
    `expire_time`      DATETIME                                                      DEFAULT NULL COMMENT '本次订阅过期时间(=最后下发时间+expires)',
    `last_notify_time` DATETIME                                                      DEFAULT NULL COMMENT '最近一次收到该类通知的时间',
    `extend`           TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '告警过滤等扩展(FastJSON2)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_subscription` (`device_id`, `sub_type`) USING BTREE,
    KEY `idx_device_subscription_expire` (`status`, `expire_time`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='设备订阅状态表';


-- ----------------------------
-- Table structure for tb_device_position  (��动位置落库)
-- ----------------------------
DROP TABLE IF EXISTS `tb_device_position`;
CREATE TABLE `tb_device_position`
(
    `id`            BIGINT UNSIGNED                                      NOT NULL AUTO_INCREMENT,
    `create_time`   DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME                                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `device_id`     VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '设备GB28181编码',
    `channel_id`    VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '通道GB28181编码',
    `longitude`     VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '经度',
    `latitude`      VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '纬度',
    `speed`         VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '速度',
    `direction`     VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '方向',
    `altitude`      VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '海拔',
    `position_time` DATETIME                                                      DEFAULT NULL COMMENT '设备上报的定位时间',
    `extend`        TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    KEY `idx_device_position_device` (`device_id`, `position_time`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='设备移动位置表';


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 插入拉流代理管理菜单
-- ----------------------------
INSERT INTO `tb_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `menu_type`, `path`, `component`, `icon`,
                       `sort_order`, `status`, `permission`, `meta`)
VALUES
-- 拉流代理管理主菜单
(304, 300, 'MediaStreamProxy', 'media.streamProxy.title', 2, '/media/stream-proxy', '/media/stream-proxy/list',
 'mdi:video-switch', 4, 1, 'Media:StreamProxy:List',
 JSON_OBJECT('icon', 'mdi:video-switch', 'title', 'media.streamProxy.title', 'hideInMenu', false))
ON DUPLICATE KEY UPDATE `menu_name`  = VALUES(`menu_name`),
                        `path`       = VALUES(`path`),
                        `component`  = VALUES(`component`),
                        `icon`       = VALUES(`icon`),
                        `sort_order` = VALUES(`sort_order`),
                        `permission` = VALUES(`permission`),
                        `meta`       = VALUES(`meta`);

-- ----------------------------
-- 插入拉流代理管理按钮权限
-- ----------------------------
INSERT INTO `tb_menu` (`id`, `parent_id`, `menu_code`, `menu_name`, `menu_type`, `path`, `component`, `icon`,
                       `sort_order`, `status`, `permission`, `meta`)
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
 }')

ON DUPLICATE KEY UPDATE `menu_name`  = VALUES(`menu_name`),
                        `permission` = VALUES(`permission`),
                        `meta`       = VALUES(`meta`);

-- ----------------------------
-- 给管理员角色分配新菜单权限
-- ----------------------------
INSERT INTO `tb_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id`
FROM `tb_menu`
WHERE `id` IN (304, 30401, 30402, 30403, 30404, 30405)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

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
INSERT INTO `tb_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id`
FROM `tb_menu`
WHERE `id` IN (305, 30501, 30502)
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

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
FROM `tb_menu` m
WHERE m.id IN (304, 30401, 30402, 30403, 30404, 30405)
ORDER BY m.id;

-- ----------------------------
-- 验证角色权限分配
-- ----------------------------
SELECT rm.role_id,
       rm.menu_id,
       m.menu_name,
       m.permission
FROM `tb_role_menu` rm
         JOIN `tb_menu` m ON rm.menu_id = m.id
WHERE rm.menu_id IN (304, 30401, 30402, 30403, 30404, 30405)
ORDER BY rm.menu_id;

SET FOREIGN_KEY_CHECKS = 1;