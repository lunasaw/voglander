## ADDED Requirements

### Requirement: 系统必须支持定时同步 IPTV 数据
系统 SHALL 提供定时任务，按配置的 cron 表达式周期性从 IPTV API 拉取数据并同步到本地数据库。

#### Scenario: 按配置周期执行同步
- **WHEN** 到达配置的 cron 时间点（如每天凌晨 2 点）
- **THEN** 系统自动触发 IPTV 数据同步任务

#### Scenario: 禁用定时同步
- **WHEN** 配置 `iptv.api.enable=false`
- **THEN** 系统不执行定时同步任务

#### Scenario: 同步任务正在执行时不重复触发
- **WHEN** 上一次同步任务尚未完成，新的定时触发到达
- **THEN** 系统跳过本次触发并记录警告日志

### Requirement: 系统必须支持全量同步模式
系统 SHALL 支持全量同步模式，清空本地 IPTV 数据并重新拉取全部数据。

#### Scenario: 全量同步清空旧数据
- **WHEN** 执行全量同步（`iptv.api.sync-mode=full`）
- **THEN** 系统先删除 `tb_iptv_channel`、`tb_iptv_category`、`tb_iptv_language`、`tb_iptv_country` 表中的所有数据

#### Scenario: 全量同步插入新数据
- **WHEN** 清空旧数据后，从 IPTV API 获取到新数据
- **THEN** 系统批量插入新数据到对应表中

#### Scenario: 全量同步记录同步日志
- **WHEN** 全量同步完成
- **THEN** 系统在 `tb_iptv_sync_log` 中记录同步类型（FULL）、开始时间、结束时间、同步数量、状态

### Requirement: 系统必须支持增量同步模式
系统 SHALL 支持增量同步模式，仅更新变化的数据，避免重复插入。

#### Scenario: 增量同步识别新增数据
- **WHEN** 执行增量同步（`iptv.api.sync-mode=incremental`），IPTV API 返回本地不存在的频道
- **THEN** 系统插入新频道数据到 `tb_iptv_channel`

#### Scenario: 增量同步识别更新数据
- **WHEN** 执行增量同步，IPTV API 返回的频道信息与本地不一致（如 logo URL 变更）
- **THEN** 系统更新本地频道数据

#### Scenario: 增量同步跳过已存在数据
- **WHEN** 执行增量同步，IPTV API 返回的数据与本地完全一致
- **THEN** 系统跳过该数据，不执行数据库操作

#### Scenario: 增量同步记录同步日志
- **WHEN** 增量同步完成
- **THEN** 系统在 `tb_iptv_sync_log` 中记录同步类型（INCREMENTAL）、新增数量、更新数量、跳过数量

### Requirement: 系统必须支持同步失败重试机制
系统 SHALL 在 API 调用失败时，按配置的重试策略重新尝试同步。

#### Scenario: API 调用失败后重试
- **WHEN** IPTV API 调用返回 5xx 错误
- **THEN** 系统等待 5 秒后重试，最多重试 3 次

#### Scenario: 重试全部失败后记录错误日志
- **WHEN** 重试 3 次后仍然失败
- **THEN** 系统记录 ERROR 日志并在同步日志中标记状态为 FAILED

#### Scenario: 部分数据同步失败继续处理
- **WHEN** 同步频道数据失败，但分类、语言、国家数据可正常获取
- **THEN** 系统继续同步其他数据，并记录部分失败的详细信息

### Requirement: 系统必须记录详细的同步日志
系统 SHALL 在每次同步任务执行时，记录完整的同步日志到 `tb_iptv_sync_log` 表。

#### Scenario: 记录同步开始时间
- **WHEN** 同步任务开始执行
- **THEN** 系统插入一条 `tb_iptv_sync_log` 记录，状态为 RUNNING，记录开始时间

#### Scenario: 记录同步成功详情
- **WHEN** 同步任务成功完成
- **THEN** 系统更新同步日志，记录结束时间、同步类型、各类数据的同步数量、状态为 SUCCESS

#### Scenario: 记录同步失败详情
- **WHEN** 同步任务失败
- **THEN** 系统更新同步日志，记录结束时间、错误信息、状态为 FAILED

#### Scenario: 记录每个 API 端点的同步结果
- **WHEN** 同步任务涉及多个 API 端点（channels、categories、languages、countries）
- **THEN** 系统在同步日志的详情字段中记录每个端点的同步数量和耗时

### Requirement: 系统必须支持手动触发同步
系统 SHALL 提供管理接口，允许管理员手动触发全量或增量同步。

#### Scenario: 手动触发全量同步
- **WHEN** 管理员调用 `/iptv/sync/trigger?mode=full`
- **THEN** 系统立即执行全量同步任务，不等待定时触发

#### Scenario: 手动触发增量同步
- **WHEN** 管理员调用 `/iptv/sync/trigger?mode=incremental`
- **THEN** 系统立即执行增量同步任务

#### Scenario: 同步任务正在执行时拒绝手动触发
- **WHEN** 管理员尝试手动触发同步，但上一次同步任务尚未完成
- **THEN** 系统返回错误响应，提示同步任务正在执行中

### Requirement: 系统必须支持批量数据库操作优化
系统 SHALL 使用批量插入/更新操作，提升同步性能。

#### Scenario: 批量插入频道数据
- **WHEN** 从 IPTV API 获取到 1000 条频道数据
- **THEN** 系统使用 MyBatis Plus 的 `saveBatch()` 方法批量插入，每批 500 条

#### Scenario: 批量更新频道数据
- **WHEN** 增量同步识别出 200 条需要更新的频道
- **THEN** 系统使用 MyBatis Plus 的 `updateBatchById()` 方法批量更新，每批 500 条

### Requirement: 系统必须支持数据一致性校验
系统 SHALL 在同步完成后，校验本地数据与 IPTV API 数据的一致性。

#### Scenario: 校验数据总量一致性
- **WHEN** 全量同步完成后
- **THEN** 系统比对本地频道总数与 API 返回的总数，若不一致则记录警告日志

#### Scenario: 校验关键字段完整性
- **WHEN** 同步数据插入数据库后
- **THEN** 系统检查频道 ID、名称等必填字段是否为空，发现空值则记录错误日志并跳过该条数据
