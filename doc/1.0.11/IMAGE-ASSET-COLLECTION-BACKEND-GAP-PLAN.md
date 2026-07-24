# 图像资产与图像采集后端缺口与实施计划

> 版本：1.0.11
>
> 日期：2026-07-22
>
> 状态：后端就绪度审计完成，待按门禁实施
>
> 前端设计输入：`voglander-vben-frontend/apps/web-antd/doc/1.0.9/IMAGE-ASSET-COLLECTION-UI-TECH-PLAN.md`
>
> 后端审计基线：`voglander` / `dev` / `2fdc49c`
>
> 前端核对基线：`voglander-vben-frontend` / `feat/protocol-lab-external-register` / `8aff77c9`

---

## 0. 审计结论

后端接口尚未全部就绪，不能直接进入图像资产与图像采集 UI 替换阶段。

按本方案实际依赖的 27 项后端能力统计：7 项已就绪、19 项部分就绪、1 项完全缺失。完全缺失项是受控缩略图端点；部分就绪项中包含 3 个发布阻断级安全问题，因此“已有接口路径”不能等同于“满足上线条件”。

统计口径为 10 个资产端点、5 个 Collection 端点、9 个相关 Durable Business Task 端点、1 个 SSE 能力和 2 个设备/通道远程搜索能力；不把同一端点的字段逐个重复计数。

### 0.1 发布阻断项

1. Business Task 查询、详情、执行历史仅经过全局 JWT 登录拦截，没有执行 `Task:Query` 或按任务资源类型执行 `Image:Collection:Query`，且使用 `BizTaskAccessScopeDTO.global()` 查询全局数据。
2. `IMAGE_COLLECTION` 的暂停、恢复、取消、人工重试和调整计划没有同时校验 `Task:Control` 与 `Image:Collection:Control`。
3. SSE 只校验 token 是否有效，订阅注册和逐事件投递均未校验 topic、任务类型或图像资源权限。
4. `GET /api/v1/images/{assetId}/thumbnail?profile=table|gallery` 完全不存在，宫格与列表不能按方案接入。
5. 任务行乐观锁 `version` 未返回，现有控制请求会把前端的 `undefined` 传给后端，而 Manager 明确要求 `expectedVersion` 非空。
6. 采集分页的设备/通道过滤发生在分页之后，`total` 和当前页结果不正确。
7. 上传和任务创建只按 key 早返回，无法识别“相同 key、不同业务内容”，并存在并发唯一键竞争处理不完整的问题。

### 0.2 门禁判定

| 门禁 | 当前状态 | 判定 |
| --- | --- | --- |
| Gate 0：工程与现状基线 | 通过 | 定向 10 个测试类共 39 个测试通过，但只证明旧契约未破坏 |
| Gate 1：thumbnail 与 `sourceMetadata` | 未通过 | thumbnail 缺失；来源 JSON 已持久化但未通过 Web API 返回，名称快照未写入来源 JSON |
| Gate 2：私有 Blob 与幂等 | 未通过 | content 基础响应较完整；download 权限不一致；幂等内容冲突和并发竞争未闭环 |
| Gate 3：状态、capability、双权限、version | 未通过 | capability 基础模型存在，但字段、资源鉴权、双权限、版本和部分状态策略不完整 |
| Gate 4：E2E、慢网、SSE、性能 | 未通过 | 缩略图资源限制、SSE 授权、关联查询性能和新契约 E2E 均未建立 |

结论：必须先完成 B0-B4 后端工作包，至少通过 Gate 1、Gate 2、Gate 3 后，前端才能分别替换宫格、私有二进制入口和采集控制/详情。

---

## 1. 审计范围与方法

本次审计覆盖以下范围：

- 图像资产 constraints、statistics、分页、详情、上传、thumbnail、content、download、删除和重试删除。
- 图像采集 constraints、创建、分页、详情和 reschedule。
- Durable Business Task 任务查询、详情、执行查询、执行详情、暂停、恢复、取消和人工重试。
- 图像与任务 SSE 的订阅鉴权、topic 映射和逐事件投递。
- 设备/通道远程搜索能力、分页正确性、关联查询次数、错误码、HTTP 状态、幂等和并发竞争。
- Web VO、Assembler、Manager、Service、Repository、数据库索引、配置和现有自动化测试。

审计方法为：对照 1.0.9 UI 方案逐端点检查 Controller 到 Repository 的实际实现，并用定向测试验证当前基线。定向测试覆盖 10 个测试类、39 个测试，结果为 0 failure、0 error、0 skipped。现有测试中有用例明确锁定“发现相同 key 后，不读取新内容或校验新 payload 就返回”的旧行为，因此测试全绿不代表新幂等契约已满足。

Thumbnail、sourceMetadata、version、双权限和幂等冲突是 UI 方案明确依赖；Business Task 全局读取、SSE 逐事件授权、分页后过滤、N+1 和并发唯一键竞争是本次代码审计发现的后端安全/正确性问题。后者虽不是新增 UI 组件本身提出的端点，也必须作为发布门禁处理。

---

## 2. 接口就绪度总表

### 2.1 图像资产

| 方法与路径　　　　　　　　　　　　　　　　　 | 状态　　 | 已有能力　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　| 未就绪部分　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 |
| ----------------------------------------------| ----------| -------------------------------------------------------------------------------| ----------------------------------------------------------------------|
| `GET /api/v1/images/constraints`　　　　　　 | 已就绪　 | 鉴权、格式、大小、像素等约束可用　　　　　　　　　　　　　　　　　　　　　　　| 无本期阻断项　　　　　　　　　　　　　　　　　　　　　　　　　　　　 |
| `GET /api/v1/images/statistics`　　　　　　　| 已就绪　 | `total/available/today/deleteFailed` 可用　　　　　　　　　　　　　　　　　　 | 无本期阻断项　　　　　　　　　　　　　　　　　　　　　　　　　　　　 |
| `POST /api/v1/images/getPage`　　　　　　　　| 部分就绪 | 分页和现有筛选可用　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　| 每行单独查 source；响应缺少嵌套 `source.sourceMetadata`　　　　　　　|
| `GET /api/v1/images/{assetId}`　　　　　　　 | 部分就绪 | 基础资产与扁平来源字段可用　　　　　　　　　　　　　　　　　　　　　　　　　　| 详情权限未对齐 View；缺嵌套 source；需要状态契约测试　　　　　　　　 |
| `POST /api/v1/images/uploads`　　　　　　　　| 部分就绪 | 上传、校验、暂存、提升、补偿和首次幂等可用　　　　　　　　　　　　　　　　　　| 同 key 不同内容不冲突；并发唯一键竞争可能返回 null 或遗留已提升对象　|
| `GET /api/v1/images/{assetId}/thumbnail`　　 | 未实现　 | 无　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　| 完整实现 profile、变换、限流、超时、缓存、ETag、错误和测试　　　　　 |
| `GET /api/v1/images/{assetId}/content`　　　 | 部分就绪 | View 权限、原始 Blob、checksum ETag、304、private cache、inline、nosniff 已有 | 缺 404/410/409 状态区分和完整二进制契约测试　　　　　　　　　　　　　|
| `GET /api/v1/images/{assetId}/download`　　　| 部分就绪 | 安全 RFC 5987 attachment 文件名、checksum ETag 和 Blob 可用　　　　　　　　　 | 当前检查 `Image:Asset:Download`，方案要求统一检查 `Image:Asset:View` |
| `DELETE /api/v1/images/{assetId}`　　　　　　| 已就绪　 | 删除请求、状态迁移、审计可用　　　　　　　　　　　　　　　　　　　　　　　　　| 保持现状，并补回归测试　　　　　　　　　　　　　　　　　　　　　　　 |
| `POST /api/v1/images/{assetId}/delete:retry` | 已就绪　 | 删除失败重试和审计可用　　　　　　　　　　　　　　　　　　　　　　　　　　　　| 保持现状，并补回归测试　　　　　　　　　　　　　　　　　　　　　　　 |

### 2.2 图像采集

| 方法与路径　　　　　　　　　　　　　　　　　　　　　　　　| 状态　　 | 已有能力　　　　　　　　　　　　　　　　　　　　　　　　| 未就绪部分　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　|
| -----------------------------------------------------------| ----------| ---------------------------------------------------------| -----------------------------------------------------------------------------------|
| `GET /api/v1/image-collection-tasks/constraints`　　　　　| 已就绪　 | 模式、最小间隔、最大计划数、保留策略可用　　　　　　　　| 无本期阻断项　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　|
| `POST /api/v1/image-collection-tasks`　　　　　　　　　　 | 部分就绪 | 领域校验、设备/通道校验、任务和配置创建可用　　　　　　 | 同 key 不同内容不冲突；并发任务创建竞争未闭环　　　　　　　　　　　　　　　　　　 |
| `POST /api/v1/image-collection-tasks/getPage`　　　　　　 | 部分就绪 | 基础任务与机位信息可用　　　　　　　　　　　　　　　　　| camera 过滤晚于分页；N+1；缺 version、capabilities、last execution 和 result 字段 |
| `GET /api/v1/image-collection-tasks/{taskId}`　　　　　　 | 部分就绪 | 基础任务、调度、机位与计数可用　　　　　　　　　　　　　| 缺 version、scheduleVersion、capabilities、last execution 和 result 字段　　　　　|
| `POST /api/v1/image-collection-tasks/{taskId}:reschedule` | 部分就绪 | PAUSED、FIXED_RATE、计划校验和 expectedVersion 下传已有 | 只检查图像控制权限；未检查 Task 控制权限和 `RESCHEDULE` capability　　　　　　　　|

### 2.3 Durable Business Task 与 SSE

| 能力 | 状态 | 未就绪部分 |
| --- | --- | --- |
| 任务分页、详情和 constraints | 部分就绪 | 缺查询权限和资源类型范围；VO 缺任务 `version`；详情始终不返回 active execution |
| 执行分页和执行详情 | 部分就绪 | 缺查询权限和父任务资源类型范围；执行详情时间线固定上限但分页入口已有 |
| pause、resume、cancel | 部分就绪 | IMAGE_COLLECTION 缺双权限；依赖未暴露的 version |
| manual retry | 部分就绪 | IMAGE_COLLECTION 缺双权限；当前允许 `PARTIAL_COMPLETED`，与 UI 仅允许 `FAILED` 不一致 |
| SSE `/api/v1/stream/events` | 部分就绪 | 只检查 token；不校验 topic 和逐事件资源范围；缺独立 `image.asset.deleting` topic |

### 2.4 可直接复用能力

以下能力已经满足本期需要，不应重复建设：

- 设备分页支持 device ID 精确查询、名称模糊查询、状态和分页；通道分页支持 device ID/channel ID 精确查询、名称模糊查询、状态和分页。
- 任务状态模型、执行事实、事件时间线、进度、pause/resume/cancel/manual retry 基础命令和 `expectedVersion` 请求字段已经存在。
- IMAGE_COLLECTION Handler 已声明 pause、cancel、manual retry、progress、reschedule 等 capability；`PAUSE` 可继续表示暂停生命周期的暂停与恢复两个方向。
- 采集任务已经保存设备/通道名称快照；采集产出的 asset source 已保存 device ID 和 channel ID。
- 资产约束、统计、生命周期管理、内容流 ETag/private cache 和安全下载文件名已经存在。
- Business Task 执行分页默认按 `planned_at DESC, execution_id DESC`，支持按计划时间和创建时间排序，可直接用于每页 20 条“加载更多”。
- MySQL、PostgreSQL、SQLite 均已有 task/config、asset/source 关联所需的唯一键与主要索引。

---

## 3. 完整缺口登记

### 3.1 安全与资源授权

| ID     | 等级 | 当前证据　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　| 风险　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　 | 目标　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　|
| --------| ------| -----------------------------------------------------------------------------------------------------------------------------------------------------------------------| ------------------------------------------------------------------------------| -----------------------------------------------------------------------------------------------------------------------------------|
| SEC-01 | P0　 | `BusinessTaskController.java:97-185` 的查询方法不接收 Authorization，并使用 `BizTaskAccessScopeDTO.global()`；`ResourcesConfig.java:60-69` 只保证登录　　　　　　　　 | 任意登录用户可能读取全局任务、执行和事件事实　　　　　　　　　　　　　　　　 | 为任务、执行查询增加资源感知授权；Task 中心使用 `Task:Query`，IMAGE_COLLECTION 可由 `Image:Collection:Query` 在限定任务类型内读取 |
| SEC-02 | P0　 | `BusinessTaskController.java:188-280` 只经 `requireControlPermission` 检查 `Task:Control`；`ImageCollectionController.java:107-122` 只检查 `Image:Collection:Control` | 单个权限可绕过图像域双控制协议　　　　　　　　　　　　　　　　　　　　　　　 | IMAGE_COLLECTION 的 pause/resume/cancel/retry/reschedule 全部要求两个权限；其他任务类型保持 `Task:Control`　　　　　　　　　　　　|
| SEC-03 | P0　 | `SseController.java:29-40` 只验证 token；Local/Redis EventBus 分别在 `LocalSseEventBus.java:70-83`、`RedisBackedSseEventBus.java:119-133` 只按 topic 投递　　　　　　 | 无资源权限用户可订阅任务或资产标识事件；图像查询用户可能收到其他任务类型事件 | 注册时校验 topic；Emitter 保存授权上下文；逐事件按 topic、`taskType` 和权限过滤　　　　　　　　　　　　　　　　　　　　　　　　　 |
| SEC-04 | P1　 | `ImageAssetController.java:106-111` 的详情只检查 Query；download 在 `ImageAssetController.java:174-178` 检查独立 Download　　　　　　　　　　　　　　　　　　　　　　 | 与 UI 的 Query/View 分层不一致　　　　　　　　　　　　　　　　　　　　　　　 | 列表元数据使用 Query；详情使用 Query+View；thumbnail/content/download 统一使用 View　　　　　　　　　　　　　　　　　　　　　　　 |

资源授权必须在查询条件或关联查询中落实，不能先全局加载记录再返回 403，否则会产生存在性侧信道。无权用户对存在和不存在的资源应得到相同的 403 行为；有权用户才区分 404、409 和 410。

### 3.2 Thumbnail 与来源元数据

| ID | 等级 | 当前证据 | 风险 | 目标 |
| --- | --- | --- | --- | --- |
| THM-01 | P0 | `ImageAssetController.java:144` 从 content 直接跳到 download，没有 thumbnail mapping 或服务 | 宫格/列表只能使用原图或无法显示，违反 Gate 1 | 新增固定 table/gallery profile 的私有缩略图端点 |
| THM-02 | P0 | 不存在 profile enum、变换执行器、并发/队列/超时限制、派生缓存和算法版本 | 任意或高频图片变换可能耗尽 CPU/内存 | 只允许两个 profile；有界队列、超时、像素预检、内存上限和稳定 ETag |
| SRC-01 | P0 | `ImageAssetSourceDTO.java:21` 已有 JSON；`ImageCollectionTaskHandler.java:207-215` 写入 device/channel ID；`ImageAssetVO.java:6-23` 和 Assembler 未返回 JSON | 前端无法可靠显示采集机位来源 | 新增嵌套 `source`，透传白名单化 `sourceMetadata`；保留旧扁平字段兼容 |
| SRC-02 | P1 | 采集配置在 `ImageCollectionApplicationService.java:97-105` 已保存名称快照，但 Handler 未写入 asset source metadata | 历史资产只能显示 ID，名称变更后也无采集时快照 | 采集时把非空 deviceName/channelName 快照写入来源 JSON |

Thumbnail 目标行为：

| profile | 目标盒 | 变换 | 响应上限 |
| --- | --- | --- | --- |
| `table` | 112x84 | EXIF 方向校正后居中 cover；只缩小、不放大 | 64 KiB |
| `gallery` | 320x240 | EXIF 方向校正后居中 cover；只缩小、不放大 | 256 KiB |

当原图小于目标盒时，保持原始有效分辨率，不进行放大；服务端仍按 4:3 进行安全裁切，前端容器负责最终展示尺寸。编码质量逐级降低仍无法满足字节上限时，返回明确错误，不得回传原图。透明图像如转 JPEG，必须先合成固定中性背景，不能由默认黑底或未初始化像素决定结果。

### 3.3 VO、详情和控制矩阵

| ID | 等级 | 当前证据 | 风险 | 目标 |
| --- | --- | --- | --- | --- |
| DTO-01 | P0 | `BizTaskDTO.java:55` 有 version；`BusinessTaskVO.java` 和 `ImageCollectionVO.java` 均无 version | 前端无法提交有效 expectedVersion | 任务列表、详情、控制响应统一返回任务行 `version` |
| DTO-02 | P0 | `ImageCollectionVO.java:7-30` 缺 capabilities、scheduleVersion、lastExecutionId、resultRefType/resultRefId/resultSummary | 采集列表不能按后端能力渲染操作和结果入口 | 列表与详情补齐这些字段，不通过每行额外请求 Task 详情实现 |
| DTO-03 | P1 | `BusinessTaskController.java:128` 总是把 active execution 传 null | 任务详情无法展示正在执行的权威事实 | 依据 `lastExecutionId` 查询非终态 execution 并填入 activeExecution |
| CTL-01 | P0 | `ImageCollectionApplicationService.java:157-190` 校验 PAUSED 和 FIXED_RATE，但未校验 Handler 的 RESCHEDULE | capability 不是命令入口的服务端约束 | reschedule 在执行状态检查前后均校验对应 payloadVersion Handler 的 RESCHEDULE |
| CTL-02 | P1 | `BizTaskCreateService.java:58-60` 允许 FAILED 和 PARTIAL_COMPLETED 人工重试 | IMAGE_COLLECTION 与 UI 操作矩阵不一致 | 仅对 IMAGE_COLLECTION 限制为 FAILED；其他任务类型保留现有策略 |

需要明确区分三个版本字段：

- `version`：`tb_biz_task.version`，用于 pause/resume/cancel/reschedule 的 `expectedVersion`。
- `scheduleVersion`：任务调度计划版本，用于执行事实和排障，不替代乐观锁版本。
- collection config 的内部 `version`：本期不作为 UI 控制版本返回，也不得覆盖任务 `version`。

### 3.4 查询正确性与性能

| ID | 等级 | 当前证据 | 风险 | 目标 |
| --- | --- | --- | --- | --- |
| QRY-01 | P0 | `ImageCollectionApplicationService.java:123-135` 先分页 task，再在内存中过滤 deviceId/channelId | total 错误、页为空或变短、其他页匹配记录不可见 | task 与 collection config 在数据库中 join，camera 条件在 count/page 前生效 |
| QRY-02 | P1 | 同一方法在循环中逐行 `configManager.getByTaskId`；`ImageCollectionConfigManager.java:67-81` 也有相同模式 | 每页 20 条产生 21 次查询 | 使用 join 分页一次返回 task+config，或最多采用 1+1 批量查询 |
| QRY-03 | P1 | `ImageAssetController.java:100-102` 对分页每行调用 `getSourceByAssetId`；Mapper 已为筛选 join source，但没有把 source 映射到结果 | 资产默认 24 条产生 25 次查询 | 扩展现有 join 的组合映射，一次返回 asset+source，保持现有筛选和 total 语义 |

关联查询必须保持原有排序的确定性，并用稳定 ID 作为第二排序键。查询优化不改变现有 `POST .../getPage`、query 中的 page/size 或 `{ total, items }` 响应形状。

### 3.5 幂等、并发和错误契约

| ID | 等级 | 当前证据 | 风险 | 目标 |
| --- | --- | --- | --- | --- |
| IDM-01 | P0 | `ImageIngestService.java:74-77` 和 `BizTaskCreateService.java:87-91` 在读取/校验新内容前直接返回旧结果 | 同 key 不同文件、名称、机位或计划被误当成安全重放 | 比较规范化业务内容；相同返回首次结果，不同返回确定性 409 业务码 |
| IDM-02 | P0 | `ImageAssetManager.java:78-80` insertIfAbsent 失败后按新 assetId 查询；唯一键若冲突在 idempotency identity 上，查询可返回 null | Controller NPE、错误成功响应或已提升对象遗留 | 竞争失败后按 owner+key 重查 winner、比较内容，并补偿删除 loser 的 final object |
| IDM-03 | P0 | `BizTaskManager.java:73-100` 采用先查后 save；并发唯一键异常未转为重放/冲突 | 同 key 并发时结果不确定，可能返回通用异常 | 使用跨数据库 insert-if-absent，未插入时重查 winner、比较规范内容，确定性返回或 409 |
| IDM-04 | P0 | `ImageCollectionApplicationService.java:64-80` 在 task 幂等查询前重新检查设备/通道存在性、在线状态和计划 | 首次已成功后设备离线或删除，同 key 重试不再返回首次结果 | 先按 identity 查已接受事实并比较 canonical command；同内容直接重放，只有新 identity 才执行可变外部状态校验 |
| ERR-01 | P0 | `ServiceExceptionEnum.java:50-66` 无 idempotency conflict、asset gone 和 thumbnail 错误；`GlobalExceptionHandler.java:77-91` 无对应映射 | 前端无法区分确定性拒绝和未知结果，也无法区分 404/410 | 增加稳定业务码和真实 HTTP 状态，并补全 Handler 测试 |
| ERR-02 | P1 | `ImageAssetController.java:153` 把不存在和所有非 AVAILABLE 状态统一映射为 404 | 已删除、状态冲突和不存在语义混淆 | 权限通过后按 404/410/409 区分；不得降级到其他二进制端点 |

本期采用“不新增数据库列”的幂等实现，符合 1.0.9 第一阶段边界：

- 上传 fingerprint 由 owner scope、文件 SHA-256、规范化原文件名和规范化有效 assetName 组成。重放仍需暂存并计算摘要，比较完成后立即丢弃暂存对象。
- 任务创建 fingerprint 由 task type、taskName、mode、调度字段、规范化 payload、owner/subject 和 retry origin 组成；从已接受 task 字段与 payload 可重建比较值。
- IMAGE_COLLECTION 的 deviceId、channelId、collectionMode 已在 payload 中；retention 本期固定为 PERMANENT。若将来开放可变 retention，必须先把它加入 payload 和 fingerprint。
- 指纹采用字段名排序后的 canonical JSON UTF-8 字节做 SHA-256；比较使用完整摘要，不使用展示字符串或异常消息。
- 数据库现有唯一键继续充当最终并发裁决。先查只用于快路径，唯一键竞争后的 requery+compare 才是正确性路径。
- 上传 loser 在任何未成为 winner 的路径都必须删除自己已提升的 final object；删除失败进入现有 orphan recorder，不得静默忽略。
- `Idempotency-Key` 对历史调用保持可选以兼容既有 API；图像 UI 必须发送。服务端对已提供的 key 校验为 1-128 个可见 ASCII 字符，拒绝空白、控制字符和超长值，且不在业务日志中输出原值。

处理顺序必须固定：先校验 key 和可安全规范化的请求字段，再查询已接受 identity；存在时只完成内容比较并返回首次结果或 409，不重新执行设备在线、当前约束或其他可变外部状态校验。identity 不存在时才执行完整领域校验、存储提升和数据库创建。竞争 loser 在 insert-if-absent 返回 0 后采用相同的 compare 分支。

建议新增并固定以下错误码：

| 业务码 | 数值 | HTTP | 使用场景 |
| --- | --- | --- | --- |
| `IDEMPOTENCY_KEY_REUSED` | 600007 | 409 | 同一幂等 identity 对应不同规范化业务内容 |
| `IMAGE_ASSET_GONE` | 710009 | 410 | 有权用户请求已 DELETED 资产的 thumbnail/content/download |
| `IMAGE_THUMBNAIL_PROFILE_INVALID` | 710010 | 400 | profile 缺失、大小写错误或非 table/gallery |
| `IMAGE_THUMBNAIL_UNAVAILABLE` | 710013 | 503 | 解码/编码无法安全完成、队列满或变换超时；不得返回原图 |

`IMAGE_FILE_TYPE_UNSUPPORTED`、`IMAGE_DECODE_FAILED` 继续用于上传输入校验；已入库资产的派生图失败使用 `IMAGE_THUMBNAIL_UNAVAILABLE`，避免把服务端派生故障误报成用户上传请求错误。

### 3.6 SSE 主题与投递

| ID | 等级 | 当前证据 | 风险 | 目标 |
| --- | --- | --- | --- | --- |
| SSE-01 | P0 | `ImageConstant.java:19-20` 只有 created/deleted；`ImageAssetManager.java:167-173` 把 DELETING 和 DELETE_FAILED 都折叠到 deleted | UI 监听的 `image.asset.deleting` 永远不会收到 | 新增独立 deleting 常量；DELETING 发 deleting，DELETED/DELETE_FAILED 发 deleted |
| SSE-02 | P0 | Emitter holder 只保存 userId/topics；事件总线只做 topic 前缀匹配 | 无法实施资源授权 | 保存允许权限与任务类型，逐事件执行 allowlist 过滤 |
| SSE-03 | P1 | 缺 topic 权限和 Local/Redis 一致性测试 | 单机正确不代表集群广播正确 | 同一授权器同时用于本地直发和 Redis remote delivery，并建立参数化测试 |
| SSE-04 | P0 | `SseController.java:37-38` 在 JWT userId 解析失败时把完整 token 当 userId；EventBus 会把它拼入 emitterId 并写 debug 日志 | Bearer token 可能进入内存标识和日志 | 必须从已认证 UserDTO 取稳定用户 ID；缺 ID 时拒绝订阅，emitterId 使用随机连接 ID，任何日志不得包含 token |

`BusinessTaskSseEvent.java:22-35` 已包含 `taskType`，可以作为任务类型过滤依据。图像资产事件当前把 `taskType` 置为 `IMAGE_ASSET`，可沿用该字段区分资产事件。SSE 仍只是刷新提示，事件 payload 不扩展业务对象或敏感内容。

---

## 4. 目标接口契约

### 4.1 资产响应

资产分页和详情增加嵌套 `source`，同时暂时保留已有扁平来源字段，避免旧调用方立即破坏。时间字段继续使用后端已有的 `capturedAt/ingestedAt`；当前前端类型中的 `capturedTime/ingestedTime` 应由前端对齐，不作为后端重命名任务。

```json
{
  "assetId": "img_0123456789abcdef0123456789abcdef",
  "assetName": "north-gate",
  "status": "AVAILABLE",
  "contentType": "image/jpeg",
  "imageFormat": "JPEG",
  "fileSize": 123456,
  "width": 1920,
  "height": 1080,
  "capturedAt": 1784700000000,
  "ingestedAt": 1784700001000,
  "checksum": "...",
  "sourceType": "CAMERA_CAPTURE",
  "sourceTaskId": "btask_0123456789abcdef0123456789abcdef",
  "sourceExecutionId": "bexec_0123456789abcdef0123456789abcdef",
  "sourceEntityId": "device:channel",
  "originalFilename": null,
  "source": {
    "sourceType": "CAMERA_CAPTURE",
    "sourceSystem": "VOGLANDER_CAPTURE",
    "sourceEntityType": "CAMERA",
    "sourceEntityId": "device:channel",
    "sourceTaskId": "btask_0123456789abcdef0123456789abcdef",
    "sourceExecutionId": "bexec_0123456789abcdef0123456789abcdef",
    "originalFilename": null,
    "sourceMetadata": {
      "deviceId": "device-001",
      "channelId": "channel-001",
      "deviceName": "北门设备",
      "channelName": "北门通道"
    }
  }
}
```

Web 层只能返回 `sourceMetadata` 的允许字段。采集来源至少保证非空 deviceId/channelId；deviceName/channelName 可缺失。既有 protocol、nodeServerId、streamId 属于内部诊断字段，本期不通过 Web VO 返回。

### 4.2 Thumbnail 二进制契约

请求：

```http
GET /api/v1/images/{assetId}/thumbnail?profile=table|gallery
Authorization: Bearer <token>
If-None-Match: "sha256:<derived-hash>"
```

成功响应必须包含：

- 浏览器安全的 `Content-Type`，不相信数据库之外的客户端声明。
- `Content-Length`，且不超过 profile 上限。
- `ETag`，由 `asset.checksum + profile + thumbnailAlgorithmVersion` 生成。
- `Cache-Control: private, max-age=300`、`Vary: Authorization`、`X-Content-Type-Options: nosniff`。
- `Content-Disposition: inline`。
- 匹配 `If-None-Match` 时返回 304，不读取原图、不执行变换。

算法版本固定配置名，例如 `thumb-v1`。算法、裁切、方向处理或编码策略变化时必须提升版本，使旧派生缓存自然失效。

派生缓存采用节点内有界内存缓存，不建立数据库表、公共 CDN 或跨会话持久缓存。缓存键为完整 ETag，配置总字节数、单项字节数、条目数和 TTL；缓存只能优化性能，不能绕过每次请求的权限和资产状态检查。

### 4.3 Collection 列表与详情

`ImageCollectionVO` 在保持现有字段的基础上增加：

```json
{
  "version": 7,
  "scheduleVersion": 3,
  "capabilities": ["PAUSE", "CANCEL", "MANUAL_RETRY", "PROGRESS", "RESCHEDULE"],
  "lastExecutionId": "bexec_0123456789abcdef0123456789abcdef",
  "resultRefType": "IMAGE_ASSET",
  "resultRefId": "img_0123456789abcdef0123456789abcdef",
  "resultSummary": "{...}",
  "cancelledCount": 0,
  "progressMessage": "...",
  "progressRevision": 12
}
```

`capabilities` 表示 Handler 生命周期能力，不根据当前状态删除。前端和后端命令入口再把 capability、当前状态和权限三者求交集。`resultSummary` 继续经过 `BusinessTaskDataSanitizer`，不能透传 payload、存储路径或异常堆栈。

### 4.4 Business Task 详情

- `BusinessTaskVO` 和 `BusinessTaskDetailVO` 增加 `version`，Assembler 从 `BizTaskDTO.version` 映射。
- 详情根据 `lastExecutionId` 返回处于 PENDING、RUNNING 或 RETRY_WAIT 的 `activeExecution`；不存在或已经终态时为 null。
- IMAGE_COLLECTION 详情必须返回 capability；暂停状态仍返回 `PAUSE`，用于表示 resume。
- 执行分页默认最新优先，每页由调用方指定 20；不新增固定“前 100 条”旁路接口。

### 4.5 权限矩阵

| 端点/操作 | 必需权限 | 资源约束 |
| --- | --- | --- |
| 资产 constraints/statistics/page | `Image:Asset:Query` | 图像模块范围 |
| 资产详情 | `Image:Asset:Query` + `Image:Asset:View` | 授权后查询 asset |
| thumbnail/content/download | `Image:Asset:View` | 授权后按 404/409/410 判定；Download 权限不再单独放行 |
| 上传 | `Image:Asset:Upload` | owner 取登录用户，不接受请求伪造 |
| 删除/重试删除 | `Image:Asset:Delete` | 保持现有状态 CAS |
| Collection constraints/page/detail | `Image:Collection:Query` | taskType 固定 IMAGE_COLLECTION |
| Collection create | `Image:Collection:Create` | owner 取登录用户 |
| 通用 Task 中心查询 | `Task:Query` | 可读取允许的通用任务范围 |
| 图像页读取 Task/Execution | `Image:Collection:Query` | 只允许 taskType=IMAGE_COLLECTION |
| IMAGE_COLLECTION pause/resume/cancel/retry/reschedule | `Task:Control` + `Image:Collection:Control` | 同时满足状态、capability、expectedVersion |
| 其他任务控制 | `Task:Control` | 保持相应 Handler 状态/capability |
| `image.asset.*` SSE | `Image:Asset:Query` | 只投递资产刷新提示 |
| IMAGE_COLLECTION 的 `business.task.*` SSE | `Task:Query` 或 `Image:Collection:Query` | 后者只接收 taskType=IMAGE_COLLECTION |
| 其他 `business.task.*` SSE | `Task:Query` | 不向只有图像权限的订阅者投递 |

`Image:Asset:Download` 暂时保留在菜单和角色数据中，避免直接删除历史权限码；1.0.11 二进制端点不再把它作为单独放行条件。上线前应检查自定义角色，确保原来只有 Download 的角色按产品预期补授 View，避免权限收敛造成意外回归。

### 4.6 控制状态矩阵

| 操作 | 状态 | capability | 版本 | IMAGE_COLLECTION 权限 |
| --- | --- | --- | --- | --- |
| pause | SCHEDULED、RUNNING | PAUSE | 必填 | 双权限 |
| resume | PAUSED | PAUSE | 必填 | 双权限 |
| reschedule | PAUSED 且 FIXED_RATE | RESCHEDULE | 必填 | 双权限 |
| manual retry | FAILED | MANUAL_RETRY | executionId + idempotency key | 双权限 |
| cancel | SCHEDULED、RUNNING、PAUSED | CANCEL | 必填 | 双权限 |

`CANCELLING` 和所有完成态不允许控制。状态检查必须在后端执行，UI 隐藏按钮不是安全边界。

### 4.7 二进制状态与错误

权限在资产查询之前校验。权限通过后：

| 资产事实 | HTTP/业务码 |
| --- | --- |
| 不存在 | 404 / `IMAGE_ASSET_NOT_FOUND` |
| DELETED | 410 / `IMAGE_ASSET_GONE` |
| DELETING、DELETE_FAILED | 409 / `IMAGE_ASSET_STATE_CONFLICT` |
| AVAILABLE 但存储读取失败 | 503 / `IMAGE_STORAGE_READ_FAILED` |
| thumbnail profile 非法 | 400 / `IMAGE_THUMBNAIL_PROFILE_INVALID` |
| thumbnail 安全派生失败、超时或队列满 | 503 / `IMAGE_THUMBNAIL_UNAVAILABLE` |

content/download 保留 checksum ETag；thumbnail 使用派生 ETag。三类端点均返回原始二进制或空 304，不进入 `{ code, data }` 包装；错误响应仍使用统一 JSON 错误结构。

---

## 5. 分层实施拆解

### 5.1 Common

修改：

- `voglander-common/.../constant/image/ImageConstant.java`：增加 `SSE_ASSET_DELETING`，保留 created/deleted。
- `voglander-common/.../exception/ServiceExceptionEnum.java`：增加 600007、710009、710010、710013。
- 新增固定 `ThumbnailProfile` 枚举，profile 值、目标尺寸、最大字节由服务端代码定义，不接受请求宽高。
- 新增 canonical JSON/fingerprint 工具，只处理明确字段，不序列化任意 DTO 的非业务字段。

### 5.2 Client 与配置

修改或新增：

- 保持 `ImageStorageService` 只负责原始对象读写，不把 thumbnail 变换塞入存储 provider。
- 在 `ImageProperties` 增加 thumbnail 配置：enabled、algorithmVersion、workerCount、queueCapacity、timeoutMillis、cacheMaxBytes、cacheMaxEntries、cacheTtlSeconds。
- 配置必须有安全默认值并做启动校验；worker/queue/cache 不能为无界。
- 继续使用已引入的 TwelveMonkeys WebP reader 和 ImageIO；输出格式由 thumbnail 服务端选择并通过契约测试锁定。

建议默认值：workerCount 不超过可用 CPU 的 2，queueCapacity=64，timeoutMillis=3000，cacheMaxBytes=64 MiB，cacheMaxEntries=512，cacheTtlSeconds=300。部署可调小，不允许通过请求覆盖。

### 5.3 Repository

新增三类关联查询：

1. `ImageAssetMapper`：保留现有 asset LEFT JOIN source 的筛选与分页 SQL，扩展关联 result map/组合 DTO 一次回填 source；详情也使用同一组合映射，移除 Controller 逐行查询。
2. `ImageCollectionConfigMapper` 或专用 read mapper：biz task INNER JOIN collection config 的 count/page/detail 查询；deviceId/channelId 在分页前生效。
3. `BizTaskMapper`：为 SQLite/MySQL/PostgreSQL 分别增加 `INSERT OR IGNORE`、`INSERT IGNORE`、`ON CONFLICT DO NOTHING` 的 task insert-if-absent；返回 0 后才按 idempotency identity 重查。
4. `BizTaskExecutionMapper`：复用现有 task join，增加允许 taskType 集合或等价资源范围条件。

数据库决定：本期不增加列、不新增 thumbnail 表、不增加 fingerprint 列。现有 `uk_image_asset_idempotency`、`uk_biz_task_idempotency`、`uk_image_collection_config_task`、`idx_image_collection_config_camera`、source/task 索引足以支撑本期正确性路径。实现后必须对 MySQL、PostgreSQL、SQLite 三套 SQL 做相同语义测试。

### 5.4 Manager

修改：

- `ImageAssetManager` 返回 asset+source 组合 DTO；`createWithSource` insert 竞争失败后按 idempotency identity 重查，不再按 loser assetId 返回 null。
- `ImageCollectionConfigManager` 移除 enriched 分页的逐行配置查询，改用 Repository join。
- `BizTaskManager` 和 `BizTaskExecutionManager` 的 access scope 增加 allowedTaskTypes 或等价限制，不能把资源过滤只留在 Controller。
- `BizTaskManager.create` 使用 mapper insert-if-absent 裁决竞争并执行 requery+fingerprint compare；仅确认是同一业务内容时返回 winner。不得依赖捕获 PostgreSQL 唯一键异常后在同一事务继续查询。
- 提供按 `lastExecutionId` 读取 active execution 的安全方法，复用同一个授权 scope。

### 5.5 Service

新增 `ImageThumbnailService`，职责包括：

- 在存储读取前校验资产权限后的状态事实。
- 根据 checksum/profile/algorithmVersion 计算 ETag，优先处理 304 和本地派生缓存。
- 限制输入字节和像素；方向校正、4:3 cover、只缩小、编码质量迭代和响应字节检查。
- 使用隔离的有界 executor，处理排队失败、超时和中断；所有流、ImageReader/ImageWriter 和中间缓冲在异常路径关闭。
- 返回不可变的 contentType、bytes、etag，不返回原始 storageKey。

修改：

- `ImageCollectionTaskHandler` 把 config 中的非空名称快照写入采集 asset source metadata。
- `ImageIngestService` 先 stage/inspect/fingerprint，再判定 replay；winner/loser 所有路径都有确定的 staged/final cleanup。
- `BizTaskCreateService` 在早返回之前完成 canonical 内容比较；IMAGE_COLLECTION 人工重试只允许 FAILED。
- `ImageCollectionApplicationService.reschedule` 增加 RESCHEDULE capability 和双权限所需的领域校验入口。
- 提取统一 Business Task 授权服务，供查询、控制和 SSE 使用，避免 Controller 各自复制权限分支。

### 5.6 Web

修改：

- `ImageAssetController`：新增 thumbnail；统一二进制响应 header；detail 与 download 权限对齐；状态错误明确化。
- `ImageAssetVO`/`ImageAssetWebAssembler`：增加嵌套 source 和白名单 metadata；旧扁平字段暂时保留。
- `ImageCollectionVO`/`ImageCollectionWebAssembler`：补齐 version、scheduleVersion、capabilities、lastExecutionId、result 字段和补充计数/进度字段。
- `BusinessTaskVO`/`BusinessTaskWebAssembler`：补 `version`；详情填 active execution。
- `BusinessTaskController`：所有读端点接收 Authorization 并构建资源 scope；所有 IMAGE_COLLECTION 控制执行双权限。
- `GlobalExceptionHandler`：增加新业务码 HTTP 映射；保持非图像旧错误码行为不变。
- OpenAPI：记录二进制 200/304 与 JSON 400/401/403/404/409/410/503，记录 profile enum、Idempotency-Key 和 expectedVersion 必填语义。

### 5.7 SSE

修改：

- `SseController` 从已认证 UserDTO 解析稳定用户 ID 和权限并拒绝未授权 topic；topics 去空白、去重、校验 allowlist，拒绝空 topic 和任意前缀滥用，不把 token 作为 userId 或日志字段。
- `SseEventBus.register` 接收不可变 `SseSubscriptionContext`，至少包含 userId、允许 topic、是否有 Task:Query、是否有 Image Query、允许 task types。
- Local/Redis EventBus 在 send 前调用同一 `SseDeliveryAuthorizer`；Redis 广播只传业务事件，不传用户权限。
- `ImageAssetManager` 发送 created/deleting/deleted 三个稳定 topic；DELETE_FAILED 继续触发 deleted 列表刷新，但 eventType 保留 `ASSET_DELETE_FAILED`。

---

## 6. 工作包与实施顺序

### B0：资源授权与安全边界

范围：SEC-01、SEC-02、SEC-03、SEC-04。

交付：

- Business Task 查询和执行查询的资源感知 scope。
- IMAGE_COLLECTION 双权限控制。
- SSE topic 注册与逐事件过滤。
- 资产详情和所有二进制权限对齐。

完成标准：权限组合参数化测试全部通过；无权用户无法通过 ID 猜测区分存在/不存在；Local/Redis SSE 行为一致。

### B1：Thumbnail 与来源契约

范围：THM-01、THM-02、SRC-01、SRC-02。

交付：

- table/gallery thumbnail 端点与有界变换服务。
- ETag/304/private cache/字节限制/方向/不放大/无原图降级。
- 资产嵌套 source 和采集机位 metadata。

完成标准：Gate 1 契约测试通过，前端可以只使用 thumbnail 构建列表和宫格。

### B2：任务 DTO、控制矩阵与查询正确性

范围：DTO-01、DTO-02、DTO-03、CTL-01、CTL-02、QRY-01、QRY-02、QRY-03。

交付：

- version/capabilities/result/active execution 完整返回。
- reschedule capability 和 IMAGE_COLLECTION retry 状态策略。
- 资产/source、task/config 的正确分页关联查询。

完成标准：设备/通道过滤 total 正确；一页查询次数稳定为常数；所有控制都有有效 expectedVersion。

### B3：幂等与错误元数据

范围：IDM-01、IDM-02、IDM-03、IDM-04、ERR-01、ERR-02。

交付：

- 上传、collection create、manual retry 的 canonical fingerprint compare。
- 并发唯一键 winner/loser 和存储补偿闭环。
- 600007/710009/710010/710013 与 HTTP 映射。

完成标准：相同内容稳定重放、不同内容稳定 409；并发测试无重复事实、无 null、无未记录孤儿对象。

### B4：SSE 主题、性能和发布证据

范围：SSE-01、SSE-02、SSE-03、SSE-04 及所有性能/E2E 测试。

交付：

- `image.asset.deleting`。
- 高频事件、断线重连、集群广播的授权测试。
- 慢存储、超大图、队列满、超时和缓存命中的资源测试。
- OpenAPI 与发布验收报告。

完成标准：Gate 4 所需后端证据齐全，且没有通过扩大线程池、放宽像素或回传原图规避失败。

实施顺序固定为 B0 -> B1 -> B2 -> B3 -> B4。B0 可与 B1 的纯 thumbnail 单元实现并行开发，但任何环境联调必须先合入 B0；B2/B3 都涉及任务与响应契约，应在同一发布分支完成回归。

---

## 7. 测试矩阵

### 7.1 权限与资源范围

| 场景 | 期望 |
| --- | --- |
| 无 token 访问 REST | 401 |
| 登录但无 Query 访问 task/collection/asset page | 403 |
| 只有 Task:Query | 可读通用 Task；不获得图像二进制 View 权限 |
| 只有 Image:Collection:Query | 只读 IMAGE_COLLECTION task/execution；不能读其他 task type |
| 只有 Task:Control 或只有 Image:Collection:Control 控制图像任务 | 403，无状态变化，无 accepted audit |
| 两个 Control 权限且状态/capability/version 正确 | 控制成功 |
| 无 View 请求存在或不存在的 asset binary | 均为 403 |
| 无 SSE topic 权限订阅 | 注册被拒绝 |
| 图像查询用户订阅 business.task topic | 只接收 IMAGE_COLLECTION 事件 |

### 7.2 Thumbnail

至少覆盖：

- table 精确 profile、gallery 精确 profile、缺失 profile、未知 profile、大小写错误。
- 横图、竖图、正方形、小于目标图、带 EXIF 1/3/6/8 方向、透明 PNG、JPEG、PNG、WEBP 输入。
- 输出只缩小、4:3 cover、方向正确、table <=64 KiB、gallery <=256 KiB。
- 相同 checksum/profile/version 得到稳定 ETag；不同 profile 或算法版本 ETag 不同。
- `If-None-Match` 命中返回 304，且 storage.open、decode、encode 都未调用。
- `Cache-Control: private`、`Vary: Authorization`、Content-Type、Content-Length、inline、nosniff。
- 不存在 404、DELETED 410、DELETING/DELETE_FAILED 409、无权限 403、读取失败 503。
- 队列满、变换超时、编码无法满足字节上限均返回 710013，不回传原图。
- 并发压测下工作线程、队列、缓存字节不超过配置；失败路径不泄露线程、流或缓存引用。

### 7.3 来源与查询

- CAMERA_CAPTURE source 至少返回 deviceId/channelId；名称存在时返回快照，缺失时字段省略。
- USER_UPLOAD source 不伪造 camera 字段。
- 内部 protocol/nodeServerId/streamId 不进入 Web 响应。
- 资产 page 在 0/1/24 条下查询次数为常数，total 与 source 过滤一致。
- Collection 分别按 deviceId、channelId、二者组合过滤；匹配记录位于原全量第 2 页时，过滤后的第 1 页仍能返回且 total 正确。
- task/config 缺失的异常数据不使 total 与 items 长期不一致；应有告警并按明确 inner join 语义处理。
- MySQL、PostgreSQL、SQLite 的 join、count、排序和分页结果一致。

### 7.4 Task 与控制

- Task/Collection list 和 detail 的 `version` 等于数据库任务行 version，不等于 scheduleVersion。
- pause/resume/cancel/reschedule 缺 expectedVersion 被确定性拒绝；旧版本返回 409 并不改变状态。
- PAUSED + PAUSE capability 可 resume；PAUSED + RESCHEDULE 可调整计划。
- 缺 capability 即使状态和权限正确也拒绝。
- IMAGE_COLLECTION 的 PARTIAL_COMPLETED 不显示也不接受 manual retry；FAILED + failed execution 可重试。
- `lastExecutionId`、resultRefType/resultRefId/resultSummary 正确映射；非终态 last execution 才进入 activeExecution。
- 执行分页按 plannedAt DESC 稳定加载多页，刷新不会固定截断为 100 条。

### 7.5 幂等与竞争

上传：

- 同 owner/key、同文件/同名称返回首次 assetId。
- 同 owner/key、不同文件摘要、不同规范化名称或不同有效原文件名返回 600007/409。
- 两线程同 key 同内容只有一个 asset/source；loser 返回 winner；loser staged/final 对象已清理。
- 两线程同 key 不同内容只有一个 winner，另一方 409；无 null 响应和未记录 orphan。
- 数据库失败、promote 后失败、补偿删除失败分别产生正确响应和 orphan record。

任务：

- 同 owner/type/key、相同 canonical command 返回首次 taskId。
- taskName、device/channel、mode、时间、interval、payload 或 retry origin 任一变化返回 600007/409。
- 首次创建成功后，即使设备/通道随后离线或删除，同 key 同内容仍返回首次 taskId；新 key 仍执行当前机位校验。
- 并发同内容只创建一个 task 和一份首 execution/config。
- 并发不同内容有唯一 winner，另一方 409；不返回通用 500。

原有 `ImageIngestServiceTest.ingest_shouldReturnIdempotentAssetWithoutReadingOrStagingAgain` 和 `BizTaskCreateServiceTest.create_shouldReturnIdempotentTaskBeforePayloadWork` 必须改写，因为它们锁定了与目标契约冲突的早返回行为。

### 7.6 SSE

- created/deleting/deleted topic 与 eventType 映射准确。
- DELETE_FAILED 触发 deleted 刷新 topic，eventType 保留失败语义。
- 精确 topic 和允许的域前缀可以订阅；未知 topic、空 topic、越权 topic 被拒绝。
- Local 和 Redis remote delivery 对同一用户/权限/事件给出相同 allow/deny。
- 只有 Image:Collection:Query 的用户不会收到非 IMAGE_COLLECTION taskId。
- 只有 Image:Asset:Query 的用户不会收到 business.task 事件。
- 心跳、客户端断开和 emitter 清理不绕过授权，也不在 holder 中保留过期权限快照超过连接生命周期。
- JWT 无稳定 userId 时订阅失败；任何 emitterId、debug/warn 日志和 Redis 消息均不包含原始 token。

### 7.7 回归与工程验证

至少保留并扩展当前 10 个相关测试类：

- `ImageCollectionApplicationServiceTest`
- `ImageCollectionTaskHandlerContractTest`
- `ImageCollectionTaskHandlerExecutionTest`
- `ImageAssetControllerContentTest`
- `ImageAssetControllerQueryTest`
- `ImageCollectionControllerContractTest`
- `ImageControllerPermissionTest`
- `BusinessTaskControlControllerTest`
- `BusinessTaskControllerTest`
- `GlobalExceptionHandlerImageTest`

新增 thumbnail service/controller、关联 mapper、idempotency concurrency、SSE authorization 的独立测试。所有测试必须验证 HTTP 状态和业务码，不能只断言 message 文本。

---

## 8. 验收门禁

### Gate 0：绿色基线

- 当前相关测试继续全绿。
- 新增错误码不改变非图像历史业务码仍返回 HTTP 200 的兼容行为。
- MySQL、PostgreSQL、SQLite migration/mapper 启动检查通过。

### Gate 1：Thumbnail 与来源

- 两个 profile 的尺寸、字节、方向、不放大、ETag 和 304 测试通过。
- 不存在任何 thumbnail -> content 原图降级路径。
- sourceMetadata 契约测试通过，内部诊断字段不泄露。

### Gate 2：私有 Blob 与幂等

- thumbnail/content/download 权限、header、403/404/409/410/503 测试通过。
- 上传与任务创建的同内容重放、不同内容冲突和并发竞争测试通过。
- 故障注入后不存在未记录的 staged/final 对象。

### Gate 3：任务详情与控制

- version、scheduleVersion、capabilities、activeExecution、result 字段契约通过。
- 全状态/capability/双权限/version/executionId 参数化矩阵通过。
- Collection camera 分页 total 和常数查询次数通过。

### Gate 4：发布候选

- REST、SSE、Redis 广播、慢存储、队列满、超时、超大图 E2E 通过。
- OpenAPI 与实际响应一致。
- 前端 Gate 4 所需的慢网、SSE 高频和私有 Blob 验收没有后端阻断项。
- 保存测试报告、关键响应 header、查询次数/SQL 和资源上限监控结果作为发布证据。

任一 Gate 失败不得使用临时原图缩略图、跳过后端权限、允许 null version、扩大无界队列或把 5xx 改成 HTTP 200 业务成功来规避。

---

## 9. 非后端缺口与边界

以下内容不应误列为后端未实现：

- 前端 `AssetVO` 当前使用 `capturedTime/ingestedTime`，而后端稳定字段为 `capturedAt/ingestedAt`。UI 方案要求保持既有语义，本期由前端类型和映射对齐，不要求后端改名。
- `/image/collection` 与兼容 `/image/collections?taskId=...` 的路由 alias、Drawer 打开和浏览器前进/后退属于前端任务；后端只保证 taskId 详情与权限契约。
- Blob Object URL、最多 6 个浏览器并发、AbortController、引用计数和 revoke 属于前端；后端负责 thumbnail 工作队列和响应资源上限。
- SSE 合并刷新、maxWait、single-flight、页面可见性和 30 秒兜底轮询属于前端；后端负责事件正确性、授权和连接能力。
- `suppressGlobalError/requestWithErrorMeta` 属于前端 RequestClient；后端只需返回稳定 HTTP 状态与业务码。
- 375px 响应式、主题、国际化、可访问性和视觉截图属于前端验收。

本期不建设任意尺寸图片变换、公共 URL、token query 图片访问、CDN、跨会话持久 thumbnail 缓存、批量上传/删除、采集统计接口或通用页面引擎。

---

## 10. 审计证据索引

本文引用的关键实现文件均以 `voglander` 仓库根目录为基准：

| 领域 | 文件 |
| --- | --- |
| 资产 Web 端点 | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/image/ImageAssetController.java` |
| Collection Web 端点 | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/image/ImageCollectionController.java` |
| Task Web 端点 | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/task/BusinessTaskController.java` |
| 资产 VO/Assembler | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/image/vo/ImageAssetVO.java`、`voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/image/assembler/ImageAssetWebAssembler.java` |
| Collection VO/Assembler | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/image/vo/ImageCollectionVO.java`、`voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/image/assembler/ImageCollectionWebAssembler.java` |
| Task VO/Assembler | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/task/vo/BusinessTaskVO.java`、`voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/task/assembler/BusinessTaskWebAssembler.java` |
| 资产上传与校验 | `voglander-service/src/main/java/io/github/lunasaw/voglander/service/image/ImageIngestService.java`、`voglander-service/src/main/java/io/github/lunasaw/voglander/service/image/ImageValidationService.java` |
| Collection 应用与执行 | `voglander-service/src/main/java/io/github/lunasaw/voglander/service/image/ImageCollectionApplicationService.java`、`voglander-service/src/main/java/io/github/lunasaw/voglander/service/image/ImageCollectionTaskHandler.java` |
| Task 创建 | `voglander-service/src/main/java/io/github/lunasaw/voglander/service/task/BizTaskCreateService.java` |
| 资产 Manager | `voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/manager/ImageAssetManager.java` |
| Collection Manager | `voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/manager/ImageCollectionConfigManager.java` |
| Task/Execution Manager | `voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/manager/BizTaskManager.java`、`voglander-manager/src/main/java/io/github/lunasaw/voglander/manager/manager/BizTaskExecutionManager.java` |
| SSE Controller/EventBus | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/api/sse/controller/SseController.java`、`voglander-service/src/main/java/io/github/lunasaw/voglander/service/sse/LocalSseEventBus.java`、`voglander-service/src/main/java/io/github/lunasaw/voglander/service/sse/RedisBackedSseEventBus.java` |
| SSE 事件契约 | `voglander-common/src/main/java/io/github/lunasaw/voglander/common/event/BusinessTaskSseEvent.java` |
| 图像常量与错误码 | `voglander-common/src/main/java/io/github/lunasaw/voglander/common/constant/image/ImageConstant.java`、`voglander-common/src/main/java/io/github/lunasaw/voglander/common/exception/ServiceExceptionEnum.java` |
| HTTP 错误映射 | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/exception/GlobalExceptionHandler.java` |
| 登录拦截边界 | `voglander-web/src/main/java/io/github/lunasaw/voglander/web/config/ResourcesConfig.java`、`voglander-web/src/main/java/io/github/lunasaw/voglander/web/interceptor/JwtAuthInterceptor.java` |
| 图像表结构 | `sql/migration/{mysql,postgresql,sqlite}/1.0.9-image-asset-collection.sql` |
| Task 表结构 | `sql/migration/{mysql,postgresql,sqlite}/1.0.9-durable-business-task-engine.sql` |
| 冲突的旧幂等测试 | `voglander-web/src/test/java/io/github/lunasaw/voglander/service/image/ImageIngestServiceTest.java`、`voglander-web/src/test/java/io/github/lunasaw/voglander/service/task/BizTaskCreateServiceTest.java` |

前端 `version` 使用证据位于 `voglander-vben-frontend/apps/web-antd/src/views/image/collection/list.vue:233-258`；前端类型已声明但后端尚未返回的字段位于 `voglander-vben-frontend/apps/web-antd/src/api/image/types.ts:86-112`。

---

## 11. 最终就绪清单

全部勾选后，才能把后端标记为“满足 IMAGE-ASSET-COLLECTION-UI-TECH-PLAN 1.0.9”：

- [ ] Business Task 读接口按 Task:Query / Image:Collection:Query 做资源范围授权。
- [ ] IMAGE_COLLECTION 所有控制执行双权限、状态、capability 和 version 校验。
- [ ] SSE 注册和逐事件投递均授权，Local/Redis 一致。
- [ ] table/gallery thumbnail 端点、资源上限、ETag、304 和无原图降级完成。
- [ ] asset source 嵌套 VO 与 device/channel/name metadata 完成。
- [ ] Task/Collection version、capabilities、last execution、result 和 active execution 完成。
- [ ] Collection camera 过滤在分页前生效，asset/source 与 task/config 无 N+1。
- [ ] 上传和任务创建支持同内容重放、不同内容 409、并发 winner/loser 闭环。
- [ ] 600007、710009、710010、710013 与真实 HTTP 状态完成。
- [ ] thumbnail/content/download 权限和 403/404/409/410/503 语义完成。
- [ ] `image.asset.deleting` 和 SSE 授权测试完成。
- [ ] SSE emitter 标识和日志不包含原始 token。
- [ ] Gate 0-Gate 4 的测试和发布证据全部完成。

当前结论保持为：后端部分就绪，存在 P0 阻断项，不能直接按前端方案上线。
