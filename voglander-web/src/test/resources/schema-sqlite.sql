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
    extend           TEXT,
    last_seen_time   DATETIME,
    status_source    VARCHAR(32),
    missing_count    INTEGER NOT NULL DEFAULT 0,
    UNIQUE (channel_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_device_status ON tb_device_channel (device_id, status);

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

-- 媒体会话表
DROP TABLE IF EXISTS tb_media_session;
CREATE TABLE tb_media_session
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    call_id      VARCHAR(255) NOT NULL,
    device_id    VARCHAR(64),
    channel_id   VARCHAR(64),
    ssrc         VARCHAR(32),
    stream       VARCHAR(255),
    status       INTEGER      NOT NULL DEFAULT 2,
    session_type VARCHAR(32),
    extend       TEXT,
    stream_id      VARCHAR(128),
    node_server_id VARCHAR(64),
    ref_count      INTEGER NOT NULL DEFAULT 0,
    UNIQUE (call_id),
    UNIQUE (stream_id)
);
CREATE INDEX IF NOT EXISTS idx_media_session_status_node ON tb_media_session (status, node_server_id);
CREATE INDEX IF NOT EXISTS idx_media_session_device_channel ON tb_media_session (device_id, channel_id, status);

-- 告警表
DROP TABLE IF EXISTS tb_alarm;
CREATE TABLE tb_alarm
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    device_id   VARCHAR(64) NOT NULL,
    channel_id  VARCHAR(64),
    alarm_type  INTEGER,
    alarm_level INTEGER,
    alarm_time  DATETIME,
    description VARCHAR(512),
    ack_status  INTEGER     NOT NULL DEFAULT 0,
    extend      TEXT
);
CREATE INDEX IF NOT EXISTS idx_alarm_device_time ON tb_alarm (device_id, alarm_time);
CREATE INDEX IF NOT EXISTS idx_alarm_level_status ON tb_alarm (alarm_level, ack_status);

-- 级联上级平台表
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
CREATE UNIQUE INDEX IF NOT EXISTS uk_cascade_platform_id ON tb_cascade_platform (platform_id);

-- 级联上报通道表
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
CREATE UNIQUE INDEX IF NOT EXISTS uk_cascade_platform_local ON tb_cascade_channel (platform_id, local_channel_id);

-- 级联上级订阅表（上级订本平台 → 本平台据此主动推送）
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
CREATE UNIQUE INDEX IF NOT EXISTS uk_cascade_subscribe ON tb_cascade_subscribe (platform_id, sub_type);
CREATE INDEX IF NOT EXISTS idx_cascade_subscribe_expire ON tb_cascade_subscribe (status, expire_time);

-- 级联录像查询请求上下文表（上级查录像 → 转查真实设备 → 异步聚合回包）
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
CREATE INDEX IF NOT EXISTS idx_cascade_record_local ON tb_cascade_record_request (local_device_id, status);
CREATE INDEX IF NOT EXISTS idx_cascade_record_created ON tb_cascade_record_request (create_time);


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
CREATE UNIQUE INDEX IF NOT EXISTS uk_device_subscription ON tb_device_subscription (device_id, sub_type);
CREATE INDEX IF NOT EXISTS idx_device_subscription_expire ON tb_device_subscription (status, expire_time);

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
CREATE INDEX IF NOT EXISTS idx_device_position_device ON tb_device_position (device_id, position_time);