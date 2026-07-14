## 1. 前置变更与真实链路门控

- [ ] 1.1 严格校验并完成 `add-durable-business-task-engine` 的核心表、SPI、状态机、统一 API 和任务中心验收
- [ ] 1.2 添加启动依赖测试：image collection enabled 时必须存在通用任务服务和唯一 `IMAGE_COLLECTION` Handler 注册
- [ ] 1.3 记录当前 `voglander`、`vue-vben-admin` 工作树、基线 commit 和数据库 migration key，避免覆盖用户改动
- [ ] 1.4 对一条在线 GB28181 通道执行 `MediaPlayService.startLive`，记录 stream/node/URL 与引用计数基线
- [ ] 1.5 分别使用 RTSP、HTTP-FLV、HLS 调用 ZLM getSnap，验证 timeout、expire=0、savePath 和临时文件清理
- [ ] 1.6 调用 stopLive 并证明只释放 POC 增加的一项引用，固化 URL 优先级与失败分类证据

## 2. 图像契约、枚举与任务 Handler 契约

- [ ] 2.1 新增图像资产状态、来源类型、格式、storage provider、采集模式和 retention 枚举，不新增图像任务/执行状态枚举
- [ ] 2.2 在 `ImageConstant` 定义资产 ID、图像权限、资产 SSE、每机位 guard 和图像限制，任务常量复用通用模块
- [ ] 2.3 分配 710000 图像错误码，只包含资产、存储、验证、机位和截图错误；状态/lease/attempt 使用 720000 通用码
- [ ] 2.4 在 `voglander-client` 新增 image storage/snapshot command/result，保证不依赖 Web、Repository 或 ZLM 类型
- [ ] 2.5 定义并测试 `ImageStorageService` stage/openStaged/promote/open/delete/exists/discardStaged 契约
- [ ] 2.6 定义并测试 `MediaSnapshotAdapter` 与可关闭 `SnapshotContent` 契约
- [ ] 2.7 定义 `IMAGE_COLLECTION` payload v1、capability、result reference、retry classifier 和 completion participant 契约
- [ ] 2.8 在根 POM 锁定 TwelveMonkeys WebP 依赖并检查无 Jackson/Gson、`javax.*` 或反向模块依赖

## 3. 三张图像表与增量迁移

- [ ] 3.1 新增 Schema 失败测试，断言 asset、source、collection_config 三表、业务唯一键、索引和 700 段菜单
- [ ] 3.2 在 MySQL 全量脚本增加三张图像表、索引、菜单、图像按钮权限和管理员授权
- [ ] 3.3 在 SQLite 全量脚本增加等价三表/索引/菜单，并验证与通用三表共同存在
- [ ] 3.4 在 PostgreSQL 全量脚本增加等价结构，使用原生 `BIGSERIAL/TIMESTAMP/ON CONFLICT`
- [ ] 3.5 新增 MySQL、SQLite、PostgreSQL `1.0.9-image-asset-collection` 非破坏性增量脚本和验证 SQL
- [ ] 3.6 扩展 Repository 构建，把 SQLite migration 以 filtering=false 复制到 classpath
- [ ] 3.7 编写 `SqliteImageSchemaMigratorTest`，覆盖缺少 core migration、旧库升级、重复启动、半失败回滚和已有数据保留
- [ ] 3.8 实现 image migrator：先验证 durable-task migration key，再创建三表/索引/菜单并写 image migration key
- [ ] 3.9 更新 Schema 完整性测试，以 migration key 分别验证通用任务与图像表，不只断言总表数
- [ ] 3.10 在三种数据库执行全量和增量脚本，保存表、唯一键、索引和菜单证据

## 4. Repository、DTO 与 Config Manager

- [ ] 4.1 新增 `ImageAssetDO/Mapper`，字段、`IdType.AUTO` 和 `LocalDateTime` 与设计一致
- [ ] 4.2 新增 `ImageAssetSourceDO/Mapper`，验证可空唯一 `sourceExecutionId` 和 FastJSON2 metadata
- [ ] 4.3 新增 `ImageCollectionConfigDO/Mapper`，只保存 taskId、机位快照、retention 和 capture options
- [ ] 4.4 新增三个基础 IService/实现，不暴露给 Web
- [ ] 4.5 实现资产/来源/config 组合查询，固定排序并参数化 device/channel/task/execution 条件
- [ ] 4.6 添加 Repository 测试，验证 assetId、taskId、sourceExecutionId 唯一和多 NULL 行语义
- [ ] 4.7 新增资产、来源、config、enriched task query DTO 和 Assembler 完整字段测试
- [ ] 4.8 实现 `ImageCollectionConfigManager.create/getByTaskId/getEnrichedPage/getEnrichedDetail`

## 5. 资产事务与生命周期

- [ ] 5.1 编写 `ImageAssetManagerTest` 的 create-with-source、幂等查询、详情和组合分页失败用例
- [ ] 5.2 实现 `ImageAssetManager.createWithSource`、稳定业务 ID 查询、分页和 access scope
- [ ] 5.3 实现 total/available/today/deleteFailed 统计并保证同一可见范围
- [ ] 5.4 编写资产删除状态并发测试，覆盖 AVAILABLE→DELETING、DELETE_FAILED→DELETING、重复 DELETED 和版本冲突
- [ ] 5.5 实现 markDeleting/markDeleted/markDeleteFailed 条件更新和幂等语义
- [ ] 5.6 实现 sourceTaskId/sourceExecutionId/assetId 双向查询，禁止返回 storage 内部字段

## 6. 本地存储与图像验证

- [ ] 6.1 编写 LocalImageStorageService 路径 containment、绝对路径、`..`、符号链接、CREATE_NEW 和幂等删除测试
- [ ] 6.2 实现 `ImageProperties`，只包含 upload/storage/snapshot 和 image collection constraints，不复制 task executor 配置
- [ ] 6.3 初始化 canonical local root 与 `.staging/{workerNode}` 并提供健康检查
- [ ] 6.4 实现限字节流式 stage、SHA-256、唯一 part 文件、所有分支关闭和失败清理
- [ ] 6.5 实现原子 promote、fallback 指标、禁止覆盖、final open/exists/delete 和 key containment
- [ ] 6.6 编写真实 JPEG/PNG/WebP、损坏、伪 MIME、超字节、超像素和溢出验证样本
- [ ] 6.7 实现签名、ImageIO reader、宽高/像素、受限解码和服务端 contentType 映射
- [ ] 6.8 实现原始文件名净化、assetName fallback 和 CRLF/Unicode/路径测试
- [ ] 6.9 实现 staging TTL sweep 和可注入 Clock 测试
- [ ] 6.10 运行 storage provider contract tests

## 7. 上传接入与删除补偿

- [ ] 7.1 编写 stage→validate→promote→DB 成功顺序和 verified metadata 测试
- [ ] 7.2 实现 image ID 和日期分区 final key 生成器及边界测试
- [ ] 7.3 实现上传 owner+idempotency 查找，相同请求不重新读取文件
- [ ] 7.4 实现 USER_UPLOAD 接入，owner 来自 actor、retention 固定 PERMANENT
- [ ] 7.5 实现 DB 失败后的 final object 删除补偿和孤儿指标
- [ ] 7.6 实现 delete/retryDelete，provider 不存在视为成功，失败消息脱敏
- [ ] 7.7 实现 page/detail/statistics/content descriptor 与统一 access scope
- [ ] 7.8 实现 staging/unregistered/missing object reconciliation，生产默认 report-only

## 8. 资产 Web API、身份与内容响应

- [ ] 8.1 编写 ImageActorResolver Bearer、过期、缺 token 和禁止 body owner 测试
- [ ] 8.2 实现 ImageAccessService 模块级全局 scope，并预留未来 ACL policy
- [ ] 8.3 添加无权限 403、资源越权 404 和图像控制同时校验 Task 权限的 Controller 测试
- [ ] 8.4 新增 Asset Req/VO/Resp 和 Web Assembler，时间转 Unix 毫秒且隐藏 provider/key/node/path
- [ ] 8.5 实现 constraints/statistics/getPage/detail/upload API 与 OpenAPI 注解
- [ ] 8.6 编写 content/download MIME、length、ETag、private cache、nosniff、304 和 stream close 测试
- [ ] 8.7 实现 StreamingResponseBody 与 RFC 5987 文件名，client abort 时关闭 provider stream
- [ ] 8.8 实现 DELETE/delete:retry、结构化审计和无敏感路径测试
- [ ] 8.9 完成全部 710000 图像错误码 HTTP 映射，不复制通用任务错误码

## 9. 图像采集配置与通用任务创建

- [ ] 9.1 编写 image schedule constraints 测试，覆盖 ONCE 禁止 schedule、SCHEDULED 三字段、inclusive count 和图像上限
- [ ] 9.2 实现 image constraints validator，并映射 SCHEDULED 到通用 FIXED_RATE 模式
- [ ] 9.3 编写 config+通用 task 创建事务测试，覆盖 ONCE、FIXED_RATE、机位快照和 owner/type/key 幂等
- [ ] 9.4 实现 `ImageCollectionApplicationService.create`，通过受信任内部 API 创建 `IMAGE_COLLECTION` task
- [ ] 9.5 实现 payload v1，仅包含 config/task identity 和版本，不包含 secret、URL 或路径
- [ ] 9.6 实现按 device/channel 的 enriched page/detail，把通用任务 DTO 与 config 组合且不影子复制状态
- [ ] 9.7 实现 image reschedule 校验：只允许通用 PAUSED、机位不可变、图像 constraints 合法，再调用通用内部 command
- [ ] 9.8 实现 manual retry config snapshot factory，由通用引擎创建新 ONCE task 并保存 origin

## 10. ZLM Adapter 与媒体引用

- [ ] 10.1 编写 ZlmMediaSnapshotAdapter 静态 mock 测试，覆盖节点、timeout=15、expire=0、唯一 savePath 和清理
- [ ] 10.2 实现 node resolve、URL 请求、返回路径 canonical、非空文件和 AutoCloseable 临时内容
- [ ] 10.3 编写 CaptureStreamLeaseService 已有流、新流、缺 node、URL fallback 和 close 防重复测试
- [ ] 10.4 实现 acquire/close，任何成功 acquire 后异常均精确 stopLive 一次
- [ ] 10.5 添加真实 ZLM POC 契约测试并记录 URL 优先级

## 11. IMAGE_COLLECTION Handler

- [ ] 11.1 编写 Handler contract test，验证稳定 taskType、payloadVersion、capabilities 和重复注册失败
- [ ] 11.2 编写成功执行测试：通用 claim 后读取 config、校验机位、stream、snapshot、validation、storage 和 result
- [ ] 11.3 实现执行前 device/channel 存在、归属、权限和在线校验，不通过时不调用 ZLM
- [ ] 11.4 实现统一 snapshot ingest 和来源快照，包含 device/channel/protocol/node/stream 的脱敏元数据
- [ ] 11.5 实现 cancellation token 检查和所有 finally 资源释放
- [ ] 11.6 实现 image retry classifier：timeout/临时节点/临时存储可重试，配置/缺资源/权限/验证永久失败
- [ ] 11.7 添加离线、建流超时、截图失败、损坏图、存储失败和取消全分支测试
- [ ] 11.8 实现每机位并发 guard，资源忙时返回窗口内可重试结果，不替代通用 claim

## 12. Completion Participant 与原子结果

- [ ] 12.1 扩展通用 Handler completion participant 契约，限定同数据源 DB 写入、幂等和无外部 I/O
- [ ] 12.2 编写 asset+source+generic execution/task/event 同事务提交和回滚测试
- [ ] 12.3 实现 `ImageCollectionCompletionParticipant`，以 sourceExecutionId/assetId 唯一约束防重复登记
- [ ] 12.4 实现 resultRef=`IMAGE_ASSET/{assetId}` 和脱敏 result summary
- [ ] 12.5 实现事务失败后的 provider object compensation 与补偿失败 orphan 记录
- [ ] 12.6 添加重复完成、worker crash 后重跑和 unique conflict 测试，证明最多一项资产和一次成功计数

## 13. 图像采集 Web API 与通用任务 API 联动

- [ ] 13.1 新增 collection constraints/create/enriched page/detail Req/VO/Resp 和 Unix 毫秒 Web Assembler
- [ ] 13.2 实现图像领域创建和 device/channel enriched 查询 API，不新增专属执行表/API
- [ ] 13.3 确认 pause/resume/cancel/retry/execution/event 使用通用 business-task API，并添加无重复 endpoint 架构测试
- [ ] 13.4 添加 Image+Task 权限组合、状态冲突、幂等和资源越权 Controller 测试
- [ ] 13.5 在事务提交后发布 asset SSE；任务/执行/进度事件由通用引擎发布
- [ ] 13.6 注册 image handler/snapshot/storage/orphan 指标，任务 lag/queue/lease 指标复用通用模块
- [ ] 13.7 添加 traceId/actor/task/execution/asset/device/channel 日志测试，禁止 Base64、JWT、secret、URL query 和路径

## 14. OpenAPI 与前端 API 基础

- [ ] 14.1 更新后端 OpenAPI，核对 image 创建/enriched 查询和通用 task/execution/resultRef 契约
- [ ] 14.2 同步权威 API 文档到前端仓库，完成 backend-first 契约评审
- [ ] 14.3 新增 `src/api/image/types.ts`，只定义 Asset/Source/Config/ImageCollection 创建和 enriched 类型
- [ ] 14.4 新增 asset API 和 method/path/body/header/download 单测
- [ ] 14.5 新增 collection constraints/create/page/detail API，状态控制和执行查询导入 `src/api/task/*`
- [ ] 14.6 新增 image 路由和中英文 i18n，验证菜单 component 与 key 对称
- [ ] 14.7 在通用 task type registry 注册 IMAGE_COLLECTION label/icon/detailRoute/resultRenderer 和 fallback 测试

## 15. 图像资产前端页面

- [ ] 15.1 编写 asset query、route merge、format/size/status/action 纯函数测试
- [ ] 15.2 实现资产统计、Schema filters、gallery/table 共享 query/page/selection 和固定操作列
- [ ] 15.3 实现 preview composable，并发 6、Blob URL 引用计数、失败 retry 和精确 revoke
- [ ] 15.4 实现 asset card/gallery skeleton、error/empty、键盘选择、alt 和响应式列数
- [ ] 15.5 实现 upload Drawer、每次新提交 idempotency key 和未知结果重试复用
- [ ] 15.6 实现 asset detail、预览、下载、删除、删除重试和权限双检
- [ ] 15.7 实现 asset SSE debounce refresh 和断线全量 query

## 16. 图像采集前端页面

- [ ] 16.1 编写 inclusive count、constraints、通用 state+capability+Image/Task permission 操作矩阵测试
- [ ] 16.2 实现 collection filters、enriched VxeGrid、进度、计数和操作矩阵
- [ ] 16.3 实现 device-channel selector 的设备分页、展开加载通道、在线状态和单选
- [ ] 16.4 实现 ONCE/SCHEDULED 创建 Drawer、桌面左右/移动纵向布局和服务端 constraints
- [ ] 16.5 实现提交 loading、字段定位、Idempotency-Key 和未知结果重查
- [ ] 16.6 使用通用 task API 实现 pause/resume/cancel/retry，click-time 双权限和状态冲突刷新
- [ ] 16.7 实现 enriched task detail、通用 execution/event Drawer 和 asset/resultRef 跳转
- [ ] 16.8 实现 asset+business-task SSE 合并刷新和断线全量查询
- [ ] 16.9 添加创建、机位懒加载、控制、retry、未知 task type 和 i18n 组件测试

## 17. 设备联动、可访问性与视觉验收

- [ ] 17.1 在设备通道增加“图像采集/查看图像”，使用图标、权限和 click-time 双检
- [ ] 17.2 实现 collection/assets 的 deviceId/channelId 深链和返回状态保持
- [ ] 17.3 验证 375/768/1024/1440 无页面级溢出、VxeGrid 固定列和 Drawer 自适应
- [ ] 17.4 验证明暗主题对比度、状态非颜色唯一表达和 quantified/indeterminate 进度
- [ ] 17.5 验证键盘顺序、focus trap/return、44px 点击区、aria-label/alt/role=alert
- [ ] 17.6 验证 reduced-motion、skeleton 空间预留和无明显 CLS

## 18. E2E、故障注入与发布

- [ ] 18.1 E2E 上传 JPEG/PNG/WebP，验证统计、图库、详情、预览、下载、删除和重复删除
- [ ] 18.2 E2E 在线通道 ONCE，验证通用 execution SUCCEEDED、唯一 asset、来源和直播引用归还
- [ ] 18.3 E2E 离线/不存在/错归属，验证稳定 image failure、无资产和无 ZLM 调用
- [ ] 18.4 E2E FIXED_RATE 跨多点，验证通用 fixed timeline、进度、计数、暂停/恢复 MISSED 和终态
- [ ] 18.5 E2E 重启与双实例，验证通用恢复、唯一 execution/source/asset 和不补拍历史点
- [ ] 18.6 故障注入 storage 后 DB 失败、completion rollback、compensation 失败、worker crash、队列满和 SSE 断线
- [ ] 18.7 运行 Java 测试、三数据库 migration、真实 ZLM POC 和 `mvn clean compile`
- [ ] 18.8 运行前端 image/task 单测、`pnpm check`、`pnpm lint` 和 `pnpm build:antd`
- [ ] 18.9 运行 placeholder/secret/path/重复任务引擎扫描
- [ ] 18.10 更新 1.0.9 详细设计、配置、迁移、共享存储、Handler 运维和回滚文档
- [ ] 18.11 以 image API/Handler enabled、通用 scheduler disabled 发布上传试点，再对小组机位开启 task scheduler
- [ ] 18.12 完成验收报告：变更、测试、数据库、前端截图、任务内核依赖、风险和后续 MinIO/retention/ACL
