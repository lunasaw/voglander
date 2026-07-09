## ADDED Requirements

### Requirement: 系统必须支持 IPTV 频道分页查询
系统 SHALL 提供 REST API 支持分页查询 IPTV 频道列表，返回频道基本信息。

#### Scenario: 查询第一页频道
- **WHEN** 前端调用 `/iptv/channel/page?page=1&size=20`
- **THEN** 系统返回前 20 条频道数据，包含 total（总数）和 items（频道列表）

#### Scenario: 查询指定页码
- **WHEN** 前端调用 `/iptv/channel/page?page=3&size=50`
- **THEN** 系统返回第 101-150 条频道数据

#### Scenario: 默认按创建时间降序排列
- **WHEN** 查询请求未指定排序字段
- **THEN** 系统按 `create_time` 降序返回频道列表

### Requirement: 系统必须支持按分类过滤频道
系统 SHALL 支持按频道分类（category）过滤查询结果。

#### Scenario: 按单个分类过滤
- **WHEN** 前端调用 `/iptv/channel/page?category=Sports&page=1&size=20`
- **THEN** 系统返回分类为 Sports 的频道列表

#### Scenario: 分类不存在返回空列表
- **WHEN** 前端查询不存在的分类（如 `category=InvalidCategory`）
- **THEN** 系统返回空列表，total 为 0

### Requirement: 系统必须支持按国家过滤频道
系统 SHALL 支持按国家代码（country）过滤查询结果。

#### Scenario: 按单个国家过滤
- **WHEN** 前端调用 `/iptv/channel/page?country=CN&page=1&size=20`
- **THEN** 系统返回国家代码为 CN 的频道列表

#### Scenario: 按多个国家过滤
- **WHEN** 前端调用 `/iptv/channel/page?country=CN,US,JP&page=1&size=20`
- **THEN** 系统返回国家代码为 CN、US 或 JP 的频道列表

### Requirement: 系统必须支持按语言过滤频道
系统 SHALL 支持按语言代码（language）过滤查询结果。

#### Scenario: 按单个语言过滤
- **WHEN** 前端调用 `/iptv/channel/page?language=zh&page=1&size=20`
- **THEN** 系统返回语言代码为 zh 的频道列表

#### Scenario: 按多个语言过滤
- **WHEN** 前端调用 `/iptv/channel/page?language=zh,en&page=1&size=20`
- **THEN** 系统返回语言代码为 zh 或 en 的频道列表

### Requirement: 系统必须支持按频道名称模糊搜索
系统 SHALL 支持按频道名称进行模糊搜索。

#### Scenario: 按名称关键词搜索
- **WHEN** 前端调用 `/iptv/channel/page?name=CCTV&page=1&size=20`
- **THEN** 系统返回名称包含 "CCTV" 的频道列表

#### Scenario: 搜索关键词为空返回全部
- **WHEN** 前端调用 `/iptv/channel/page?name=&page=1&size=20`
- **THEN** 系统返回全部频道（不过滤）

### Requirement: 系统必须支持组合条件查询
系统 SHALL 支持同时使用多个过滤条件进行组合查询。

#### Scenario: 组合分类和国家过滤
- **WHEN** 前端调用 `/iptv/channel/page?category=Sports&country=CN&page=1&size=20`
- **THEN** 系统返回分类为 Sports 且国家为 CN 的频道列表

#### Scenario: 组合所有过滤条件
- **WHEN** 前端调用 `/iptv/channel/page?category=News&country=US&language=en&name=CNN&page=1&size=20`
- **THEN** 系统返回同时满足分类、国家、语言、名称条件的频道列表

### Requirement: 系统必须支持查询单个频道详情
系统 SHALL 提供 REST API 根据频道 ID 查询单个频道的详细信息。

#### Scenario: 根据 ID 查询频道详情
- **WHEN** 前端调用 `/iptv/channel/get?id=12345`
- **THEN** 系统返回 ID 为 12345 的频道完整信息（包含 logo URL、分类、国家、语言等）

#### Scenario: 频道 ID 不存在
- **WHEN** 前端查询不存在的频道 ID
- **THEN** 系统返回错误响应，提示频道不存在

### Requirement: 系统必须支持查询所有分类列表
系统 SHALL 提供 REST API 查询所有 IPTV 频道分类。

#### Scenario: 查询全部分类
- **WHEN** 前端调用 `/iptv/category/list`
- **THEN** 系统返回所有分类列表，包含分类 ID 和名称

#### Scenario: 分类按名称排序
- **WHEN** 查询分类列表
- **THEN** 系统按分类名称字母顺序排列返回

### Requirement: 系统必须支持查询所有语言列表
系统 SHALL 提供 REST API 查询所有语言选项。

#### Scenario: 查询全部语言
- **WHEN** 前端调用 `/iptv/language/list`
- **THEN** 系统返回所有语言列表，包含语言代码和名称

#### Scenario: 语言按代码排序
- **WHEN** 查询语言列表
- **THEN** 系统按语言代码字母顺序排列返回

### Requirement: 系统必须支持查询所有国家列表
系统 SHALL 提供 REST API 查询所有国家选项。

#### Scenario: 查询全部国家
- **WHEN** 前端调用 `/iptv/country/list`
- **THEN** 系统返回所有国家列表，包含国家代码和名称

#### Scenario: 国家按代码排序
- **WHEN** 查询国家列表
- **THEN** 系统按国家代码字母顺序排列返回

### Requirement: 系统必须支持查询数据统计信息
系统 SHALL 提供 REST API 查询 IPTV 数据的统计信息。

#### Scenario: 查询总体统计
- **WHEN** 前端调用 `/iptv/statistics`
- **THEN** 系统返回频道总数、分类总数、语言总数、国家总数

#### Scenario: 查询按分类统计频道数量
- **WHEN** 前端调用 `/iptv/statistics/category`
- **THEN** 系统返回每个分类下的频道数量

#### Scenario: 查询按国家统计频道数量
- **WHEN** 前端调用 `/iptv/statistics/country`
- **THEN** 系统返回每个国家的频道数量

### Requirement: 系统必须返回统一的响应格式
系统 SHALL 所有查询接口返回统一的 `AjaxResult` 包装格式。

#### Scenario: 查询成功返回标准格式
- **WHEN** 任意查询接口执行成功
- **THEN** 系统返回 `{ "code": 0, "data": {...}, "msg": "success" }`

#### Scenario: 查询失败返回错误格式
- **WHEN** 查询接口执行失败（如参数校验失败）
- **THEN** 系统返回 `{ "code": 500, "data": null, "msg": "错误描述" }`

### Requirement: 系统必须支持参数校验
系统 SHALL 对所有查询接口的入参进行校验。

#### Scenario: 分页参数校验
- **WHEN** 前端传入 `page=0` 或 `size=0`
- **THEN** 系统返回参数错误响应

#### Scenario: 分页大小上限校验
- **WHEN** 前端传入 `size=10000`（超过限制）
- **THEN** 系统返回错误响应，提示单页最大 1000 条
