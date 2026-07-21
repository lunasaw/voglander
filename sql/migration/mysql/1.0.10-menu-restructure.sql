-- ============================================================================================
-- 1.0.10 菜单结构调整：修复ID冲突，将级联通道管理移至设备管理下
-- ============================================================================================

-- 步骤1：删除旧的级联管理目录（ID=600会与任务管理冲突）
DELETE FROM tb_role_menu WHERE menu_id IN (600, 601, 602, 60101, 60102, 60103, 60104, 60201, 60202, 60203);
DELETE FROM tb_menu WHERE id IN (60203, 60202, 60201, 60104, 60103, 60102, 60101, 602, 601, 600);

-- 步骤2：将级联平台管理（601）和级联通道管理（602）作为设备管理的子菜单插入
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
(601, 500, 'CascadePlatform', 'cascade.platform.title', 2, '/device/cascade/platform', '/cascade/platform/list',
 'mdi:server-network-outline', 3, 1, 'Cascade:Platform:List',
 JSON_OBJECT('icon', 'mdi:server-network-outline', 'title', 'cascade.platform.title', 'hideInMenu', false)),
(602, 500, 'CascadeChannel', 'cascade.channel.title', 2, '/device/cascade/channel', '/cascade/channel/list',
 'mdi:swap-horizontal', 4, 1, 'Cascade:Channel:List',
 JSON_OBJECT('icon', 'mdi:swap-horizontal', 'title', 'cascade.channel.title', 'hideInMenu', false));

-- 步骤3：插入级联平台和通道的按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
(60101, 601, 'CascadePlatformCreate', 'cascade.platform.create', 3, null, null, '', 1, 1, 'Cascade:Platform:Create',
 JSON_OBJECT('title', 'cascade.platform.create', 'hideInMenu', true)),
(60102, 601, 'CascadePlatformEdit', 'cascade.platform.edit', 3, null, null, '', 2, 1, 'Cascade:Platform:Edit',
 JSON_OBJECT('title', 'cascade.platform.edit', 'hideInMenu', true)),
(60103, 601, 'CascadePlatformDelete', 'cascade.platform.delete', 3, null, null, '', 3, 1, 'Cascade:Platform:Delete',
 JSON_OBJECT('title', 'cascade.platform.delete', 'hideInMenu', true)),
(60104, 601, 'CascadePlatformStatus', 'cascade.platform.status', 3, null, null, '', 4, 1, 'Cascade:Platform:Status',
 JSON_OBJECT('title', 'cascade.platform.status', 'hideInMenu', true)),
(60201, 602, 'CascadeChannelCreate', 'cascade.channel.create', 3, null, null, '', 1, 1, 'Cascade:Channel:Create',
 JSON_OBJECT('title', 'cascade.channel.create', 'hideInMenu', true)),
(60202, 602, 'CascadeChannelEdit', 'cascade.channel.edit', 3, null, null, '', 2, 1, 'Cascade:Channel:Edit',
 JSON_OBJECT('title', 'cascade.channel.edit', 'hideInMenu', true)),
(60203, 602, 'CascadeChannelDelete', 'cascade.channel.delete', 3, null, null, '', 3, 1, 'Cascade:Channel:Delete',
 JSON_OBJECT('title', 'cascade.channel.delete', 'hideInMenu', true));

-- 步骤4：删除旧的任务管理菜单（如果存在ID=600/601冲突）
DELETE FROM tb_role_menu WHERE menu_id IN (600, 601, 60101, 60102) AND menu_id IN (SELECT id FROM tb_menu WHERE menu_code = 'TaskManagement' OR parent_id IN (SELECT id FROM tb_menu WHERE menu_code = 'TaskManagement'));
DELETE FROM tb_menu WHERE menu_code IN ('TaskQuery', 'TaskControl') AND parent_id IN (SELECT id FROM tb_menu WHERE menu_code = 'TaskCenter' AND parent_id IN (SELECT id FROM tb_menu WHERE menu_code = 'TaskManagement'));
DELETE FROM tb_menu WHERE menu_code = 'TaskCenter' AND parent_id IN (SELECT id FROM tb_menu WHERE menu_code = 'TaskManagement');
DELETE FROM tb_menu WHERE menu_code = 'TaskManagement';

-- 步骤5：插入任务管理菜单（使用新ID 800系列）
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
(800, 0, 'TaskManagement', 'task.management.title', 1, '/task', 'BasicLayout', 'lucide:list-checks', 6, 1, '',
 JSON_OBJECT('icon', 'lucide:list-checks', 'title', 'task.management.title', 'order', 6)),
(801, 800, 'TaskCenter', 'task.center.title', 2, '/task/center', '/task/center/list', 'lucide:activity', 1, 1,
 'Task:Query', JSON_OBJECT('icon', 'lucide:activity', 'title', 'task.center.title')),
(80101, 801, 'TaskQuery', 'task.action.query', 3, NULL, NULL, '', 1, 1, 'Task:Query',
 JSON_OBJECT('title', 'task.action.query', 'hideInMenu', true)),
(80102, 801, 'TaskControl', 'task.action.control', 3, NULL, NULL, '', 2, 1, 'Task:Control',
 JSON_OBJECT('title', 'task.action.control', 'hideInMenu', true));

-- 步骤6：为管理员角色分配所有新菜单权限
INSERT IGNORE INTO tb_role_menu (role_id, menu_id)
SELECT 1, id FROM tb_menu
WHERE id IN (601, 602, 60101, 60102, 60103, 60104, 60201, 60202, 60203, 800, 801, 80101, 80102);
