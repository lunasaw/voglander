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
    UNIQUE KEY `uk_device` (`device_id`) USING BTREE
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
    `channel_id`  varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通道Id',
    `device_id`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin        NOT NULL COMMENT '设备ID',
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci         DEFAULT NULL COMMENT '通道名称',
    `extend`      text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `channel_id` (`channel_id`, `device_id`) USING BTREE
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


select nextval('seq_test1_num1')

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
    visible     TINYINT         NOT NULL DEFAULT 1 COMMENT '是否显示 1显示 0隐藏',
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