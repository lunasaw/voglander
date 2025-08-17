-- 推流代理管理表 SQLite 版本
CREATE TABLE tb_push_proxy
(
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    app           VARCHAR(255)                           NOT NULL,
    stream        VARCHAR(255)                           NOT NULL,
    dst_url       VARCHAR(1000)                          NOT NULL,
    schema        VARCHAR(50)  DEFAULT 'rtmp'            NOT NULL,
    status        INTEGER      DEFAULT 1                 NOT NULL,
    online_status INTEGER      DEFAULT 0                 NOT NULL,
    proxy_key     VARCHAR(255) DEFAULT NULL,
    server_id     VARCHAR(64)  DEFAULT NULL,
    enabled       INTEGER      DEFAULT 1                 NOT NULL,
    description   VARCHAR(500) DEFAULT '',
    extend        TEXT -- 扩展字段，包含vhost、retryCount、rtpType、timeoutSec等ZLM特定参数
);

CREATE UNIQUE INDEX uk_push_app_stream ON tb_push_proxy (app, stream);
CREATE INDEX idx_push_proxy_key ON tb_push_proxy (proxy_key);
CREATE INDEX idx_push_proxy_status ON tb_push_proxy (status);
CREATE INDEX idx_push_proxy_online_status ON tb_push_proxy (online_status);
CREATE INDEX idx_push_proxy_server_id ON tb_push_proxy (server_id);
CREATE INDEX idx_push_proxy_schema ON tb_push_proxy (schema);


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
