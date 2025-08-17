-- 推流代理管理表 MySQL 版本
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
    `extend`        TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin COMMENT '扩展字段，包含vhost、retryCount、rtpType、timeoutSec等ZLM特定参数',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_stream` (`app`, `stream`) USING BTREE,
    KEY `idx_push_proxy_key` (`proxy_key`) USING BTREE,
    KEY `idx_push_proxy_status` (`status`) USING BTREE,
    KEY `idx_push_proxy_online_status` (`online_status`) USING BTREE,
    KEY `idx_push_proxy_server_id` (`server_id`) USING BTREE,
    KEY `idx_push_proxy_schema` (`schema`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT ='推流代理管理表';


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