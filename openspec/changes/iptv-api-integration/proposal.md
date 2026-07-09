## Why

IPTV API (https://github.com/iptv-org/api) 提供了全球电视频道、分类、语言、国家等结构化数据。当前系统缺乏对第三方 IPTV 数据源的集成能力，导致频道数据需要手动维护。通过接入 IPTV API 并实现定时同步机制，可以自动化频道数据管理，扩展系统对公开 IPTV 资源的支持能力。

## What Changes

- 新增 IPTV API 客户端模块，支持调用 channels、categories、languages、countries 等端点
- 实现定时任务，支持按配置周期从 IPTV API 拉取数据并同步到本地数据库
- 新增 IPTV 频道、分类、语言、国家等实体表及对应的 Mapper/Service/Manager 层
- 提供完整的 REST API 用于查询 IPTV 数据（频道列表、分类筛选、国家/语言过滤等）
- 新增管理后台界面，展示 IPTV 数据同步状态、频道列表和查询功能
- 支持增量同步与全量同步两种模式，避免重复数据

## Capabilities

### New Capabilities

- `iptv-api-client`: IPTV API 客户端封装，支持频道、分类、语言、国家数据的 HTTP 调用与响应解析
- `iptv-data-sync`: IPTV 数据定时同步任务，支持增量/全量同步、错误重试、同步日志记录
- `iptv-data-query`: IPTV 数据查询服务，提供多维度过滤（分类、国家、语言、状态）与分页查询
- `iptv-management-ui`: IPTV 管理界面，展示同步状态、频道列表、数据统计

### Modified Capabilities

<!-- 无现有能力的需求变更 -->

## Impact

- **新增模块**：`voglander-integration` 下新增 `iptv` 包，包含 API 客户端、DTO、Wrapper
- **新增数据表**：`tb_iptv_channel`（频道）、`tb_iptv_category`（分类）、`tb_iptv_language`（语言）、`tb_iptv_country`（国家）、`tb_iptv_sync_log`（同步日志）
- **新增配置项**：`application-inte.yml` 增加 `iptv.api` 配置段（base-url、enable、sync-cron、sync-mode）
- **新增依赖**：无需新增外部依赖，使用现有 RestTemplate + FastJSON2
- **前端新增模块**：`apps/web-antd/src/views/iptv/` 下新增频道管理、同步日志页面
- **定时任务**：新增 `IptvSyncJob`，依赖 Spring `@Scheduled` 或 XXL-Job（待确认）
- **技术文档**：输出到 `doc/1.10.0/IPTV-API-INTEGRATION-TECH-PLAN.md`
