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