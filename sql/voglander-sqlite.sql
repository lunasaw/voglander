create table sequence
(
    seq_name      VARCHAR(50)   not null
        primary key,
    current_val   INT           not null,
    increment_val INT default 1 not null
);

create table sqlite_master
(
    type     TEXT,
    name     TEXT,
    tbl_name TEXT,
    rootpage INT,
    sql      TEXT
);

create table sqlite_sequence
(
    name,
    seq
);

create table tb_dept
(
    id          INTEGER
        primary key autoincrement,
    create_time DATETIME default CURRENT_TIMESTAMP not null,
    update_time DATETIME default CURRENT_TIMESTAMP not null,
    parent_id   INTEGER  default 0,
    dept_name   VARCHAR(100)                       not null,
    dept_code   VARCHAR(50),
    remark      VARCHAR(500),
    status      INTEGER  default 1,
    sort_order  INTEGER  default 0,
    leader      VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    extend      TEXT
);

create index idx_dept_name
    on tb_dept (dept_name);

create index idx_dept_parent_id
    on tb_dept (parent_id);

create index idx_dept_status
    on tb_dept (status);

create table tb_device
(
    id             INTEGER
        primary key autoincrement,
    create_time    DATETIME     default CURRENT_TIMESTAMP,
    update_time    DATETIME     default CURRENT_TIMESTAMP,
    device_id      VARCHAR(64)                            not null
        unique,
    type           INTEGER      default 1                 not null,
    status         INTEGER      default 0                 not null,
    name           VARCHAR(255) default '',
    ip             VARCHAR(64)                            not null,
    port           INTEGER                                not null,
    register_time  DATETIME     default CURRENT_TIMESTAMP not null,
    keepalive_time DATETIME     default CURRENT_TIMESTAMP not null,
    server_ip      VARCHAR(255)                           not null,
    extend         TEXT
);

create table tb_device_channel
(
    id          INTEGER
        primary key autoincrement,
    create_time DATETIME default CURRENT_TIMESTAMP,
    update_time DATETIME default CURRENT_TIMESTAMP,
    status      INTEGER  default 0 not null,
    channel_id  VARCHAR(50)        not null,
    device_id   VARCHAR(64)        not null,
    name        VARCHAR(255),
    extend      TEXT,
    unique (channel_id, device_id)
);

create table tb_device_config
(
    id           INTEGER
        primary key autoincrement,
    create_time  DATETIME default CURRENT_TIMESTAMP,
    update_time  DATETIME default CURRENT_TIMESTAMP,
    device_id    INTEGER      not null,
    config_key   VARCHAR(64)  not null,
    config_value VARCHAR(255) not null,
    extend       TEXT,
    unique (device_id, config_key)
);

create table tb_export_task
(
    id         INTEGER
        primary key autoincrement,
    gmt_create DATETIME default CURRENT_TIMESTAMP,
    gmt_update DATETIME default CURRENT_TIMESTAMP,
    biz_id     VARCHAR(255) not null,
    member_cnt INTEGER  default 0,
    format     VARCHAR(10)  not null,
    apply_time DATETIME default CURRENT_TIMESTAMP,
    export_time DATETIME,
    url         VARCHAR(2500),
    status     INTEGER  default 0,
    deleted    INTEGER  default 0,
    param       TEXT,
    name        VARCHAR(500),
    type       INTEGER  default 0,
    expired    INTEGER  default 0,
    extend      TEXT,
    apply_user  VARCHAR(256)
);

create table tb_media_node
(
    id           INTEGER
        primary key autoincrement,
    create_time  DATETIME     default CURRENT_TIMESTAMP not null,
    update_time  DATETIME     default CURRENT_TIMESTAMP not null,
    server_id    VARCHAR(64)                            not null
        constraint server_id_uk
            unique,
    name         VARCHAR(255) default '',
    host         VARCHAR(255)                           not null,
    secret       VARCHAR(255)                           not null,
    enabled      INTEGER      default 1                 not null,
    hook_enabled INTEGER      default 1                 not null,
    weight       INTEGER      default 100               not null,
    keepalive    INTEGER      default 0,
    status       INTEGER      default 0                 not null,
    description  VARCHAR(500) default '',
    extend       TEXT
);

create table tb_menu
(
    id          INTEGER
        primary key autoincrement,
    create_time DATETIME     default CURRENT_TIMESTAMP,
    update_time DATETIME     default CURRENT_TIMESTAMP,
    parent_id   INTEGER      default 0 not null,
    menu_code   VARCHAR(64)            not null
        unique,
    menu_name   VARCHAR(255)           not null,
    menu_type   INTEGER      default 1 not null,
    path        VARCHAR(255) default '',
    component   VARCHAR(255) default '',
    icon        VARCHAR(255) default '',
    sort_order  INTEGER      default 0 not null,
    status      INTEGER      default 1 not null,
    permission  VARCHAR(255) default '',
    extend      TEXT,
    meta        TEXT
);

create index idx_menu_parent_id
    on tb_menu (parent_id);

create table tb_role
(
    id          INTEGER
        primary key autoincrement,
    create_time DATETIME     default CURRENT_TIMESTAMP,
    update_time DATETIME     default CURRENT_TIMESTAMP,
    role_name   VARCHAR(255)           not null,
    description VARCHAR(500) default '',
    status      INTEGER      default 1 not null,
    extend      TEXT
);

create table tb_role_menu
(
    id          INTEGER
        primary key autoincrement,
    create_time DATETIME default CURRENT_TIMESTAMP,
    role_id     INTEGER not null,
    menu_id     INTEGER not null,
    unique (role_id, menu_id)
);

create table tb_user
(
    id          INTEGER
        primary key autoincrement,
    create_time DATETIME     default CURRENT_TIMESTAMP,
    update_time DATETIME     default CURRENT_TIMESTAMP,
    username    VARCHAR(64)            not null
        unique,
    password    VARCHAR(255)           not null,
    nickname    VARCHAR(255) default '',
    email       VARCHAR(255) default '',
    phone       VARCHAR(20)  default '',
    avatar      VARCHAR(500) default '',
    status      INTEGER      default 1 not null,
    last_login  DATETIME     default NULL,
    extend      TEXT
);

create table tb_user_role
(
    id          INTEGER
        primary key autoincrement,
    create_time DATETIME default CURRENT_TIMESTAMP,
    user_id     INTEGER not null,
    role_id     INTEGER not null,
    unique (user_id, role_id)
);

create table tb_stream_proxy
(
    id            INTEGER
        primary key autoincrement,
    create_time   DATETIME     default CURRENT_TIMESTAMP not null,
    update_time   DATETIME     default CURRENT_TIMESTAMP not null,
    app           VARCHAR(255)                           not null,
    stream        VARCHAR(255)                           not null,
    url           VARCHAR(1000)                          not null,
    status        INTEGER      default 1                 not null,
    online_status INTEGER      default 0                 not null,
    proxy_key     VARCHAR(255)                           not null,
    enabled       INTEGER      default 1                 not null,
    description   VARCHAR(500) default '',
    extend        TEXT,
    unique (app, stream)
);

create index idx_stream_proxy_app_stream
    on tb_stream_proxy (app, stream);

create index idx_stream_proxy_key
    on tb_stream_proxy (proxy_key);

create index idx_stream_proxy_status
    on tb_stream_proxy (status);

create index idx_stream_proxy_online_status
    on tb_stream_proxy (online_status);

