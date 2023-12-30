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

 Date: 30/12/2023 20:05:39
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
    `status`         int                                                    NOT NULL DEFAULT '0' COMMENT '状态 1在线 0离线',
    `name`           varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin          DEFAULT NULL COMMENT '自定义名称',
    `ip`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT 'IP',
    `port`           int                                                    NOT NULL COMMENT '端口',
    `register_time`  datetime                                               NOT NULL COMMENT '注册时间',
    `keepalive_time` datetime                                               NOT NULL COMMENT '心跳时间',
    `server_ip`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '注册节点',
    `extend`         text COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device` (`device_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 103
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

-- ----------------------------
-- Table structure for tb_device_config
-- ----------------------------
DROP TABLE IF EXISTS `tb_device_config`;
CREATE TABLE `tb_device_config`
(
    `id`          bigint unsigned                                        NOT NULL AUTO_INCREMENT,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `device_id`   bigint                                                 NOT NULL COMMENT '设备ID',
    `key`         varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin  NOT NULL COMMENT '键',
    `value`       varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '值',
    `extend`      text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `device_id` (`device_id`, `key`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin;

SET FOREIGN_KEY_CHECKS = 1;
