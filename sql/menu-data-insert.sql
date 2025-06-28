-- 新菜单数据插入SQL
-- 基于前端JSON菜单数据转换

-- 清空现有菜单数据（可选，根据需要决定是否执行）
-- DELETE FROM tb_role_menu;
-- DELETE FROM tb_menu;

-- 插入根级菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- Workspace 工作台
(1, 0, 'Workspace', 'Workspace', 2, '/workspace', '/dashboard/workspace/index', 'carbon:workspace', 0, 1, 1, '',
 JSON_OBJECT('icon', 'carbon:workspace', 'title', 'page.dashboard.workspace', 'affixTab', true, 'order', 0)),

-- System 系统管理目录
(2, 0, 'System', 'System', 1, '/system', '', 'carbon:settings', 9997, 1, 1, '',
 JSON_OBJECT('icon', 'carbon:settings', 'order', 9997, 'title', 'system.title', 'badge', 'new', 'badgeType', 'normal',
             'badgeVariants', 'primary')),

-- Project 项目管理目录
(9, 0, 'Project', 'Project', 1, '/vben-admin', '', 'carbon:data-center', 9998, 1, 1, '',
 JSON_OBJECT('badgeType', 'dot', 'order', 9998, 'title', 'demos.vben.title', 'icon', 'carbon:data-center')),

-- About 关于页面
(10, 0, 'About', 'About', 2, '/about', '_core/about/index', 'lucide:copyright', 9999, 1, 1, '',
 JSON_OBJECT('icon', 'lucide:copyright', 'order', 9999, 'title', 'demos.vben.about'));

-- 插入System子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- 系统菜单管理
(201, 2, 'SystemMenu', 'SystemMenu', 2, '/system/menu', '/system/menu/list', 'carbon:menu', 1, 1, 1, 'System:Menu:List',
 JSON_OBJECT('icon', 'carbon:menu', 'title', 'system.menu.title')),

-- 系统部门管理
(202, 2, 'SystemDept', 'SystemDept', 2, '/system/dept', '/system/dept/list', 'carbon:container-services', 2, 1, 1,
 'System:Dept:List',
 JSON_OBJECT('icon', 'carbon:container-services', 'title', 'system.dept.title'));

-- 插入System菜单的按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- 菜单管理按钮
(20101, 201, 'SystemMenuCreate', 'SystemMenuCreate', 3, '', '', '', 1, 1, 1, 'System:Menu:Create',
 JSON_OBJECT('title', 'common.create')),
(20102, 201, 'SystemMenuEdit', 'SystemMenuEdit', 3, '', '', '', 2, 1, 1, 'System:Menu:Edit',
 JSON_OBJECT('title', 'common.edit')),
(20103, 201, 'SystemMenuDelete', 'SystemMenuDelete', 3, '', '', '', 3, 1, 1, 'System:Menu:Delete',
 JSON_OBJECT('title', 'common.delete')),

-- 部门管理按钮
(20401, 202, 'SystemDeptCreate', 'SystemDeptCreate', 3, '', '', '', 1, 1, 1, 'System:Dept:Create',
 JSON_OBJECT('title', 'common.create')),
(20402, 202, 'SystemDeptEdit', 'SystemDeptEdit', 3, '', '', '', 2, 1, 1, 'System:Dept:Edit',
 JSON_OBJECT('title', 'common.edit')),
(20403, 202, 'SystemDeptDelete', 'SystemDeptDelete', 3, '', '', '', 3, 1, 1, 'System:Dept:Delete',
 JSON_OBJECT('title', 'common.delete'));


-- 插入System菜单的按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES (203, 2, 'SystemUser', 'SystemUser', 2, '/system/user', '/system/user/list', 'User', 3, 1, 1, 'System:User:List',
        JSON_OBJECT('title', 'system.user.title'));

-- 插入用户管理按钮权限
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- 用户管理按钮
(20501, 203, 'SystemUserCreate', 'SystemUserCreate', 3, '', '', '', 1, 1, 1, 'System:User:Create',
 JSON_OBJECT('title', 'common.create')),
(20502, 203, 'SystemUserEdit', 'SystemUserEdit', 3, '', '', '', 2, 1, 1, 'System:User:Edit',
 JSON_OBJECT('title', 'common.edit')),
(20503, 203, 'SystemUserDelete', 'SystemUserDelete', 3, '', '', '', 3, 1, 1, 'System:User:Delete',
 JSON_OBJECT('title', 'common.delete'));

-- 插入Project子菜单
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission, meta)
VALUES
-- VbenDocument 文档
(901, 9, 'VbenDocument', 'VbenDocument', 2, '/vben-admin/document', 'IFrameView', 'carbon:book', 1, 1, 1, '',
 JSON_OBJECT('icon', 'carbon:book', 'iframeSrc', 'https://doc.vben.pro', 'title', 'demos.vben.document')),

-- VbenGithub Github链接
(902, 9, 'VbenGithub', 'VbenGithub', 2, '/vben-admin/github', 'IFrameView', 'carbon:logo-github', 2, 1, 1, '',
 JSON_OBJECT('icon', 'carbon:logo-github', 'link', 'https://github.com/vbenjs/vue-vben-admin', 'title', 'Github')),

-- VbenAntdv Antdv链接 (状态为禁用)
(903, 9, 'VbenAntdv', 'VbenAntdv', 2, '/vben-admin/antdv', 'IFrameView', 'carbon:hexagon-vertical-solid', 3, 1, 0, '',
 JSON_OBJECT('icon', 'carbon:hexagon-vertical-solid', 'badgeType', 'dot', 'link', 'https://ant.vben.pro', 'title',
             'demos.vben.antdv'));

-- 重置AUTO_INCREMENT值（可选）
-- ALTER TABLE tb_menu AUTO_INCREMENT = 1000;

-- 给管理员角色分配新菜单权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE id IN (1, 2, 9, 10, 201, 202, 20101, 20102, 20103, 20401, 20402, 20403, 901, 902, 903)
