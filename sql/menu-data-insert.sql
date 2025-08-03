-- 新菜单数据插入SQL
-- 基于前端JSON菜单数据转换

-- 清空现有菜单数据（可选，根据需要决定是否执行）
DELETE
FROM tb_role_menu;
DELETE
FROM tb_menu;

-- 插入根级菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- Dashboard 仪表板目录
(1, 0, 'Dashboard', 'page.dashboard.title', 1, '/dashboard', '', 'carbon:dashboard', -1, 1, '',
 JSON_OBJECT('order', -1, 'title', 'page.dashboard.title', 'hideInMenu', false)),

-- System 系统管理目录
(2, 0, 'System', 'system.title', 1, '/system', '', 'carbon:settings', 9997, 1, '',
 JSON_OBJECT('icon', 'carbon:settings', 'order', 9997, 'title', 'system.title', 'badge', 'new', 'badgeType', 'normal',
             'badgeVariants', 'primary', 'hideInMenu', false)),

-- Media 媒体管理目录
(300, 0, 'Media', 'media.title', 1, '/media', '', 'mdi:server-network', 9996, 1, '',
 JSON_OBJECT('icon', 'mdi:server-network', 'order', 9996, 'title', 'media.title', 'hideInMenu', false)),

-- Project 项目管理目录
(9, 0, 'Project', 'demos.vben.title', 1, '/vben-admin', '', 'carbon:data-center', 9998, 1, '',
 JSON_OBJECT('badgeType', 'dot', 'order', 9998, 'title', 'demos.vben.title', 'icon', 'carbon:data-center', 'hideInMenu',
             false)),

-- About 关于页面
(10, 0, 'VbenAbout', 'demos.vben.about', 2, '/vben-admin/about', '#/views/_core/about/index.vue', 'lucide:copyright',
 9999, 1, '',
 JSON_OBJECT('icon', 'lucide:copyright', 'order', 9999, 'title', 'demos.vben.about', 'hideInMenu', false));


-- 插入Dashboard子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- Analytics 分析页面
(101, 1, 'Analytics', 'page.dashboard.analytics', 2, '/analytics', '/dashboard/analytics/index', 'carbon:analytics', 1,
 1, '',
 JSON_OBJECT('affixTab', true, 'title', 'page.dashboard.analytics', 'hideInMenu', false)),

-- Workspace 工作台
(102, 1, 'Workspace', 'page.dashboard.workspace', 2, '/workspace', '/dashboard/workspace/index', 'carbon:workspace', 2,
 1, '',
 JSON_OBJECT('title', 'page.dashboard.workspace', 'hideInMenu', false));

-- 插入System子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 系统菜单管理
(201, 2, 'SystemMenu', 'system.menu.title', 2, '/system/menu', '/system/menu/list', 'mdi:menu', 1, 1,
 'System:Menu:List',
 JSON_OBJECT('icon', 'mdi:menu', 'title', 'system.menu.title', 'hideChildrenInMenu', true, 'hideInMenu', false)),

-- 系统角色管理
(202, 2, 'SystemRole', 'system.role.title', 2, '/system/role', '/system/role/list', 'mdi:account-group', 2, 1,
 'System:Role:List',
 JSON_OBJECT('icon', 'mdi:account-group', 'title', 'system.role.title', 'hideChildrenInMenu', true, 'hideInMenu',
             false)),

-- 系统用户管理
(203, 2, 'SystemUser', 'system.user.title', 2, '/system/user', '/system/user/list', 'mdi:account', 3, 1,
 'System:User:List',
 JSON_OBJECT('icon', 'mdi:account', 'title', 'system.user.title', 'hideChildrenInMenu', true, 'hideInMenu', false)),

-- 系统部门管理
(204, 2, 'SystemDept', 'system.dept.title', 2, '/system/dept', '/system/dept/list', 'charm:organisation', 4, 1,
 'System:Dept:List',
 JSON_OBJECT('icon', 'charm:organisation', 'title', 'system.dept.title', 'hideChildrenInMenu', true, 'hideInMenu',
             false));

-- 插入Media子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 流媒体节点管理
(301, 300, 'MediaNode', 'media.node.title', 2, '/media/node', '/media/node/list', 'mdi:server-network', 1, 1,
 'Media:Node:List',
 JSON_OBJECT('icon', 'mdi:server-network', 'title', 'media.node.title', 'hideInMenu', false)),

-- 流媒体节点详情 (隐藏在菜单中)
(302, 300, 'MediaNodeDetail', 'media.node.detail', 2, '/media/node/detail/:nodeKey', '/media/node/detail',
 'mdi:server-network', 2, 1,
 'Media:Node:List',
 JSON_OBJECT('hideInMenu', true, 'icon', 'mdi:server-network', 'title', 'media.node.detail')),

-- 流媒体列表
(303, 300, 'MediaList', 'media.list.title', 2, '/media/list', '/media/list/list', 'mdi:video-outline', 3, 1,
 'Media:List:Query',
 JSON_OBJECT('icon', 'mdi:video-outline', 'title', 'media.list.title', 'hideInMenu', false));

-- 插入System菜单的按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- 菜单管理按钮
(20101, 201, 'SystemMenuCreate', 'system.menu.create', 3, null, null, '', 1, 1, 'System:Menu:Create',
 JSON_OBJECT('title', 'system.menu.create', 'hideInMenu', true)),
(20102, 201, 'SystemMenuEdit', 'system.menu.edit', 3, null, null, '', 2, 1, 'System:Menu:Edit',
 JSON_OBJECT('title', 'system.menu.edit', 'hideInMenu', true)),
(20103, 201, 'SystemMenuDelete', 'system.menu.delete', 3, null, null, '', 3, 1, 'System:Menu:Delete',
 JSON_OBJECT('title', 'system.menu.delete', 'hideInMenu', true)),

-- 角色管理按钮
(20201, 202, 'SystemRoleCreate', 'system.role.create', 3, null, null, '', 1, 1, 'System:Role:Create',
 JSON_OBJECT('title', 'system.role.create', 'hideInMenu', true)),
(20202, 202, 'SystemRoleEdit', 'system.role.edit', 3, null, null, '', 2, 1, 'System:Role:Edit',
 JSON_OBJECT('title', 'system.role.edit', 'hideInMenu', true)),
(20203, 202, 'SystemRoleDelete', 'system.role.delete', 3, null, null, '', 3, 1, 'System:Role:Delete',
 JSON_OBJECT('title', 'system.role.delete', 'hideInMenu', true)),

-- 用户管理按钮
(20301, 203, 'SystemUserCreate', 'system.user.create', 3, null, null, '', 1, 1, 'System:User:Create',
 JSON_OBJECT('title', 'system.user.create', 'hideInMenu', true)),
(20302, 203, 'SystemUserEdit', 'system.user.edit', 3, null, null, '', 2, 1, 'System:User:Edit',
 JSON_OBJECT('title', 'system.user.edit', 'hideInMenu', true)),
(20303, 203, 'SystemUserDelete', 'system.user.delete', 3, null, null, '', 3, 1, 'System:User:Delete',
 JSON_OBJECT('title', 'system.user.delete', 'hideInMenu', true)),

-- 部门管理按钮
(20401, 204, 'SystemDeptCreate', 'system.dept.create', 3, null, null, '', 1, 1, 'System:Dept:Create',
 JSON_OBJECT('title', 'system.dept.create', 'hideInMenu', true)),
(20402, 204, 'SystemDeptEdit', 'system.dept.edit', 3, null, null, '', 2, 1, 'System:Dept:Edit',
 JSON_OBJECT('title', 'system.dept.edit', 'hideInMenu', true)),
(20403, 204, 'SystemDeptDelete', 'system.dept.delete', 3, null, null, '', 3, 1, 'System:Dept:Delete',
 JSON_OBJECT('title', 'system.dept.delete', 'hideInMenu', true)),

-- 媒体节点管理按钮
(30101, 301, 'MediaNodeCreate', 'media.node.create', 3, null, null, '', 1, 1, 'Media:Node:Create',
 JSON_OBJECT('title', 'media.node.create', 'hideInMenu', true)),
(30102, 301, 'MediaNodeEdit', 'media.node.edit', 3, null, null, '', 2, 1, 'Media:Node:Edit',
 JSON_OBJECT('title', 'media.node.edit', 'hideInMenu', true)),
(30103, 301, 'MediaNodeDelete', 'media.node.delete', 3, null, null, '', 3, 1, 'Media:Node:Delete',
 JSON_OBJECT('title', 'media.node.delete', 'hideInMenu', true)),

-- 流媒体列表按钮
(30301, 303, 'MediaStreamClose', 'media.list.actions.close', 3, null, null, '', 1, 1, 'Media:Stream:Close',
 JSON_OBJECT('title', 'media.list.actions.close', 'hideInMenu', true)),
(30302, 303, 'MediaStreamExport', 'media.list.actions.export', 3, null, null, '', 2, 1, 'Media:List:Export',
 JSON_OBJECT('title', 'media.list.actions.export', 'hideInMenu', true));

-- 插入Project子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status,
                     permission, meta)
VALUES
-- VbenDocument 文档
(901, 9, 'VbenDocument', 'demos.vben.document', 2, '/vben-admin/document', 'IFrameView', 'carbon:book', 1, 1, '',
 JSON_OBJECT('icon', 'carbon:book', 'iframeSrc', 'https://doc.vben.pro', 'title', 'demos.vben.document', 'hideInMenu',
             false)),

-- VbenGithub Github链接
(902, 9, 'VbenGithub', 'Github', 2, '/vben-admin/github', 'IFrameView', 'carbon:logo-github', 2, 1, '',
 JSON_OBJECT('icon', 'carbon:logo-github', 'link', 'https://github.com/vbenjs/vue-vben-admin', 'title', 'Github',
             'hideInMenu', false)),

-- VbenAntdv Antdv链接 (状态为禁用)
(903, 9, 'VbenAntdv', 'demos.vben.antdv', 2, '/vben-admin/antdv', 'IFrameView', 'carbon:hexagon-vertical-solid', 3,
 0, '',
 JSON_OBJECT('icon', 'carbon:hexagon-vertical-solid', 'badgeType', 'dot', 'link', 'https://ant.vben.pro', 'title',
             'demos.vben.antdv', 'hideInMenu', false));

-- 重置AUTO_INCREMENT值（可选）
-- ALTER TABLE tb_menu AUTO_INCREMENT = 1000;

-- 给管理员角色分配新菜单权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN
      (1, 101, 102, 2, 3, 9, 10, 201, 202, 203, 204, 301, 302, 303, 20101, 20102, 20103, 20201, 20202, 20203, 20301,
       20302, 20303, 20401,
       20402,
       20403, 30101, 30102, 30103, 30301, 30302, 901, 902, 903)
ON CONFLICT(role_id, menu_id) DO NOTHING;
