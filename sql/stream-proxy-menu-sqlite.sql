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
REPLACE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
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
REPLACE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
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
REPLACE INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (304, 30401, 30402, 30403, 30404, 30405);

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

PRAGMA foreign_keys = ON;