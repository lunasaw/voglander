## ADDED Requirements

### Requirement: IPTV API 客户端必须支持频道数据查询
系统 SHALL 提供 HTTP 客户端调用 IPTV API 的 channels 端点，获取全球电视频道列表数据。

#### Scenario: 成功获取频道列表
- **WHEN** 调用 `IptvApiClient.getChannels()` 方法
- **THEN** 系统返回包含频道 ID、名称、logo、国家、语言、分类等字段的频道列表

#### Scenario: API 返回错误状态码
- **WHEN** IPTV API 返回 4xx 或 5xx 状态码
- **THEN** 系统抛出 `IptvApiException` 并记录错误日志

#### Scenario: API 响应超时
- **WHEN** API 调用超过配置的超时时间（默认 30 秒）
- **THEN** 系统抛出 `IptvApiTimeoutException` 并记录超时日志

### Requirement: IPTV API 客户端必须支持分类数据查询
系统 SHALL 提供 HTTP 客户端调用 IPTV API 的 categories 端点，获取频道分类数据。

#### Scenario: 成功获取分类列表
- **WHEN** 调用 `IptvApiClient.getCategories()` 方法
- **THEN** 系统返回包含分类 ID、名称等字段的分类列表

#### Scenario: 分类数据为空
- **WHEN** IPTV API 返回空数组
- **THEN** 系统返回空列表并记录信息日志

### Requirement: IPTV API 客户端必须支持语言数据查询
系统 SHALL 提供 HTTP 客户端调用 IPTV API 的 languages 端点，获取语言数据。

#### Scenario: 成功获取语言列表
- **WHEN** 调用 `IptvApiClient.getLanguages()` 方法
- **THEN** 系统返回包含语言代码、名称等字段的语言列表

#### Scenario: API 返回无效 JSON
- **WHEN** IPTV API 返回非 JSON 格式响应
- **THEN** 系统抛出 `IptvApiParseException` 并记录原始响应内容

### Requirement: IPTV API 客户端必须支持国家数据查询
系统 SHALL 提供 HTTP 客户端调用 IPTV API 的 countries 端点，获取国家数据。

#### Scenario: 成功获取国家列表
- **WHEN** 调用 `IptvApiClient.getCountries()` 方法
- **THEN** 系统返回包含国家代码、名称等字段的国家列表

#### Scenario: 网络连接失败
- **WHEN** 无法连接到 IPTV API 服务器
- **THEN** 系统抛出 `IptvApiConnectionException` 并记录网络错误详情

### Requirement: IPTV API 客户端必须支持可配置的基础 URL
系统 SHALL 允许通过配置文件设置 IPTV API 的 base URL，支持切换不同的 API 环境。

#### Scenario: 使用默认 API 地址
- **WHEN** 配置文件未指定 `iptv.api.base-url`
- **THEN** 系统使用默认地址 `https://iptv-org.github.io/api`

#### Scenario: 使用自定义 API 地址
- **WHEN** 配置文件指定 `iptv.api.base-url=https://custom.iptv.api`
- **THEN** 系统使用自定义地址发起所有 API 请求

### Requirement: IPTV API 客户端必须使用 FastJSON2 解析响应
系统 SHALL 使用项目统一的 FastJSON2 库解析 IPTV API 的 JSON 响应，禁止使用 Jackson 或 Gson。

#### Scenario: 解析频道响应为 DTO
- **WHEN** 接收到 IPTV API 的频道 JSON 响应
- **THEN** 系统使用 `JSON.parseArray()` 将响应解析为 `IptvChannelDTO` 列表

#### Scenario: 字段映射失败
- **WHEN** API 响应缺少必填字段（如 channel id）
- **THEN** 系统抛出 `IptvApiParseException` 并记录缺失字段名称

### Requirement: IPTV API 客户端必须记录操作日志
系统 SHALL 使用 `@Slf4j` 记录所有 API 调用的开始、成功、失败日志。

#### Scenario: 记录成功调用日志
- **WHEN** API 调用成功
- **THEN** 系统记录 INFO 级别日志，包含端点名称、响应数据量、耗时

#### Scenario: 记录失败调用日志
- **WHEN** API 调用失败
- **THEN** 系统记录 ERROR 级别日志，包含端点名称、错误码、错误消息、堆栈信息
