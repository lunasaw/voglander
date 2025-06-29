-- 流媒体节点详情页面菜单数据插入语句
-- 基于现有的 media-menu-insert.sql 结构

-- 插入节点详情页面菜单（页面级别）
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 流媒体节点详情页面
(302, 301, 'MediaNodeDetail', 'media.node.detail', 2, '/media/node/detail/:nodeKey', '/media/node/detail',
 'mdi:server-network', 2, 1,
 'Media:Node:List',
 JSON_OBJECT('icon', 'mdi:server-network', 'title', 'media.node.detail', 'hideInMenu', true));

-- 为管理员角色分配节点详情页面权限
INSERT INTO tb_role_menu (role_id, menu_id)
VALUES (1, 302);

-- 注释说明：
-- 菜单类型说明：
-- menu_type = 2: 页面菜单

-- ID分配规则：
-- 302: MediaNodeDetail 流媒体节点详情页面

-- 权限编码说明：
-- Media:Node:List - 复用节点列表查看权限（详情页面通常与列表页面使用相同权限）

-- 国际化键值说明：
-- media.node.detail - 节点详情

-- 特殊配置说明：
-- hideInMenu: true - 在菜单中隐藏该页面
-- 路径使用动态参数 :nodeKey 用于传递节点标识
