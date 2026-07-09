## 1. 数据库设计

- [ ] 1.1 创建 SQLite 建表脚本 `sql/iptv-schema.sql`（5 张表：channel/category/language/country/sync_log）
- [ ] 1.2 创建 MySQL 建表脚本 `sql/iptv-schema-mysql.sql`
- [ ] 1.3 创建 PostgreSQL 建表脚本 `sql/iptv-schema-postgresql.sql`
- [ ] 1.4 为频道表的 `channel_id` 添加 UNIQUE 索引
- [ ] 1.5 为频道表的 `category_id`/`country_code`/`language_code` 添加普通索引
- [ ] 1.6 验证建表脚本在三种数据库中均可执行

## 2. Integration 层 - IPTV API 客户端

- [ ] 2.1 创建 `voglander-integration/src/main/java/io/github/lunasaw/voglander/integration/iptv/client/IptvApiClient.java`
- [ ] 2.2 实现 `getChannels()` 方法调用 IPTV API 的 channels 端点
- [ ] 2.3 实现 `getCategories()` 方法调用 categories 端点
- [ ] 2.4 实现 `getLanguages()` 方法调用 languages 端点
- [ ] 2.5 实现 `getCountries()` 方法调用 countries 端点
- [ ] 2.6 添加超时配置（默认 30 秒）和重试逻辑（3 次，间隔 5 秒）
- [ ] 2.7 使用 `@Slf4j` 记录所有 API 调用的开始、成功、失败日志
- [ ] 2.8 使用 FastJSON2 解析 API 响应为 DTO

## 3. Integration 层 - DTO 和异常

- [ ] 3.1 创建 `IptvChannelDTO`（channel_id, name, logo, category_id, country_code, language_code）
- [ ] 3.2 创建 `IptvCategoryDTO`（category_id, name）
- [ ] 3.3 创建 `IptvLanguageDTO`（language_code, name）
- [ ] 3.4 创建 `IptvCountryDTO`（country_code, name）
- [ ] 3.5 创建 `IptvSyncLogDTO`（sync_type, status, start_time, end_time, sync_details）
- [ ] 3.6 创建自定义异常类 `IptvApiException`/`IptvApiTimeoutException`/`IptvApiParseException`/`IptvApiConnectionException`

## 4. Integration 层 - Wrapper

- [ ] 4.1 创建 `IptvApiWrapper`，封装 `IptvApiClient`，统一异常处理
- [ ] 4.2 所有方法返回 `ResultDTO<T>`，失败时返回 `ResultDTO` 包装的错误信息
- [ ] 4.3 使用 `WrapperExceptionHandler.executeWithExceptionHandling()` 处理异常
- [ ] 4.4 记录操作开始/成功/失败日志

## 5. Repository 层 - DO 实体

- [ ] 5.1 创建 `IptvChannelDO`（继承 `BaseDO`，对应 `tb_iptv_channel`）
- [ ] 5.2 创建 `IptvCategoryDO`（对应 `tb_iptv_category`）
- [ ] 5.3 创建 `IptvLanguageDO`（对应 `tb_iptv_language`）
- [ ] 5.4 创建 `IptvCountryDO`（对应 `tb_iptv_country`）
- [ ] 5.5 创建 `IptvSyncLogDO`（对应 `tb_iptv_sync_log`）
- [ ] 5.6 所有时间字段使用 `LocalDateTime` 类型
- [ ] 5.7 添加 `@TableName` 注解指定表名

## 6. Repository 层 - Mapper

- [ ] 6.1 创建 `IptvChannelMapper extends BaseMapper<IptvChannelDO>`
- [ ] 6.2 创建 `IptvCategoryMapper extends BaseMapper<IptvCategoryDO>`
- [ ] 6.3 创建 `IptvLanguageMapper extends BaseMapper<IptvLanguageDO>`
- [ ] 6.4 创建 `IptvCountryMapper extends BaseMapper<IptvCountryDO>`
- [ ] 6.5 创建 `IptvSyncLogMapper extends BaseMapper<IptvSyncLogDO>`
- [ ] 6.6 所有 Mapper 添加 `@Mapper` 注解

## 7. Service 层

- [ ] 7.1 创建 `IptvChannelService extends IService<IptvChannelDO>`，实现基础 CRUD
- [ ] 7.2 创建 `IptvCategoryService extends IService<IptvCategoryDO>`
- [ ] 7.3 创建 `IptvLanguageService extends IService<IptvLanguageDO>`
- [ ] 7.4 创建 `IptvCountryService extends IService<IptvCountryDO>`
- [ ] 7.5 创建 `IptvSyncLogService extends IService<IptvSyncLogDO>`
- [ ] 7.6 实现对应的 ServiceImpl，注入相应的 Mapper

## 8. Manager 层 - Assembler

- [ ] 8.1 创建 `IptvManagerAssembler`，实现 DTO ↔ DO 转换
- [ ] 8.2 实现 `channelDtoToDo()` 和 `channelDoToDto()`
- [ ] 8.3 实现 `categoryDtoToDo()` 和 `categoryDoToDto()`
- [ ] 8.4 实现 `languageDtoToDo()` 和 `languageDoToDto()`
- [ ] 8.5 实现 `countryDtoToDo()` 和 `countryDoToDto()`
- [ ] 8.6 实现 `syncLogDtoToDo()` 和 `syncLogDoToDto()`

## 9. Manager 层 - 频道管理

- [ ] 9.1 创建 `IptvChannelManager`，实现 `add(IptvChannelDTO)` 方法
- [ ] 9.2 实现 `update(IptvChannelDTO queryDTO, IptvChannelDTO updateDTO)` 方法
- [ ] 9.3 实现 `updateById(Long id, IptvChannelDTO updateDTO)` 方法
- [ ] 9.4 实现 `get(IptvChannelDTO dto)` 方法
- [ ] 9.5 实现 `deleteOne(IptvChannelDTO dto)` 方法
- [ ] 9.6 实现 `deleteBatch(IptvChannelDTO dto)` 方法
- [ ] 9.7 实现 `getPage(IptvChannelDTO dto, int page, int size)` 方法，支持多维度过滤
- [ ] 9.8 实现 `clearCache(Long id, String channelId)` 方法，清理 Redis 缓存
- [ ] 9.9 实现 `getByChannelId(String channelId)` 方法，按外部 ID 查询

## 10. Manager 层 - 其他实体管理

- [ ] 10.1 创建 `IptvCategoryManager`，实现统一模板方法（add/update/updateById/get/deleteOne/deleteBatch/getPage）
- [ ] 10.2 创建 `IptvLanguageManager`，实现统一模板方法
- [ ] 10.3 创建 `IptvCountryManager`，实现统一模板方法
- [ ] 10.4 创建 `IptvSyncLogManager`，实现统一模板方法
- [ ] 10.5 为 Category/Language/Country Manager 添加列表缓存（key=`iptv:{entity}:list`，TTL 1 小时）

## 11. Manager 层 - 同步任务管理

- [ ] 11.1 创建 `IptvSyncManager`，实现 `fullSync()` 全量同步方法
- [ ] 11.2 实现 `incrementalSync()` 增量同步方法
- [ ] 11.3 实现 `syncChannels()` 同步频道数据，支持批量插入/更新（每批 500 条）
- [ ] 11.4 实现 `syncCategories()` 同步分类数据
- [ ] 11.5 实现 `syncLanguages()` 同步语言数据
- [ ] 11.6 实现 `syncCountries()` 同步国家数据
- [ ] 11.7 实现 `recordSyncLog()` 记录同步日志到 `tb_iptv_sync_log`
- [ ] 11.8 实现 `compareAndUpdate()` 比对字段变化，决定是否更新
- [ ] 11.9 添加数据一致性校验逻辑（校验总量、必填字段完整性）

## 12. Web 层 - Req/VO 和 Assembler

- [ ] 12.1 创建 `IptvChannelQueryReq`（page, size, name, category, country, language）
- [ ] 12.2 创建 `IptvChannelVO`（包含所有字段，时间字段返回 Unix 毫秒时间戳）
- [ ] 12.3 创建 `IptvChannelListResp`（total, items）
- [ ] 12.4 创建 `IptvSyncTriggerReq`（mode: FULL/INCREMENTAL）
- [ ] 12.5 创建 `IptvSyncLogVO` 和 `IptvSyncLogListResp`
- [ ] 12.6 创建 `IptvStatisticsVO`（channel_count, category_count, language_count, country_count）
- [ ] 12.7 创建 `IptvWebAssembler`，实现 Req → DTO 和 DTO → VO 转换
- [ ] 12.8 DTO → VO 转换时，将 `LocalDateTime` 转换为 Unix 毫秒时间戳

## 13. Web 层 - Controller

- [ ] 13.1 创建 `IptvChannelController`，实现 `/iptv/channel/page` 分页查询接口
- [ ] 13.2 实现 `/iptv/channel/get` 查询单个频道详情接口
- [ ] 13.3 实现 `/iptv/channel/statistics` 查询统计信息接口
- [ ] 13.4 创建 `IptvCategoryController`，实现 `/iptv/category/list` 查询所有分类接口
- [ ] 13.5 创建 `IptvLanguageController`，实现 `/iptv/language/list` 查询所有语言接口
- [ ] 13.6 创建 `IptvCountryController`，实现 `/iptv/country/list` 查询所有国家接口
- [ ] 13.7 创建 `IptvSyncController`，实现 `/iptv/sync/trigger` 手动触发同步接口
- [ ] 13.8 实现 `/iptv/sync/log/page` 查询同步日志分页接口
- [ ] 13.9 所有接口添加 `@Valid` 参数校验和 OpenAPI 注解（`@Tag`, `@Operation`, `@Parameter`）
- [ ] 13.10 所有接口返回 `AjaxResult.success(data)` 或 `AjaxResult.error(msg)`

## 14. 定时任务

- [ ] 14.1 创建 `IptvSyncJob`，添加 `@Component` 和 `@Scheduled(cron = "${iptv.api.sync-cron}")` 注解
- [ ] 14.2 添加 `@ConditionalOnProperty(name = "iptv.api.enable", havingValue = "true")` 控制开关
- [ ] 14.3 在任务开始时使用 `RedisLockUtil` 获取分布式锁（key=`iptv:sync:lock`）
- [ ] 14.4 获取锁失败时跳过本次同步，记录日志
- [ ] 14.5 根据配置的 `iptv.api.sync-mode` 决定执行全量或增量同步
- [ ] 14.6 同步完成后释放分布式锁

## 15. 配置

- [ ] 15.1 在 `application-inte.yml` 中新增 `iptv.api` 配置段
- [ ] 15.2 添加 `base-url`（默认 `https://iptv-org.github.io/api`）
- [ ] 15.3 添加 `enable`（默认 `false`）和 `hook-enable`（默认 `true`）
- [ ] 15.4 添加 `sync-cron`（默认 `0 0 2 * * ?` 每天凌晨 2 点）
- [ ] 15.5 添加 `sync-mode`（默认 `incremental`）
- [ ] 15.6 添加 `timeout`（默认 `30000` 毫秒）和 `retry-times`（默认 `3`）
- [ ] 15.7 创建 `IptvApiProperties` 配置类，使用 `@ConfigurationProperties(prefix = "iptv.api")`

## 16. 前端 - API 封装

- [ ] 16.1 在 `vue-vben-admin/apps/web-antd/src/api/` 下创建 `iptv.ts`
- [ ] 16.2 实现 `getIptvChannelPage()` 调用 `/iptv/channel/page`
- [ ] 16.3 实现 `getIptvChannelDetail()` 调用 `/iptv/channel/get`
- [ ] 16.4 实现 `getIptvStatistics()` 调用 `/iptv/channel/statistics`
- [ ] 16.5 实现 `getIptvCategoryList()` 调用 `/iptv/category/list`
- [ ] 16.6 实现 `getIptvLanguageList()` 调用 `/iptv/language/list`
- [ ] 16.7 实现 `getIptvCountryList()` 调用 `/iptv/country/list`
- [ ] 16.8 实现 `triggerIptvSync()` 调用 `/iptv/sync/trigger`
- [ ] 16.9 实现 `getIptvSyncLogPage()` 调用 `/iptv/sync/log/page`
- [ ] 16.10 所有 API 使用 `@vben/request` 统一请求工具

## 17. 前端 - 频道管理页面

- [ ] 17.1 在 `vue-vben-admin/apps/web-antd/src/views/iptv/` 下创建 `channel/` 目录
- [ ] 17.2 创建 `IptvChannelList.vue`，使用 `VbenVxeTable` 展示频道列表
- [ ] 17.3 添加分页组件，支持切换页码和每页条数
- [ ] 17.4 添加搜索框（按名称搜索）和过滤器（分类、国家、语言）
- [ ] 17.5 添加统计卡片组件，展示频道/分类/语言/国家总数
- [ ] 17.6 添加同步状态展示组件，显示最近一次同步的状态、时间、数量
- [ ] 17.7 添加手动触发同步按钮（全量同步、增量同步）
- [ ] 17.8 实现频道详情抽屉，点击频道行展示详情
- [ ] 17.9 添加响应式布局支持（桌面展示表格，移动端展示卡片列表）

## 18. 前端 - 同步日志页面

- [ ] 18.1 在 `views/iptv/` 下创建 `sync-log/` 目录
- [ ] 18.2 创建 `IptvSyncLog.vue`，使用 `VbenVxeTable` 展示同步日志列表
- [ ] 18.3 添加分页组件
- [ ] 18.4 添加状态过滤器（成功/失败/进行中）
- [ ] 18.5 实现同步详情抽屉，点击详情按钮展示 JSON 格式的 `sync_details`
- [ ] 18.6 展示同步时间、类型、状态、同步数量、耗时

## 19. 前端 - 国际化和路由

- [ ] 19.1 在 `locales/zh-CN.json` 中添加 IPTV 相关的中文国际化键
- [ ] 19.2 在 `locales/en-US.json` 中添加 IPTV 相关的英文国际化键
- [ ] 19.3 使用 `module.entity.action` 格式命名国际化键（如 `iptv.channel.name`）
- [ ] 19.4 在路由配置中添加 `/iptv/channel` 和 `/iptv/sync-log` 路由
- [ ] 19.5 在侧边栏菜单中添加 IPTV 菜单项
- [ ] 19.6 添加权限控制（`v-if="$access('iptv:view')"`）

## 20. 技术文档

- [ ] 20.1 在 `doc/1.10.0/` 下创建 `IPTV-API-INTEGRATION-TECH-PLAN.md`
- [ ] 20.2 编写需求背景章节
- [ ] 20.3 编写架构设计章节（模块划分、分层职责、数据流图）
- [ ] 20.4 编写接口设计章节（API 端点列表、请求响应示例）
- [ ] 20.5 编写数据库设计章节（表结构、字段说明、索引、约束）
- [ ] 20.6 编写配置说明章节（`application-inte.yml` 配置项详解）
- [ ] 20.7 编写部署说明章节（数据库脚本执行步骤、依赖版本）
- [ ] 20.8 编写测试策略章节（单元测试、集成测试、E2E 测试）

## 21. 单元测试 - Integration 层

- [ ] 21.1 创建 `IptvApiClientTest`，测试 API 客户端调用逻辑
- [ ] 21.2 使用 `@ExtendWith(MockitoExtension.class)` 和 `@Mock` 模拟 RestTemplate
- [ ] 21.3 测试成功获取频道列表场景
- [ ] 21.4 测试 API 返回错误状态码场景
- [ ] 21.5 测试 API 响应超时场景
- [ ] 21.6 测试 API 返回无效 JSON 场景
- [ ] 21.7 创建 `IptvApiWrapperTest`，测试 Wrapper 异常处理逻辑

## 22. 单元测试 - Service 层

- [ ] 22.1 创建 `IptvChannelServiceTest`，测试基础 CRUD 方法
- [ ] 22.2 使用 `@ExtendWith(MockitoExtension.class)` 和 `@Mock` 模拟 Mapper
- [ ] 22.3 测试 `save()` 方法
- [ ] 22.4 测试 `updateById()` 方法
- [ ] 22.5 测试 `getById()` 方法
- [ ] 22.6 测试 `removeById()` 方法

## 23. 集成测试 - Manager 层

- [ ] 23.1 创建 `IptvChannelManagerTest`，使用 `@SpringBootTest` + `BaseTest`
- [ ] 23.2 测试 `add()` 方法插入频道数据
- [ ] 23.3 测试 `updateById()` 方法更新频道数据
- [ ] 23.4 测试 `getPage()` 方法分页查询，验证多维度过滤
- [ ] 23.5 测试 `get()` 方法查询单个频道
- [ ] 23.6 测试 `deleteOne()` 方法删除频道
- [ ] 23.7 测试缓存清理逻辑（插入后查询缓存，更新后缓存失效）
- [ ] 23.8 使用 `@BeforeEach/@AfterEach` 清理测试数据和缓存
- [ ] 23.9 创建 `IptvSyncManagerTest`，测试全量同步和增量同步逻辑

## 24. 集成测试 - Controller 层

- [ ] 24.1 创建 `IptvChannelControllerTest`，使用 `@SpringBootTest` + `TestRestTemplate`
- [ ] 24.2 测试 `/iptv/channel/page` 接口返回正确的分页数据
- [ ] 24.3 测试参数校验逻辑（page=0 返回错误）
- [ ] 24.4 测试过滤条件（按分类、国家、语言、名称过滤）
- [ ] 24.5 测试 `/iptv/channel/get` 接口返回频道详情
- [ ] 24.6 测试 `/iptv/channel/statistics` 接口返回统计数据
- [ ] 24.7 创建 `IptvSyncControllerTest`，测试手动触发同步接口

## 25. 集成测试 - 定时任务

- [ ] 25.1 创建 `IptvSyncJobTest`，手动触发定时任务方法
- [ ] 25.2 测试全量同步任务执行成功
- [ ] 25.3 测试增量同步任务执行成功
- [ ] 25.4 测试分布式锁逻辑（模拟多节点并发执行）
- [ ] 25.5 测试同步失败后记录错误日志

## 26. E2E 测试 - 前端

- [ ] 26.1 使用 Playwright 测试频道管理页面加载
- [ ] 26.2 测试搜索功能（输入关键词后列表更新）
- [ ] 26.3 测试过滤功能（选择分类后列表更新）
- [ ] 26.4 测试分页功能（切换页码后数据更新）
- [ ] 26.5 测试手动触发全量同步（点击按钮后状态更新）
- [ ] 26.6 测试同步日志页面加载和分页

## 27. 部署和验证

- [ ] 27.1 在开发环境执行数据库建表脚本（SQLite）
- [ ] 27.2 在 `application-inte.yml` 中配置 `iptv.api` 参数
- [ ] 27.3 启动后端应用，验证定时任务不执行（`iptv.api.enable=false`）
- [ ] 27.4 手动调用 `/iptv/sync/trigger?mode=full` 触发全量同步
- [ ] 27.5 验证数据成功插入 5 张表
- [ ] 27.6 验证同步日志记录在 `tb_iptv_sync_log`
- [ ] 27.7 验证前端页面可访问并展示数据
- [ ] 27.8 验证搜索、过滤、分页功能正常
- [ ] 27.9 启动定时任务（`iptv.api.enable=true`），验证按 cron 自动同步
- [ ] 27.10 验证增量同步逻辑（修改 API 数据后，仅更新变化的记录）
