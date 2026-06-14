---
name: add-menu-page
description: 在 voglander 新增一个前端页面时，配套完成「后端菜单记录 + 权限码 + 两份初始化 SQL 维护 + 现有 app.db 即时生效」的全流程。当用户说「新增页面/菜单」「加一个 XX 管理页」「页面进不去/菜单不显示」「补菜单权限」时使用本 skill。
---

# 新增页面 → 菜单 / 权限 / SQL 维护

voglander 前端 `vue-vben-admin/apps/web-antd` 运行在 **`accessMode: 'backend'`**（见 `src/preferences.ts`）。
∴ **菜单与可访问路由完全由后端 `tb_menu`（经 `tb_role_menu` 按登录角色过滤）驱动**，本地
`router/routes/modules/*.ts` 路由文件在 backend 模式下**不驱动菜单**。

**只写前端路由文件 / Vue 组件，页面进不去** —— 必须在 `tb_menu` 落库并授权给角色。

---

## 何时需要本 skill

新增一个前端页面（`apps/web-antd/src/views/<area>/<page>.vue`）时，除了写组件，还要：

1. 在两份初始化 SQL 各加菜单记录（目录可选 + 页面必需）。
2. 给页面挂权限码（沿用约定，可空）。
3. 把记录授权给角色（插在 admin 自动授权语句**之前**即自动获权）。
4. **对现有 `app.db` 手动执行 INSERT**（否则改 seed 文件对已存在的库零效果）。

---

## 字段映射（核对自 `MenuWebAssembler#dtoToPermissionResp`，voglander-web）

后端 `tb_menu` 列 → 前端路由字段：

| `tb_menu` 列 | 前端字段 | 说明 |
|---|---|---|
| `menu_code` | （管理后台用）| `uk_menu_code` 唯一，**全表不可重复** |
| `menu_name` | `name` + `meta.title` | 填 **i18n key**（如 `protocolLab.menu`），不是中文字面�� |
| `permission` | `authCode` | 前端权限码；目录留空，页面/按钮填 `Area:Entity:Action` |
| `path` | `path` | 路由路径，与前端路由文件一致 |
| `component` | `component` | 见下方「component 解析规则」 |
| `menu_type` | — | **1=目录 2=菜单(页面) 3=按钮** |
| `parent_id` | `pid` | 目录的 id；顶级目录填 0 |
| `sort_order` / `meta.order` | 排序 | 目录用 `meta.order`（9995/9996…），页面用 `sort_order` |

### component 解析规则（核对自 `generate-routes-backend.ts` → `normalizeViewPath`）

- `pageMap = import.meta.glob('../views/**/*.vue')`，key 归一化时**去掉 `/views` 前缀**。
- 后端 `component` 经同一归一化 + 补 `.vue` 后匹配 pageMap。
- ∴ Vue 文件 `views/protocol-lab/index.vue` → 后端 `component` 写 **`/protocol-lab/index`**（不带 `.vue`，不带 `views`）。
- **目录**（menu_type=1）`component` 留空字符串 `''`：自动套 `BasicLayout`。
- 匹配不到 pageMap → 前端 console 报 `route component is invalid` 并回退 not-found（这是页面白屏/404 的根因，先查这里）。

---

## 两份必须同步维护的 SQL（单一事实源）

| 文件 | 数据库 | meta 写法 | 备注 |
|---|---|---|---|
| `sql/voglander.sql` | MySQL（生产） | `JSON_OBJECT('title', 'x.y', ...)` | DBA 手动执行；DROP TABLE 破坏性 |
| `sql/voglander-sqlite.sql` | SQLite（开发/测试） | JSON 字符串 `'{"title":"x.y"}'` | **构建期由 `voglander-repository/pom.xml` 拷进 classpath `db/`** |

> 两文件**内容必须等价**，只是 meta 语法不同。`menu_code` 全表唯一。

### 插入位置（关键）

两文件末尾都有一句把 **所有 `status=1` 菜单自动授权给 admin 角色**：

- MySQL：`INSERT INTO tb_role_menu (role_id, menu_id) SELECT 1, id FROM tb_menu WHERE status=1 ...`
- SQLite：`INSERT OR IGNORE INTO tb_role_menu ... SELECT r.id, m.id FROM tb_role r, tb_menu m WHERE r.role_name='系统管理员' AND m.status=1;`

**只要把新菜单 INSERT 放在这句之前，admin 自动获权，无需手写 `tb_role_menu`。** 放之后则要补授权语句。
现有约定：新菜单块插在「Media 子菜单」与「Project 子菜单」之间。

### id 取值

避免与现有冲突。已用区段：`1/2/9/10`(顶级)、`101/102`(dashboard)、`2xx`(system)、`3xx`(media)、`4xx`(protocol-lab)、`9xx`(project)、`2xxxx/3xxxx`(按钮)。
新顶级目录取下一个空闲百位段（如 `500`），其页面 `501/502…`，按钮 `50101…`。

---

## 操作流程（照做）

### 1. 确认前端产物已就位
- Vue 组件：`apps/web-antd/src/views/<area>/<page>.vue`
- i18n：`apps/web-antd/src/locales/langs/{zh-CN,en-US}/<ns>.json`（`import.meta.glob` 自动加载，零注册）
- `menu_name` 用的 i18n key 必须在上面 json 里存在。

### 2. 编辑两份 SQL（插在 admin 授权语句之前）

MySQL（`sql/voglander.sql`）：
```sql
INSERT INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status, permission, meta)
VALUES
(500, 0, '<Area>', '<ns>.title', 1, '/<area>', '', '<icon>', <order>, 1, '',
 JSON_OBJECT('icon', '<icon>', 'order', <order>, 'title', '<ns>.title', 'hideInMenu', false)),
(501, 500, '<Area><Page>', '<ns>.menu', 2, '/<area>/<page>', '/<area>/<page>', '<icon>', 1, 1, '<Area>:<Entity>:Query',
 JSON_OBJECT('icon', '<icon>', 'title', '<ns>.menu', 'hideInMenu', false));
```

SQLite（`sql/voglander-sqlite.sql`，同记录、JSON 字符串、`INSERT OR IGNORE`）：
```sql
INSERT OR IGNORE INTO tb_menu (id, parent_id, menu_code, menu_name, menu_type, path, component, icon, sort_order, status, permission, meta)
VALUES
(500, 0, '<Area>', '<ns>.title', 1, '/<area>', '', '<icon>', <order>, 1, '',
 '{"icon": "<icon>", "order": <order>, "title": "<ns>.title", "hideInMenu": false}'),
(501, 500, '<Area><Page>', '<ns>.menu', 2, '/<area>/<page>', '/<area>/<page>', '<icon>', 1, 1, '<Area>:<Entity>:Query',
 '{"icon": "<icon>", "title": "<ns>.menu", "hideInMenu": false}');
```
> 单页面也可不建目录，直接一条 menu_type=2、parent_id=0、path 指页面、component 指 vue（参考 About 菜单 id=10）。

### 3. ⚠️ 对现有 app.db 即时生效（改 seed 文件对它们无效）

`SqliteSchemaInitializer` 只在**空库**（sentinel 表 `tb_cascade_channel` 缺失）时执行建表+种子。
现有 `app.db` / `voglander-web/app.db` 已建库 → 改 seed 文件对它们**零效果**。两种生效方式：

**A. 手动 INSERT（推荐，不丢数据）**
```bash
cd voglander
APPLY="
INSERT OR IGNORE INTO tb_menu (id,parent_id,menu_code,menu_name,menu_type,path,component,icon,sort_order,status,permission,meta) VALUES
(500,0,'<Area>','<ns>.title',1,'/<area>','','<icon>',<order>,1,'','{\"icon\":\"<icon>\",\"order\":<order>,\"title\":\"<ns>.title\",\"hideInMenu\":false}'),
(501,500,'<Area><Page>','<ns>.menu',2,'/<area>/<page>','/<area>/<page>','<icon>',1,1,'<Area>:<Entity>:Query','{\"icon\":\"<icon>\",\"title\":\"<ns>.menu\",\"hideInMenu\":false}');
INSERT OR IGNORE INTO tb_role_menu (role_id,menu_id) SELECT r.id,m.id FROM tb_role r,tb_menu m WHERE r.role_name='系统管理员' AND m.id IN (500,501);
"
for db in app.db voglander-web/app.db; do [ -f "$db" ] && sqlite3 "$db" "$APPLY"; done
```

**B. 删库重建（会丢业务数据，仅本地可用）**：删 `app.db*` `voglander-web/app.db*`，重启应用，`SqliteSchemaInitializer` 重新跑 seed。

### 4. 验证
```bash
sqlite3 -header -column app.db "SELECT id,parent_id,menu_code,menu_name,menu_type,path,component,permission FROM tb_menu WHERE id IN (500,501);"
sqlite3 app.db "SELECT count(*) FROM tb_role_menu WHERE menu_id IN (500,501) AND role_id=1;"  -- 应=2
```
前端：重新登录（菜单在登录时拉取）→ 左侧出现新菜单 → 点进不报 `route component is invalid`。

---

## 检查清单

- [ ] Vue 组件 + 两份 i18n json 已就位，`menu_name` 的 i18n key 存在
- [ ] `sql/voglander.sql` 与 `sql/voglander-sqlite.sql` 都加了记录（meta 语法各自正确）
- [ ] 记录插在 admin 自动授权语句**之前**（否则补 `tb_role_menu`）
- [ ] `menu_code` 全表唯一、id 不冲突
- [ ] `component` = vue 路径去 `/views` 去 `.vue`；目录 component 为空
- [ ] 已对现有 `app.db` / `voglander-web/app.db` 执行 INSERT（或删库重建）
- [ ] sqlite 验证：菜单 2 行、role_menu role_id=1 命中
- [ ] 重新登录后菜单可见、页面可进

## 常见坑

- **菜单不显示**：backend 模式必须落 `tb_menu` + 授权；只改前端路由文件无效。
- **改了 seed 但本地还是没有**：现有 app.db 不重新播种 → 走第 3 步手动 INSERT。
- **页面白屏 / not-found**：`component` 路径写错（带了 `.vue`/`views`，或 vue 文件名不匹配 glob）。看浏览器 console `route component is invalid`。
- **menu_name 显示成 `protocolLab.menu` 原文**：i18n key 不存在或 json 未加载。
- **两份 SQL 不同步**：MySQL 用 `JSON_OBJECT`，SQLite 用 JSON 字符串；漏改一份会导致换库后菜单缺失。
