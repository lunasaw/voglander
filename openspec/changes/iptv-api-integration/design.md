## Context

当前 Voglander 平台支持 GB28181、GT1078、ONVIF 等协议的视频监控设备接入，但缺乏对第三方公开 IPTV 数据源的集成能力。IPTV API (https://github.com/iptv-org/api) 提供了全球电视频道、分类、语言、国家等结构化数据，通过 REST API 公开访问。

现有约束：
- 必须遵循项目分层架构：`web → manager → service → repository → common`
- 必须使用 FastJSON2（禁止 Jackson/Gson）
- 必须使用 MyBatis Plus 基础方法（简单 CRUD 无需自定义 SQL）
- 时间类型：DO/DTO 层使用 `LocalDateTime`，VO 返回 Unix 毫秒时间戳
- Manager 层统一模板方法：`add/update/updateById/get/deleteOne/deleteBatch/getPage`
- Web 层入参必须用专用 `*Req` 类，不得直接收 DTO
- 外部系统集成统一走 Wrapper + `ResultDTO` 响应

技术栈：
- Spring Boot 3.5.3 + Java 17
- MyBatis Plus 3.5.5
- SQLite（默认）/ MySQL 8 / PostgreSQL
- RestTemplate（HTTP 客户端）
- FastJSON2（JSON 序列化）
- Spring `@Scheduled`（定时任务）

## Goals / Non-Goals

**Goals:**
- 实现 IPTV API 客户端封装，支持 channels、categories、languages、countries 四个端点调用
- 实现定时同步任务，支持全量/增量两种模式，避免重复数据
- 提供完整的数据查询 REST API，支持多维度过滤（分类、国家、语言、名称）与分页
- 提供 Vue 3 管理界面，展示频道列表、同步状态、同步日志
- 输出完整的技术方案文档到 `doc/1.10.0/IPTV-API-INTEGRATION-TECH-PLAN.md`

**Non-Goals:**
- 不支持 IPTV 流媒体播放（仅元数据管理）
- 不修改现有 ZLM/GB28181 流媒体架构
- 不支持 IPTV 频道的增删改操作（仅同步展示）
- 不实现实时订阅更新（仅定时轮询）

## Decisions

### 决策 1：集成层放置位置 - `voglander-integration/iptv`

**选择**：在 `voglander-integration` 模块下新增 `iptv` 包，包含 API 客户端、DTO、Wrapper。

**理由**：
- 符合项目架构原则：外部系统集成统一在 `integration` 模块
- 与现有 GB28181/ZLM 集成模式一致
- 便于统一异常处理和 `ResultDTO` 包装

**替代方案**：
- ❌ 放在 `voglander-client` 下：该模块侧重外部服务客户端与 DTO，不含业务包装器
- ❌ 新建独立模块 `voglander-iptv`：过度设计，功能不足以支撑独立模块

### 决策 2：HTTP 客户端选择 - RestTemplate

**选择**：使用 Spring RestTemplate 调用 IPTV API。

**理由**：
- 项目已有 RestTemplate Bean 配置，无需新增依赖
- 简单的 RESTful GET 请求场景，RestTemplate 足够
- 与项目现有 HTTP 调用模式一致

**替代方案**：
- ❌ WebClient：引入 Reactor 响应式编程复杂度，项目未采用响应式架构
- ❌ Feign：需要额外依赖，且目标 API 无需服务发现和负载均衡

### 决策 3：数据同步策略 - 全量/增量两种模式

**选择**：支持配置切换全量同步和增量同步。

**理由**：
- **全量同步**：简单可靠，适合初次导入或数据异常修复，清空本地数据后重新拉取
- **增量同步**：性能优化，仅处理新增/更新数据，按频道 ID（外部 API 的唯一标识）判重

**实现细节**：
- 全量同步：先 `DELETE FROM tb_iptv_*`，再批量 `INSERT`
- 增量同步：按频道 ID 查询本地是否存在，存在则比对字段（logo/name 等）决定是否 `UPDATE`，不存在则 `INSERT`
- 使用 MyBatis Plus 的 `saveBatch()` 和 `updateBatchById()` 批量操作（每批 500 条）

**替代方案**：
- ❌ 仅支持全量同步：每次清空全表，数据量大时性能差，且同步期间数据不可用
- ❌ 仅支持增量同步：初次导入复杂，且无法修复数据异常

### 决策 4：定时任务实现 - Spring `@Scheduled`

**选择**：使用 Spring 原生 `@Scheduled` 实现定时同步任务。

**理由**：
- 项目已有 `@EnableScheduling` 配置
- 简单的 cron 定时任务场景，无需引入 XXL-Job 等分布式调度框架
- 配置灵活：`iptv.api.sync-cron` 支持动态配置（如 `0 0 2 * * ?` 每天凌晨 2 点）

**实现细节**：
- `IptvSyncJob` 类标注 `@Component` + `@Scheduled(cron = "${iptv.api.sync-cron}")`
- 使用 `@ConditionalOnProperty(name = "iptv.api.enable", havingValue = "true")` 控制开关
- 使用分布式锁（`RedisLockUtil`）防止多节点重复执行

**替代方案**：
- ❌ XXL-Job：引入额外复杂度，项目未部署 XXL-Job 调度中心，且需求不涉及复杂调度策略

### 决策 5：数据库表设计 - 5 张表

**选择**：新增 5 张表：

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `tb_iptv_channel` | 频道主表 | `id`(自增), `channel_id`(外部ID), `name`, `logo`, `category_id`, `country_code`, `language_code` |
| `tb_iptv_category` | 分类字典 | `id`(自增), `category_id`(外部ID), `name` |
| `tb_iptv_language` | 语言字典 | `id`(自增), `language_code`, `name` |
| `tb_iptv_country` | 国家字典 | `id`(自增), `country_code`, `name` |
| `tb_iptv_sync_log` | 同步日志 | `id`(自增), `sync_type`(FULL/INCREMENTAL), `status`(SUCCESS/FAILED/RUNNING), `start_time`, `end_time`, `sync_details`(JSON) |

**理由**：
- **外部 ID 与内部 ID 分离**：`channel_id`/`category_id` 等存储 IPTV API 的原始 ID，`id` 为本地自增主键，避免外部 ID 变更影响关联关系
- **字典表独立**：分类、语言、国家可能被多个频道引用，独立表便于维护和查询
- **同步日志详情字段用 JSON**：`sync_details` 存储各端点的同步数量、耗时等详细信息，使用 FastJSON2 序列化为 TEXT 类型

**约束**：
- `channel_id` 添加 UNIQUE 索引，防止重复插入
- `category_id`/`language_code`/`country_code` 添加外键约束（可选，视数据完整性要求）

### 决策 6：Manager 层模板方法实现

**选择**：为 4 个实体各创建一个 Manager，实现统一模板方法。

| Manager | 核心方法 | 缓存策略 |
|---------|---------|---------|
| `IptvChannelManager` | `add/update/updateById/getPage/get/deleteOne/deleteBatch` | Redis 缓存单个频道（key=`iptv:channel:{id}`），清理缓存时同时清理 `channel_id` 维度 |
| `IptvCategoryManager` | 同上 | Redis 缓存分类列表（key=`iptv:category:list`），TTL 1 小时 |
| `IptvLanguageManager` | 同上 | Redis 缓存语言列表（key=`iptv:language:list`），TTL 1 小时 |
| `IptvCountryManager` | 同上 | Redis 缓存国家列表（key=`iptv:country:list`），TTL 1 小时 |

**理由**：
- 符合项目 Manager 层规范，统一入口、参数校验、缓存清理、日志
- 分类/语言/国家数据变化频率低，适合缓存
- 频道数据查询频繁，按 ID 缓存单条记录，避免大列表缓存内存占用

### 决策 7：Web 层 Controller 设计

**选择**：新增 4 个 Controller：

| Controller | 路径前缀 | 核心接口 |
|-----------|---------|---------|
| `IptvChannelController` | `/iptv/channel` | `/page`(分页查询), `/get`(详情), `/statistics`(统计) |
| `IptvCategoryController` | `/iptv/category` | `/list`(全部分类) |
| `IptvLanguageController` | `/iptv/language` | `/list`(全部语言) |
| `IptvCountryController` | `/iptv/country` | `/list`(全部国家) |
| `IptvSyncController` | `/iptv/sync` | `/trigger`(手动触发), `/log/page`(同步日志分页) |

**入参规范**：
- `IptvChannelQueryReq`：分页查询入参（`page`, `size`, `name`, `category`, `country`, `language`）
- `IptvSyncTriggerReq`：同步触发入参（`mode`: FULL/INCREMENTAL）
- 所有 Req 经 `IptvWebAssembler` 转换为 DTO

**出参规范**：
- 统一返回 `AjaxResult.success(data)` 或 `AjaxResult.error(msg)`
- 分页包装为 `IptvChannelListResp`（含 `total` + `items`）
- VO 层时间字段返回 Unix 毫秒时间戳（字段名以 `Time` 结尾）

### 决策 8：前端架构 - Vue 3 + Vben Admin

**选择**：在 `vue-vben-admin/apps/web-antd/src/views/iptv/` 下新增页面：

| 页面 | 路径 | 组件 |
|------|------|------|
| 频道管理 | `/iptv/channel` | `IptvChannelList.vue` |
| 同步日志 | `/iptv/sync-log` | `IptvSyncLog.vue` |

**理由**：
- 复用 Vben Admin 现有组件和工具：
  - `VbenVxeTable`：Schema 驱动表格组件，声明式配置列、过滤器、分页
  - `@vben/request`：统一 HTTP 请求工具
  - `v-if="$access('iptv:sync')"`：权限控制指令
- 使用 `module.entity.action` 国际化键规范（如 `iptv.channel.name`）
- 响应式布局：桌面展示完整表格，移动端转为卡片列表

**实现细节**：
- 统计卡片：使用 Ant Design Vue 的 `<a-statistic>` 组件展示频道/分类/语言/国家总数
- 同步状态：使用 `<a-badge>` 组件展示成功/失败/进行中状态
- 手动同步按钮：调用 `/iptv/sync/trigger`，全量同步前弹出确认对话框
- 同步日志详情：使用 `<a-drawer>` 抽屉组件展示 JSON 格式的 `sync_details`

### 决策 9：技术文档输出位置

**选择**：输出到 `doc/1.10.0/IPTV-API-INTEGRATION-TECH-PLAN.md`。

**理由**：
- 符合项目版本文档管理规范（按版本号组织文档）
- 与现有 GB28181/ZLM 技术方案文档位置一致

**内容结构**：
1. 需求背景
2. 架构设计（模块划分、分层职责、数据流）
3. 接口设计（API 端点、请求响应示例）
4. 数据库设计（表结构、索引、约束）
5. 配置说明（`application-inte.yml` 配置段）
6. 部署说明（数据库脚本执行、依赖版本）
7. 测试策略（单元测试、集成测试、E2E 测试）

## Risks / Trade-offs

### 风险 1：IPTV API 稳定性和可用性

**风险**：IPTV API 是第三方公开服务，可能出现：
- API 地址变更或服务下线
- 响应超时或频率限制
- 数据格式变更（字段增删、类型变化）

**缓解措施**：
- 配置化 API base URL（`iptv.api.base-url`），支持快速切换备用地址
- 设置合理的连接超时（30s）和重试策略（3 次，间隔 5s）
- 字段映射使用宽松模式，API 新增字段不影响解析，缺失必填字段时记录错误并跳过该条数据
- 定期监控同步日志，失败率超过阈值时告警

### 风险 2：数据量增长导致同步性能下降

**风险**：IPTV API 可能返回数万条频道数据，全量同步耗时长，影响用户体验。

**缓解措施**：
- 优先使用增量同步，减少数据库操作
- 使用批量插入/更新（每批 500 条），避免逐条操作
- 同步任务异步执行，不阻塞主线程，前端通过轮询或 SSE 获取同步状态
- 考虑分页拉取 API 数据（如果 API 支持）

### 风险 3：外部 ID 冲突

**风险**：IPTV API 的 `channel_id` 可能与本地 `id` 冲突，或不同端点返回相同 ID。

**缓解措施**：
- 使用独立的 `channel_id` 字段存储外部 ID，本地 `id` 自增
- `channel_id` 添加 UNIQUE 索引，数据库层面防止重复
- 增量同步时按 `channel_id` 查询现有记录，避免重复插入

### 风险 4：定时任务在多节点部署时重复执行

**风险**：项目可能部署多个实例，`@Scheduled` 会在每个节点上触发。

**缓解措施**：
- 使用 Redis 分布式锁（`RedisLockUtil`），锁键为 `iptv:sync:lock`
- 获取锁失败的节点跳过本次同步，记录日志

### Trade-off 1：增量同步的复杂性 vs 性能收益

**权衡**：增量同步需要逐条比对数据，代码复杂度高于全量同步。

**决策**：支持两种模式，默认增量，管理员可手动触发全量。

**理由**：
- 增量同步在数据量大时性能优势明显
- 全量同步作为兜底方案，用于初次导入或数据修复

### Trade-off 2：缓存策略 vs 数据实时性

**权衡**：缓存提升查询性能，但可能导致数据不一致。

**决策**：
- 频道数据按 ID 缓存单条，同步后清理缓存
- 分类/语言/国家列表缓存 1 小时，变化频率低

**理由**：
- IPTV 数据更新频率低（按天级别），1 小时 TTL 可接受
- 同步任务完成后主动清理缓存，保证数据一致性

## Migration Plan

### 阶段 1：数据库初始化
1. 在 `sql/` 目录下新增 `iptv-schema.sql`（SQLite）、`iptv-schema-mysql.sql`、`iptv-schema-postgresql.sql`
2. 手动执行或集成到自动建表逻辑（现有 `DatabaseInitializer`）

### 阶段 2：后端开发
1. `voglander-integration` 下实现 `IptvApiClient`、DTO、Wrapper
2. `voglander-repository` 下实现 DO、Mapper
3. `voglander-service` 下实现 Service 层（继承 `IService<DO>`）
4. `voglander-manager` 下实现 Manager 层（统一模板方法 + 缓存清理）
5. `voglander-web` 下实现 Controller 层（Req → DTO → VO 转换）
6. 实现定时任务 `IptvSyncJob`

### 阶段 3：前端开发
1. 在 `vue-vben-admin/apps/web-antd/src/api/` 下新增 `iptv.ts`（API 调用封装）
2. 在 `views/iptv/` 下实现频道管理和同步日志页面
3. 添加国际化键到 `locales/`
4. 添加路由配置和菜单项

### 阶段 4：配置和文档
1. 在 `application-inte.yml` 中新增 `iptv.api` 配置段
2. 输出技术方案文档到 `doc/1.10.0/IPTV-API-INTEGRATION-TECH-PLAN.md`

### 阶段 5：测试
1. 单元测试：Service/Manager 层业务逻辑
2. 集成测试：API 客户端调用、数据库操作、同步任务
3. E2E 测试：前端页面交互、手动同步流程

### 回滚策略
- **数据库回滚**：删除 5 张 `tb_iptv_*` 表
- **代码回滚**：Git 回退提交，删除 `integration/iptv` 包和相关文件
- **配置回滚**：设置 `iptv.api.enable=false`，禁用定时任务

## Open Questions

1. **IPTV API 是否支持分页拉取？** 需要查阅 API 文档，如果支持，可优化大数据量场景下的同步性能。
2. **是否需要支持频道的手动增删改？** 当前设计仅支持同步展示，如需支持手动编辑，需新增 CRUD 接口和前端表单。
3. **定时任务调度框架最终确认**：当前设计使用 Spring `@Scheduled`，如未来需要分布式调度能力（如任务编排、失败重试可视化），是否迁移到 XXL-Job？
4. **数据归档策略**：同步日志是否需要定期归档或清理？建议保留最近 30 天日志，超过后自动删除。
