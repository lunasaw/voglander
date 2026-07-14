## Why

Voglander 目前只有与导出字段强耦合的 `tb_export_task` 三态记录，而图像采集、批量导入和后续 AI 分析都需要持久化排队、调度、进度、重试、取消、多节点领取和重启恢复。继续由各业务自建状态推进会造成重复状态机、不可一致恢复和前端体验分裂，因此需要先建立独立的业务长任务内核。

## What Changes

- 新增通用业务任务、执行和事件三事实模型，统一稳定业务 ID、任务类型、调度、总体状态、执行租约、重试、进度、结果引用和不可变审计事件
- 新增 `LongTaskHandler` SPI 与注册表，按 `taskType` 隔离领域参数校验、实际执行、错误分类、结果摘要和补偿逻辑；通用内核不依赖图像、导出或 AI 类型
- 新增持久化 Scheduler、Dispatcher、Worker 和 Lease Recovery，支持立即执行、指定时刻执行和固定间隔计划，多节点下通过条件更新与唯一约束保证事实幂等
- 新增统一任务控制与查询 API，支持创建、分页、详情、执行历史、暂停、恢复、取消和人工重试；进度、失败码和时间语义保持跨任务类型一致
- 新增 Vue Vben Admin“任务中心”，提供统一筛选、状态/进度展示、任务控制、执行时间线、失败诊断和业务详情跳转
- 新增统一 SSE、指标、审计、权限、容量限制和数据保留设计；SSE 只作为刷新提示，数据库始终是任务事实源
- **BREAKING**：直接删除 `tb_export_task`，移除其 DO/Mapper/Service/Manager/Assembler/枚举、全部 `/api/v1/exportTask/*` 接口、OpenAPI 契约和相关测试，不提供双写、旧接口转发或历史数据迁移
- 同步 SQLite、MySQL、PostgreSQL 全量脚本和增量迁移；执行破坏性删除前必须备份并验证旧表为空，非空环境由发布负责人显式确认
- 图像采集通过独立变更注册 `IMAGE_COLLECTION` Handler 并复用本内核；系统心跳、会话 GC、SIP 保活、订阅刷新等技术定时器不纳入业务任务

## Capabilities

### New Capabilities

- `durable-business-task`: 通用业务长任务生命周期、调度、执行事实、Handler SPI、进度、重试、租约、多节点防重和崩溃恢复
- `business-task-management`: 统一任务/执行 REST API、权限、审计、SSE、指标和 Vue Vben Admin 任务中心，以及旧导出任务能力的破坏性退役

### Modified Capabilities

无。当前 OpenSpec 基线没有既有业务任务规格；图像采集对本内核的接入由 `add-image-asset-collection` 变更同步修订。

## Impact

- **后端模块**：`voglander-common`、`voglander-client`、`voglander-repository`、`voglander-manager`、`voglander-service`、`voglander-web`
- **前端仓库**：`vue-vben-admin/apps/web-antd` 新增任务 API、任务中心、路由、权限、i18n 和测试，并移除旧导出任务契约文档
- **数据库**：新增 `tb_biz_task`、`tb_biz_task_execution`、`tb_biz_task_event`；从三种全量脚本与已部署数据库中删除 `tb_export_task`
- **API**：新增 `/api/v1/business-tasks/*` 与 `/api/v1/business-task-executions/*`；删除 `/api/v1/exportTask/*`
- **兼容性**：旧导出任务表和 API 是明确的破坏性删除；当前开发库旧表为空，发布仍需执行环境级空表门禁
- **运行环境**：新增任务扫描、分发、租约、重试、事件保留和有界线程池配置；业务任务默认启用，但无 Handler 或无任务时不产生执行负载
