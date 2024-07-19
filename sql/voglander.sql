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
    `register_time`  datetime                                               NOT NULL COMMENT '注册时间',
    `keepalive_time` datetime                                               NOT NULL COMMENT '心跳时间',
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