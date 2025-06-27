-- 用户表
CREATE TABLE tb_user
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    username    VARCHAR(64)     NOT NULL COMMENT '用户名',
    password    VARCHAR(255)    NOT NULL COMMENT '密码',
    nickname    VARCHAR(255)             DEFAULT '' COMMENT '昵称',
    email       VARCHAR(255)             DEFAULT '' COMMENT '邮箱',
    phone       VARCHAR(20)              DEFAULT '' COMMENT '手机号',
    avatar      VARCHAR(500)             DEFAULT '' COMMENT '头像URL',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    last_login  DATETIME                 DEFAULT NULL COMMENT '最后登录时间',
    extend      TEXT COMMENT '扩展字段',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '用户表';

-- 角色表
CREATE TABLE tb_role
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    role_name   VARCHAR(255)    NOT NULL COMMENT '角色名称',
    description VARCHAR(500)             DEFAULT '' COMMENT '角色描述',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    extend      TEXT COMMENT '扩展字段',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '角色表';

-- 菜单表
CREATE TABLE tb_menu
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    parent_id   BIGINT          NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    menu_code   VARCHAR(64)     NOT NULL COMMENT '菜单编码',
    menu_name   VARCHAR(255)    NOT NULL COMMENT '菜单名称',
    menu_type   TINYINT         NOT NULL DEFAULT 1 COMMENT '菜单类型 1目录 2菜单 3按钮',
    path        VARCHAR(255)             DEFAULT '' COMMENT '路由路径',
    component   VARCHAR(255)             DEFAULT '' COMMENT '组件路径',
    icon        VARCHAR(255)             DEFAULT '' COMMENT '菜单图标',
    sort_order  INT             NOT NULL DEFAULT 0 COMMENT '排序',
    visible     TINYINT         NOT NULL DEFAULT 1 COMMENT '是否显示 1显示 0隐藏',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
    permission  VARCHAR(255)             DEFAULT '' COMMENT '权限标识',
    meta JSON COMMENT '菜单元数据(JSON格式)',
    extend      TEXT COMMENT '扩展字段',
    PRIMARY KEY (id),
    UNIQUE KEY uk_menu_code (menu_code),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '菜单表';

-- 用户角色关联表
CREATE TABLE tb_user_role
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    user_id     BIGINT          NOT NULL COMMENT '用户ID',
    role_id     BIGINT          NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '用户角色关联表';

-- 角色菜单关联表
CREATE TABLE tb_role_menu
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    role_id     BIGINT          NOT NULL COMMENT '角色ID',
    menu_id     BIGINT          NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_bin COMMENT = '角色菜单关联表';

-- 插入默认管理员用户 (密码: admin123)
-- 注意：这里的密码是使用PasswordUtils.encode("admin123")生成的
INSERT INTO tb_user (username, password, nickname, status)
VALUES ('admin', '$2a$10$salt123456789012345678901234567890123456789012345678901234567890', '管理员', 1);

-- 插入默认角色
INSERT INTO tb_role (role_name, description, status)
VALUES ('系统管理员', '系统管理员角色', 1),
       ('普通用户', '普通用户角色', 1);

-- 插入默认菜单
INSERT INTO tb_menu (parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, visible, status,
                     permission)
VALUES (0, 'DASHBOARD', '仪表盘', 1, '/dashboard', '', 'dashboard', 1, 1, 1, ''),
       (0, 'DEVICE', '设备管理', 1, '/device', '', 'device', 2, 1, 1, ''),
       (2, 'DEVICE_LIST', '设备列表', 2, '/device/list', 'device/DeviceList', '', 1, 1, 1, 'device:list'),
       (2, 'DEVICE_CHANNEL', '设备通道', 2, '/device/channel', 'device/DeviceChannel', '', 2, 1, 1, 'device:channel'),
       (0, 'MEDIA', '流媒体管理', 1, '/media', '', 'media', 3, 1, 1, ''),
       (5, 'MEDIA_NODE', '节点管理', 2, '/media/node', 'media/MediaNode', '', 1, 1, 1, 'media:node'),
       (0, 'SYSTEM', '系统管理', 1, '/system', '', 'system', 4, 1, 1, ''),
       (7, 'SYSTEM_USER', '用户管理', 2, '/system/user', 'system/User', '', 1, 1, 1, 'system:user'),
       (7, 'SYSTEM_ROLE', '角色管理', 2, '/system/role', 'system/Role', '', 2, 1, 1, 'system:role'),
       (7, 'SYSTEM_MENU', '菜单管理', 2, '/system/menu', 'system/Menu', '', 3, 1, 1, 'system:menu');

-- 给管理员用户分配管理员角色
INSERT INTO tb_user_role (user_id, role_id)
VALUES (1, 1);

-- 给管理员角色分配所有菜单权限
INSERT INTO tb_role_menu (role_id, menu_id)
SELECT 1, id
FROM tb_menu
WHERE status = 1;