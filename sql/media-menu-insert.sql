-- 媒体管理模块菜单数据插入SQL
-- 基于Vue-Vben-Admin管理后台的菜单结构

-- 插入媒体管理根级菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- Media 媒体管理目录
(300, 0, 'Media', 'media.title', 1, '/media', '', 'mdi:server-network', 9996, 1, 1, '',
 JSON_OBJECT('icon', 'mdi:server-network', 'order', 9996, 'title', 'media.title'));

-- 插入媒体管理子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- 流媒体节点管理
(301, 300, 'MediaNode', 'media.node.title', 2, '/media/node', '/media/node/list', 'mdi:server-network', 1, 1, 1,
 'Media:Node:List',
 JSON_OBJECT('icon', 'mdi:server-network', 'title', 'media.node.title', 'hideChildrenInMenu', true));

-- 插入流媒体节点管理的按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- 节点管理按钮
(30101, 301, 'MediaNodeCreate', 'media.node.create', 3, null, null, '', 1, 1, 1, 'Media:Node:Create',
 JSON_OBJECT('title', 'media.node.create')),
(30102, 301, 'MediaNodeEdit', 'media.node.edit', 3, null, null, '', 2, 1, 1, 'Media:Node:Edit',
 JSON_OBJECT('title', 'media.node.edit')),
(30103, 301, 'MediaNodeDelete', 'media.node.delete', 3, null, null, '', 3, 1, 1, 'Media:Node:Delete',
 JSON_OBJECT('title', 'media.node.delete'));

-- 给管理员角色分配媒体管理菜单权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (300, 301, 30101, 30102, 30103)
ON CONFLICT(role_id, menu_id) DO NOTHING;

-- 注释说明：
-- 菜单类型说明：
-- menu_type = 1: 目录菜单
-- menu_type = 2: 页面菜单
-- menu_type = 3: 按钮权限

-- ID分配规则：
-- 媒体管理模块：300-399
-- 300: Media 媒体管理根目录
-- 301: MediaNode 流媒体节点管理页面
-- 30101-30103: MediaNode 按钮权限

-- 权限编码规范：
-- Media:Node:List - 节点列表查看权限
-- Media:Node:Create - 节点创建权限
-- Media:Node:Edit - 节点编辑权限
-- Media:Node:Delete - 节点删除权限

-- 国际化键值说明：
-- media.title - 媒体管理
-- media.node.title - 流媒体节点管理
-- media.node.create - 新增节点
-- media.node.edit - 编辑节点
-- media.node.delete - 删除节点
