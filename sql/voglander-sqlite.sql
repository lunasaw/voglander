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
-- Table structure for tb_export_task
-- ----------------------------
DROP TABLE IF EXISTS tb_export_task;
CREATE TABLE tb_export_task
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    gmt_create  DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    gmt_update  DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    biz_id      VARCHAR(255)                            NOT NULL,
    member_cnt  INTEGER       DEFAULT 0                 NOT NULL,
    format      VARCHAR(10)                             NOT NULL,
    apply_time  DATETIME      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    export_time DATETIME      DEFAULT NULL,
    url         VARCHAR(2500) DEFAULT NULL,
    status      INTEGER       DEFAULT 0                 NOT NULL,
    deleted     INTEGER       DEFAULT 0                 NOT NULL,
    param       TEXT,
    name        VARCHAR(500)  DEFAULT NULL,
    type        INTEGER       DEFAULT 0                 NOT NULL,
    expired     INTEGER       DEFAULT 0                 NOT NULL,
    extend      TEXT,
    apply_user  VARCHAR(256)  DEFAULT NULL
);

CREATE INDEX idx_biz_id ON tb_export_task (biz_id);

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
    extend       TEXT
);

CREATE UNIQUE INDEX uk_call_id ON tb_media_session (call_id);
CREATE INDEX idx_media_session_device_id ON tb_media_session (device_id);
CREATE INDEX idx_media_session_status ON tb_media_session (status);

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

PRAGMA foreign_keys = ON;


/*
 SQLite Schema for Stream Proxy Menu Data
 Converted from MySQL schema
 Date: 12/08/2025 Stream Proxy Menu Creation
*/

PRAGMA foreign_keys = OFF;

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
       json_extract(m.meta, '$.title') as meta_title
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

PRAGMA foreign_keys = ON;