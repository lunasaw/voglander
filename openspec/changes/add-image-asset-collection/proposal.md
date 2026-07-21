## Why

Voglander 已具备设备目录、直播建流和 ZLMediaKit 截图基础，但缺少可长期管理的图像资产、受控存储、采集任务和可视化管理闭环，ZLM 临时截图也无法作为稳定业务资产。现在补齐这层图像底座，并接入 `add-durable-business-task-engine` 提供的统一业务长任务能力，可以支持用户上传、摄像机单次/定时采集和来源追踪，同时为后续独立 SkyEye 平台提供可靠、来源无关的图像查询基础。

## What Changes

- 新增统一图像资产与来源模型，使用稳定 `assetId` 管理元数据、内容读取、下载、删除和来源追踪
- 新增安全的图像接入与存储抽象，第一阶段支持本地文件存储、JPEG/PNG/WebP 校验、SHA-256、临时写入和失败补偿
- 新增摄像机单次采集和定时采集领域能力，注册 `IMAGE_COLLECTION` Handler，复用现有设备/通道目录、直播会话与 ZLM 截图能力
- 复用统一业务长任务的任务、执行、事件、调度、进度、重试、租约、取消和 `MISSED` 语义；图像域不再建设第二套状态推进内核
- 新增图像资产、采集创建/领域查询 REST API，并复用统一任务与执行查询/控制 API，支持幂等、权限、分页查询、受控内容响应和稳定错误码
- 新增“图像资产”和“图像采集”两个 Vue Vben Admin 页面，提供图库/列表、上传、详情、机位选择、任务控制、执行追踪和资产跳转
- 同步 SQLite、MySQL、PostgreSQL 表结构、索引、菜单、按钮权限与管理员角色授权
- 新增安全、审计、SSE 增量通知、指标、容量与孤儿对象治理设计，以及后端、前端和真实 ZLM 链路测试
- SkyEye、人脸检测/向量检索、历史录像抽帧、批量机位采集、自动保留策略和第一阶段对象存储实现不在本次范围内

## Capabilities

### New Capabilities

- `image-asset-lifecycle`: 统一图像资产、来源追踪、分页查询、授权预览/下载、幂等删除和删除失败恢复
- `image-ingest-storage`: 用户上传、摄像机截图接入、图像安全校验、存储提供方抽象以及文件与数据库一致性补偿
- `camera-image-collection`: 基于设备/通道的单次与定时采集领域配置、`IMAGE_COLLECTION` Handler、媒体会话复用、截图接入和通用任务结果关联
- `image-management-ui`: 图像资产与图像采集页面、权限化交互、图库/列表、任务控制、执行详情、来源链路和 SSE 刷新

### Modified Capabilities

无。当前 OpenSpec 基线没有需要修改的既有能力规格；设备目录、直播与 SSE 仅被复用，不改变原有需求语义。

## Impact

- **后端模块**：`voglander-common`、`voglander-client`、`voglander-repository`、`voglander-manager`、`voglander-service`、`voglander-integration`、`voglander-web`
- **前端仓库**：`vue-vben-admin/apps/web-antd` 新增 API、路由、页面、组件、i18n 和单元测试，并更新 OpenAPI 快照
- **数据库**：新增 `tb_image_asset`、`tb_image_asset_source`、`tb_image_collection_config`，任务、执行和事件复用 `tb_biz_task*` 三表；同步三种数据库脚本与现有开发库菜单数据
- **外部集成**：复用 zlm starter 的截图接口；不修改 SIP/GB28181/ONVIF 协议行为，不要求修改上游仓库
- **运行环境**：新增 `voglander.image.*` 配置、本地持久卷要求和多节点共享存储约束；任务扫描、线程池和租约使用 `voglander.task.*`
- **前置变更**：必须先完成并启用 `add-durable-business-task-engine`；本变更不复制其表、状态机、API 或前端任务中心
- **兼容性**：图像能力本身为新增；旧导出任务的破坏性删除归属于前置任务引擎变更
