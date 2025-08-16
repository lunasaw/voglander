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

-- 流代理管理表
DROP TABLE IF EXISTS tb_stream_proxy;
CREATE TABLE tb_stream_proxy
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    app           VARCHAR(255)  NOT NULL,
    stream        VARCHAR(255)  NOT NULL,
    url           VARCHAR(1000) NOT NULL,
    status        INTEGER       NOT NULL DEFAULT 1,
    online_status INTEGER       NOT NULL DEFAULT 0,
    proxy_key     VARCHAR(255),
    server_id     VARCHAR(64),
    enabled       INTEGER       NOT NULL DEFAULT 1,
    description   VARCHAR(500)           DEFAULT '',
    extend        TEXT,
    UNIQUE (app, stream)
);

-- Table structure for tb_push_proxy
DROP TABLE IF EXISTS tb_push_proxy;
CREATE TABLE tb_push_proxy
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    app           VARCHAR(255)  NOT NULL,
    stream        VARCHAR(255)  NOT NULL,
    dst_url       VARCHAR(1000) NOT NULL,
    schema        VARCHAR(50)   NOT NULL DEFAULT 'rtmp',
    vhost         VARCHAR(255)  NOT NULL DEFAULT '__defaultVhost__',
    retry_count   INTEGER       NOT NULL DEFAULT -1,
    rtp_type      INTEGER       NOT NULL DEFAULT 0,
    timeout_sec   INTEGER       NOT NULL DEFAULT 10,
    status        INTEGER       NOT NULL DEFAULT 1,
    online_status INTEGER       NOT NULL DEFAULT 0,
    proxy_key     VARCHAR(255),
    server_id     VARCHAR(64),
    enabled       INTEGER       NOT NULL DEFAULT 1,
    description   VARCHAR(500)           DEFAULT '',
    extend        TEXT,
    UNIQUE (app, stream)
);