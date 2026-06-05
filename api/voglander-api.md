---
title: 默认模块
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# 默认模块

Base URLs:

# Authentication

# 首页控制器

## GET 首页重定向到logo展示页面

GET /

> 返回示例

> 200 Response

```
"string"
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|string|

## GET 首页直接访问

GET /home

> 返回示例

> 200 Response

```
"string"
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|string|

# 枚举控制器 - 提供各种枚举值的查询接口

## GET 获取设备种类枚举

GET /api/v1/enum/device-sub-types

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取设备协议枚举

GET /api/v1/enum/device-protocols

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取设备协议类型枚举

GET /api/v1/enum/device-agreement-types

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 根据设备种类和协议计算协议类型

GET /api/v1/enum/device-agreement-type

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|subType|query|integer| 否 |设备种类|
|protocol|query|integer| 否 |设备协议|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取所有枚举数据

GET /api/v1/enum/all

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

# 健康检查控制器（Phase 8-G 完整化）

## GET 简单健康检查（向后兼容）

GET /api/v1/check

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 依赖级健康检查

GET /api/v1/health

返回结构：{status: UP/DEGRADED/DOWN, components:{db, redis-A, redis-B, zlm}}

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultMapObject](#schemaajaxresultmapobject)|

# 导出任务管理

## GET 根据ID获取导出任务

GET /api/v1/exportTask/get/{id}

根据ID获取导出任务
通过导出任务ID获取导出任务详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |导出任务ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 根据条件查询导出任务

GET /api/v1/exportTask/get

根据条件查询导出任务
通过导出任务实体条件查询导出任务信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |ID自增|
|gmtCreate|query|string| 否 |创建时间|
|gmtUpdate|query|string| 否 |更新时间|
|bizId|query|integer(int64)| 否 |任务唯一Id|
|memberCnt|query|integer(int64)| 否 |导出的客户总数|
|format|query|string| 否 |文件格式|
|applyTime|query|string| 否 |申请时间|
|exportTime|query|string| 否 |导出报表时间|
|url|query|string| 否 |文件下载地址, 多个url用、隔开|
|status|query|integer| 否 |{@link ExportTaskStatusEnum}|
|expired|query|integer| 否 |是否过期，1 -> 过期，0 -> 未过期|
|deleted|query|integer| 否 |是否删除，1 -> 删除, 0 -> 未删除|
|param|query|string| 否 |搜索条件序列化|
|name|query|string| 否 |导出名称|
|type|query|integer| 否 |{@link ExportTaskTypeEnums}|
|applyUser|query|string| 否 |none|
|extend|query|string| 否 |none|

#### 详细说明

**status**: {@link ExportTaskStatusEnum}
是否完成，1 -> 完成, 0->处理中, -1 -> 出错

**type**: {@link ExportTaskTypeEnums}
导出类型

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 根据业务ID获取导出任务

GET /api/v1/exportTask/getBizId/{bizId}

根据业务ID获取导出任务
通过业务ID获取导出任务信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|bizId|path|integer| 是 |业务ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取导出任务列表

GET /api/v1/exportTask/list

获取导出任务列表
根据条件获取导出任务列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |ID自增|
|gmtCreate|query|string| 否 |创建时间|
|gmtUpdate|query|string| 否 |更新时间|
|bizId|query|integer(int64)| 否 |任务唯一Id|
|memberCnt|query|integer(int64)| 否 |导出的客户总数|
|format|query|string| 否 |文件格式|
|applyTime|query|string| 否 |申请时间|
|exportTime|query|string| 否 |导出报表时间|
|url|query|string| 否 |文件下载地址, 多个url用、隔开|
|status|query|integer| 否 |{@link ExportTaskStatusEnum}|
|expired|query|integer| 否 |是否过期，1 -> 过期，0 -> 未过期|
|deleted|query|integer| 否 |是否删除，1 -> 删除, 0 -> 未删除|
|param|query|string| 否 |搜索条件序列化|
|name|query|string| 否 |导出名称|
|type|query|integer| 否 |{@link ExportTaskTypeEnums}|
|applyUser|query|string| 否 |none|
|extend|query|string| 否 |none|

#### 详细说明

**status**: {@link ExportTaskStatusEnum}
是否完成，1 -> 完成, 0->处理中, -1 -> 出错

**type**: {@link ExportTaskTypeEnums}
导出类型

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 分页查询导出任务

GET /api/v1/exportTask/pageListByEntity/{page}/{size}

分页查询导出任务
根据条件分页查询导出任务列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|path|integer| 是 |页码|
|size|path|integer| 是 |每页大小|
|id|query|integer(int64)| 否 |ID自增|
|gmtCreate|query|string| 否 |创建时间|
|gmtUpdate|query|string| 否 |更新时间|
|bizId|query|integer(int64)| 否 |任务唯一Id|
|memberCnt|query|integer(int64)| 否 |导出的客户总数|
|format|query|string| 否 |文件格式|
|applyTime|query|string| 否 |申请时间|
|exportTime|query|string| 否 |导出报表时间|
|url|query|string| 否 |文件下载地址, 多个url用、隔开|
|status|query|integer| 否 |{@link ExportTaskStatusEnum}|
|expired|query|integer| 否 |是否过期，1 -> 过期，0 -> 未过期|
|deleted|query|integer| 否 |是否删除，1 -> 删除, 0 -> 未删除|
|param|query|string| 否 |搜索条件序列化|
|name|query|string| 否 |导出名称|
|type|query|integer| 否 |{@link ExportTaskTypeEnums}|
|applyUser|query|string| 否 |none|
|extend|query|string| 否 |none|

#### 详细说明

**status**: {@link ExportTaskStatusEnum}
是否完成，1 -> 完成, 0->处理中, -1 -> 出错

**type**: {@link ExportTaskTypeEnums}
导出类型

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 简单分页查询

GET /api/v1/exportTask/pageList/{page}/{size}

简单分页查询
分页查询所有导出任务

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|path|integer| 是 |页码|
|size|path|integer| 是 |每页大小|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## POST 创建导出任务

POST /api/v1/exportTask/insert

创建导出任务
添加新的导出任务

> Body 请求参数

```json
{
  "bizId": 0,
  "memberCnt": 0,
  "format": "string",
  "applyTime": "string",
  "param": "string",
  "name": "string",
  "type": 0,
  "applyUser": "string",
  "extend": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[ExportTaskCreateReq](#schemaexporttaskcreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## POST 批量创建导出任务

POST /api/v1/exportTask/insertBatch

批量创建导出任务
批量添加导出任务

> Body 请求参数

```json
[
  {
    "bizId": 0,
    "memberCnt": 0,
    "format": "string",
    "applyTime": "string",
    "param": "string",
    "name": "string",
    "type": 0,
    "applyUser": "string",
    "extend": "string"
  }
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[ExportTaskCreateReq](#schemaexporttaskcreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 更新导出任务

PUT /api/v1/exportTask/update

更新导出任务
更新导出任务信息

> Body 请求参数

```json
{
  "id": 0,
  "bizId": 0,
  "memberCnt": 0,
  "format": "string",
  "applyTime": "string",
  "exportTime": "string",
  "url": "string",
  "status": 0,
  "expired": 0,
  "param": "string",
  "name": "string",
  "type": 0,
  "applyUser": "string",
  "extend": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[ExportTaskUpdateReq](#schemaexporttaskupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 批量更新导出任务

PUT /api/v1/exportTask/updateBatch

批量更新导出任务
批量更新导出任务信息

> Body 请求参数

```json
[
  {
    "id": 0,
    "bizId": 0,
    "memberCnt": 0,
    "format": "string",
    "applyTime": "string",
    "exportTime": "string",
    "url": "string",
    "status": 0,
    "expired": 0,
    "param": "string",
    "name": "string",
    "type": 0,
    "applyUser": "string",
    "extend": "string"
  }
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[ExportTaskUpdateReq](#schemaexporttaskupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 更新导出任务状态

PUT /api/v1/exportTask/updateStatus/{bizId}/{status}

更新导出任务状态
根据业务ID更新导出任务状态

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|bizId|path|integer| 是 |业务ID|
|status|path|integer| 是 |任务状态|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 标记任务完成

PUT /api/v1/exportTask/markCompleted/{bizId}

标记任务完成
将导出任务标记为已完成状态

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|bizId|path|integer| 是 |业务ID|
|url|query|string| 是 |导出文件URL|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 标记任务失败

PUT /api/v1/exportTask/markError/{bizId}

标记任务失败
将导出任务标记为失败状态

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|bizId|path|integer| 是 |业务ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 删除导出任务

DELETE /api/v1/exportTask/delete/{id}

删除导出任务
根据ID删除导出任务

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |导出任务ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 根据业务ID删除导出任务

DELETE /api/v1/exportTask/deleteBizId/{bizId}

根据业务ID删除导出任务

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|bizId|path|integer| 是 |业务ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 批量删除导出任务

DELETE /api/v1/exportTask/deleteIds

批量删除导出任务
根据ID列表批量删除导出任务

> Body 请求参数

```json
[
  0
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|array[integer]| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 统计导出任务总数

GET /api/v1/exportTask/count

统计导出任务总数
获取导出任务总数量

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 按条件统计导出任务

GET /api/v1/exportTask/countByEntity

按条件统计导出任务
根据条件统计导出任务数量

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |ID自增|
|gmtCreate|query|string| 否 |创建时间|
|gmtUpdate|query|string| 否 |更新时间|
|bizId|query|integer(int64)| 否 |任务唯一Id|
|memberCnt|query|integer(int64)| 否 |导出的客户总数|
|format|query|string| 否 |文件格式|
|applyTime|query|string| 否 |申请时间|
|exportTime|query|string| 否 |导出报表时间|
|url|query|string| 否 |文件下载地址, 多个url用、隔开|
|status|query|integer| 否 |{@link ExportTaskStatusEnum}|
|expired|query|integer| 否 |是否过期，1 -> 过期，0 -> 未过期|
|deleted|query|integer| 否 |是否删除，1 -> 删除, 0 -> 未删除|
|param|query|string| 否 |搜索条件序列化|
|name|query|string| 否 |导出名称|
|type|query|integer| 否 |{@link ExportTaskTypeEnums}|
|applyUser|query|string| 否 |none|
|extend|query|string| 否 |none|

#### 详细说明

**status**: {@link ExportTaskStatusEnum}
是否完成，1 -> 完成, 0->处理中, -1 -> 出错

**type**: {@link ExportTaskTypeEnums}
导出类型

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

# 用户管理

## GET 获取用户信息

GET /user/info

获取用户信息
获取用户信息
获取当前登录用户的详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |访问令牌|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultUserInfoVO](#schemaajaxresultuserinfovo)|

## GET 分页查询用户列表

GET /user/list

分页查询用户列表
分页查询用户列表
根据查询条件分页获取用户列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|username|query|string| 否 |用户名|
|nickname|query|string| 否 |昵称|
|email|query|string| 否 |邮箱|
|phone|query|string| 否 |手机号|
|status|query|integer| 否 |状态 1启用 0禁用|
|pageNum|query|integer| 否 |页码|
|pageSize|query|integer| 否 |每页数量|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultUserListResp](#schemaajaxresultuserlistresp)|

## GET 获取用户详情

GET /user/{id}

根据ID获取用户详情
获取用户详情
根据用户ID获取用户详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |用户ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultUserVO](#schemaajaxresultuservo)|

## PUT 更新用户

PUT /user/{id}

更新用户
更新用户
根据用户ID更新用户信息

> Body 请求参数

```json
{
  "id": 0,
  "password": "string",
  "nickname": "string",
  "email": "user@example.com",
  "phone": "string",
  "avatar": "string",
  "status": 0,
  "roleIds": [
    0
  ]
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |用户ID|
|body|body|[UserUpdateReq](#schemauserupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## DELETE 删除用户

DELETE /user/{id}

删除用户
删除用户
根据用户ID删除用户

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |用户ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST 创建用户

POST /user

创建用户
创建用户
创建新的用户

> Body 请求参数

```json
{
  "username": "string",
  "password": "string",
  "nickname": "string",
  "email": "user@example.com",
  "phone": "string",
  "avatar": "string",
  "status": 0,
  "roleIds": [
    0
  ]
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[UserCreateReq](#schemausercreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## GET 检查用户名

GET /user/check-username/{username}

检查用户名是否存在
检查用户名
检查用户名是否已存在

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|username|path|string| 是 |用户名|
|excludeId|query|integer| 否 |排除的用户ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## GET 检查邮箱

GET /user/check-email/{email}

检查邮箱是否存在
检查邮箱
检查邮箱是否已存在

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|email|path|string| 是 |邮箱|
|excludeId|query|integer| 否 |排除的用户ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## GET 检查手机号

GET /user/check-phone/{phone}

检查手机号是否存在
检查手机号
检查手机号是否已存在

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|phone|path|string| 是 |手机号|
|excludeId|query|integer| 否 |排除的用户ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

# 角色管理

## GET 获取角色列表

GET /system/role/list

获取角色列表
获取角色列表
分页查询角色列表数据

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|name|query|string| 否 |角色名称|
|status|query|integer| 否 |状态 1启用 0禁用|
|pageNum|query|integer| 否 |页码|
|pageSize|query|integer| 否 |每页数量|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultRoleListResp](#schemaajaxresultrolelistresp)|

## GET 获取角色详情

GET /system/role/{id}

根据ID获取角色详情
获取角色详情
根据角色ID获取角色详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |角色ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultRoleVO](#schemaajaxresultrolevo)|

## PUT 更新角色

PUT /system/role/{id}

更新角色
更新角色
更新指定角色的信息

> Body 请求参数

```json
{
  "name": "string",
  "remark": "string",
  "status": 0,
  "permissions": [
    0
  ]
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |角色ID|
|body|body|[RoleUpdateReq](#schemaroleupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## DELETE 删除角色

DELETE /system/role/{id}

删除角色
删除角色
删除指定的系统角色

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |角色ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## POST 创建角色

POST /system/role

创建角色
创建角色
创建新的系统角色

> Body 请求参数

```json
{
  "name": "string",
  "remark": "string",
  "status": 0,
  "permissions": [
    0
  ]
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RoleCreateReq](#schemarolecreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

# 内部命令接收端点：接收其他节点转发的命令并本地下发。

## POST handleCommand

POST /internal/sip/command

> Body 请求参数

```json
{
  "key": {}
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapObject](#schemamapobject)| 否 |none|

> 返回示例

> 200 Response

```json
{}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

# 设备管理

## GET 根据ID获取设备

GET /api/v1/device/get/{id}

根据ID获取设备
通过设备ID获取设备详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |设备ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 根据条件查询设备

GET /api/v1/device/get

根据条件查询设备
通过设备实体条件查询设备信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |none|
|createTime|query|string| 否 |none|
|updateTime|query|string| 否 |none|
|deviceId|query|string| 否 |设备ID|
|status|query|integer| 否 |状态 1在线 0离线|
|name|query|string| 否 |自定义名称|
|ip|query|string| 否 |IP|
|port|query|integer| 否 |端口|
|registerTime|query|string| 否 |注册时间|
|keepaliveTime|query|string| 否 |心跳时间|
|serverIp|query|string| 否 |注册节点|
|type|query|integer| 否 |协议类型{@link DeviceAgreementEnum}|
|extend|query|string| 否 |扩展字段|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取设备列表

GET /api/v1/device/list

获取设备列表
根据条件获取设备列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |none|
|createTime|query|string| 否 |none|
|updateTime|query|string| 否 |none|
|deviceId|query|string| 否 |设备ID|
|status|query|integer| 否 |状态 1在线 0离线|
|name|query|string| 否 |自定义名称|
|ip|query|string| 否 |IP|
|port|query|integer| 否 |端口|
|registerTime|query|string| 否 |注册时间|
|keepaliveTime|query|string| 否 |心跳时间|
|serverIp|query|string| 否 |注册节点|
|type|query|integer| 否 |协议类型{@link DeviceAgreementEnum}|
|extend|query|string| 否 |扩展字段|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 分页查询设备

GET /api/v1/device/pageListByEntity/{page}/{size}

分页查询设备
根据条件分页查询设备列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|path|integer| 是 |页码|
|size|path|integer| 是 |每页大小|
|id|query|integer(int64)| 否 |none|
|createTime|query|string| 否 |none|
|updateTime|query|string| 否 |none|
|deviceId|query|string| 否 |设备ID|
|status|query|integer| 否 |状态 1在线 0离线|
|name|query|string| 否 |自定义名称|
|ip|query|string| 否 |IP|
|port|query|integer| 否 |端口|
|registerTime|query|string| 否 |注册时间|
|keepaliveTime|query|string| 否 |心跳时间|
|serverIp|query|string| 否 |注册节点|
|type|query|integer| 否 |协议类型{@link DeviceAgreementEnum}|
|extend|query|string| 否 |扩展字段|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 简单分页查询

GET /api/v1/device/pageList/{page}/{size}

简单分页查询
分页查询所有设备

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|path|integer| 是 |页码|
|size|path|integer| 是 |每页大小|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## POST 创建设备

POST /api/v1/device/insert

创建设备
添加新的设备

> Body 请求参数

```json
{
  "deviceId": "string",
  "name": "string",
  "ip": "string",
  "port": 0,
  "type": 0,
  "subType": 0,
  "protocol": 0,
  "serverIp": "string",
  "extendInfo": {
    "serialNumber": "string",
    "transport": "string",
    "expires": 0,
    "password": "string",
    "streamMode": "string",
    "charset": "string",
    "deviceInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceCreateReq](#schemadevicecreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## POST 批量创建设备

POST /api/v1/device/insertBatch

批量创建设备
批量添加设备

> Body 请求参数

```json
[
  {
    "deviceId": "string",
    "name": "string",
    "ip": "string",
    "port": 0,
    "type": 0,
    "subType": 0,
    "protocol": 0,
    "serverIp": "string",
    "extendInfo": {
      "serialNumber": "string",
      "transport": "string",
      "expires": 0,
      "password": "string",
      "streamMode": "string",
      "charset": "string",
      "deviceInfo": "string"
    }
  }
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceCreateReq](#schemadevicecreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 更新设备

PUT /api/v1/device/update

更新设备
更新设备信息

> Body 请求参数

```json
{
  "id": 0,
  "deviceId": "string",
  "name": "string",
  "ip": "string",
  "port": 0,
  "type": 0,
  "subType": 0,
  "protocol": 0,
  "serverIp": "string",
  "status": 0,
  "extendInfo": {
    "serialNumber": "string",
    "transport": "string",
    "expires": 0,
    "password": "string",
    "streamMode": "string",
    "charset": "string",
    "deviceInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceUpdateReq](#schemadeviceupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 批量更新设备

PUT /api/v1/device/updateBatch

批量更新设备
批量更新设备信息

> Body 请求参数

```json
[
  {
    "id": 0,
    "deviceId": "string",
    "name": "string",
    "ip": "string",
    "port": 0,
    "type": 0,
    "subType": 0,
    "protocol": 0,
    "serverIp": "string",
    "status": 0,
    "extendInfo": {
      "serialNumber": "string",
      "transport": "string",
      "expires": 0,
      "password": "string",
      "streamMode": "string",
      "charset": "string",
      "deviceInfo": "string"
    }
  }
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceUpdateReq](#schemadeviceupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 删除设备

DELETE /api/v1/device/delete/{id}

删除设备
根据ID删除设备

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |设备ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 批量删除设备

DELETE /api/v1/device/deleteIds

批量删除设备
根据ID列表批量删除设备

> Body 请求参数

```json
[
  0
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|array[integer]| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 统计设备总数

GET /api/v1/device/count

统计设备总数
获取设备总数量

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 按条件统计设备

GET /api/v1/device/countByEntity

按条件统计设备
根据条件统计设备数量

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |none|
|createTime|query|string| 否 |none|
|updateTime|query|string| 否 |none|
|deviceId|query|string| 否 |设备ID|
|status|query|integer| 否 |状态 1在线 0离线|
|name|query|string| 否 |自定义名称|
|ip|query|string| 否 |IP|
|port|query|integer| 否 |端口|
|registerTime|query|string| 否 |注册时间|
|keepaliveTime|query|string| 否 |心跳时间|
|serverIp|query|string| 否 |注册节点|
|type|query|integer| 否 |协议类型{@link DeviceAgreementEnum}|
|extend|query|string| 否 |扩展字段|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

# 系统部门管理

## GET 获取部门列表

GET /system/dept/list

获取部门列表数据
复杂业务场景：需要查询和树形构建，调用Manager层
获取部门列表
获取所有部门数据，返回树形结构

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListDeptResp](#schemaajaxresultlistdeptresp)|

## POST 创建部门

POST /system/dept

创建部门
复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
创建部门
创建新的部门

> Body 请求参数

```json
{
  "name": "string",
  "remark": "string",
  "status": 0,
  "parentId": "string",
  "sortOrder": 0,
  "leader": "string",
  "phone": "string",
  "email": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeptReq](#schemadeptreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 更新部门

PUT /system/dept/{id}

更新部门
复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
更新部门
更新指定ID的部门

> Body 请求参数

```json
{
  "name": "string",
  "remark": "string",
  "status": 0,
  "parentId": "string",
  "sortOrder": 0,
  "leader": "string",
  "phone": "string",
  "email": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |部门ID|
|body|body|[DeptReq](#schemadeptreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 删除部门

DELETE /system/dept/{id}

删除部门
复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
删除部门
删除指定ID的部门

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |部门ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取部门详情

GET /system/dept/{id}

获取部门详情
简单业务场景：获取单个实体，但需要DTO转换，调用Manager层
获取部门详情
根据ID获取部门详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |部门ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

# 认证管理

## POST 用户登录

POST /auth/login

用户登录
用户登录
用户通过用户名和密码进行登录认证

> Body 请求参数

```json
{
  "username": "string",
  "password": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[LoginReq](#schemaloginreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLoginResp](#schemaajaxresultloginresp)|

## POST 用户登出

POST /auth/logout

用户登出
用户登出
用户登出系统

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |访问令牌|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## POST 刷新访问令牌

POST /auth/refresh

刷新token
刷新访问令牌
使用当前令牌刷新获取新的访问令牌

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |访问令牌|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取用户权限码

GET /auth/codes

获取用户权限码
获取用户权限码
获取当前登录用户的所有权限码

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |访问令牌|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

# 设备通道管理

## POST 新增数据

POST /api/v1/deviceChannel/add

新增数据
标准数据创建，校验参数并插入数据库

> Body 请求参数

```json
{
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "extendInfo": {
    "channelInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelCreateReq](#schemadevicechannelcreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## PUT 更新数据

PUT /api/v1/deviceChannel/update

更新数据
通过主键ID更新指定字段，要求必须携带ID

> Body 请求参数

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0,
  "extendInfo": {
    "channelInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelUpdateReq](#schemadevicechannelupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## POST 灵活单条查询

POST /api/v1/deviceChannel/get

灵活单条查询
支持多种条件查询

> Body 请求参数

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelQueryReq](#schemadevicechannelqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultDeviceChannelVO](#schemaajaxresultdevicechannelvo)|

## DELETE 单条记录删除

DELETE /api/v1/deviceChannel/deleteOne

单条记录删除
支持多种删除策略

> Body 请求参数

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0,
  "extendInfo": {
    "channelInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelUpdateReq](#schemadevicechannelupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## DELETE 批量记录删除

DELETE /api/v1/deviceChannel/deleteBatch

批量记录删除
支持多种条件的批量删除

> Body 请求参数

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelQueryReq](#schemadevicechannelqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## POST 分页条件查询

POST /api/v1/deviceChannel/getPage

分页条件查询
全量分页条件搜索

> Body 请求参数

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|query|integer| 是 |页码|
|size|query|integer| 是 |页大小|
|body|body|[DeviceChannelQueryReq](#schemadevicechannelqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultDeviceChannelListResp](#schemaajaxresultdevicechannellistresp)|

## DELETE 业务删除

DELETE /api/v1/deviceChannel/deleteDeviceChannel

业务删除
业务删除，包含操作日志记录

> Body 请求参数

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0,
  "extendInfo": {
    "channelInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelUpdateReq](#schemadevicechannelupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST 业务创建

POST /api/v1/deviceChannel/createDeviceChannel

业务创建
业务创建，包含完整的业务逻辑

> Body 请求参数

```json
{
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "extendInfo": {
    "channelInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelCreateReq](#schemadevicechannelcreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## PUT 业务更新

PUT /api/v1/deviceChannel/updateDeviceChannel

业务更新
业务更新，包含完整的业务逻辑

> Body 请求参数

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0,
  "extendInfo": {
    "channelInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelUpdateReq](#schemadevicechannelupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## GET 根据ID获取设备通道

GET /api/v1/deviceChannel/get/{id}

根据ID获取设备通道
通过设备通道ID获取设备通道详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |设备通道ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultDeviceChannelVO](#schemaajaxresultdevicechannelvo)|

## POST 创建设备通道

POST /api/v1/deviceChannel/insert

创建设备通道
添加新的设备通道

> Body 请求参数

```json
{
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "extendInfo": {
    "channelInfo": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelCreateReq](#schemadevicechannelcreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## POST 批量创建设备通道

POST /api/v1/deviceChannel/insertBatch

批量创建设备通道
批量添加设备通道

> Body 请求参数

```json
[
  {
    "channelId": "string",
    "deviceId": "string",
    "name": "string",
    "extendInfo": {
      "channelInfo": "string"
    }
  }
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[DeviceChannelCreateReq](#schemadevicechannelcreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultString](#schemaajaxresultstring)|

## GET 简单分页查询

GET /api/v1/deviceChannel/pageList/{page}/{size}

简单分页查询
分页查询所有设备通道

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|path|integer| 是 |页码|
|size|path|integer| 是 |每页大小|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultDeviceChannelListResp](#schemaajaxresultdevicechannellistresp)|

# 流媒体节点管理

## GET 根据ID获取节点

GET /api/v1/mediaNode/get/{id}

根据ID获取节点
通过数据库主键ID获取流媒体节点详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |节点数据库ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultMediaNodeVO](#schemaajaxresultmedianodevo)|

## GET 根据节点ID获取节点

GET /api/v1/mediaNode/getByServerId/{serverId}

根据节点ID获取节点
通过节点服务ID获取流媒体节点信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|serverId|path|string| 是 |节点服务ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultMediaNodeVO](#schemaajaxresultmedianodevo)|

## GET 根据条件查询节点

GET /api/v1/mediaNode/get

根据条件查询节点
通过节点实体条件查询流媒体节点信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |none|
|createTime|query|string| 否 |创建时间|
|updateTime|query|string| 否 |修改时间|
|serverId|query|string| 否 |节点ID|
|name|query|string| 否 |节点名称|
|host|query|string| 否 |节点地址|
|secret|query|string| 否 |API密钥|
|enabled|query|boolean| 否 |是否启用 1启用 0禁用|
|hookEnabled|query|boolean| 否 |是否启用Hook 1启用 0禁用|
|weight|query|integer| 否 |节点权重|
|keepalive|query|integer(int64)| 否 |心跳时间戳|
|status|query|integer| 否 |节点状态 1在线 0离线|
|description|query|string| 否 |节点描述|
|extend|query|string| 否 |扩展字段|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultMediaNodeVO](#schemaajaxresultmedianodevo)|

## GET 获取节点列表

GET /api/v1/mediaNode/list

获取节点列表
根据条件获取流媒体节点列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|query|integer(int64)| 否 |none|
|createTime|query|string| 否 |创建时间|
|updateTime|query|string| 否 |修改时间|
|serverId|query|string| 否 |节点ID|
|name|query|string| 否 |节点名称|
|host|query|string| 否 |节点地址|
|secret|query|string| 否 |API密钥|
|enabled|query|boolean| 否 |是否启用 1启用 0禁用|
|hookEnabled|query|boolean| 否 |是否启用Hook 1启用 0禁用|
|weight|query|integer| 否 |节点权重|
|keepalive|query|integer(int64)| 否 |心跳时间戳|
|status|query|integer| 否 |节点状态 1在线 0离线|
|description|query|string| 否 |节点描述|
|extend|query|string| 否 |扩展字段|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListMediaNodeVO](#schemaajaxresultlistmedianodevo)|

## GET 获取启用的节点列表

GET /api/v1/mediaNode/listEnabled

获取启用的节点列表
获取所有启用状态的流媒体节点列表

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListMediaNodeVO](#schemaajaxresultlistmedianodevo)|

## GET 获取在线的节点列表

GET /api/v1/mediaNode/listOnline

获取在线的节点列表
获取所有在线状态的流媒体节点列表

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListMediaNodeVO](#schemaajaxresultlistmedianodevo)|

## GET 分页查询节点

GET /api/v1/mediaNode/pageListByEntity/{page}/{size}

分页查询节点
根据条件分页查询流媒体节点列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|path|integer| 是 |页码|
|size|path|integer| 是 |每页大小|
|id|query|integer(int64)| 否 |none|
|createTime|query|string| 否 |创建时间|
|updateTime|query|string| 否 |修改时间|
|serverId|query|string| 否 |节点ID|
|name|query|string| 否 |节点名称|
|host|query|string| 否 |节点地址|
|secret|query|string| 否 |API密钥|
|enabled|query|boolean| 否 |是否启用 1启用 0禁用|
|hookEnabled|query|boolean| 否 |是否启用Hook 1启用 0禁用|
|weight|query|integer| 否 |节点权重|
|keepalive|query|integer(int64)| 否 |心跳时间戳|
|status|query|integer| 否 |节点状态 1在线 0离线|
|description|query|string| 否 |节点描述|
|extend|query|string| 否 |扩展字段|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultMediaNodeListResp](#schemaajaxresultmedianodelistresp)|

## GET 简单分页查询

GET /api/v1/mediaNode/pageList/{page}/{size}

简单分页查询
分页查询所有流媒体节点

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|path|integer| 是 |页码|
|size|path|integer| 是 |每页大小|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultMediaNodeListResp](#schemaajaxresultmedianodelistresp)|

## POST 创建节点

POST /api/v1/mediaNode/insert

创建节点
添加新的流媒体节点

> Body 请求参数

```json
{
  "serverId": "string",
  "name": "string",
  "host": "string",
  "secret": "string",
  "enabled": true,
  "hookEnabled": true,
  "weight": 1,
  "description": "string",
  "extend": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaNodeCreateReq](#schemamedianodecreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## POST 批量创建节点

POST /api/v1/mediaNode/insertBatch

批量创建节点
批量添加流媒体节点

> Body 请求参数

```json
[
  {
    "serverId": "string",
    "name": "string",
    "host": "string",
    "secret": "string",
    "enabled": true,
    "hookEnabled": true,
    "weight": 1,
    "description": "string",
    "extend": "string"
  }
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaNodeCreateReq](#schemamedianodecreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 更新节点

PUT /api/v1/mediaNode/update

更新节点
更新流媒体节点信息

> Body 请求参数

```json
{
  "id": 0,
  "serverId": "string",
  "name": "string",
  "host": "string",
  "secret": "string",
  "enabled": true,
  "hookEnabled": true,
  "weight": 1,
  "description": "string",
  "extend": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaNodeUpdateReq](#schemamedianodeupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 批量更新节点

PUT /api/v1/mediaNode/updateBatch

批量更新节点
批量更新流媒体节点信息

> Body 请求参数

```json
[
  {
    "id": 0,
    "serverId": "string",
    "name": "string",
    "host": "string",
    "secret": "string",
    "enabled": true,
    "hookEnabled": true,
    "weight": 1,
    "description": "string",
    "extend": "string"
  }
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaNodeUpdateReq](#schemamedianodeupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 更新节点状态

PUT /api/v1/mediaNode/updateStatus/{serverId}

更新节点状态
更新流媒体节点在线状态

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|serverId|path|string| 是 |节点服务ID|
|status|query|integer| 是 |节点状态 1在线 0离线|
|keepalive|query|integer| 否 |心跳时间戳|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 删除节点

DELETE /api/v1/mediaNode/delete/{id}

删除节点
根据数据库ID删除流媒体节点

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |节点数据库ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 根据节点ID删除

DELETE /api/v1/mediaNode/deleteByServerId/{serverId}

根据节点ID删除
根据节点服务ID删除流媒体节点

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|serverId|path|string| 是 |节点服务ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 批量删除节点

DELETE /api/v1/mediaNode/deleteIds

批量删除节点
根据数据库ID列表批量删除流媒体节点

> Body 请求参数

```json
[
  0
]
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|array[integer]| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 根据条件删除节点

DELETE /api/v1/mediaNode/deleteByCondition

根据条件删除节点
根据指定条件删除流媒体节点

> Body 请求参数

```json
{
  "id": 0,
  "createTime": "string",
  "updateTime": "string",
  "serverId": "string",
  "name": "string",
  "host": "string",
  "secret": "string",
  "enabled": true,
  "hookEnabled": true,
  "weight": 0,
  "keepalive": 0,
  "status": 0,
  "description": "string",
  "extend": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaNodeDO](#schemamedianodedo)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

# 系统菜单管理

## GET 获取用户菜单

GET /system/menu/all

获取用户所有菜单
复杂业务场景：需要多表查询和树形构建，调用Manager层
获取用户菜单
获取当前登录用户的所有可访问菜单，返回树形结构

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |访问令牌|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListMenuResp](#schemaajaxresultlistmenuresp)|

## GET 获取用户权限菜单

GET /system/menu/permissions

获取用户权限菜单（前端路由格式）
复杂业务场景：需要多表查询和树形构建，调用Manager层
获取用户权限菜单
获取当前登录用户的权限菜单，返回前端路由格式

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|Authorization|header|string| 否 |访问令牌|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListMenuVO](#schemaajaxresultlistmenuvo)|

## GET 获取菜单列表

GET /system/menu/list

获取菜单数据列表
复杂业务场景：需要查询和树形构建，调用Manager层
获取菜单列表
获取所有菜单数据，返回树形结构

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListMenuResp](#schemaajaxresultlistmenuresp)|

## GET 检查菜单名称

GET /system/menu/name-exists

检查菜单名称是否存在
复杂业务场景：需要业务逻辑验证，调用Manager层
检查菜单名称
检查菜单名称是否已存在

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|name|query|string| 是 |菜单名称|
|id|query|string| 否 |排除的菜单ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 检查菜单路径

GET /system/menu/path-exists

检查菜单路径是否存在
复杂业务场景：需要业务逻辑验证，调用Manager层
检查菜单路径
检查菜单路径是否已存在

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|path|query|string| 是 |菜单路径|
|id|query|string| 否 |排除的菜单ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## POST 创建菜单

POST /system/menu

创建菜单
复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
创建菜单
创建新的菜单

> Body 请求参数

```json
{
  "authCode": "string",
  "component": "string",
  "name": "string",
  "path": "string",
  "pid": "string",
  "redirect": "string",
  "type": "string",
  "meta": {
    "activeIcon": "string",
    "activePath": "string",
    "affixTab": true,
    "affixTabOrder": 0,
    "badge": "string",
    "badgeType": "string",
    "badgeVariants": "string",
    "hideChildrenInMenu": true,
    "hideInBreadcrumb": true,
    "hideInMenu": true,
    "hideInTab": true,
    "icon": "string",
    "iframeSrc": "string",
    "keepAlive": true,
    "link": "string",
    "maxNumOfOpenTab": 0,
    "noBasicLayout": true,
    "openInNewWindow": true,
    "order": 0,
    "title": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MenuReq](#schemamenureq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## PUT 更新菜单

PUT /system/menu/{id}

更新菜单
复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
更新菜单
更新指定ID的菜单

> Body 请求参数

```json
{
  "authCode": "string",
  "component": "string",
  "name": "string",
  "path": "string",
  "pid": "string",
  "redirect": "string",
  "type": "string",
  "meta": {
    "activeIcon": "string",
    "activePath": "string",
    "affixTab": true,
    "affixTabOrder": 0,
    "badge": "string",
    "badgeType": "string",
    "badgeVariants": "string",
    "hideChildrenInMenu": true,
    "hideInBreadcrumb": true,
    "hideInMenu": true,
    "hideInTab": true,
    "icon": "string",
    "iframeSrc": "string",
    "keepAlive": true,
    "link": "string",
    "maxNumOfOpenTab": 0,
    "noBasicLayout": true,
    "openInNewWindow": true,
    "order": 0,
    "title": "string"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |菜单ID|
|body|body|[MenuReq](#schemamenureq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## DELETE 删除菜单

DELETE /system/menu/{id}

删除菜单
复杂业务场景：需要业务逻辑验证和事务处理，调用Manager层
删除菜单
删除指定ID的菜单

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |菜单ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

## GET 获取菜单详情

GET /system/menu/{id}

根据ID获取菜单详情
简单业务场景：单表查询，直接调用Service层
获取菜单详情
根据ID获取菜单详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|string| 是 |菜单ID|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult](#schemaajaxresult)|

# 拉流代理管理

## GET 根据ID获取代理

GET /api/v1/proxy/get/{id}

根据ID获取代理
通过数据库主键ID获取拉流代理详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |代理数据库ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultStreamProxyVO](#schemaajaxresultstreamproxyvo)|

## POST 新增拉流代理

POST /api/v1/proxy/add

新增拉流代理
标准数据创建，校验参数并插入数据库

> Body 请求参数

```json
{
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyCreateReq](#schemastreamproxycreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## PUT 更新拉流代理

PUT /api/v1/proxy/update

更新拉流代理
智能更新，优先使用ID，否则使用业务键

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "extend": "string",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyUpdateReq](#schemastreamproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## POST 灵活单条查询

POST /api/v1/proxy/get

灵活单条查询
支持ID、app+stream、proxyKey等多种条件查询

> Body 请求参数

```json
{
  "id": 0,
  "app": "live",
  "stream": "test",
  "proxyKey": "string",
  "url": "string",
  "description": "string",
  "status": 0,
  "onlineStatus": 0,
  "serverId": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyQueryReq](#schemastreamproxyqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultStreamProxyVO](#schemaajaxresultstreamproxyvo)|

## DELETE 单条记录删除

DELETE /api/v1/proxy/deleteOne

单条记录删除
支持ID、proxyKey、app+stream优先级删除策略

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "extend": "string",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyUpdateReq](#schemastreamproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## DELETE 批量删除

DELETE /api/v1/proxy/deleteBatch

批量删除
支持多种条件组合的批量删除

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "extend": "string",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyUpdateReq](#schemastreamproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## POST 分页条件查询

POST /api/v1/proxy/getPage

分页条件查询
全量分页条件搜索，支持复杂条件查询

> Body 请求参数

```json
{
  "id": 0,
  "app": "live",
  "stream": "test",
  "proxyKey": "string",
  "url": "string",
  "description": "string",
  "status": 0,
  "onlineStatus": 0,
  "serverId": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|query|integer| 是 |页码，默认1|
|size|query|integer| 是 |页大小，默认10|
|body|body|[StreamProxyQueryReq](#schemastreamproxyqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultStreamProxyListResp](#schemaajaxresultstreamproxylistresp)|

## POST 业务创建代理

POST /api/v1/proxy/createStreamProxy

业务创建代理
业务创建，设置默认值并记录操作日志

> Body 请求参数

```json
{
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyCreateReq](#schemastreamproxycreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## PUT 业务更新代理

PUT /api/v1/proxy/updateStreamProxy

业务更新代理
业务更新，包含操作日志记录

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "extend": "string",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|operationDesc|query|string| 是 |操作描述|
|body|body|[StreamProxyUpdateReq](#schemastreamproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## DELETE 业务删除代理

DELETE /api/v1/proxy/deleteStreamProxy

业务删除代理
业务删除，包含操作日志记录

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "extend": "string",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyUpdateReq](#schemastreamproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## PUT 更新代理状态

PUT /api/v1/proxy/updateStatus/{id}

更新代理状态
根据ID启用/禁用代理，status：1启用 0禁用

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |代理数据库ID|
|status|query|integer| 是 |状态：1启用 0禁用|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

# 推流代理管理

## GET 根据ID获取推流代理

GET /api/v1/push-proxy/get/{id}

根据ID获取推流代理
通过数据库主键ID获取推流代理详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |推流代理数据库ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultPushProxyVO](#schemaajaxresultpushproxyvo)|

## POST 新增推流代理

POST /api/v1/push-proxy/add

新增推流代理
标准数据创建，校验参数并插入数据库

> Body 请求参数

```json
{
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyCreateReq](#schemapushproxycreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## PUT 更新推流代理

PUT /api/v1/push-proxy/update

更新推流代理
通过主键ID更新指定字段，要求必须携带ID

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyUpdateReq](#schemapushproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## POST 灵活单条查询

POST /api/v1/push-proxy/get

灵活单条查询
支持ID、app+stream、proxyKey等多种条件查询

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "onlineStatus": 1,
  "proxyKey": "push_proxy_key",
  "serverId": "zlm-node-1",
  "enabled": 1,
  "description": "测试推流代理"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyQueryReq](#schemapushproxyqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultPushProxyVO](#schemaajaxresultpushproxyvo)|

## DELETE 单条记录删除

DELETE /api/v1/push-proxy/deleteOne

单条记录删除
支持ID、proxyKey、app+stream优先级删除策略

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyUpdateReq](#schemapushproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## DELETE 批量删除

DELETE /api/v1/push-proxy/deleteBatch

批量删除
支持多种条件组合的批量删除

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyUpdateReq](#schemapushproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## POST 分页条件查询

POST /api/v1/push-proxy/getPage

分页条件查询
全量分页条件搜索，支持复杂条件查询

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "onlineStatus": 1,
  "proxyKey": "push_proxy_key",
  "serverId": "zlm-node-1",
  "enabled": 1,
  "description": "测试推流代理"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|page|query|integer| 是 |页码，默认1|
|size|query|integer| 是 |页大小，默认10|
|body|body|[PushProxyQueryReq](#schemapushproxyqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultPushProxyListResp](#schemaajaxresultpushproxylistresp)|

## POST 业务创建推流代理

POST /api/v1/push-proxy/createPushProxy

业务创建推流代理
业务创建，设置默认值并记录操作日志

> Body 请求参数

```json
{
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyCreateReq](#schemapushproxycreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## POST 业务创建推流代理（指定节点）

POST /api/v1/push-proxy/createPushProxyWithNode

业务创建推流代理（指定节点）
在指定节点创建推流代理并启动

> Body 请求参数

```json
{
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyCreateReq](#schemapushproxycreatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLong](#schemaajaxresultlong)|

## PUT 业务更新推流代理

PUT /api/v1/push-proxy/updatePushProxy

业务更新推流代理
业务更新，包含操作日志记录

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|operationDesc|query|string| 是 |操作描述|
|body|body|[PushProxyUpdateReq](#schemapushproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## DELETE 业务删除推流代理

DELETE /api/v1/push-proxy/deletePushProxy

业务删除推流代理
业务删除，包含操作日志记录

> Body 请求参数

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PushProxyUpdateReq](#schemapushproxyupdatereq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## PUT 更新推流代理状态

PUT /api/v1/push-proxy/updateStatus/{id}

更新推流代理状态
根据ID启用/禁用推流代理，status：1启用 0禁用

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |推流代理数据库ID|
|status|query|integer| 是 |状态：1启用 0禁用|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST 启动推流代理

POST /api/v1/push-proxy/start/{id}

启动推流代理
启动指定ID的推流代理

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |推流代理数据库ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST 停止推流代理

POST /api/v1/push-proxy/stop/{id}

停止推流代理
停止指定ID的推流代理

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |推流代理数据库ID|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## GET 检查源流是否在线

GET /api/v1/push-proxy/checkSource

检查源流是否在线
检查指定应用和流的源流是否在线

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|serverId|query|string| 是 |节点ID|
|app|query|string| 是 |应用名称|
|stream|query|string| 是 |流名称|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

# SSE 实时事件

## GET 订阅实时事件流

GET /api/v1/stream/events

订阅实时事件流

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|topics|query|string| 是 |none|
|token|query|string| 否 |none|

> 返回示例

> 200 Response

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[SseEmitter](#schemasseemitter)|

# 告警管理

## POST getPage

POST /api/v1/alarm/getPage

> Body 请求参数

```json
{
  "deviceId": "string",
  "alarmLevel": 0,
  "alarmType": 0,
  "startTime": "string",
  "endTime": "string",
  "page": 1,
  "size": 20
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[AlarmQueryReq](#schemaalarmqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultAlarmListResp](#schemaajaxresultalarmlistresp)|

## GET get

GET /api/v1/alarm/get/{id}

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|id|path|integer| 是 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultAlarmVO](#schemaajaxresultalarmvo)|

## POST ack

POST /api/v1/alarm/ack

> Body 请求参数

```json
{
  "key": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapLong](#schemamaplong)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

# 设备主动指令

## POST queryCatalog

POST /api/v1/device-cmd/query-catalog

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## POST queryInfo

POST /api/v1/device-cmd/query-info

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

## POST 重启设备（记录操作日志）

POST /api/v1/device-cmd/reboot

重启设备（记录操作日志）

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST record

POST /api/v1/device-cmd/record

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

# 录像回放

## POST start

POST /api/v1/playback/start

> Body 请求参数

```json
{
  "deviceId": "string",
  "channelId": "string",
  "startTime": "string",
  "endTime": "string",
  "streamMode": "UDP"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PlaybackStartReq](#schemaplaybackstartreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult?](#schemaajaxresult?)|

## POST stop

POST /api/v1/playback/stop

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": null
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResult?](#schemaajaxresult?)|

## POST control

POST /api/v1/playback/control

> Body 请求参数

```json
{
  "streamId": "string",
  "action": "string",
  "param": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PlaybackControlReq](#schemaplaybackcontrolreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST queryRecords

POST /api/v1/playback/records

> Body 请求参数

```json
{
  "deviceId": "string",
  "channelId": "string",
  "startTime": "string",
  "endTime": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RecordQueryReq](#schemarecordqueryreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultVoid](#schemaajaxresultvoid)|

# 直播管理

## POST 开始直播（首播/复用）

POST /api/v1/live/start

开始直播（首播/复用）

> Body 请求参数

```json
{
  "deviceId": "string",
  "channelId": "string",
  "protocol": "FLV",
  "streamMode": "UDP"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[LiveStartReq](#schemalivestartreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLivePlayVO](#schemaajaxresultliveplayvo)|

## POST 停止直播（引用计数）

POST /api/v1/live/stop

停止直播（引用计数）

> Body 请求参数

```json
{
  "streamId": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[LiveStopReq](#schemalivestopreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## GET 查询直播状态（轮询兜底）

GET /api/v1/live/{streamId}

查询直播状态（轮询兜底）

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|streamId|path|string| 是 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultLivePlayVO](#schemaajaxresultliveplayvo)|

## POST 心跳续约

POST /api/v1/live/keepalive

心跳续约

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## GET 活跃会话列表

GET /api/v1/live/list

活跃会话列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|deviceId|query|string| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultListLivePlayVO](#schemaajaxresultlistliveplayvo)|

# PTZ 控制

## POST control

POST /api/v1/ptz/control

> Body 请求参数

```json
{
  "deviceId": "string",
  "channelId": "string",
  "command": "string",
  "speed": 128
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PtzControlReq](#schemaptzcontrolreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST stop

POST /api/v1/ptz/stop

> Body 请求参数

```json
{
  "deviceId": "string",
  "channelId": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PtzStopReq](#schemaptzstopreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

## POST preset

POST /api/v1/ptz/preset

> Body 请求参数

```json
{
  "deviceId": "string",
  "channelId": "string",
  "action": "string",
  "presetId": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[PresetReq](#schemapresetreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "key": {}
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[AjaxResultBoolean](#schemaajaxresultboolean)|

# ZLM媒体服务器管理

## GET 获取版本信息

GET /zlm/api/version

获取版本信息
获取版本信息
获取ZLMediaKit服务器的版本信息

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "buildTime": "string",
    "branchName": "string",
    "commitHash": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseVersion](#schemaserverresponseversion)|

## GET 获取API列表

GET /zlm/api/api/list

获取API列表
获取API列表
获取ZLMediaKit服务器支持的所有API接口列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|key|query|any| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    "string"
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListString](#schemaserverresponseliststring)|

## GET 获取网络线程负载

GET /zlm/api/threads/load

获取网络线程负载
获取网络线程负载
获取ZLMediaKit服务器网络线程的负载情况

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "delay": "string",
      "load": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListThreadLoad](#schemaserverresponselistthreadload)|

## GET 获取统计信息

GET /zlm/api/statistic

获取主要对象个数
获取统计信息
获取ZLMediaKit服务器主要对象的统计数量

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "Buffer": 0,
    "RtpPacket": 0,
    "Frame": 0,
    "RtmpPacket": 0,
    "TcpSession": 0,
    "UdpServer": 0,
    "TcpServer": 0,
    "FrameImp": 0,
    "BufferList": 0,
    "BufferRaw": 0,
    "MediaSource": 0,
    "MultiMediaSourceMuxer": 0,
    "TcpClient": 0,
    "BufferLikeString": 0,
    "Socket": 0,
    "UdpSession": 0
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseImportantObjectNum](#schemaserverresponseimportantobjectnum)|

## GET 获取后台线程负载

GET /zlm/api/work-threads/load

获取后台线程负载
获取后台线程负载
获取ZLMediaKit服务器后台工作线程的负载情况

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "delay": "string",
      "load": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListThreadLoad](#schemaserverresponselistthreadload)|

## GET 获取服务器配置

GET /zlm/api/server/config

获取服务器配置
获取服务器配置
获取ZLMediaKit服务器的配置信息

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "rtp.rtpMaxSize": "string",
      "protocol.hls_demand": "string",
      "rtp_proxy.opus_pt": "string",
      "rtp_proxy.timeoutSec": "string",
      "rtmp.port": "string",
      "hook.on_ip_not_found": "string",
      "record.fileRepeat": "string",
      "general.flowThreshold": "string",
      "rtsp.rtpTransportType": "string",
      "hook.retry_delay": "string",
      "http.rootPath": "string",
      "rtsp.keepAliveSecond": "string",
      "hook.on_server_started": "string",
      "api.defaultSnap": "string",
      "cluster.origin_url": "string",
      "http.port": "string",
      "http.virtualPath": "string",
      "http.keepAliveSecond": "string",
      "ffmpeg.log": "string",
      "hook.on_flow_report": "string",
      "http.dirMenu": "string",
      "rtsp.directProxy": "string",
      "ffmpeg.cmd": "string",
      "rtp.lowLatency": "string",
      "protocol.enable_rtsp": "string",
      "rtsp.port": "string",
      "rtmp.sslport": "string",
      "protocol.hls_save_path": "string",
      "http.charSet": "string",
      "http.sendBufSize": "string",
      "hls.broadcastRecordTs": "string",
      "api.apiDebug": "string",
      "general.mergeWriteMS": "string",
      "http.forbidCacheSuffix": "string",
      "http.notFound": "string",
      "hook.retry": "string",
      "record.appName": "string",
      "hls.fileBufSize": "string",
      "hook.timeoutSec": "string",
      "rtsp.sslport": "string",
      "hls.deleteDelaySec": "string",
      "hook.on_rtp_server_timeout": "string",
      "hook.on_send_rtp_stopped": "string",
      "hook.on_record_mp4": "string",
      "hook.alive_interval": "string",
      "rtmp.handshakeSecond": "string",
      "hook.stream_changed_schemas": "string",
      "rtc.externIP": "string",
      "rtc.rembBitRate": "string",
      "general.streamNoneReaderDelayMS": "string",
      "protocol.mp4_max_second": "string",
      "hook.on_publish": "string",
      "rtp_proxy.port": "string",
      "http.sslport": "string",
      "rtp.audioMtuSize": "string",
      "general.check_nvidia_dev": "string",
      "record.fastStart": "string",
      "hook.on_stream_not_found": "string",
      "rtp_proxy.port_range": "string",
      "protocol.enable_rtmp": "string",
      "srt.timeoutSec": "string",
      "rtsp.handshakeSecond": "string",
      "hls.segDur": "string",
      "protocol.mp4_as_player": "string",
      "api.secret": "string",
      "hls.segRetain": "string",
      "protocol.rtsp_demand": "string",
      "srt.port": "string",
      "srt.pktBufSize": "string",
      "rtp_proxy.gop_cache": "string",
      "shell.maxReqSize": "string",
      "ffmpeg.snap": "string",
      "general.maxStreamWaitMS": "string",
      "multicast.addrMax": "string",
      "general.wait_add_track_ms": "string",
      "http.allow_cross_domains": "string",
      "protocol.modify_stamp": "string",
      "rtp.videoMtuSize": "string",
      "api.snapRoot": "string",
      "protocol.enable_audio": "string",
      "hook.on_server_keepalive": "string",
      "multicast.addrMin": "string",
      "protocol.ts_demand": "string",
      "protocol.enable_fmp4": "string",
      "rtsp.lowLatency": "string",
      "http.allow_ip_range": "string",
      "hook.on_rtsp_realm": "string",
      "hook.on_stream_changed": "string",
      "http.forwarded_ip_header": "string",
      "rtp_proxy.h265_pt": "string",
      "hook.on_del_mp4": "string",
      "protocol.enable_hls": "string",
      "protocol.enable_mp4": "string",
      "rtc.port": "string",
      "protocol.fmp4_demand": "string",
      "record.sampleMS": "string",
      "shell.port": "string",
      "hook.on_shell_login": "string",
      "cluster.retry_count": "string",
      "general.enableVhost": "string",
      "general.unready_frame_cache": "string",
      "rtc.preferredCodecV": "string",
      "rtp_proxy.h264_pt": "string",
      "protocol.auto_close": "string",
      "srt.latencyMul": "string",
      "hook.on_server_exited": "string",
      "general.resetWhenRePlay": "string",
      "protocol.mp4_save_path": "string",
      "protocol.continue_push_ms": "string",
      "rtp_proxy.dumpDir": "string",
      "rtp_proxy.ps_pt": "string",
      "hook.enable": "string",
      "rtc.timeoutSec": "string",
      "rtc.preferredCodecA": "string",
      "hls.segKeep": "string",
      "multicast.udpTTL": "string",
      "rtp.h264_stap_a": "string",
      "hook.on_stream_none_reader": "string",
      "hook.on_record_ts": "string",
      "ffmpeg.bin": "string",
      "protocol.enable_ts": "string",
      "protocol.enable_hls_fmp4": "string",
      "hls.segNum": "string",
      "http.maxReqSize": "string",
      "rtc.tcpPort": "string",
      "cluster.timeout_sec": "string",
      "general.enable_ffmpeg_log": "string",
      "general.mediaServerId": "string",
      "hook.on_http_access": "string",
      "general.wait_track_ready_ms": "string",
      "rtsp.authBasic": "string",
      "hook.on_rtsp_auth": "string",
      "protocol.rtmp_demand": "string",
      "protocol.add_mute_audio": "string",
      "record.fileBufSize": "string",
      "rtmp.keepAliveSecond": "string",
      "hook.on_play": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListServerNodeConfig](#schemaserverresponselistservernodeconfig)|

## POST 设置服务器配置

POST /zlm/api/server/config

设置服务器配置
设置服务器配置
修改ZLMediaKit服务器的配置参数

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 重启服务器

POST /zlm/api/server/restart

重启服务器
重启服务器
重启ZLMediaKit服务器

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {},
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseObject](#schemaserverresponseobject)|

## POST 获取流列表

POST /zlm/api/media/list

获取流列表
获取流列表
获取ZLMediaKit服务器中的媒体流列表

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaReq](#schemamediareq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "app": "string",
      "readerCount": 0,
      "totalReaderCount": 0,
      "schema": "string",
      "stream": "string",
      "originSock": {
        "identifier": "string",
        "local_ip": "string",
        "local_port": 0,
        "peer_ip": "string",
        "peer_port": 0,
        "typeid": "string"
      },
      "originType": 0,
      "originTypeStr": "string",
      "originUrl": "string",
      "createStamp": 0,
      "aliveSecond": 0,
      "bytesSpeed": 0,
      "tracks": [
        {
          "channels": 0,
          "codec_id": 0,
          "codec_id_name": "string",
          "codec_type": 0,
          "fps": 0,
          "height": 0,
          "ready": true,
          "width": 0,
          "frames": 0,
          "sample_bit": 0,
          "sample_rate": 0,
          "gop_interval_ms": 0,
          "gop_size": 0,
          "key_frames": 0,
          "duration": 0,
          "loss": 0
        }
      ],
      "vhost": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListMediaData](#schemaserverresponselistmediadata)|

## POST 关断单个流

POST /zlm/api/media/close

关断单个流
关断单个流
关闭指定的媒体流

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaReq](#schemamediareq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 批量关断流

POST /zlm/api/media/close-batch

批量关断流
批量关断流
批量关闭多个媒体流

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "force": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[CloseStreamsReq](#schemaclosestreamsreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponse](#schemaserverresponse)|

## POST 检查流是否在线

POST /zlm/api/media/online

流是否在线
检查流是否在线
检查指定媒体流是否在线

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaReq](#schemamediareq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success",
  "online": true
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[MediaOnlineStatus](#schemamediaonlinestatus)|

## POST 获取媒体流播放器列表

POST /zlm/api/media/player/list

获取媒体流播放器列表
获取媒体流播放器列表
获取指定媒体流的播放器列表

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaReq](#schemamediareq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "identifier": "string",
    "local_ip": "string",
    "local_port": 0,
    "peer_ip": "string",
    "peer_port": 0,
    "typeid": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseMediaPlayer](#schemaserverresponsemediaplayer)|

## POST 获取流信息

POST /zlm/api/media/info

获取流信息
获取流信息
获取指定媒体流的详细信息

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaReq](#schemamediareq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "readerCount": 0,
    "totalReaderCount": 0,
    "tracks": [
      {
        "channels": 0,
        "codec_id": 0,
        "codec_id_name": "string",
        "codec_type": 0,
        "fps": 0,
        "height": 0,
        "ready": true,
        "width": 0,
        "frames": 0,
        "sample_bit": 0,
        "sample_rate": 0,
        "gop_interval_ms": 0,
        "gop_size": 0,
        "key_frames": 0,
        "duration": 0,
        "loss": 0
      }
    ],
    "aliveSecond": 0,
    "app": "string",
    "bytesSpeed": 0,
    "createStamp": 0,
    "isRecordingHLS": true,
    "isRecordingMP4": true,
    "originSock": {
      "identifier": "string",
      "local_ip": "string",
      "local_port": 0,
      "peer_ip": "string",
      "peer_port": 0
    },
    "originType": 0,
    "originTypeStr": "string",
    "originUrl": "string",
    "params": "string",
    "schema": "string",
    "stream": "string",
    "totalBytes": 0,
    "vhost": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseMediaInfo](#schemaserverresponsemediainfo)|

## POST 获取播放地址

POST /zlm/api/media/play-urls

获取播放地址
获取播放地址
获取指定媒体流的多协议播放地址

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MediaReq](#schemamediareq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "rtsp": "string",
    "rtmp": "string",
    "http_flv": "string",
    "ws_flv": "string",
    "hls": "string",
    "http_ts": "string",
    "ws_ts": "string",
    "http_fmp4": "string",
    "ws_fmp4": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponsePlayUrl](#schemaserverresponseplayurl)|

## POST 广播WebRTC消息

POST /zlm/api/broadcast/message

广播webrtc datachannel消息
广播WebRTC消息
广播WebRTC datachannel消息

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponse](#schemaserverresponse)|

## GET 获取TCP会话列表

GET /zlm/api/session/list

获取所有TcpSession列表
获取TCP会话列表
获取ZLMediaKit服务器中所有TCP连接会话的列表

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|localPort|query|string| 否 |本地端口|
|peerIp|query|string| 否 |对端IP|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "id": "string",
      "local_ip": "string",
      "local_port": 0,
      "peer_ip": "string",
      "peer_port": 0,
      "typeid": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListTcpLink](#schemaserverresponselisttcplink)|

## DELETE 断开TCP连接

DELETE /zlm/api/session/{sessionId}

断开tcp连接
断开TCP连接
根据会话ID断开指定的TCP连接

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|sessionId|path|string| 是 |会话ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 批量断开TCP连接

POST /zlm/api/session/kick-batch

批量断开tcp连接
批量断开TCP连接
根据条件批量断开TCP连接

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 添加代理拉流

POST /zlm/api/proxy/add

添加代理拉流
添加代理拉流
添加一个拉流代理，用于从外部拉取媒体流

> Body 请求参数

```json
{
  "vhost": "string",
  "app": "string",
  "stream": "string",
  "url": "string",
  "retry_count": 0,
  "rtp_type": 0,
  "timeout_sec": 0,
  "enable_hls": true,
  "enable_hls_fmp4": true,
  "enable_mp4": true,
  "enable_rtsp": true,
  "enable_rtmp": true,
  "enable_ts": true,
  "enable_fmp4": true,
  "hls_demand": true,
  "rtsp_demand": true,
  "rtmp_demand": true,
  "ts_demand": true,
  "fmp4_demand": true,
  "enable_audio": true,
  "add_mute_audio": true,
  "mp4_save_path": "string",
  "mp4_max_second": 0,
  "mp4_as_player": true,
  "hls_save_path": "string",
  "modify_stamp": 0,
  "auto_close": true
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamProxyItem](#schemastreamproxyitem)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "key": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseStreamKey](#schemaserverresponsestreamkey)|

## DELETE 关闭拉流代理

DELETE /zlm/api/proxy/{key}

关闭拉流代理
关闭拉流代理
根据代理key关闭指定的拉流代理

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|key|path|string| 是 |代理key|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "flag": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseStringDelFlag](#schemaserverresponsestringdelflag)|

## POST 获取拉流代理信息

POST /zlm/api/proxy/info

获取拉流代理信息
获取拉流代理信息
获取拉流代理的详细信息

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponse](#schemaserverresponse)|

## POST 添加推流代理

POST /zlm/api/pusher/add

添加推流代理
添加推流代理
添加一个推流代理，用于向外部推送媒体流

> Body 请求参数

```json
{
  "vhost": "string",
  "schema": "string",
  "app": "string",
  "stream": "string",
  "dst_url": "string",
  "retry_count": 0,
  "rtp_type": 0,
  "timeout_sec": 0
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamPusherItem](#schemastreampusheritem)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "key": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseStreamKey](#schemaserverresponsestreamkey)|

## DELETE 关闭推流代理

DELETE /zlm/api/pusher/{key}

关闭推流代理
关闭推流代理
根据代理key关闭指定的推流代理

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|key|path|string| 是 |代理key|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "flag": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseStringDelFlag](#schemaserverresponsestringdelflag)|

## POST 获取推流代理信息

POST /zlm/api/pusher/info

获取推流代理信息
获取推流代理信息
获取推流代理的详细信息

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponse](#schemaserverresponse)|

## POST 添加FFmpeg拉流代理

POST /zlm/api/ffmpeg/add

添加FFmpeg拉流代理
添加FFmpeg拉流代理
添加一个FFmpeg拉流代理，用于从外部拉取媒体流

> Body 请求参数

```json
{
  "src_url": "string",
  "dst_url": "string",
  "timeout_ms": 0,
  "enable_hls": true,
  "enable_mp4": true,
  "ffmpeg_cmd_key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StreamFfmpegItem](#schemastreamffmpegitem)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "key": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseStreamKey](#schemaserverresponsestreamkey)|

## DELETE 关闭FFmpeg拉流代理

DELETE /zlm/api/ffmpeg/{key}

关闭FFmpeg拉流代理
关闭FFmpeg拉流代理
根据代理key关闭指定的FFmpeg拉流代理

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|key|path|string| 是 |代理key|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "flag": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseStringDelFlag](#schemaserverresponsestringdelflag)|

## POST 获取录制文件列表

POST /zlm/api/record/files

获取录制文件列表
获取录制文件列表
获取指定媒体流的录制文件列表

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "period": "string",
  "customized_path": "string",
  "max_seconds": "string",
  "type": 0,
  "speed": "string",
  "stamp": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RecordReq](#schemarecordreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "paths": [
      "string"
    ],
    "rootPath": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseMp4RecordFile](#schemaserverresponsemp4recordfile)|

## POST 删除录像文件夹

POST /zlm/api/record/delete-directory

删除录像文件夹
删除录像文件夹
删除指定的录像文件夹

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success",
  "path": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[DeleteRecordDirectory](#schemadeleterecorddirectory)|

## POST 开始录制

POST /zlm/api/record/start

开始录制
开始录制
开始录制指定的媒体流

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "period": "string",
  "customized_path": "string",
  "max_seconds": "string",
  "type": 0,
  "speed": "string",
  "stamp": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RecordReq](#schemarecordreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 设置录像速度

POST /zlm/api/record/speed

设置录像速度
设置录像速度
设置录像文件的播放速度

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "period": "string",
  "customized_path": "string",
  "max_seconds": "string",
  "type": 0,
  "speed": "string",
  "stamp": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RecordReq](#schemarecordreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 设置录像播放位置

POST /zlm/api/record/seek

设置录像流播放位置
设置录像播放位置
设置录像流的播放位置

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "period": "string",
  "customized_path": "string",
  "max_seconds": "string",
  "type": 0,
  "speed": "string",
  "stamp": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RecordReq](#schemarecordreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 停止录制

POST /zlm/api/record/stop

停止录制
停止录制
停止录制指定的媒体流

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "period": "string",
  "customized_path": "string",
  "max_seconds": "string",
  "type": 0,
  "speed": "string",
  "stamp": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RecordReq](#schemarecordreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 检查录制状态

POST /zlm/api/record/status

是否正在录制
检查录制状态
检查指定媒体流是否正在录制

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "period": "string",
  "customized_path": "string",
  "max_seconds": "string",
  "type": 0,
  "speed": "string",
  "stamp": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[RecordReq](#schemarecordreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 查询文件概览

POST /zlm/api/record/summary

查询文件概览
查询文件概览
查询录制文件的概览信息

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 获取截图

POST /zlm/api/snapshot

获取截图
获取截图
获取指定媒体流的截图

> Body 请求参数

```json
{
  "url": "string",
  "timeout_sec": 30,
  "expire_sec": 5
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[SnapshotReq](#schemasnapshotreq)| 否 |none|

> 返回示例

> 200 Response

```json
"string"
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|string|

## POST 获取截图URL

POST /zlm/api/snapshot-url

获取截图URL - 返回可访问的URL路径
获取截图URL
获取指定媒体流的截图并返回可访问的URL

> Body 请求参数

```json
{
  "url": "string",
  "timeout_sec": 30,
  "expire_sec": 5
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[SnapshotReq](#schemasnapshotreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## GET 获取RTP推流信息

GET /zlm/api/rtp/info/{streamId}

获取rtp推流信息
获取RTP推流信息
根据流ID获取RTP推流的详细信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|streamId|path|string| 是 |流ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "exist": true,
  "peer_ip": "string",
  "peer_port": 0,
  "local_ip": "string",
  "local_port": 0
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[RtpInfoResult](#schemartpinforesult)|

## POST 创建RTP服务器

POST /zlm/api/rtp/server/open

创建RTP服务器
创建RTP服务器
创建一个RTP服务器用于接收RTP推流

> Body 请求参数

```json
{
  "port": 0,
  "tcp_mode": 0,
  "stream_id": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[OpenRtpServerReq](#schemaopenrtpserverreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "port": "string",
  "code": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[OpenRtpServerResult](#schemaopenrtpserverresult)|

## POST 创建多路复用RTP服务器

POST /zlm/api/rtp/server/open-multiplex

创建多路复用RTP服务器
创建多路复用RTP服务器
创建一个多路复用RTP服务器

> Body 请求参数

```json
{
  "port": 0,
  "tcp_mode": 0,
  "stream_id": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[OpenRtpServerReq](#schemaopenrtpserverreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "port": "string",
  "code": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[OpenRtpServerResult](#schemaopenrtpserverresult)|

## POST 连接RTP服务器

POST /zlm/api/rtp/server/connect

连接RTP服务器
连接RTP服务器
连接到指定的RTP服务器

> Body 请求参数

```json
{
  "dst_port": 0,
  "dst_url": 0,
  "stream_id": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[ConnectRtpServerReq](#schemaconnectrtpserverreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "port": "string",
  "code": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[OpenRtpServerResult](#schemaopenrtpserverresult)|

## DELETE 关闭RTP服务器

DELETE /zlm/api/rtp/server/{streamId}

关闭RTP服务器
关闭RTP服务器
根据流ID关闭指定的RTP服务器

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|streamId|path|string| 是 |流ID|

> 返回示例

> 200 Response

```json
{
  "hit": "string",
  "code": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[CloseRtpServerResult](#schemaclosertpserverresult)|

## PUT 更新RTP服务器SSRC

PUT /zlm/api/rtp/server/{streamId}/ssrc/{ssrc}

更新RTP服务器过滤SSRC
更新RTP服务器SSRC
更新RTP服务器的过滤SSRC

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|streamId|path|string| 是 |流ID|
|ssrc|path|string| 是 |SSRC值|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 暂停RTP超时检查

POST /zlm/api/rtp/server/{streamId}/pause-check

暂停RTP超时检查
暂停RTP超时检查
暂停指定RTP服务器的超时检查

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|streamId|path|string| 是 |流ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 恢复RTP超时检查

POST /zlm/api/rtp/server/{streamId}/resume-check

恢复RTP超时检查
恢复RTP超时检查
恢复指定RTP服务器的超时检查

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|streamId|path|string| 是 |流ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## GET 获取RTP服务器列表

GET /zlm/api/rtp/server/list

获取RTP服务器列表
获取RTP服务器列表
获取所有正在运行的RTP服务器列表

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "port": "string",
      "streamId": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListRtpServer](#schemaserverresponselistrtpserver)|

## POST 开始发送RTP

POST /zlm/api/rtp/send/start

开始发送rtp
开始发送RTP
开始向指定地址发送RTP流

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "ssrc": 0,
  "dst_url": "string",
  "dst_port": 0,
  "is_udp": true,
  "src_port": 0,
  "pt": 0,
  "use_ps": 0,
  "only_audio": true
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StartSendRtpReq](#schemastartsendrtpreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "local_port": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[StartSendRtpResult](#schemastartsendrtpresult)|

## POST 开始被动发送RTP

POST /zlm/api/rtp/send/start-passive

开始tcp passive被动发送rtp
开始被动发送RTP
开始TCP passive模式被动发送RTP流

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "ssrc": 0,
  "dst_url": "string",
  "dst_port": 0,
  "is_udp": true,
  "src_port": 0,
  "pt": 0,
  "use_ps": 0,
  "only_audio": true
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[StartSendRtpReq](#schemastartsendrtpreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": "string",
  "local_port": "string"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[StartSendRtpResult](#schemastartsendrtpresult)|

## POST 停止发送RTP

POST /zlm/api/rtp/send/stop

停止发送rtp
停止发送RTP
停止向指定地址发送RTP流

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "ssrc": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[CloseSendRtpReq](#schemaclosesendrtpreq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## POST 开始多文件推流

POST /zlm/api/mp4/publish/start

多文件推流
开始多文件推流
开始推流多个MP4文件

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponse](#schemaserverresponse)|

## POST 停止多文件推流

POST /zlm/api/mp4/publish/stop

关闭多文件推流
停止多文件推流
停止推流多个MP4文件

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponse](#schemaserverresponse)|

## POST 点播MP4文件

POST /zlm/api/mp4/load

点播mp4文件
点播MP4文件
加载并点播指定MP4文件

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponse](#schemaserverresponse)|

## POST 获取存储空间信息

POST /zlm/api/storage/space

获取存储信息
获取存储空间信息
获取服务器的存储空间信息

> Body 请求参数

```json
{
  "key": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|body|body|[MapString](#schemamapstring)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseString](#schemaserverresponsestring)|

## GET 指定节点获取版本信息

GET /zlm/api/node/{nodeId}/version

指定节点获取版本信息
指定节点获取版本信息
获取指定ZLM节点的版本信息

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|nodeId|path|string| 是 |节点ID|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": {
    "buildTime": "string",
    "branchName": "string",
    "commitHash": "string"
  },
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseVersion](#schemaserverresponseversion)|

## POST 指定节点获取流列表

POST /zlm/api/node/{nodeId}/media/list

指定节点获取流列表
指定节点获取流列表
获取指定ZLM节点中的媒体流列表

> Body 请求参数

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}
```

### 请求参数

|名称|位置|类型|必选|说明|
|---|---|---|---|---|
|nodeId|path|string| 是 |节点ID|
|body|body|[MediaReq](#schemamediareq)| 否 |none|

> 返回示例

> 200 Response

```json
{
  "code": 0,
  "data": [
    {
      "app": "string",
      "readerCount": 0,
      "totalReaderCount": 0,
      "schema": "string",
      "stream": "string",
      "originSock": {
        "identifier": "string",
        "local_ip": "string",
        "local_port": 0,
        "peer_ip": "string",
        "peer_port": 0,
        "typeid": "string"
      },
      "originType": 0,
      "originTypeStr": "string",
      "originUrl": "string",
      "createStamp": 0,
      "aliveSecond": 0,
      "bytesSpeed": 0,
      "tracks": [
        {
          "channels": 0,
          "codec_id": 0,
          "codec_id_name": "string",
          "codec_type": 0,
          "fps": 0,
          "height": 0,
          "ready": true,
          "width": 0,
          "frames": 0,
          "sample_bit": 0,
          "sample_rate": 0,
          "gop_interval_ms": 0,
          "gop_size": 0,
          "key_frames": 0,
          "duration": 0,
          "loss": 0
        }
      ],
      "vhost": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|[ServerResponseListMediaData](#schemaserverresponselistmediadata)|

## GET 获取所有节点列表

GET /zlm/api/nodes

获取所有节点列表
获取所有节点列表
获取当前配置的所有ZLM节点信息

> 返回示例

> 200 Response

```json
[
  {
    "serverId": "zlm",
    "host": "http://127.0.0.1:9092",
    "secret": "string",
    "enabled": true,
    "hookEnabled": true,
    "weight": 100
  }
]
```

### 返回结果

|状态码|状态码含义|说明|数据模型|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|none|Inline|

### 返回数据结构

状态码 **200**

*获取成功*

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|*anonymous*|[[ZlmNode](#schemazlmnode)]|false|none||获取成功|
|» serverId|string|false|none||The id of this node.|
|» host|string|false|none||The host of this node. eg: <a href="http://127.0.0.1:9092">node</a>|
|» secret|string|false|none||The secret of this host.|
|» enabled|boolean|false|none||Whether enable this host.|
|» hookEnabled|boolean|false|none||Whether enable hook.|
|» weight|integer|false|none||The weight of this host.|

# 数据模型

<h2 id="tocS_Version">Version</h2>

<a id="schemaversion"></a>
<a id="schema_Version"></a>
<a id="tocSversion"></a>
<a id="tocsversion"></a>

```json
{
  "buildTime": "string",
  "branchName": "string",
  "commitHash": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|buildTime|string|false|none||none|
|branchName|string|false|none||none|
|commitHash|string|false|none||none|

<h2 id="tocS_HookResult">HookResult</h2>

<a id="schemahookresult"></a>
<a id="schema_HookResult"></a>
<a id="tocShookresult"></a>
<a id="tocshookresult"></a>

```json
{
  "code": 0,
  "msg": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||错误代码，0代表允许推流|
|msg|string|false|none||不允许推流时的错误提示|

<h2 id="tocS_AjaxResultMediaNodeVO">AjaxResultMediaNodeVO</h2>

<a id="schemaajaxresultmedianodevo"></a>
<a id="schema_AjaxResultMediaNodeVO"></a>
<a id="tocSajaxresultmedianodevo"></a>
<a id="tocsajaxresultmedianodevo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseVersion">ServerResponseVersion</h2>

<a id="schemaserverresponseversion"></a>
<a id="schema_ServerResponseVersion"></a>
<a id="tocSserverresponseversion"></a>
<a id="tocsserverresponseversion"></a>

```json
{
  "code": 0,
  "data": {
    "buildTime": "string",
    "branchName": "string",
    "commitHash": "string"
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[Version](#schemaversion)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_KeepLiveData">KeepLiveData</h2>

<a id="schemakeeplivedata"></a>
<a id="schema_KeepLiveData"></a>
<a id="tocSkeeplivedata"></a>
<a id="tocskeeplivedata"></a>

```json
{
  "Buffer": 0,
  "BufferLikeString": 0,
  "BufferList": 0,
  "BufferRaw": 0,
  "Frame": 0,
  "FrameImp": 0,
  "MediaSource": 0,
  "MultiMediaSourceMuxer": 0,
  "RtmpPacket": 0,
  "RtpPacket": 0,
  "Socket": 0,
  "TcpClient": 0,
  "TcpServer": 0,
  "TcpSession": 0,
  "UdpServer": 0,
  "UdpSession": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|Buffer|integer|false|none||缓冲区|
|BufferLikeString|integer|false|none||类似字符串的缓冲区|
|BufferList|integer|false|none||缓冲区列表|
|BufferRaw|integer|false|none||原始缓冲区|
|Frame|integer|false|none||框架|
|FrameImp|integer|false|none||框架实现|
|MediaSource|integer|false|none||媒体源|
|MultiMediaSourceMuxer|integer|false|none||多媒体源复用器|
|RtmpPacket|integer|false|none||Rtmp数据包|
|RtpPacket|integer|false|none||Rtp数据包|
|Socket|integer|false|none||套接字|
|TcpClient|integer|false|none||Tcp客户端|
|TcpServer|integer|false|none||Tcp服务器|
|TcpSession|integer|false|none||Tcp会话|
|UdpServer|integer|false|none||Udp服务器|
|UdpSession|integer|false|none||Udp会话|

<h2 id="tocS_AjaxResultListMediaNodeVO">AjaxResultListMediaNodeVO</h2>

<a id="schemaajaxresultlistmedianodevo"></a>
<a id="schema_AjaxResultListMediaNodeVO"></a>
<a id="tocSajaxresultlistmedianodevo"></a>
<a id="tocsajaxresultlistmedianodevo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseListString">ServerResponseListString</h2>

<a id="schemaserverresponseliststring"></a>
<a id="schema_ServerResponseListString"></a>
<a id="tocSserverresponseliststring"></a>
<a id="tocsserverresponseliststring"></a>

```json
{
  "code": 0,
  "data": [
    "string"
  ],
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[string]|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_OnServerKeepaliveHookParam">OnServerKeepaliveHookParam</h2>

<a id="schemaonserverkeepalivehookparam"></a>
<a id="schema_OnServerKeepaliveHookParam"></a>
<a id="tocSonserverkeepalivehookparam"></a>
<a id="tocsonserverkeepalivehookparam"></a>

```json
{
  "data": {
    "Buffer": 0,
    "BufferLikeString": 0,
    "BufferList": 0,
    "BufferRaw": 0,
    "Frame": 0,
    "FrameImp": 0,
    "MediaSource": 0,
    "MultiMediaSourceMuxer": 0,
    "RtmpPacket": 0,
    "RtpPacket": 0,
    "Socket": 0,
    "TcpClient": 0,
    "TcpServer": 0,
    "TcpSession": 0,
    "UdpServer": 0,
    "UdpSession": 0
  },
  "hook_index": "string",
  "mediaServerId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|data|[KeepLiveData](#schemakeeplivedata)|false|none||none|
|hook_index|string|false|none||none|
|mediaServerId|string|false|none||none|

<h2 id="tocS_AjaxResultMediaNodeListResp">AjaxResultMediaNodeListResp</h2>

<a id="schemaajaxresultmedianodelistresp"></a>
<a id="schema_AjaxResultMediaNodeListResp"></a>
<a id="tocSajaxresultmedianodelistresp"></a>
<a id="tocsajaxresultmedianodelistresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_AjaxResult">AjaxResult</h2>

<a id="schemaajaxresult"></a>
<a id="schema_AjaxResult"></a>
<a id="tocSajaxresult"></a>
<a id="tocsajaxresult"></a>

```json
{
  "key": null
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|null|false|none||none|

<h2 id="tocS_ThreadLoad">ThreadLoad</h2>

<a id="schemathreadload"></a>
<a id="schema_ThreadLoad"></a>
<a id="tocSthreadload"></a>
<a id="tocsthreadload"></a>

```json
{
  "delay": "string",
  "load": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|delay|string|false|none||该线程延时|
|load|string|false|none||该线程负载，0 ~ 100|

<h2 id="tocS_OnPlayHookParam">OnPlayHookParam</h2>

<a id="schemaonplayhookparam"></a>
<a id="schema_OnPlayHookParam"></a>
<a id="tocSonplayhookparam"></a>
<a id="tocsonplayhookparam"></a>

```json
{
  "mediaServerId": "string",
  "id": "string",
  "app": "string",
  "stream": "string",
  "ip": "string",
  "params": "string",
  "port": 0,
  "schema": "string",
  "vhost": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|
|id|string|false|none||none|
|app|string|false|none||none|
|stream|string|false|none||none|
|ip|string|false|none||none|
|params|string|false|none||none|
|port|integer|false|none||none|
|schema|string|false|none||none|
|vhost|string|false|none||none|

<h2 id="tocS_ServerResponseListThreadLoad">ServerResponseListThreadLoad</h2>

<a id="schemaserverresponselistthreadload"></a>
<a id="schema_ServerResponseListThreadLoad"></a>
<a id="tocSserverresponselistthreadload"></a>
<a id="tocsserverresponselistthreadload"></a>

```json
{
  "code": 0,
  "data": [
    {
      "delay": "string",
      "load": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[[ThreadLoad](#schemathreadload)]|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_MediaNodeCreateReq">MediaNodeCreateReq</h2>

<a id="schemamedianodecreatereq"></a>
<a id="schema_MediaNodeCreateReq"></a>
<a id="tocSmedianodecreatereq"></a>
<a id="tocsmedianodecreatereq"></a>

```json
{
  "serverId": "string",
  "name": "string",
  "host": "string",
  "secret": "string",
  "enabled": true,
  "hookEnabled": true,
  "weight": 1,
  "description": "string",
  "extend": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|serverId|string|true|none||节点ID|
|name|string|false|none||节点名称|
|host|string|true|none||节点地址|
|secret|string|false|none||API密钥|
|enabled|boolean|false|none||是否启用 true启用 false禁用|
|hookEnabled|boolean|false|none||是否启用Hook true启用 false禁用|
|weight|integer|false|none||节点权重|
|description|string|false|none||节点描述|
|extend|string|false|none||扩展字段|

<h2 id="tocS_HookResultForOnPublish">HookResultForOnPublish</h2>

<a id="schemahookresultforonpublish"></a>
<a id="schema_HookResultForOnPublish"></a>
<a id="tocShookresultforonpublish"></a>
<a id="tocshookresultforonpublish"></a>

```json
{
  "enable_hls": true,
  "enable_hls_fmp4": true,
  "enable_mp4": true,
  "enable_rtsp": true,
  "enable_rtmp": true,
  "enable_ts": true,
  "enable_fmp4": true,
  "hls_demand": true,
  "rtsp_demand": true,
  "rtmp_demand": true,
  "ts_demand": true,
  "fmp4_demand": true,
  "enable_audio": true,
  "add_mute_audio": true,
  "mp4_save_path": "string",
  "mp4_max_second": 0,
  "mp4_as_player": true,
  "hls_save_path": "string",
  "modify_stamp": 0,
  "continue_push_ms": 0,
  "auto_close": true,
  "stream_replace": "string",
  "code": 0,
  "msg": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|enable_hls|boolean|false|none||是否转换成hls-mpegts协议|
|enable_hls_fmp4|boolean|false|none||是否转换成hls-fmp4协议|
|enable_mp4|boolean|false|none||是否允许mp4录制|
|enable_rtsp|boolean|false|none||是否转rtsp协议|
|enable_rtmp|boolean|false|none||是否转rtmp/flv协议|
|enable_ts|boolean|false|none||是否转http-ts/ws-ts协议|
|enable_fmp4|boolean|false|none||是否转http-fmp4/ws-fmp4协议|
|hls_demand|boolean|false|none||该协议是否有人观看才生成|
|rtsp_demand|boolean|false|none||该协议是否有人观看才生成|
|rtmp_demand|boolean|false|none||该协议是否有人观看才生成|
|ts_demand|boolean|false|none||该协议是否有人观看才生成|
|fmp4_demand|boolean|false|none||该协议是否有人观看才生成|
|enable_audio|boolean|false|none||转协议时是否开启音频|
|add_mute_audio|boolean|false|none||转协议时，无音频是否添加静音aac音频|
|mp4_save_path|string|false|none||mp4录制文件保存根目录，置空使用默认|
|mp4_max_second|integer|false|none||mp4录制切片大小，单位秒|
|mp4_as_player|boolean|false|none||MP4录制是否当作观看者参与播放人数计数|
|hls_save_path|string|false|none||hls文件保存保存根目录，置空使用默认|
|modify_stamp|integer|false|none||该流是否开启时间戳覆盖(0:绝对时间戳/1:系统时间戳/2:相对时间戳)|
|continue_push_ms|integer(int64)|false|none||断连续推延时，单位毫秒，置空使用配置文件默认值|
|auto_close|boolean|false|none||无人观看是否自动关闭流(不触发无人观看hook)|
|stream_replace|string|false|none||是否修改流id, 通过此参数可以自定义流id(譬如替换ssrc)|
|code|integer|false|none||错误代码，0代表允许推流|
|msg|string|false|none||不允许推流时的错误提示|

<h2 id="tocS_ImportantObjectNum">ImportantObjectNum</h2>

<a id="schemaimportantobjectnum"></a>
<a id="schema_ImportantObjectNum"></a>
<a id="tocSimportantobjectnum"></a>
<a id="tocsimportantobjectnum"></a>

```json
{
  "Buffer": 0,
  "RtpPacket": 0,
  "Frame": 0,
  "RtmpPacket": 0,
  "TcpSession": 0,
  "UdpServer": 0,
  "TcpServer": 0,
  "FrameImp": 0,
  "BufferList": 0,
  "BufferRaw": 0,
  "MediaSource": 0,
  "MultiMediaSourceMuxer": 0,
  "TcpClient": 0,
  "BufferLikeString": 0,
  "Socket": 0,
  "UdpSession": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|Buffer|integer|false|none||none|
|RtpPacket|integer|false|none||none|
|Frame|integer|false|none||none|
|RtmpPacket|integer|false|none||none|
|TcpSession|integer|false|none||none|
|UdpServer|integer|false|none||none|
|TcpServer|integer|false|none||none|
|FrameImp|integer|false|none||none|
|BufferList|integer|false|none||none|
|BufferRaw|integer|false|none||none|
|MediaSource|integer|false|none||none|
|MultiMediaSourceMuxer|integer|false|none||none|
|TcpClient|integer|false|none||none|
|BufferLikeString|integer|false|none||none|
|Socket|integer|false|none||none|
|UdpSession|integer|false|none||none|

<h2 id="tocS_MediaNodeUpdateReq">MediaNodeUpdateReq</h2>

<a id="schemamedianodeupdatereq"></a>
<a id="schema_MediaNodeUpdateReq"></a>
<a id="tocSmedianodeupdatereq"></a>
<a id="tocsmedianodeupdatereq"></a>

```json
{
  "id": 0,
  "serverId": "string",
  "name": "string",
  "host": "string",
  "secret": "string",
  "enabled": true,
  "hookEnabled": true,
  "weight": 1,
  "description": "string",
  "extend": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|true|none||数据库主键ID|
|serverId|string|false|none||节点ID|
|name|string|false|none||节点名称|
|host|string|false|none||节点地址|
|secret|string|false|none||API密钥|
|enabled|boolean|false|none||是否启用 true启用 false禁用|
|hookEnabled|boolean|false|none||是否启用Hook true启用 false禁用|
|weight|integer|false|none||节点权重|
|description|string|false|none||节点描述|
|extend|string|false|none||扩展字段|

<h2 id="tocS_OnPublishHookParam">OnPublishHookParam</h2>

<a id="schemaonpublishhookparam"></a>
<a id="schema_OnPublishHookParam"></a>
<a id="tocSonpublishhookparam"></a>
<a id="tocsonpublishhookparam"></a>

```json
{
  "mediaServerId": "string",
  "id": "string",
  "app": "string",
  "stream": "string",
  "ip": "string",
  "params": "string",
  "port": 0,
  "schema": "string",
  "vhost": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|
|id|string|false|none||app	string	流应用名<br />id	string	TCP链接唯一ID<br />ip	string	推流器ip<br />params	string	推流url参数<br />port	unsigned short	推流器端口号<br />schema	string	推流的协议，可能是rtsp、rtmp<br />stream	string	流ID<br />vhost	string	流虚拟主机<br />mediaServerId	string	服务器id,通过配置文件设置|
|app|string|false|none||none|
|stream|string|false|none||none|
|ip|string|false|none||none|
|params|string|false|none||none|
|port|integer|false|none||none|
|schema|string|false|none||none|
|vhost|string|false|none||none|

<h2 id="tocS_ServerResponseImportantObjectNum">ServerResponseImportantObjectNum</h2>

<a id="schemaserverresponseimportantobjectnum"></a>
<a id="schema_ServerResponseImportantObjectNum"></a>
<a id="tocSserverresponseimportantobjectnum"></a>
<a id="tocsserverresponseimportantobjectnum"></a>

```json
{
  "code": 0,
  "data": {
    "Buffer": 0,
    "RtpPacket": 0,
    "Frame": 0,
    "RtmpPacket": 0,
    "TcpSession": 0,
    "UdpServer": 0,
    "TcpServer": 0,
    "FrameImp": 0,
    "BufferList": 0,
    "BufferRaw": 0,
    "MediaSource": 0,
    "MultiMediaSourceMuxer": 0,
    "TcpClient": 0,
    "BufferLikeString": 0,
    "Socket": 0,
    "UdpSession": 0
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[ImportantObjectNum](#schemaimportantobjectnum)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_MediaNodeDO">MediaNodeDO</h2>

<a id="schemamedianodedo"></a>
<a id="schema_MediaNodeDO"></a>
<a id="tocSmedianodedo"></a>
<a id="tocsmedianodedo"></a>

```json
{
  "id": 0,
  "createTime": "string",
  "updateTime": "string",
  "serverId": "string",
  "name": "string",
  "host": "string",
  "secret": "string",
  "enabled": true,
  "hookEnabled": true,
  "weight": 0,
  "keepalive": 0,
  "status": 0,
  "description": "string",
  "extend": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||none|
|createTime|string|false|none||创建时间|
|updateTime|string|false|none||修改时间|
|serverId|string|false|none||节点ID|
|name|string|false|none||节点名称|
|host|string|false|none||节点地址|
|secret|string|false|none||API密钥|
|enabled|boolean|false|none||是否启用 1启用 0禁用|
|hookEnabled|boolean|false|none||是否启用Hook 1启用 0禁用|
|weight|integer|false|none||节点权重|
|keepalive|integer(int64)|false|none||心跳时间戳|
|status|integer|false|none||节点状态 1在线 0离线|
|description|string|false|none||节点描述|
|extend|string|false|none||扩展字段|

<h2 id="tocS_ServerNodeConfig">ServerNodeConfig</h2>

<a id="schemaservernodeconfig"></a>
<a id="schema_ServerNodeConfig"></a>
<a id="tocSservernodeconfig"></a>
<a id="tocsservernodeconfig"></a>

```json
{
  "rtp.rtpMaxSize": "string",
  "protocol.hls_demand": "string",
  "rtp_proxy.opus_pt": "string",
  "rtp_proxy.timeoutSec": "string",
  "rtmp.port": "string",
  "hook.on_ip_not_found": "string",
  "record.fileRepeat": "string",
  "general.flowThreshold": "string",
  "rtsp.rtpTransportType": "string",
  "hook.retry_delay": "string",
  "http.rootPath": "string",
  "rtsp.keepAliveSecond": "string",
  "hook.on_server_started": "string",
  "api.defaultSnap": "string",
  "cluster.origin_url": "string",
  "http.port": "string",
  "http.virtualPath": "string",
  "http.keepAliveSecond": "string",
  "ffmpeg.log": "string",
  "hook.on_flow_report": "string",
  "http.dirMenu": "string",
  "rtsp.directProxy": "string",
  "ffmpeg.cmd": "string",
  "rtp.lowLatency": "string",
  "protocol.enable_rtsp": "string",
  "rtsp.port": "string",
  "rtmp.sslport": "string",
  "protocol.hls_save_path": "string",
  "http.charSet": "string",
  "http.sendBufSize": "string",
  "hls.broadcastRecordTs": "string",
  "api.apiDebug": "string",
  "general.mergeWriteMS": "string",
  "http.forbidCacheSuffix": "string",
  "http.notFound": "string",
  "hook.retry": "string",
  "record.appName": "string",
  "hls.fileBufSize": "string",
  "hook.timeoutSec": "string",
  "rtsp.sslport": "string",
  "hls.deleteDelaySec": "string",
  "hook.on_rtp_server_timeout": "string",
  "hook.on_send_rtp_stopped": "string",
  "hook.on_record_mp4": "string",
  "hook.alive_interval": "string",
  "rtmp.handshakeSecond": "string",
  "hook.stream_changed_schemas": "string",
  "rtc.externIP": "string",
  "rtc.rembBitRate": "string",
  "general.streamNoneReaderDelayMS": "string",
  "protocol.mp4_max_second": "string",
  "hook.on_publish": "string",
  "rtp_proxy.port": "string",
  "http.sslport": "string",
  "rtp.audioMtuSize": "string",
  "general.check_nvidia_dev": "string",
  "record.fastStart": "string",
  "hook.on_stream_not_found": "string",
  "rtp_proxy.port_range": "string",
  "protocol.enable_rtmp": "string",
  "srt.timeoutSec": "string",
  "rtsp.handshakeSecond": "string",
  "hls.segDur": "string",
  "protocol.mp4_as_player": "string",
  "api.secret": "string",
  "hls.segRetain": "string",
  "protocol.rtsp_demand": "string",
  "srt.port": "string",
  "srt.pktBufSize": "string",
  "rtp_proxy.gop_cache": "string",
  "shell.maxReqSize": "string",
  "ffmpeg.snap": "string",
  "general.maxStreamWaitMS": "string",
  "multicast.addrMax": "string",
  "general.wait_add_track_ms": "string",
  "http.allow_cross_domains": "string",
  "protocol.modify_stamp": "string",
  "rtp.videoMtuSize": "string",
  "api.snapRoot": "string",
  "protocol.enable_audio": "string",
  "hook.on_server_keepalive": "string",
  "multicast.addrMin": "string",
  "protocol.ts_demand": "string",
  "protocol.enable_fmp4": "string",
  "rtsp.lowLatency": "string",
  "http.allow_ip_range": "string",
  "hook.on_rtsp_realm": "string",
  "hook.on_stream_changed": "string",
  "http.forwarded_ip_header": "string",
  "rtp_proxy.h265_pt": "string",
  "hook.on_del_mp4": "string",
  "protocol.enable_hls": "string",
  "protocol.enable_mp4": "string",
  "rtc.port": "string",
  "protocol.fmp4_demand": "string",
  "record.sampleMS": "string",
  "shell.port": "string",
  "hook.on_shell_login": "string",
  "cluster.retry_count": "string",
  "general.enableVhost": "string",
  "general.unready_frame_cache": "string",
  "rtc.preferredCodecV": "string",
  "rtp_proxy.h264_pt": "string",
  "protocol.auto_close": "string",
  "srt.latencyMul": "string",
  "hook.on_server_exited": "string",
  "general.resetWhenRePlay": "string",
  "protocol.mp4_save_path": "string",
  "protocol.continue_push_ms": "string",
  "rtp_proxy.dumpDir": "string",
  "rtp_proxy.ps_pt": "string",
  "hook.enable": "string",
  "rtc.timeoutSec": "string",
  "rtc.preferredCodecA": "string",
  "hls.segKeep": "string",
  "multicast.udpTTL": "string",
  "rtp.h264_stap_a": "string",
  "hook.on_stream_none_reader": "string",
  "hook.on_record_ts": "string",
  "ffmpeg.bin": "string",
  "protocol.enable_ts": "string",
  "protocol.enable_hls_fmp4": "string",
  "hls.segNum": "string",
  "http.maxReqSize": "string",
  "rtc.tcpPort": "string",
  "cluster.timeout_sec": "string",
  "general.enable_ffmpeg_log": "string",
  "general.mediaServerId": "string",
  "hook.on_http_access": "string",
  "general.wait_track_ready_ms": "string",
  "rtsp.authBasic": "string",
  "hook.on_rtsp_auth": "string",
  "protocol.rtmp_demand": "string",
  "protocol.add_mute_audio": "string",
  "record.fileBufSize": "string",
  "rtmp.keepAliveSecond": "string",
  "hook.on_play": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|rtp.rtpMaxSize|string|false|none||The maximum size of RTP packets.|
|protocol.hls_demand|string|false|none||Whether to enable HLS demand.|
|rtp_proxy.opus_pt|string|false|none||The Opus payload type used by the RTP proxy.|
|rtp_proxy.timeoutSec|string|false|none||The timeout value for the RTP proxy in seconds.|
|rtmp.port|string|false|none||The port used for RTMP.|
|hook.on_ip_not_found|string|false|none||The action to take when the IP address is not found.|
|record.fileRepeat|string|false|none||Whether to allow file repetition during recording.|
|general.flowThreshold|string|false|none||The threshold for the maximum flow.|
|rtsp.rtpTransportType|string|false|none||The transport type used for RTP in RTSP.|
|hook.retry_delay|string|false|none||The delay time for retrying hooks in seconds.|
|http.rootPath|string|false|none||The root path for HTTP requests.|
|rtsp.keepAliveSecond|string|false|none||The time in seconds to keep the RTSP connection alive.|
|hook.on_server_started|string|false|none||The action to take when the server is started.|
|api.defaultSnap|string|false|none||The default snapshot for the API.|
|cluster.origin_url|string|false|none||The origin URL for the cluster.|
|http.port|string|false|none||The port used for HTTP.|
|http.virtualPath|string|false|none||The virtual path for HTTP requests.|
|http.keepAliveSecond|string|false|none||The time in seconds to keep the HTTP connection alive.|
|ffmpeg.log|string|false|none||The log file for FFmpeg.|
|hook.on_flow_report|string|false|none||The action to take when a flow report is received.|
|http.dirMenu|string|false|none||Whether to enable directory listing for HTTP requests.|
|rtsp.directProxy|string|false|none||Whether to use direct proxy for RTSP.|
|ffmpeg.cmd|string|false|none||The command used for FFmpeg.|
|rtp.lowLatency|string|false|none||Whether to enable low latency for RTP.|
|protocol.enable_rtsp|string|false|none||Whether to enable RTSP.|
|rtsp.port|string|false|none||The port used for RTSP.|
|rtmp.sslport|string|false|none||The SSL port used for RTMP.|
|protocol.hls_save_path|string|false|none||The save path for HLS.|
|http.charSet|string|false|none||The character set used for HTTP requests.|
|http.sendBufSize|string|false|none||The size of the send buffer for HTTP requests.|
|hls.broadcastRecordTs|string|false|none||Whether to broadcast recorded TS files.|
|api.apiDebug|string|false|none||Whether to enable API debugging.|
|general.mergeWriteMS|string|false|none||The time in milliseconds for merging writes.|
|http.forbidCacheSuffix|string|false|none||The suffixes to forbid caching for HTTP requests.|
|http.notFound|string|false|none||The action to take when a resource is not found for HTTP requests.|
|hook.retry|string|false|none||The number of times to retry hooks.|
|record.appName|string|false|none||The application name for recording.|
|hls.fileBufSize|string|false|none||The buffer size for HLS files.|
|hook.timeoutSec|string|false|none||The timeout value for hooks in seconds.|
|rtsp.sslport|string|false|none||The SSL port used for RTSP.|
|hls.deleteDelaySec|string|false|none||The delay time for deleting HLS files in seconds.|
|hook.on_rtp_server_timeout|string|false|none||The action to take when the RTP server times out.|
|hook.on_send_rtp_stopped|string|false|none||The action to take when sending RTP is stopped.|
|hook.on_record_mp4|string|false|none||The action to take when recording MP4 files.|
|hook.alive_interval|string|false|none||The interval time for sending keepalive messages for hooks in seconds.|
|rtmp.handshakeSecond|string|false|none||The time in seconds for the RTMP handshake.|
|hook.stream_changed_schemas|string|false|none||The schemas to use for stream changes in hooks.|
|rtc.externIP|string|false|none||The external IP address for RTC.|
|rtc.rembBitRate|string|false|none||The bit rate for REMB in RTC.|
|general.streamNoneReaderDelayMS|string|false|none||The time in milliseconds for waiting for a stream reader.|
|protocol.mp4_max_second|string|false|none||The maximum duration for MP4 files in seconds.|
|hook.on_publish|string|false|none||The action to take when publishing a stream.|
|rtp_proxy.port|string|false|none||The port used for the RTP proxy.|
|http.sslport|string|false|none||The SSL port used for HTTP.|
|rtp.audioMtuSize|string|false|none||The MTU size for audio packets in RTP.|
|general.check_nvidia_dev|string|false|none||Whether to check for NVIDIA devices.|
|record.fastStart|string|false|none||Whether to enable fast start for recording.|
|hook.on_stream_not_found|string|false|none||The action to take when a stream is not found in hooks.|
|rtp_proxy.port_range|string|false|none||The port range used for the RTP proxy.|
|protocol.enable_rtmp|string|false|none||Whether to enable RTMP.|
|srt.timeoutSec|string|false|none||The timeout value for SRT in seconds.|
|rtsp.handshakeSecond|string|false|none||The time in seconds for the RTSP handshake.|
|hls.segDur|string|false|none||The duration for each HLS segment in seconds.|
|protocol.mp4_as_player|string|false|none||Whether to use MP4 as a player for the protocol.|
|api.secret|string|false|none||The secret key for the API.|
|hls.segRetain|string|false|none||The number of HLS segments to retain.|
|protocol.rtsp_demand|string|false|none||Whether to enable demand for RTSP.|
|srt.port|string|false|none||The port used for SRT.|
|srt.pktBufSize|string|false|none||The packet buffer size for SRT.|
|rtp_proxy.gop_cache|string|false|none||Whether to enable GOP caching for the RTP proxy.|
|shell.maxReqSize|string|false|none||The maximum size of requests for the shell.|
|ffmpeg.snap|string|false|none||Whether to enable snapshots for FFmpeg.|
|general.maxStreamWaitMS|string|false|none||The maximum time in milliseconds to wait for a stream reader.|
|multicast.addrMax|string|false|none||The maximum multicast address.|
|general.wait_add_track_ms|string|false|none||The time in milliseconds to wait for adding a track.|
|http.allow_cross_domains|string|false|none||Whether to allow cross-domain requests for HTTP.|
|protocol.modify_stamp|string|false|none||Whether to modify the timestamp for the protocol.|
|rtp.videoMtuSize|string|false|none||The MTU size for video packets in RTP.|
|api.snapRoot|string|false|none||The root directory for snapshots in the API.|
|protocol.enable_audio|string|false|none||Whether to enable audio for the protocol.|
|hook.on_server_keepalive|string|false|none||The action to take when the server keeps alive.|
|multicast.addrMin|string|false|none||The minimum multicast address.|
|protocol.ts_demand|string|false|none||Whether to enable demand for TS.|
|protocol.enable_fmp4|string|false|none||Whether to enable FMP4 for the protocol.|
|rtsp.lowLatency|string|false|none||Whether to enable low latency for RTSP.|
|http.allow_ip_range|string|false|none||The IP range to allow for HTTP requests.|
|hook.on_rtsp_realm|string|false|none||The action to take when the RTSP realm is accessed in hooks.|
|hook.on_stream_changed|string|false|none||The action to take when a stream is changed in hooks.|
|http.forwarded_ip_header|string|false|none||The header to use for forwarded IP addresses in HTTP requests.|
|rtp_proxy.h265_pt|string|false|none||The H.265 payload type used by the RTP proxy.|
|hook.on_del_mp4|string|false|none||The action to take when an MP4 file is deleted in hooks.|
|protocol.enable_hls|string|false|none||Whether to enable HLS for the protocol.|
|protocol.enable_mp4|string|false|none||Whether to enable MP4 for the protocol.|
|rtc.port|string|false|none||The port used for RTC.|
|protocol.fmp4_demand|string|false|none||Whether to enable demand for FMP4.|
|record.sampleMS|string|false|none||The time in milliseconds for each sample during recording.|
|shell.port|string|false|none||The port used for the shell.|
|hook.on_shell_login|string|false|none||The action to take when logging in to the shell.|
|cluster.retry_count|string|false|none||The number of times to retry connecting to the cluster.|
|general.enableVhost|string|false|none||Whether to enable virtual hosts.|
|general.unready_frame_cache|string|false|none||The size of the unready frame cache.|
|rtc.preferredCodecV|string|false|none||The preferred video codec for RTC.|
|rtp_proxy.h264_pt|string|false|none||The H.264 payload type used by the RTP proxy.|
|protocol.auto_close|string|false|none||Whether to automatically close connections for the protocol.|
|srt.latencyMul|string|false|none||The latency multiplier for SRT.|
|hook.on_server_exited|string|false|none||The action to take when the server is exited.|
|general.resetWhenRePlay|string|false|none||Whether to reset when replaying.|
|protocol.mp4_save_path|string|false|none||The save path for MP4 files in the protocol.|
|protocol.continue_push_ms|string|false|none||The time in milliseconds for continuing to push data for the protocol.|
|rtp_proxy.dumpDir|string|false|none||The dump directory for the RTP proxy.|
|rtp_proxy.ps_pt|string|false|none||The payload type used for PS in the RTP proxy.|
|hook.enable|string|false|none||Whether to enable hooks.|
|rtc.timeoutSec|string|false|none||The timeout value for RTC in seconds.|
|rtc.preferredCodecA|string|false|none||The preferred audio codec for RTC.|
|hls.segKeep|string|false|none||The number of HLS segments to keep.|
|multicast.udpTTL|string|false|none||The TTL value for UDP multicast.|
|rtp.h264_stap_a|string|false|none||Whether to enable STAP-A for H.264 in RTP.|
|hook.on_stream_none_reader|string|false|none||The action to take when there are no stream readers in hooks.|
|hook.on_record_ts|string|false|none||The action to take when recording TS files in hooks.|
|ffmpeg.bin|string|false|none||The path to the FFmpeg binary.|
|protocol.enable_ts|string|false|none||Whether to enable demand for TS in the protocol.|
|protocol.enable_hls_fmp4|string|false|none||Whether to enable HLS FMP4 in the protocol.|
|hls.segNum|string|false|none||The number of HLS segments.|
|http.maxReqSize|string|false|none||The maximum size of requests for HTTP.|
|rtc.tcpPort|string|false|none||The TCP port used for RTC.|
|cluster.timeout_sec|string|false|none||The timeout value for the cluster in seconds.|
|general.enable_ffmpeg_log|string|false|none||Whether to enable FFmpeg logging.|
|general.mediaServerId|string|false|none||The ID of the media server.|
|hook.on_http_access|string|false|none||The action to take when accessing HTTP in hooks.|
|general.wait_track_ready_ms|string|false|none||The time in milliseconds to wait for a track to be ready.|
|rtsp.authBasic|string|false|none||Whether to enable basic authentication for RTSP.|
|hook.on_rtsp_auth|string|false|none||The action to take when authenticating RTSP in hooks.|
|protocol.rtmp_demand|string|false|none||Whether to enable demand for RTMP in the protocol.|
|protocol.add_mute_audio|string|false|none||Whether to add mute audio for the protocol.|
|record.fileBufSize|string|false|none||The buffer size for recording files.|
|rtmp.keepAliveSecond|string|false|none||The time in seconds to keep the RTMP connection alive.|
|hook.on_play|string|false|none||The action to take when playing a stream in hooks.|

<h2 id="tocS_AjaxResultMapObject">AjaxResultMapObject</h2>

<a id="schemaajaxresultmapobject"></a>
<a id="schema_AjaxResultMapObject"></a>
<a id="tocSajaxresultmapobject"></a>
<a id="tocsajaxresultmapobject"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseServerNodeConfig">ServerResponseServerNodeConfig</h2>

<a id="schemaserverresponseservernodeconfig"></a>
<a id="schema_ServerResponseServerNodeConfig"></a>
<a id="tocSserverresponseservernodeconfig"></a>
<a id="tocsserverresponseservernodeconfig"></a>

```json
{
  "code": 0,
  "data": {
    "rtp.rtpMaxSize": "string",
    "protocol.hls_demand": "string",
    "rtp_proxy.opus_pt": "string",
    "rtp_proxy.timeoutSec": "string",
    "rtmp.port": "string",
    "hook.on_ip_not_found": "string",
    "record.fileRepeat": "string",
    "general.flowThreshold": "string",
    "rtsp.rtpTransportType": "string",
    "hook.retry_delay": "string",
    "http.rootPath": "string",
    "rtsp.keepAliveSecond": "string",
    "hook.on_server_started": "string",
    "api.defaultSnap": "string",
    "cluster.origin_url": "string",
    "http.port": "string",
    "http.virtualPath": "string",
    "http.keepAliveSecond": "string",
    "ffmpeg.log": "string",
    "hook.on_flow_report": "string",
    "http.dirMenu": "string",
    "rtsp.directProxy": "string",
    "ffmpeg.cmd": "string",
    "rtp.lowLatency": "string",
    "protocol.enable_rtsp": "string",
    "rtsp.port": "string",
    "rtmp.sslport": "string",
    "protocol.hls_save_path": "string",
    "http.charSet": "string",
    "http.sendBufSize": "string",
    "hls.broadcastRecordTs": "string",
    "api.apiDebug": "string",
    "general.mergeWriteMS": "string",
    "http.forbidCacheSuffix": "string",
    "http.notFound": "string",
    "hook.retry": "string",
    "record.appName": "string",
    "hls.fileBufSize": "string",
    "hook.timeoutSec": "string",
    "rtsp.sslport": "string",
    "hls.deleteDelaySec": "string",
    "hook.on_rtp_server_timeout": "string",
    "hook.on_send_rtp_stopped": "string",
    "hook.on_record_mp4": "string",
    "hook.alive_interval": "string",
    "rtmp.handshakeSecond": "string",
    "hook.stream_changed_schemas": "string",
    "rtc.externIP": "string",
    "rtc.rembBitRate": "string",
    "general.streamNoneReaderDelayMS": "string",
    "protocol.mp4_max_second": "string",
    "hook.on_publish": "string",
    "rtp_proxy.port": "string",
    "http.sslport": "string",
    "rtp.audioMtuSize": "string",
    "general.check_nvidia_dev": "string",
    "record.fastStart": "string",
    "hook.on_stream_not_found": "string",
    "rtp_proxy.port_range": "string",
    "protocol.enable_rtmp": "string",
    "srt.timeoutSec": "string",
    "rtsp.handshakeSecond": "string",
    "hls.segDur": "string",
    "protocol.mp4_as_player": "string",
    "api.secret": "string",
    "hls.segRetain": "string",
    "protocol.rtsp_demand": "string",
    "srt.port": "string",
    "srt.pktBufSize": "string",
    "rtp_proxy.gop_cache": "string",
    "shell.maxReqSize": "string",
    "ffmpeg.snap": "string",
    "general.maxStreamWaitMS": "string",
    "multicast.addrMax": "string",
    "general.wait_add_track_ms": "string",
    "http.allow_cross_domains": "string",
    "protocol.modify_stamp": "string",
    "rtp.videoMtuSize": "string",
    "api.snapRoot": "string",
    "protocol.enable_audio": "string",
    "hook.on_server_keepalive": "string",
    "multicast.addrMin": "string",
    "protocol.ts_demand": "string",
    "protocol.enable_fmp4": "string",
    "rtsp.lowLatency": "string",
    "http.allow_ip_range": "string",
    "hook.on_rtsp_realm": "string",
    "hook.on_stream_changed": "string",
    "http.forwarded_ip_header": "string",
    "rtp_proxy.h265_pt": "string",
    "hook.on_del_mp4": "string",
    "protocol.enable_hls": "string",
    "protocol.enable_mp4": "string",
    "rtc.port": "string",
    "protocol.fmp4_demand": "string",
    "record.sampleMS": "string",
    "shell.port": "string",
    "hook.on_shell_login": "string",
    "cluster.retry_count": "string",
    "general.enableVhost": "string",
    "general.unready_frame_cache": "string",
    "rtc.preferredCodecV": "string",
    "rtp_proxy.h264_pt": "string",
    "protocol.auto_close": "string",
    "srt.latencyMul": "string",
    "hook.on_server_exited": "string",
    "general.resetWhenRePlay": "string",
    "protocol.mp4_save_path": "string",
    "protocol.continue_push_ms": "string",
    "rtp_proxy.dumpDir": "string",
    "rtp_proxy.ps_pt": "string",
    "hook.enable": "string",
    "rtc.timeoutSec": "string",
    "rtc.preferredCodecA": "string",
    "hls.segKeep": "string",
    "multicast.udpTTL": "string",
    "rtp.h264_stap_a": "string",
    "hook.on_stream_none_reader": "string",
    "hook.on_record_ts": "string",
    "ffmpeg.bin": "string",
    "protocol.enable_ts": "string",
    "protocol.enable_hls_fmp4": "string",
    "hls.segNum": "string",
    "http.maxReqSize": "string",
    "rtc.tcpPort": "string",
    "cluster.timeout_sec": "string",
    "general.enable_ffmpeg_log": "string",
    "general.mediaServerId": "string",
    "hook.on_http_access": "string",
    "general.wait_track_ready_ms": "string",
    "rtsp.authBasic": "string",
    "hook.on_rtsp_auth": "string",
    "protocol.rtmp_demand": "string",
    "protocol.add_mute_audio": "string",
    "record.fileBufSize": "string",
    "rtmp.keepAliveSecond": "string",
    "hook.on_play": "string"
  },
  "msg": "string",
  "result": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[ServerNodeConfig](#schemaservernodeconfig)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_ServerResponseListServerNodeConfig">ServerResponseListServerNodeConfig</h2>

<a id="schemaserverresponselistservernodeconfig"></a>
<a id="schema_ServerResponseListServerNodeConfig"></a>
<a id="tocSserverresponselistservernodeconfig"></a>
<a id="tocsserverresponselistservernodeconfig"></a>

```json
{
  "code": 0,
  "data": [
    {
      "rtp.rtpMaxSize": "string",
      "protocol.hls_demand": "string",
      "rtp_proxy.opus_pt": "string",
      "rtp_proxy.timeoutSec": "string",
      "rtmp.port": "string",
      "hook.on_ip_not_found": "string",
      "record.fileRepeat": "string",
      "general.flowThreshold": "string",
      "rtsp.rtpTransportType": "string",
      "hook.retry_delay": "string",
      "http.rootPath": "string",
      "rtsp.keepAliveSecond": "string",
      "hook.on_server_started": "string",
      "api.defaultSnap": "string",
      "cluster.origin_url": "string",
      "http.port": "string",
      "http.virtualPath": "string",
      "http.keepAliveSecond": "string",
      "ffmpeg.log": "string",
      "hook.on_flow_report": "string",
      "http.dirMenu": "string",
      "rtsp.directProxy": "string",
      "ffmpeg.cmd": "string",
      "rtp.lowLatency": "string",
      "protocol.enable_rtsp": "string",
      "rtsp.port": "string",
      "rtmp.sslport": "string",
      "protocol.hls_save_path": "string",
      "http.charSet": "string",
      "http.sendBufSize": "string",
      "hls.broadcastRecordTs": "string",
      "api.apiDebug": "string",
      "general.mergeWriteMS": "string",
      "http.forbidCacheSuffix": "string",
      "http.notFound": "string",
      "hook.retry": "string",
      "record.appName": "string",
      "hls.fileBufSize": "string",
      "hook.timeoutSec": "string",
      "rtsp.sslport": "string",
      "hls.deleteDelaySec": "string",
      "hook.on_rtp_server_timeout": "string",
      "hook.on_send_rtp_stopped": "string",
      "hook.on_record_mp4": "string",
      "hook.alive_interval": "string",
      "rtmp.handshakeSecond": "string",
      "hook.stream_changed_schemas": "string",
      "rtc.externIP": "string",
      "rtc.rembBitRate": "string",
      "general.streamNoneReaderDelayMS": "string",
      "protocol.mp4_max_second": "string",
      "hook.on_publish": "string",
      "rtp_proxy.port": "string",
      "http.sslport": "string",
      "rtp.audioMtuSize": "string",
      "general.check_nvidia_dev": "string",
      "record.fastStart": "string",
      "hook.on_stream_not_found": "string",
      "rtp_proxy.port_range": "string",
      "protocol.enable_rtmp": "string",
      "srt.timeoutSec": "string",
      "rtsp.handshakeSecond": "string",
      "hls.segDur": "string",
      "protocol.mp4_as_player": "string",
      "api.secret": "string",
      "hls.segRetain": "string",
      "protocol.rtsp_demand": "string",
      "srt.port": "string",
      "srt.pktBufSize": "string",
      "rtp_proxy.gop_cache": "string",
      "shell.maxReqSize": "string",
      "ffmpeg.snap": "string",
      "general.maxStreamWaitMS": "string",
      "multicast.addrMax": "string",
      "general.wait_add_track_ms": "string",
      "http.allow_cross_domains": "string",
      "protocol.modify_stamp": "string",
      "rtp.videoMtuSize": "string",
      "api.snapRoot": "string",
      "protocol.enable_audio": "string",
      "hook.on_server_keepalive": "string",
      "multicast.addrMin": "string",
      "protocol.ts_demand": "string",
      "protocol.enable_fmp4": "string",
      "rtsp.lowLatency": "string",
      "http.allow_ip_range": "string",
      "hook.on_rtsp_realm": "string",
      "hook.on_stream_changed": "string",
      "http.forwarded_ip_header": "string",
      "rtp_proxy.h265_pt": "string",
      "hook.on_del_mp4": "string",
      "protocol.enable_hls": "string",
      "protocol.enable_mp4": "string",
      "rtc.port": "string",
      "protocol.fmp4_demand": "string",
      "record.sampleMS": "string",
      "shell.port": "string",
      "hook.on_shell_login": "string",
      "cluster.retry_count": "string",
      "general.enableVhost": "string",
      "general.unready_frame_cache": "string",
      "rtc.preferredCodecV": "string",
      "rtp_proxy.h264_pt": "string",
      "protocol.auto_close": "string",
      "srt.latencyMul": "string",
      "hook.on_server_exited": "string",
      "general.resetWhenRePlay": "string",
      "protocol.mp4_save_path": "string",
      "protocol.continue_push_ms": "string",
      "rtp_proxy.dumpDir": "string",
      "rtp_proxy.ps_pt": "string",
      "hook.enable": "string",
      "rtc.timeoutSec": "string",
      "rtc.preferredCodecA": "string",
      "hls.segKeep": "string",
      "multicast.udpTTL": "string",
      "rtp.h264_stap_a": "string",
      "hook.on_stream_none_reader": "string",
      "hook.on_record_ts": "string",
      "ffmpeg.bin": "string",
      "protocol.enable_ts": "string",
      "protocol.enable_hls_fmp4": "string",
      "hls.segNum": "string",
      "http.maxReqSize": "string",
      "rtc.tcpPort": "string",
      "cluster.timeout_sec": "string",
      "general.enable_ffmpeg_log": "string",
      "general.mediaServerId": "string",
      "hook.on_http_access": "string",
      "general.wait_track_ready_ms": "string",
      "rtsp.authBasic": "string",
      "hook.on_rtsp_auth": "string",
      "protocol.rtmp_demand": "string",
      "protocol.add_mute_audio": "string",
      "record.fileBufSize": "string",
      "rtmp.keepAliveSecond": "string",
      "hook.on_play": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[[ServerNodeConfig](#schemaservernodeconfig)]|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_StreamContent">StreamContent</h2>

<a id="schemastreamcontent"></a>
<a id="schema_StreamContent"></a>
<a id="tocSstreamcontent"></a>
<a id="tocsstreamcontent"></a>

```json
{
  "app": "string",
  "stream": "string",
  "ip": "string",
  "flv": "string",
  "https_flv": "string",
  "ws_flv": "string",
  "wss_flv": "string",
  "fmp4": "string",
  "https_fmp4": "string",
  "ws_fmp4": "string",
  "wss_fmp4": "string",
  "hls": "string",
  "https_hls": "string",
  "ws_hls": "string",
  "wss_hls": "string",
  "ts": "string",
  "https_ts": "string",
  "ws_ts": "string",
  "wss_ts": "string",
  "rtmp": "string",
  "rtmps": "string",
  "rtsp": "string",
  "rtsps": "string",
  "rtc": "string",
  "rtcs": "string",
  "mediaServerId": "string",
  "tracks": {},
  "startTime": "string",
  "endTime": "string",
  "progress": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|app|string|false|none||none|
|stream|string|false|none||none|
|ip|string|false|none||none|
|flv|string|false|none||none|
|https_flv|string|false|none||none|
|ws_flv|string|false|none||none|
|wss_flv|string|false|none||none|
|fmp4|string|false|none||none|
|https_fmp4|string|false|none||none|
|ws_fmp4|string|false|none||none|
|wss_fmp4|string|false|none||none|
|hls|string|false|none||none|
|https_hls|string|false|none||none|
|ws_hls|string|false|none||none|
|wss_hls|string|false|none||none|
|ts|string|false|none||none|
|https_ts|string|false|none||none|
|ws_ts|string|false|none||none|
|wss_ts|string|false|none||none|
|rtmp|string|false|none||none|
|rtmps|string|false|none||none|
|rtsp|string|false|none||none|
|rtsps|string|false|none||none|
|rtc|string|false|none||none|
|rtcs|string|false|none||none|
|mediaServerId|string|false|none||none|
|tracks|object|false|none||none|
|startTime|string|false|none||none|
|endTime|string|false|none||none|
|progress|number|false|none||none|

<h2 id="tocS_MapObject">MapObject</h2>

<a id="schemamapobject"></a>
<a id="schema_MapObject"></a>
<a id="tocSmapobject"></a>
<a id="tocsmapobject"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseString">ServerResponseString</h2>

<a id="schemaserverresponsestring"></a>
<a id="schema_ServerResponseString"></a>
<a id="tocSserverresponsestring"></a>
<a id="tocsserverresponsestring"></a>

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|string|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_AjaxResultLong">AjaxResultLong</h2>

<a id="schemaajaxresultlong"></a>
<a id="schema_AjaxResultLong"></a>
<a id="tocSajaxresultlong"></a>
<a id="tocsajaxresultlong"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_OnStreamChangedHookParam">OnStreamChangedHookParam</h2>

<a id="schemaonstreamchangedhookparam"></a>
<a id="schema_OnStreamChangedHookParam"></a>
<a id="tocSonstreamchangedhookparam"></a>
<a id="tocsonstreamchangedhookparam"></a>

```json
{
  "mediaServerId": "string",
  "regist": true,
  "app": "string",
  "stream": "string",
  "callId": "string",
  "totalReaderCount": "string",
  "schema": "string",
  "originType": 0,
  "originSock": {
    "identifier": "string",
    "local_ip": "string",
    "local_port": 0,
    "peer_ip": "string",
    "peer_port": 0,
    "typeid": "string"
  },
  "originTypeStr": "string",
  "originUrl": "string",
  "severId": "string",
  "createStamp": 0,
  "aliveSecond": 0,
  "bytesSpeed": 0,
  "tracks": [
    {
      "channels": 0,
      "codec_id": 0,
      "codec_id_name": "string",
      "codec_type": 0,
      "fps": 0,
      "height": 0,
      "ready": true,
      "width": 0,
      "frames": 0,
      "sample_bit": 0,
      "sample_rate": 0,
      "gop_interval_ms": 0,
      "gop_size": 0,
      "key_frames": 0,
      "duration": 0,
      "loss": 0
    }
  ],
  "vhost": "string",
  "docker": true,
  "streamInfo": {
    "app": "string",
    "stream": "string",
    "ip": "string",
    "flv": "string",
    "https_flv": "string",
    "ws_flv": "string",
    "wss_flv": "string",
    "fmp4": "string",
    "https_fmp4": "string",
    "ws_fmp4": "string",
    "wss_fmp4": "string",
    "hls": "string",
    "https_hls": "string",
    "ws_hls": "string",
    "wss_hls": "string",
    "ts": "string",
    "https_ts": "string",
    "ws_ts": "string",
    "wss_ts": "string",
    "rtmp": "string",
    "rtmps": "string",
    "rtsp": "string",
    "rtsps": "string",
    "rtc": "string",
    "rtcs": "string",
    "mediaServerId": "string",
    "tracks": {},
    "startTime": "string",
    "endTime": "string",
    "progress": 0
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|
|regist|boolean|false|none||注册/注销|
|app|string|false|none||应用名|
|stream|string|false|none||流id|
|callId|string|false|none||推流鉴权Id|
|totalReaderCount|string|false|none||观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv|
|schema|string|false|none||协议 包括hls/rtsp/rtmp/http-flv/ws-flv|
|originType|integer|false|none||产生源类型，<br />unknown = 0,<br />rtmp_push=1,<br />rtsp_push=2,<br />rtp_push=3,<br />pull=4,<br />ffmpeg_pull=5,<br />mp4_vod=6,<br />device_chn=7|
|originSock|[MediaPlayer](#schemamediaplayer)|false|none||客户端和服务器网络信息，可能为null类型|
|originTypeStr|string|false|none||产生源类型的字符串描述|
|originUrl|string|false|none||产生源的url|
|severId|string|false|none||服务器id|
|createStamp|integer(int64)|false|none||GMT unix系统时间戳，单位秒|
|aliveSecond|integer(int64)|false|none||存活时间，单位秒|
|bytesSpeed|integer(int64)|false|none||数据产生速度，单位byte/s|
|tracks|[[Track](#schematrack)]|false|none||音视频轨道|
|vhost|string|false|none||音视频轨道|
|docker|boolean|false|none||是否是docker部署， docker部署不会自动更新zlm使用的端口，需要自己手动修改|
|streamInfo|[StreamContent](#schemastreamcontent)|false|none||none|

<h2 id="tocS_ExtendInfoReq">ExtendInfoReq</h2>

<a id="schemaextendinforeq"></a>
<a id="schema_ExtendInfoReq"></a>
<a id="tocSextendinforeq"></a>
<a id="tocsextendinforeq"></a>

```json
{
  "channelInfo": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|channelInfo|string|false|none||设备通道信息|

<h2 id="tocS_MapString">MapString</h2>

<a id="schemamapstring"></a>
<a id="schema_MapString"></a>
<a id="tocSmapstring"></a>
<a id="tocsmapstring"></a>

```json
{
  "key": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|string|false|none||none|

<h2 id="tocS_HookResultForStreamNoneReader">HookResultForStreamNoneReader</h2>

<a id="schemahookresultforstreamnonereader"></a>
<a id="schema_HookResultForStreamNoneReader"></a>
<a id="tocShookresultforstreamnonereader"></a>
<a id="tocshookresultforstreamnonereader"></a>

```json
{
  "code": 0,
  "msg": "string",
  "close": true
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||错误代码，0代表允许推流|
|msg|string|false|none||不允许推流时的错误提示|
|close|boolean|false|none||none|

<h2 id="tocS_ServerResponseObject">ServerResponseObject</h2>

<a id="schemaserverresponseobject"></a>
<a id="schema_ServerResponseObject"></a>
<a id="tocSserverresponseobject"></a>
<a id="tocsserverresponseobject"></a>

```json
{
  "code": 0,
  "data": {},
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|object|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_OnStreamNoneReaderHookParam">OnStreamNoneReaderHookParam</h2>

<a id="schemaonstreamnonereaderhookparam"></a>
<a id="schema_OnStreamNoneReaderHookParam"></a>
<a id="tocSonstreamnonereaderhookparam"></a>
<a id="tocsonstreamnonereaderhookparam"></a>

```json
{
  "mediaServerId": "string",
  "schema": "string",
  "app": "string",
  "stream": "string",
  "vhost": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|
|schema|string|false|none||none|
|app|string|false|none||none|
|stream|string|false|none||none|
|vhost|string|false|none||none|

<h2 id="tocS_DeviceChannelCreateReq">DeviceChannelCreateReq</h2>

<a id="schemadevicechannelcreatereq"></a>
<a id="schema_DeviceChannelCreateReq"></a>
<a id="tocSdevicechannelcreatereq"></a>
<a id="tocsdevicechannelcreatereq"></a>

```json
{
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "extendInfo": {
    "channelInfo": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|channelId|string|false|none||通道Id|
|deviceId|string|false|none||设备ID|
|name|string|false|none||通道名称|
|extendInfo|[ExtendInfoReq](#schemaextendinforeq)|false|none||扩展信息|

<h2 id="tocS_ExtendInfoReq1">ExtendInfoReq1</h2>

<a id="schemaextendinforeq1"></a>
<a id="schema_ExtendInfoReq1"></a>
<a id="tocSextendinforeq1"></a>
<a id="tocsextendinforeq1"></a>

```json
{
  "channelInfo": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|channelInfo|string|false|none||设备通道信息|

<h2 id="tocS_MediaPlayer">MediaPlayer</h2>

<a id="schemamediaplayer"></a>
<a id="schema_MediaPlayer"></a>
<a id="tocSmediaplayer"></a>
<a id="tocsmediaplayer"></a>

```json
{
  "identifier": "string",
  "local_ip": "string",
  "local_port": 0,
  "peer_ip": "string",
  "peer_port": 0,
  "typeid": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|identifier|string|false|none||The unique identifier for the session, e.g., "3-309".|
|local_ip|string|false|none||The local IP address. "::" is a shorthand in IPv6 for representing multiple groups of zeros.|
|local_port|integer|false|none||The local port number.|
|peer_ip|string|false|none||The IP address of the peer in the session.|
|peer_port|integer|false|none||The port number of the peer.|
|typeid|string|false|none||The type identifier for the session, indicating it's a WebRTC session from MediaKit.|

<h2 id="tocS_OnStreamNotFoundHookParam">OnStreamNotFoundHookParam</h2>

<a id="schemaonstreamnotfoundhookparam"></a>
<a id="schema_OnStreamNotFoundHookParam"></a>
<a id="tocSonstreamnotfoundhookparam"></a>
<a id="tocsonstreamnotfoundhookparam"></a>

```json
{
  "mediaServerId": "string",
  "id": "string",
  "app": "string",
  "stream": "string",
  "ip": "string",
  "params": "string",
  "port": 0,
  "schema": "string",
  "vhost": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|
|id|string|false|none||app	string	流应用名<br />id	string	TCP链接唯一ID<br />ip	string	播放器ip<br />params	string	播放url参数<br />port	unsigned short	播放器端口号<br />schema	string	播放的协议，可能是rtsp、rtmp、http<br />stream	string	流ID<br />vhost	string	流虚拟主机<br />mediaServerId	string	服务器id,通过配置文件设置|
|app|string|false|none||none|
|stream|string|false|none||none|
|ip|string|false|none||none|
|params|string|false|none||none|
|port|integer|false|none||none|
|schema|string|false|none||none|
|vhost|string|false|none||none|

<h2 id="tocS_Track">Track</h2>

<a id="schematrack"></a>
<a id="schema_Track"></a>
<a id="tocStrack"></a>
<a id="tocstrack"></a>

```json
{
  "channels": 0,
  "codec_id": 0,
  "codec_id_name": "string",
  "codec_type": 0,
  "fps": 0,
  "height": 0,
  "ready": true,
  "width": 0,
  "frames": 0,
  "sample_bit": 0,
  "sample_rate": 0,
  "gop_interval_ms": 0,
  "gop_size": 0,
  "key_frames": 0,
  "duration": 0,
  "loss": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|channels|integer|false|none||音频通道数。|
|codec_id|integer|false|none||编码ID。H264 = 0, H265 = 1, AAC = 2, G711A = 3, G711U = 4。|
|codec_id_name|string|false|none||编码类型的名称。|
|codec_type|integer|false|none||类型。视频 = 0, 音频 = 1。|
|fps|number|false|none||视频的帧率。|
|height|integer|false|none||视频的高度。|
|ready|boolean|false|none||轨道是否准备就绪。|
|width|integer|false|none||视频的宽度。|
|frames|integer|false|none||累计接收帧数|
|sample_bit|integer|false|none||音频采样位数|
|sample_rate|integer|false|none||音频采样率|
|gop_interval_ms|integer|false|none||gop间隔时间，单位毫秒|
|gop_size|integer|false|none||gop大小，单位帧数|
|key_frames|integer|false|none||累计接收关键帧数|
|duration|integer(int64)|false|none||轨道时长，单位毫秒|
|loss|number|false|none||丢包率，-1表示未知|

<h2 id="tocS_DeviceChannelUpdateReq">DeviceChannelUpdateReq</h2>

<a id="schemadevicechannelupdatereq"></a>
<a id="schema_DeviceChannelUpdateReq"></a>
<a id="tocSdevicechannelupdatereq"></a>
<a id="tocsdevicechannelupdatereq"></a>

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0,
  "extendInfo": {
    "channelInfo": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|true|none||主键ID（更新必需）|
|channelId|string|false|none||通道Id|
|deviceId|string|false|none||设备ID|
|name|string|false|none||通道名称|
|status|integer|false|none||状态 1在线 0离线|
|extendInfo|[ExtendInfoReq1](#schemaextendinforeq1)|false|none||扩展信息|

<h2 id="tocS_MediaData">MediaData</h2>

<a id="schemamediadata"></a>
<a id="schema_MediaData"></a>
<a id="tocSmediadata"></a>
<a id="tocsmediadata"></a>

```json
{
  "app": "string",
  "readerCount": 0,
  "totalReaderCount": 0,
  "schema": "string",
  "stream": "string",
  "originSock": {
    "identifier": "string",
    "local_ip": "string",
    "local_port": 0,
    "peer_ip": "string",
    "peer_port": 0,
    "typeid": "string"
  },
  "originType": 0,
  "originTypeStr": "string",
  "originUrl": "string",
  "createStamp": 0,
  "aliveSecond": 0,
  "bytesSpeed": 0,
  "tracks": [
    {
      "channels": 0,
      "codec_id": 0,
      "codec_id_name": "string",
      "codec_type": 0,
      "fps": 0,
      "height": 0,
      "ready": true,
      "width": 0,
      "frames": 0,
      "sample_bit": 0,
      "sample_rate": 0,
      "gop_interval_ms": 0,
      "gop_size": 0,
      "key_frames": 0,
      "duration": 0,
      "loss": 0
    }
  ],
  "vhost": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|app|string|false|none||应用名|
|readerCount|integer|false|none||本协议观看人数|
|totalReaderCount|integer|false|none||观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv|
|schema|string|false|none||协议|
|stream|string|false|none||流id|
|originSock|[MediaPlayer](#schemamediaplayer)|false|none||客户端和服务器网络信息，可能为null类型|
|originType|integer|false|none||产生源类型，包括 unknown = 0,rtmp_push=1,rtsp_push=2,rtp_push=3,pull=4,ffmpeg_pull=5,mp4_vod=6,device_chn=7|
|originTypeStr|string|false|none||none|
|originUrl|string|false|none||产生源的url|
|createStamp|integer(int64)|false|none||GMT unix系统时间戳，单位秒|
|aliveSecond|integer|false|none||存活时间，单位秒|
|bytesSpeed|integer|false|none||数据产生速度，单位byte/s|
|tracks|[[Track](#schematrack)]|false|none||音视频轨道|
|vhost|string|false|none||虚拟主机名|

<h2 id="tocS_HookParam">HookParam</h2>

<a id="schemahookparam"></a>
<a id="schema_HookParam"></a>
<a id="tocShookparam"></a>
<a id="tocshookparam"></a>

```json
{
  "mediaServerId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|

<h2 id="tocS_AjaxResultDeviceChannelVO">AjaxResultDeviceChannelVO</h2>

<a id="schemaajaxresultdevicechannelvo"></a>
<a id="schema_AjaxResultDeviceChannelVO"></a>
<a id="tocSajaxresultdevicechannelvo"></a>
<a id="tocsajaxresultdevicechannelvo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseListMediaData">ServerResponseListMediaData</h2>

<a id="schemaserverresponselistmediadata"></a>
<a id="schema_ServerResponseListMediaData"></a>
<a id="tocSserverresponselistmediadata"></a>
<a id="tocsserverresponselistmediadata"></a>

```json
{
  "code": 0,
  "data": [
    {
      "app": "string",
      "readerCount": 0,
      "totalReaderCount": 0,
      "schema": "string",
      "stream": "string",
      "originSock": {
        "identifier": "string",
        "local_ip": "string",
        "local_port": 0,
        "peer_ip": "string",
        "peer_port": 0,
        "typeid": "string"
      },
      "originType": 0,
      "originTypeStr": "string",
      "originUrl": "string",
      "createStamp": 0,
      "aliveSecond": 0,
      "bytesSpeed": 0,
      "tracks": [
        {
          "channels": 0,
          "codec_id": 0,
          "codec_id_name": "string",
          "codec_type": 0,
          "fps": 0,
          "height": 0,
          "ready": true,
          "width": 0,
          "frames": 0,
          "sample_bit": 0,
          "sample_rate": 0,
          "gop_interval_ms": 0,
          "gop_size": 0,
          "key_frames": 0,
          "duration": 0,
          "loss": 0
        }
      ],
      "vhost": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[[MediaData](#schemamediadata)]|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_OnSendRtpStoppedHookParam">OnSendRtpStoppedHookParam</h2>

<a id="schemaonsendrtpstoppedhookparam"></a>
<a id="schema_OnSendRtpStoppedHookParam"></a>
<a id="tocSonsendrtpstoppedhookparam"></a>
<a id="tocsonsendrtpstoppedhookparam"></a>

```json
{
  "mediaServerId": "string",
  "app": "string",
  "stream": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|
|app|string|false|none||none|
|stream|string|false|none||none|

<h2 id="tocS_DeviceChannelQueryReq">DeviceChannelQueryReq</h2>

<a id="schemadevicechannelqueryreq"></a>
<a id="schema_DeviceChannelQueryReq"></a>
<a id="tocSdevicechannelqueryreq"></a>
<a id="tocsdevicechannelqueryreq"></a>

```json
{
  "id": 0,
  "channelId": "string",
  "deviceId": "string",
  "name": "string",
  "status": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||数据库主键ID|
|channelId|string|false|none||通道Id|
|deviceId|string|false|none||设备ID|
|name|string|false|none||通道名称|
|status|integer|false|none||状态 1在线 0离线|

<h2 id="tocS_MediaReq">MediaReq</h2>

<a id="schemamediareq"></a>
<a id="schema_MediaReq"></a>
<a id="tocSmediareq"></a>
<a id="tocsmediareq"></a>

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schema|string|false|none||筛选协议，例如 rtsp或rtmp|
|vhost|string|false|none||筛选虚拟主机，例如__defaultVhost__|
|app|string|false|none||筛选应用名，例如 live|
|stream|string|false|none||筛选流id，例如 test|

<h2 id="tocS_OnRtpServerTimeoutHookParam">OnRtpServerTimeoutHookParam</h2>

<a id="schemaonrtpservertimeouthookparam"></a>
<a id="schema_OnRtpServerTimeoutHookParam"></a>
<a id="tocSonrtpservertimeouthookparam"></a>
<a id="tocsonrtpservertimeouthookparam"></a>

```json
{
  "mediaServerId": "string",
  "local_port": 0,
  "stream_id": "string",
  "tcpMode": 0,
  "reUsePort": true,
  "ssrc": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||none|
|local_port|integer|false|none||none|
|stream_id|string|false|none||none|
|tcpMode|integer|false|none||none|
|reUsePort|boolean|false|none||none|
|ssrc|string|false|none||none|

<h2 id="tocS_AjaxResultVoid">AjaxResultVoid</h2>

<a id="schemaajaxresultvoid"></a>
<a id="schema_AjaxResultVoid"></a>
<a id="tocSajaxresultvoid"></a>
<a id="tocsajaxresultvoid"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_"></h2>

<a id="schema"></a>
<a id="schema_"></a>
<a id="tocS"></a>
<a id="tocs"></a>

```json
{}

```

### 属性

*None*

<h2 id="tocS_HookResultForOnHttpAccess">HookResultForOnHttpAccess</h2>

<a id="schemahookresultforonhttpaccess"></a>
<a id="schema_HookResultForOnHttpAccess"></a>
<a id="tocShookresultforonhttpaccess"></a>
<a id="tocshookresultforonhttpaccess"></a>

```json
{
  "err": "string",
  "path": "string",
  "second": 600,
  "code": 0,
  "msg": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|err|string|false|none||不允许访问的错误提示，允许访问请置空|
|path|string|false|none||该客户端能访问或被禁止的顶端目录，如果为空字符串，则表述为当前目录|
|second|integer|false|none||本次授权结果的有效期，单位秒|
|code|integer|false|none||错误代码，0代表允许推流|
|msg|string|false|none||不允许推流时的错误提示|

<h2 id="tocS_AjaxResultDeviceChannelListResp">AjaxResultDeviceChannelListResp</h2>

<a id="schemaajaxresultdevicechannellistresp"></a>
<a id="schema_AjaxResultDeviceChannelListResp"></a>
<a id="tocSajaxresultdevicechannellistresp"></a>
<a id="tocsajaxresultdevicechannellistresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponse">ServerResponse</h2>

<a id="schemaserverresponse"></a>
<a id="schema_ServerResponse"></a>
<a id="tocSserverresponse"></a>
<a id="tocsserverresponse"></a>

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|string|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_AjaxResultBoolean">AjaxResultBoolean</h2>

<a id="schemaajaxresultboolean"></a>
<a id="schema_AjaxResultBoolean"></a>
<a id="tocSajaxresultboolean"></a>
<a id="tocsajaxresultboolean"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_OnHttpAccessParam">OnHttpAccessParam</h2>

<a id="schemaonhttpaccessparam"></a>
<a id="schema_OnHttpAccessParam"></a>
<a id="tocSonhttpaccessparam"></a>
<a id="tocsonhttpaccessparam"></a>

```json
{
  "header.Accept": "string",
  "header.Accept-Encoding": "string",
  "header.Accept-Language": "string",
  "header.Cache-Control": "string",
  "header.Connection": "string",
  "header.Host": "string",
  "header.Upgrade-Insecure-Requests": "string",
  "header.User-Agent": "string",
  "id": "string",
  "ip": "string",
  "is_dir": true,
  "params": "string",
  "path": "string",
  "port": 0,
  "mediaServerId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|header.Accept|string|false|none||接受头|
|header.Accept-Encoding|string|false|none||接受编码头|
|header.Accept-Language|string|false|none||接受语言头|
|header.Cache-Control|string|false|none||缓存控制头|
|header.Connection|string|false|none||连接头|
|header.Host|string|false|none||主机头|
|header.Upgrade-Insecure-Requests|string|false|none||升级不安全请求头|
|header.User-Agent|string|false|none||用户代理头|
|id|string|false|none||ID|
|ip|string|false|none||IP|
|is_dir|boolean|false|none||是否为目录|
|params|string|false|none||参数|
|path|string|false|none||路径|
|port|integer|false|none||端口|
|mediaServerId|string|false|none||none|

<h2 id="tocS_CloseStreamsReq">CloseStreamsReq</h2>

<a id="schemaclosestreamsreq"></a>
<a id="schema_CloseStreamsReq"></a>
<a id="tocSclosestreamsreq"></a>
<a id="tocsclosestreamsreq"></a>

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "force": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schema|string|false|none||筛选协议，例如 rtsp或rtmp|
|vhost|string|false|none||筛选虚拟主机，例如__defaultVhost__|
|app|string|false|none||筛选应用名，例如 live|
|stream|string|false|none||筛选流id，例如 test|
|force|integer|false|none||none|

<h2 id="tocS_HookResultForOnRtspRealm">HookResultForOnRtspRealm</h2>

<a id="schemahookresultforonrtsprealm"></a>
<a id="schema_HookResultForOnRtspRealm"></a>
<a id="tocShookresultforonrtsprealm"></a>
<a id="tocshookresultforonrtsprealm"></a>

```json
{
  "code": 0,
  "msg": "string",
  "realm": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||错误代码，0代表允许推流|
|msg|string|false|none||不允许推流时的错误提示|
|realm|string|false|none||该rtsp流是否需要rtsp专有鉴权，空字符串代码不需要鉴权|

<h2 id="tocS_AjaxResultString">AjaxResultString</h2>

<a id="schemaajaxresultstring"></a>
<a id="schema_AjaxResultString"></a>
<a id="tocSajaxresultstring"></a>
<a id="tocsajaxresultstring"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_MediaOnlineStatus">MediaOnlineStatus</h2>

<a id="schemamediaonlinestatus"></a>
<a id="schema_MediaOnlineStatus"></a>
<a id="tocSmediaonlinestatus"></a>
<a id="tocsmediaonlinestatus"></a>

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success",
  "online": true
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|string|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|
|online|boolean|false|none||none|

<h2 id="tocS_OnRtspRealmHookParam">OnRtspRealmHookParam</h2>

<a id="schemaonrtsprealmhookparam"></a>
<a id="schema_OnRtspRealmHookParam"></a>
<a id="tocSonrtsprealmhookparam"></a>
<a id="tocsonrtsprealmhookparam"></a>

```json
{
  "mediaServerId": "string",
  "app": "string",
  "id": "string",
  "ip": "string",
  "params": "string",
  "port": 0,
  "schema": "string",
  "stream": "string",
  "vhost": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|mediaServerId|string|false|none||服务器id,通过配置文件设置|
|app|string|false|none||流应用名|
|id|string|false|none||TCP链接唯一ID|
|ip|string|false|none||播放器ip|
|params|string|false|none||播放url参数|
|port|integer|false|none||播放器端口号|
|schema|string|false|none||播放的协议，可能是rtsp、rtmp、http|
|stream|string|false|none||流ID|
|vhost|string|false|none||流虚拟主机|

<h2 id="tocS_ExportTaskCreateReq">ExportTaskCreateReq</h2>

<a id="schemaexporttaskcreatereq"></a>
<a id="schema_ExportTaskCreateReq"></a>
<a id="tocSexporttaskcreatereq"></a>
<a id="tocsexporttaskcreatereq"></a>

```json
{
  "bizId": 0,
  "memberCnt": 0,
  "format": "string",
  "applyTime": "string",
  "param": "string",
  "name": "string",
  "type": 0,
  "applyUser": "string",
  "extend": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|bizId|integer(int64)|false|none||任务唯一Id|
|memberCnt|integer(int64)|false|none||导出的记录总数|
|format|string|false|none||文件格式|
|applyTime|string|false|none||申请时间|
|param|string|false|none||搜索条件序列化|
|name|string|false|none||导出名称|
|type|integer|false|none||导出类型|
|applyUser|string|false|none||申请用户|
|extend|string|false|none||扩展字段|

<h2 id="tocS_ServerResponseMediaPlayer">ServerResponseMediaPlayer</h2>

<a id="schemaserverresponsemediaplayer"></a>
<a id="schema_ServerResponseMediaPlayer"></a>
<a id="tocSserverresponsemediaplayer"></a>
<a id="tocsserverresponsemediaplayer"></a>

```json
{
  "code": 0,
  "data": {
    "identifier": "string",
    "local_ip": "string",
    "local_port": 0,
    "peer_ip": "string",
    "peer_port": 0,
    "typeid": "string"
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[MediaPlayer](#schemamediaplayer)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_HookResultForOnRtspAuth">HookResultForOnRtspAuth</h2>

<a id="schemahookresultforonrtspauth"></a>
<a id="schema_HookResultForOnRtspAuth"></a>
<a id="tocShookresultforonrtspauth"></a>
<a id="tocshookresultforonrtspauth"></a>

```json
{
  "encrypted": true,
  "passwd": "string",
  "code": 0,
  "msg": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|encrypted|boolean|false|none||是否加密|
|passwd|string|false|none||密码<br />用户密码明文或摘要(md5(username:realm:password))|
|code|integer|false|none||错误代码，0代表允许推流|
|msg|string|false|none||不允许推流时的错误提示|

<h2 id="tocS_ExportTaskUpdateReq">ExportTaskUpdateReq</h2>

<a id="schemaexporttaskupdatereq"></a>
<a id="schema_ExportTaskUpdateReq"></a>
<a id="tocSexporttaskupdatereq"></a>
<a id="tocsexporttaskupdatereq"></a>

```json
{
  "id": 0,
  "bizId": 0,
  "memberCnt": 0,
  "format": "string",
  "applyTime": "string",
  "exportTime": "string",
  "url": "string",
  "status": 0,
  "expired": 0,
  "param": "string",
  "name": "string",
  "type": 0,
  "applyUser": "string",
  "extend": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||ID自增|
|bizId|integer(int64)|false|none||任务唯一Id|
|memberCnt|integer(int64)|false|none||导出的记录总数|
|format|string|false|none||文件格式|
|applyTime|string|false|none||申请时间|
|exportTime|string|false|none||导出报表时间|
|url|string|false|none||文件下载地址, 多个url用、隔开|
|status|integer|false|none||是否完成，1 -> 完成, 0->处理中, -1 -> 出错|
|expired|integer|false|none||是否过期，1 -> 过期，0 -> 未过期|
|param|string|false|none||搜索条件序列化|
|name|string|false|none||导出名称|
|type|integer|false|none||导出类型|
|applyUser|string|false|none||申请用户|
|extend|string|false|none||扩展字段|

<h2 id="tocS_Track2">Track2</h2>

<a id="schematrack2"></a>
<a id="schema_Track2"></a>
<a id="tocStrack2"></a>
<a id="tocstrack2"></a>

```json
{
  "channels": 0,
  "codec_id": 0,
  "codec_id_name": "string",
  "codec_type": 0,
  "fps": 0,
  "height": 0,
  "ready": true,
  "width": 0,
  "frames": 0,
  "sample_bit": 0,
  "sample_rate": 0,
  "gop_interval_ms": 0,
  "gop_size": 0,
  "key_frames": 0,
  "duration": 0,
  "loss": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|channels|integer|false|none||音频通道数。|
|codec_id|integer|false|none||编码ID。H264 = 0, H265 = 1, AAC = 2, G711A = 3, G711U = 4。|
|codec_id_name|string|false|none||编码类型的名称。|
|codec_type|integer|false|none||类型。视频 = 0, 音频 = 1。|
|fps|number|false|none||视频的帧率。|
|height|integer|false|none||视频的高度。|
|ready|boolean|false|none||轨道是否准备就绪。|
|width|integer|false|none||视频的宽度。|
|frames|integer|false|none||累计接收帧数|
|sample_bit|integer|false|none||音频采样位数|
|sample_rate|integer|false|none||音频采样率|
|gop_interval_ms|integer|false|none||gop间隔时间，单位毫秒|
|gop_size|integer|false|none||gop大小，单位帧数|
|key_frames|integer|false|none||累计接收关键帧数|
|duration|integer(int64)|false|none||轨道时长，单位毫秒|
|loss|number|false|none||丢包率，-1表示未知|

<h2 id="tocS_OnRtspAuthHookParam">OnRtspAuthHookParam</h2>

<a id="schemaonrtspauthhookparam"></a>
<a id="schema_OnRtspAuthHookParam"></a>
<a id="tocSonrtspauthhookparam"></a>
<a id="tocsonrtspauthhookparam"></a>

```json
{
  "app": "string",
  "id": "string",
  "ip": "string",
  "must_no_encrypt": true,
  "params": "string",
  "port": 0,
  "realm": "string",
  "schema": "string",
  "stream": "string",
  "user_name": "string",
  "vhost": "string",
  "mediaServerId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|app|string|false|none||流应用名|
|id|string|false|none||TCP链接唯一ID|
|ip|string|false|none||rtsp播放器ip|
|must_no_encrypt|boolean|false|none||请求的密码是否必须为明文(base64鉴权需要明文密码)|
|params|string|false|none||rtsp url参数|
|port|integer|false|none||rtsp播放器端口号|
|realm|string|false|none||rtsp播放鉴权加密realm|
|schema|string|false|none||rtsp或rtsps|
|stream|string|false|none||流ID|
|user_name|string|false|none||播放用户名|
|vhost|string|false|none||流虚拟主机|
|mediaServerId|string|false|none||none|

<h2 id="tocS_ExtendInfoReq3">ExtendInfoReq3</h2>

<a id="schemaextendinforeq3"></a>
<a id="schema_ExtendInfoReq3"></a>
<a id="tocSextendinforeq3"></a>
<a id="tocsextendinforeq3"></a>

```json
{
  "serialNumber": "string",
  "transport": "string",
  "expires": 0,
  "password": "string",
  "streamMode": "string",
  "charset": "string",
  "deviceInfo": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|serialNumber|string|false|none||设备序列号|
|transport|string|false|none||传输协议 UDP/TCP|
|expires|integer|false|none||注册有效期|
|password|string|false|none||密码|
|streamMode|string|false|none||数据流传输模式|
|charset|string|false|none||编码|
|deviceInfo|string|false|none||设备信息|

<h2 id="tocS_DeviceCreateReq">DeviceCreateReq</h2>

<a id="schemadevicecreatereq"></a>
<a id="schema_DeviceCreateReq"></a>
<a id="tocSdevicecreatereq"></a>
<a id="tocsdevicecreatereq"></a>

```json
{
  "deviceId": "string",
  "name": "string",
  "ip": "string",
  "port": 0,
  "type": 0,
  "subType": 0,
  "protocol": 0,
  "serverIp": "string",
  "extendInfo": {
    "serialNumber": "string",
    "transport": "string",
    "expires": 0,
    "password": "string",
    "streamMode": "string",
    "charset": "string",
    "deviceInfo": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|false|none||设备ID|
|name|string|false|none||自定义名称|
|ip|string|false|none||IP地址|
|port|integer|false|none||端口|
|type|integer|false|none||协议类型{@link DeviceAgreementEnum}|
|subType|integer|false|none||设备种类{@link io.github.lunasaw.voglander.common.enums.DeviceSubTypeEnum}|
|protocol|integer|false|none||设备协议{@link io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum}|
|serverIp|string|false|none||注册节点IP|
|extendInfo|[ExtendInfoReq3](#schemaextendinforeq3)|false|none||扩展信息|

<h2 id="tocS_OnFlowReportHookParam">OnFlowReportHookParam</h2>

<a id="schemaonflowreporthookparam"></a>
<a id="schema_OnFlowReportHookParam"></a>
<a id="tocSonflowreporthookparam"></a>
<a id="tocsonflowreporthookparam"></a>

```json
{
  "app": "string",
  "duration": 0,
  "params": "string",
  "player": true,
  "schema": "string",
  "stream": "string",
  "totalBytes": 0,
  "vhost": "string",
  "ip": "string",
  "port": 0,
  "id": "string",
  "mediaServerId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|app|string|false|none||流应用名|
|duration|integer|false|none||tcp链接维持时间，单位秒|
|params|string|false|none||推流或播放url参数|
|player|boolean|false|none||true为播放器，false为推流器|
|schema|string|false|none||播放或推流的协议，可能是rtsp、rtmp、http|
|stream|string|false|none||流ID|
|totalBytes|integer|false|none||耗费上下行流量总和，单位字节|
|vhost|string|false|none||流虚拟主机|
|ip|string|false|none||客户端ip|
|port|integer|false|none||客户端端口号|
|id|string|false|none||TCP链接唯一ID|
|mediaServerId|string|false|none||none|

<h2 id="tocS_OriginSock">OriginSock</h2>

<a id="schemaoriginsock"></a>
<a id="schema_OriginSock"></a>
<a id="tocSoriginsock"></a>
<a id="tocsoriginsock"></a>

```json
{
  "identifier": "string",
  "local_ip": "string",
  "local_port": 0,
  "peer_ip": "string",
  "peer_port": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|identifier|string|false|none||连接标识符。|
|local_ip|string|false|none||本地IP地址。|
|local_port|integer|false|none||本地端口。|
|peer_ip|string|false|none||对端IP地址。|
|peer_port|integer|false|none||对端端口。|

<h2 id="tocS_MediaInfo">MediaInfo</h2>

<a id="schemamediainfo"></a>
<a id="schema_MediaInfo"></a>
<a id="tocSmediainfo"></a>
<a id="tocsmediainfo"></a>

```json
{
  "readerCount": 0,
  "totalReaderCount": 0,
  "tracks": [
    {
      "channels": 0,
      "codec_id": 0,
      "codec_id_name": "string",
      "codec_type": 0,
      "fps": 0,
      "height": 0,
      "ready": true,
      "width": 0,
      "frames": 0,
      "sample_bit": 0,
      "sample_rate": 0,
      "gop_interval_ms": 0,
      "gop_size": 0,
      "key_frames": 0,
      "duration": 0,
      "loss": 0
    }
  ],
  "aliveSecond": 0,
  "app": "string",
  "bytesSpeed": 0,
  "createStamp": 0,
  "isRecordingHLS": true,
  "isRecordingMP4": true,
  "originSock": {
    "identifier": "string",
    "local_ip": "string",
    "local_port": 0,
    "peer_ip": "string",
    "peer_port": 0
  },
  "originType": 0,
  "originTypeStr": "string",
  "originUrl": "string",
  "params": "string",
  "schema": "string",
  "stream": "string",
  "totalBytes": 0,
  "vhost": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|readerCount|integer|false|none||本协议的观看人数。|
|totalReaderCount|integer|false|none||观看总人数，包括hls/rtsp/rtmp/http-flv/ws-flv。|
|tracks|[[Track2](#schematrack2)]|false|none||轨道列表。|
|aliveSecond|integer|false|none||存活时间，单位秒。|
|app|string|false|none||应用名。|
|bytesSpeed|integer(int64)|false|none||数据产生速度，单位byte/s。|
|createStamp|integer(int64)|false|none||数据产生时间戳。|
|isRecordingHLS|boolean|false|none||是否正在录制HLS。|
|isRecordingMP4|boolean|false|none||是否正在录制MP4。|
|originSock|[OriginSock](#schemaoriginsock)|false|none||源套接字信息。|
|originType|integer|false|none||源类型编号。|
|originTypeStr|string|false|none||源类型字符串。|
|originUrl|string|false|none||源URL。|
|params|string|false|none||参数。|
|schema|string|false|none||协议。|
|stream|string|false|none||流ID。|
|totalBytes|integer(int64)|false|none||累计接收数据总字节数。|
|vhost|string|false|none||虚拟主机。|

<h2 id="tocS_OnRecordMp4HookParam">OnRecordMp4HookParam</h2>

<a id="schemaonrecordmp4hookparam"></a>
<a id="schema_OnRecordMp4HookParam"></a>
<a id="tocSonrecordmp4hookparam"></a>
<a id="tocsonrecordmp4hookparam"></a>

```json
{
  "app": "string",
  "file_name": "string",
  "file_path": "string",
  "file_size": 0,
  "folder": "string",
  "start_time": 0,
  "stream": "string",
  "time_len": 0,
  "url": "string",
  "vhost": "string",
  "mediaServerId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|app|string|false|none||流应用名|
|file_name|string|false|none||文件名|
|file_path|string|false|none||文件绝对路径|
|file_size|integer|false|none||文件大小，单位字节|
|folder|string|false|none||文件所在目录路径|
|start_time|integer|false|none||开始录制时间戳|
|stream|string|false|none||录制的流ID|
|time_len|number|false|none||录制时长，单位秒|
|url|string|false|none||http/rtsp/rtmp点播相对url路径|
|vhost|string|false|none||流虚拟主机|
|mediaServerId|string|false|none||none|

<h2 id="tocS_ExtendInfoReq4">ExtendInfoReq4</h2>

<a id="schemaextendinforeq4"></a>
<a id="schema_ExtendInfoReq4"></a>
<a id="tocSextendinforeq4"></a>
<a id="tocsextendinforeq4"></a>

```json
{
  "serialNumber": "string",
  "transport": "string",
  "expires": 0,
  "password": "string",
  "streamMode": "string",
  "charset": "string",
  "deviceInfo": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|serialNumber|string|false|none||设备序列号|
|transport|string|false|none||传输协议 UDP/TCP|
|expires|integer|false|none||注册有效期|
|password|string|false|none||密码|
|streamMode|string|false|none||数据流传输模式|
|charset|string|false|none||编码|
|deviceInfo|string|false|none||设备信息|

<h2 id="tocS_DeviceUpdateReq">DeviceUpdateReq</h2>

<a id="schemadeviceupdatereq"></a>
<a id="schema_DeviceUpdateReq"></a>
<a id="tocSdeviceupdatereq"></a>
<a id="tocsdeviceupdatereq"></a>

```json
{
  "id": 0,
  "deviceId": "string",
  "name": "string",
  "ip": "string",
  "port": 0,
  "type": 0,
  "subType": 0,
  "protocol": 0,
  "serverIp": "string",
  "status": 0,
  "extendInfo": {
    "serialNumber": "string",
    "transport": "string",
    "expires": 0,
    "password": "string",
    "streamMode": "string",
    "charset": "string",
    "deviceInfo": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||主键ID（更新必需）|
|deviceId|string|false|none||设备ID|
|name|string|false|none||自定义名称|
|ip|string|false|none||IP地址|
|port|integer|false|none||端口|
|type|integer|false|none||协议类型{@link DeviceAgreementEnum}|
|subType|integer|false|none||设备种类{@link io.github.lunasaw.voglander.common.enums.DeviceSubTypeEnum}|
|protocol|integer|false|none||设备协议{@link io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum}|
|serverIp|string|false|none||注册节点IP|
|status|integer|false|none||状态 1在线 0离线|
|extendInfo|[ExtendInfoReq4](#schemaextendinforeq4)|false|none||扩展信息|

<h2 id="tocS_ServerResponseMediaInfo">ServerResponseMediaInfo</h2>

<a id="schemaserverresponsemediainfo"></a>
<a id="schema_ServerResponseMediaInfo"></a>
<a id="tocSserverresponsemediainfo"></a>
<a id="tocsserverresponsemediainfo"></a>

```json
{
  "code": 0,
  "data": {
    "readerCount": 0,
    "totalReaderCount": 0,
    "tracks": [
      {
        "channels": 0,
        "codec_id": 0,
        "codec_id_name": "string",
        "codec_type": 0,
        "fps": 0,
        "height": 0,
        "ready": true,
        "width": 0,
        "frames": 0,
        "sample_bit": 0,
        "sample_rate": 0,
        "gop_interval_ms": 0,
        "gop_size": 0,
        "key_frames": 0,
        "duration": 0,
        "loss": 0
      }
    ],
    "aliveSecond": 0,
    "app": "string",
    "bytesSpeed": 0,
    "createStamp": 0,
    "isRecordingHLS": true,
    "isRecordingMP4": true,
    "originSock": {
      "identifier": "string",
      "local_ip": "string",
      "local_port": 0,
      "peer_ip": "string",
      "peer_port": 0
    },
    "originType": 0,
    "originTypeStr": "string",
    "originUrl": "string",
    "params": "string",
    "schema": "string",
    "stream": "string",
    "totalBytes": 0,
    "vhost": "string"
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[MediaInfo](#schemamediainfo)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_PlayUrl">PlayUrl</h2>

<a id="schemaplayurl"></a>
<a id="schema_PlayUrl"></a>
<a id="tocSplayurl"></a>
<a id="tocsplayurl"></a>

```json
{
  "rtsp": "string",
  "rtmp": "string",
  "http_flv": "string",
  "ws_flv": "string",
  "hls": "string",
  "http_ts": "string",
  "ws_ts": "string",
  "http_fmp4": "string",
  "ws_fmp4": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|rtsp|string|false|none||RTSP协议播放地址|
|rtmp|string|false|none||RTMP协议播放地址|
|http_flv|string|false|none||HTTP-FLV协议播放地址|
|ws_flv|string|false|none||WebSocket-FLV协议播放地址|
|hls|string|false|none||HLS协议播放地址|
|http_ts|string|false|none||HTTP-TS协议播放地址|
|ws_ts|string|false|none||WebSocket-TS协议播放地址|
|http_fmp4|string|false|none||HTTP-fMP4协议播放地址|
|ws_fmp4|string|false|none||WebSocket-fMP4协议播放地址|

<h2 id="tocS_ServerResponsePlayUrl">ServerResponsePlayUrl</h2>

<a id="schemaserverresponseplayurl"></a>
<a id="schema_ServerResponsePlayUrl"></a>
<a id="tocSserverresponseplayurl"></a>
<a id="tocsserverresponseplayurl"></a>

```json
{
  "code": 0,
  "data": {
    "rtsp": "string",
    "rtmp": "string",
    "http_flv": "string",
    "ws_flv": "string",
    "hls": "string",
    "http_ts": "string",
    "ws_ts": "string",
    "http_fmp4": "string",
    "ws_fmp4": "string"
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[PlayUrl](#schemaplayurl)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_AjaxResultAlarmListResp">AjaxResultAlarmListResp</h2>

<a id="schemaajaxresultalarmlistresp"></a>
<a id="schema_AjaxResultAlarmListResp"></a>
<a id="tocSajaxresultalarmlistresp"></a>
<a id="tocsajaxresultalarmlistresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_TcpLink">TcpLink</h2>

<a id="schematcplink"></a>
<a id="schema_TcpLink"></a>
<a id="tocStcplink"></a>
<a id="tocstcplink"></a>

```json
{
  "id": "string",
  "local_ip": "string",
  "local_port": 0,
  "peer_ip": "string",
  "peer_port": 0,
  "typeid": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|string|false|none||该tcp链接唯一id|
|local_ip|string|false|none||本机网卡ip|
|local_port|integer|false|none||本机端口号 (这是个rtmp播放器或推流器)|
|peer_ip|string|false|none||客户端ip|
|peer_port|integer|false|none||客户端端口号|
|typeid|string|false|none||客户端TCPSession typeid|

<h2 id="tocS_AlarmQueryReq">AlarmQueryReq</h2>

<a id="schemaalarmqueryreq"></a>
<a id="schema_AlarmQueryReq"></a>
<a id="tocSalarmqueryreq"></a>
<a id="tocsalarmqueryreq"></a>

```json
{
  "deviceId": "string",
  "alarmLevel": 0,
  "alarmType": 0,
  "startTime": "string",
  "endTime": "string",
  "page": 1,
  "size": 20
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|false|none||none|
|alarmLevel|integer|false|none||none|
|alarmType|integer|false|none||none|
|startTime|string|false|none||none|
|endTime|string|false|none||none|
|page|integer|false|none||none|
|size|integer|false|none||none|

<h2 id="tocS_ServerResponseListTcpLink">ServerResponseListTcpLink</h2>

<a id="schemaserverresponselisttcplink"></a>
<a id="schema_ServerResponseListTcpLink"></a>
<a id="tocSserverresponselisttcplink"></a>
<a id="tocsserverresponselisttcplink"></a>

```json
{
  "code": 0,
  "data": [
    {
      "id": "string",
      "local_ip": "string",
      "local_port": 0,
      "peer_ip": "string",
      "peer_port": 0,
      "typeid": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[[TcpLink](#schematcplink)]|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_AjaxResultAlarmVO">AjaxResultAlarmVO</h2>

<a id="schemaajaxresultalarmvo"></a>
<a id="schema_AjaxResultAlarmVO"></a>
<a id="tocSajaxresultalarmvo"></a>
<a id="tocsajaxresultalarmvo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_StreamKey">StreamKey</h2>

<a id="schemastreamkey"></a>
<a id="schema_StreamKey"></a>
<a id="tocSstreamkey"></a>
<a id="tocsstreamkey"></a>

```json
{
  "key": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|string|false|none||none|

<h2 id="tocS_MapLong">MapLong</h2>

<a id="schemamaplong"></a>
<a id="schema_MapLong"></a>
<a id="tocSmaplong"></a>
<a id="tocsmaplong"></a>

```json
{
  "key": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|integer|false|none||none|

<h2 id="tocS_ServerResponseStreamKey">ServerResponseStreamKey</h2>

<a id="schemaserverresponsestreamkey"></a>
<a id="schema_ServerResponseStreamKey"></a>
<a id="tocSserverresponsestreamkey"></a>
<a id="tocsserverresponsestreamkey"></a>

```json
{
  "code": 0,
  "data": {
    "key": "string"
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[StreamKey](#schemastreamkey)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_AjaxResultUserInfoVO">AjaxResultUserInfoVO</h2>

<a id="schemaajaxresultuserinfovo"></a>
<a id="schema_AjaxResultUserInfoVO"></a>
<a id="tocSajaxresultuserinfovo"></a>
<a id="tocsajaxresultuserinfovo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_StreamProxyItem">StreamProxyItem</h2>

<a id="schemastreamproxyitem"></a>
<a id="schema_StreamProxyItem"></a>
<a id="tocSstreamproxyitem"></a>
<a id="tocsstreamproxyitem"></a>

```json
{
  "vhost": "string",
  "app": "string",
  "stream": "string",
  "url": "string",
  "retry_count": 0,
  "rtp_type": 0,
  "timeout_sec": 0,
  "enable_hls": true,
  "enable_hls_fmp4": true,
  "enable_mp4": true,
  "enable_rtsp": true,
  "enable_rtmp": true,
  "enable_ts": true,
  "enable_fmp4": true,
  "hls_demand": true,
  "rtsp_demand": true,
  "rtmp_demand": true,
  "ts_demand": true,
  "fmp4_demand": true,
  "enable_audio": true,
  "add_mute_audio": true,
  "mp4_save_path": "string",
  "mp4_max_second": 0,
  "mp4_as_player": true,
  "hls_save_path": "string",
  "modify_stamp": 0,
  "auto_close": true
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|vhost|string|false|none||添加的流的虚拟主机，例如__defaultVhost__|
|app|string|false|none||添加的流的应用名，例如live|
|stream|string|false|none||添加的流的id名，例如test|
|url|string|false|none||拉流地址，例如rtmp://live.hkstv.hk.lxdns.com/live/hks2|
|retry_count|integer|false|none||拉流重试次数，默认为-1无限重试|
|rtp_type|integer|false|none||rtsp拉流时，拉流方式，0：tcp，1：udp，2：组播|
|timeout_sec|integer|false|none||拉流超时时间，单位秒，float类型|
|enable_hls|boolean|false|none||是否转换成hls-mpegts协议|
|enable_hls_fmp4|boolean|false|none||是否转换成hls-fmp4协议|
|enable_mp4|boolean|false|none||是否允许mp4录制|
|enable_rtsp|boolean|false|none||是否转rtsp协议|
|enable_rtmp|boolean|false|none||是否转rtmp/flv协议|
|enable_ts|boolean|false|none||是否转http-ts/ws-ts协议|
|enable_fmp4|boolean|false|none||是否转http-fmp4/ws-fmp4协议|
|hls_demand|boolean|false|none||该协议是否有人观看才生成|
|rtsp_demand|boolean|false|none||该协议是否有人观看才生成|
|rtmp_demand|boolean|false|none||该协议是否有人观看才生成|
|ts_demand|boolean|false|none||该协议是否有人观看才生成|
|fmp4_demand|boolean|false|none||该协议是否有人观看才生成|
|enable_audio|boolean|false|none||转协议时是否开启音频|
|add_mute_audio|boolean|false|none||转协议时，无音频是否添加静音aac音频|
|mp4_save_path|string|false|none||mp4录制文件保存根目录，置空使用默认|
|mp4_max_second|integer|false|none||mp4录制切片大小，单位秒|
|mp4_as_player|boolean|false|none||MP4录制是否当作观看者参与播放人数计数|
|hls_save_path|string|false|none||hls文件保存保存根目录，置空使用默认|
|modify_stamp|integer|false|none||该流是否开启时间戳覆盖(0:绝对时间戳/1:系统时间戳/2:相对时间戳)|
|auto_close|boolean|false|none||无人观看是否自动关闭流(不触发无人观看hook)|

<h2 id="tocS_AjaxResultUserListResp">AjaxResultUserListResp</h2>

<a id="schemaajaxresultuserlistresp"></a>
<a id="schema_AjaxResultUserListResp"></a>
<a id="tocSajaxresultuserlistresp"></a>
<a id="tocsajaxresultuserlistresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_StringDelFlag">StringDelFlag</h2>

<a id="schemastringdelflag"></a>
<a id="schema_StringDelFlag"></a>
<a id="tocSstringdelflag"></a>
<a id="tocsstringdelflag"></a>

```json
{
  "flag": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|flag|string|false|none||none|

<h2 id="tocS_AjaxResultUserVO">AjaxResultUserVO</h2>

<a id="schemaajaxresultuservo"></a>
<a id="schema_AjaxResultUserVO"></a>
<a id="tocSajaxresultuservo"></a>
<a id="tocsajaxresultuservo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseStringDelFlag">ServerResponseStringDelFlag</h2>

<a id="schemaserverresponsestringdelflag"></a>
<a id="schema_ServerResponseStringDelFlag"></a>
<a id="tocSserverresponsestringdelflag"></a>
<a id="tocsserverresponsestringdelflag"></a>

```json
{
  "code": 0,
  "data": {
    "flag": "string"
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[StringDelFlag](#schemastringdelflag)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_UserCreateReq">UserCreateReq</h2>

<a id="schemausercreatereq"></a>
<a id="schema_UserCreateReq"></a>
<a id="tocSusercreatereq"></a>
<a id="tocsusercreatereq"></a>

```json
{
  "username": "string",
  "password": "string",
  "nickname": "string",
  "email": "user@example.com",
  "phone": "string",
  "avatar": "string",
  "status": 0,
  "roleIds": [
    0
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|username|string|true|none||用户名|
|password|string|true|none||密码|
|nickname|string|false|none||昵称|
|email|string(email)|false|none||邮箱|
|phone|string|false|none||手机号|
|avatar|string|false|none||头像URL|
|status|integer|true|none||状态 1启用 0禁用|
|roleIds|[integer]|false|none||角色ID列表（可选）|

<h2 id="tocS_StreamPusherItem">StreamPusherItem</h2>

<a id="schemastreampusheritem"></a>
<a id="schema_StreamPusherItem"></a>
<a id="tocSstreampusheritem"></a>
<a id="tocsstreampusheritem"></a>

```json
{
  "vhost": "string",
  "schema": "string",
  "app": "string",
  "stream": "string",
  "dst_url": "string",
  "retry_count": 0,
  "rtp_type": 0,
  "timeout_sec": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|vhost|string|false|none||添加的流的虚拟主机，例如__defaultVhost__|
|schema|string|false|none||协议，例如 rtsp或rtmp|
|app|string|false|none||添加的流的应用名，例如live|
|stream|string|false|none||需要转推的流id|
|dst_url|string|false|none||目标转推url，带参数需要自行url转义|
|retry_count|integer|false|none||转推失败重试次数，默认无限重试|
|rtp_type|integer|false|none||rtsp推流时，推流方式，0：tcp，1：udp|
|timeout_sec|integer|false|none||推流超时时间，单位秒，float类型|

<h2 id="tocS_UserUpdateReq">UserUpdateReq</h2>

<a id="schemauserupdatereq"></a>
<a id="schema_UserUpdateReq"></a>
<a id="tocSuserupdatereq"></a>
<a id="tocsuserupdatereq"></a>

```json
{
  "id": 0,
  "password": "string",
  "nickname": "string",
  "email": "user@example.com",
  "phone": "string",
  "avatar": "string",
  "status": 0,
  "roleIds": [
    0
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|true|none||用户ID|
|password|string|false|none||密码（可选，不传则不更新密码）|
|nickname|string|false|none||昵称|
|email|string(email)|false|none||邮箱|
|phone|string|false|none||手机号|
|avatar|string|false|none||头像URL|
|status|integer|true|none||状态 1启用 0禁用|
|roleIds|[integer]|false|none||角色ID列表（可选）|

<h2 id="tocS_StreamFfmpegItem">StreamFfmpegItem</h2>

<a id="schemastreamffmpegitem"></a>
<a id="schema_StreamFfmpegItem"></a>
<a id="tocSstreamffmpegitem"></a>
<a id="tocsstreamffmpegitem"></a>

```json
{
  "src_url": "string",
  "dst_url": "string",
  "timeout_ms": 0,
  "enable_hls": true,
  "enable_mp4": true,
  "ffmpeg_cmd_key": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|src_url|string|false|none||FFmpeg拉流地址,支持任意协议或格式(只要FFmpeg支持即可)|
|dst_url|string|false|none||FFmpeg rtmp推流地址，一般都是推给自己，例如rtmp://127.0.0.1/live/stream_form_ffmpeg|
|timeout_ms|integer|false|none||FFmpeg推流成功超时时间|
|enable_hls|boolean|false|none||是否开启hls录制|
|enable_mp4|boolean|false|none||是否开启mp4录制|
|ffmpeg_cmd_key|string|false|none||配置文件中FFmpeg命令参数模板key(非内容)，置空则采用默认模板:ffmpeg.cmd|

<h2 id="tocS_AjaxResultRoleListResp">AjaxResultRoleListResp</h2>

<a id="schemaajaxresultrolelistresp"></a>
<a id="schema_AjaxResultRoleListResp"></a>
<a id="tocSajaxresultrolelistresp"></a>
<a id="tocsajaxresultrolelistresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_Mp4RecordFile">Mp4RecordFile</h2>

<a id="schemamp4recordfile"></a>
<a id="schema_Mp4RecordFile"></a>
<a id="tocSmp4recordfile"></a>
<a id="tocsmp4recordfile"></a>

```json
{
  "paths": [
    "string"
  ],
  "rootPath": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|paths|[string]|false|none||none|
|rootPath|string|false|none||none|

<h2 id="tocS_AjaxResultRoleVO">AjaxResultRoleVO</h2>

<a id="schemaajaxresultrolevo"></a>
<a id="schema_AjaxResultRoleVO"></a>
<a id="tocSajaxresultrolevo"></a>
<a id="tocsajaxresultrolevo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseMp4RecordFile">ServerResponseMp4RecordFile</h2>

<a id="schemaserverresponsemp4recordfile"></a>
<a id="schema_ServerResponseMp4RecordFile"></a>
<a id="tocSserverresponsemp4recordfile"></a>
<a id="tocsserverresponsemp4recordfile"></a>

```json
{
  "code": 0,
  "data": {
    "paths": [
      "string"
    ],
    "rootPath": "string"
  },
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[Mp4RecordFile](#schemamp4recordfile)|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_RoleCreateReq">RoleCreateReq</h2>

<a id="schemarolecreatereq"></a>
<a id="schema_RoleCreateReq"></a>
<a id="tocSrolecreatereq"></a>
<a id="tocsrolecreatereq"></a>

```json
{
  "name": "string",
  "remark": "string",
  "status": 0,
  "permissions": [
    0
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|name|string|true|none||角色名称|
|remark|string|false|none||角色描述|
|status|integer|true|none||状态 1启用 0禁用|
|permissions|[integer]|false|none||权限列表|

<h2 id="tocS_RecordReq">RecordReq</h2>

<a id="schemarecordreq"></a>
<a id="schema_RecordReq"></a>
<a id="tocSrecordreq"></a>
<a id="tocsrecordreq"></a>

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "period": "string",
  "customized_path": "string",
  "max_seconds": "string",
  "type": 0,
  "speed": "string",
  "stamp": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schema|string|false|none||筛选协议，例如 rtsp或rtmp|
|vhost|string|false|none||筛选虚拟主机，例如__defaultVhost__|
|app|string|false|none||筛选应用名，例如 live|
|stream|string|false|none||筛选流id，例如 test|
|period|string|false|none||流的录像日期，格式为2020-02-01,如果不是完整的日期，那么是搜索录像文件夹列表，否则搜索对应日期下的mp4文件列表|
|customized_path|string|false|none||自定义搜索路径，与startRecord方法中的customized_path一样，默认为配置文件的路径|
|max_seconds|string|false|none||mp4录像切片时间大小,单位秒，置0则采用配置项|
|type|integer|false|none||0为hls，1为mp4|
|speed|string|false|none||要设置的录像倍速 eg.2.0|
|stamp|string|false|none||要设置的录像播放位置|

<h2 id="tocS_RoleUpdateReq">RoleUpdateReq</h2>

<a id="schemaroleupdatereq"></a>
<a id="schema_RoleUpdateReq"></a>
<a id="tocSroleupdatereq"></a>
<a id="tocsroleupdatereq"></a>

```json
{
  "name": "string",
  "remark": "string",
  "status": 0,
  "permissions": [
    0
  ]
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|name|string|true|none||角色名称|
|remark|string|false|none||角色描述|
|status|integer|true|none||状态 1启用 0禁用|
|permissions|[integer]|false|none||权限列表|

<h2 id="tocS_DeleteRecordDirectory">DeleteRecordDirectory</h2>

<a id="schemadeleterecorddirectory"></a>
<a id="schema_DeleteRecordDirectory"></a>
<a id="tocSdeleterecorddirectory"></a>
<a id="tocsdeleterecorddirectory"></a>

```json
{
  "code": 0,
  "data": "string",
  "msg": "success",
  "result": "success",
  "path": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|string|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|
|path|string|false|none||none|

<h2 id="tocS_AjaxResultListMenuResp">AjaxResultListMenuResp</h2>

<a id="schemaajaxresultlistmenuresp"></a>
<a id="schema_AjaxResultListMenuResp"></a>
<a id="tocSajaxresultlistmenuresp"></a>
<a id="tocsajaxresultlistmenuresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_SnapshotReq">SnapshotReq</h2>

<a id="schemasnapshotreq"></a>
<a id="schema_SnapshotReq"></a>
<a id="tocSsnapshotreq"></a>
<a id="tocssnapshotreq"></a>

```json
{
  "url": "string",
  "timeout_sec": 30,
  "expire_sec": 5
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|url|string|false|none||需要截图的url，可以是本机的，也可以是远程主机的。|
|timeout_sec|integer|false|none||截图失败超时时间，防止FFmpeg一直等待截图。|
|expire_sec|integer|false|none||截图的过期时间，该时间内产生的截图都会作为缓存返回。|

<h2 id="tocS_AjaxResultListMenuVO">AjaxResultListMenuVO</h2>

<a id="schemaajaxresultlistmenuvo"></a>
<a id="schema_AjaxResultListMenuVO"></a>
<a id="tocSajaxresultlistmenuvo"></a>
<a id="tocsajaxresultlistmenuvo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_RtpInfoResult">RtpInfoResult</h2>

<a id="schemartpinforesult"></a>
<a id="schema_RtpInfoResult"></a>
<a id="tocSrtpinforesult"></a>
<a id="tocsrtpinforesult"></a>

```json
{
  "code": 0,
  "exist": true,
  "peer_ip": "string",
  "peer_port": 0,
  "local_ip": "string",
  "local_port": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||状态码。|
|exist|boolean|false|none||会话是否存在。|
|peer_ip|string|false|none||推流客户端ip。|
|peer_port|integer|false|none||客户端端口号。|
|local_ip|string|false|none||本地监听的网卡ip。|
|local_port|integer|false|none||本地端口号。|

<h2 id="tocS_MetaReq">MetaReq</h2>

<a id="schemametareq"></a>
<a id="schema_MetaReq"></a>
<a id="tocSmetareq"></a>
<a id="tocsmetareq"></a>

```json
{
  "activeIcon": "string",
  "activePath": "string",
  "affixTab": true,
  "affixTabOrder": 0,
  "badge": "string",
  "badgeType": "string",
  "badgeVariants": "string",
  "hideChildrenInMenu": true,
  "hideInBreadcrumb": true,
  "hideInMenu": true,
  "hideInTab": true,
  "icon": "string",
  "iframeSrc": "string",
  "keepAlive": true,
  "link": "string",
  "maxNumOfOpenTab": 0,
  "noBasicLayout": true,
  "openInNewWindow": true,
  "order": 0,
  "title": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|activeIcon|string|false|none||激活时显示的图标|
|activePath|string|false|none||作为路由时，需要激活的菜单的Path|
|affixTab|boolean|false|none||固定在标签栏|
|affixTabOrder|integer|false|none||在标签栏固定的顺序|
|badge|string|false|none||徽标内容|
|badgeType|string|false|none||徽标类型|
|badgeVariants|string|false|none||徽标颜色|
|hideChildrenInMenu|boolean|false|none||在菜单中隐藏下级|
|hideInBreadcrumb|boolean|false|none||在面包屑中隐藏|
|hideInMenu|boolean|false|none||在菜单中隐藏|
|hideInTab|boolean|false|none||在标签栏中隐藏|
|icon|string|false|none||菜单图标|
|iframeSrc|string|false|none||内嵌Iframe的URL|
|keepAlive|boolean|false|none||是否缓存页面|
|link|string|false|none||外链页面的URL|
|maxNumOfOpenTab|integer|false|none||同一个路由最大打开的标签数|
|noBasicLayout|boolean|false|none||无需基础布局|
|openInNewWindow|boolean|false|none||是否在新窗口打开|
|order|integer|false|none||菜单排序|
|title|string|false|none||菜单标题|

<h2 id="tocS_OpenRtpServerResult">OpenRtpServerResult</h2>

<a id="schemaopenrtpserverresult"></a>
<a id="schema_OpenRtpServerResult"></a>
<a id="tocSopenrtpserverresult"></a>
<a id="tocsopenrtpserverresult"></a>

```json
{
  "port": "string",
  "code": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|port|string|false|none||none|
|code|string|false|none||none|

<h2 id="tocS_MenuReq">MenuReq</h2>

<a id="schemamenureq"></a>
<a id="schema_MenuReq"></a>
<a id="tocSmenureq"></a>
<a id="tocsmenureq"></a>

```json
{
  "authCode": "string",
  "component": "string",
  "name": "string",
  "path": "string",
  "pid": "string",
  "redirect": "string",
  "type": "string",
  "meta": {
    "activeIcon": "string",
    "activePath": "string",
    "affixTab": true,
    "affixTabOrder": 0,
    "badge": "string",
    "badgeType": "string",
    "badgeVariants": "string",
    "hideChildrenInMenu": true,
    "hideInBreadcrumb": true,
    "hideInMenu": true,
    "hideInTab": true,
    "icon": "string",
    "iframeSrc": "string",
    "keepAlive": true,
    "link": "string",
    "maxNumOfOpenTab": 0,
    "noBasicLayout": true,
    "openInNewWindow": true,
    "order": 0,
    "title": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|authCode|string|true|none||后端权限标识|
|component|string|false|none||组件路径|
|name|string|true|none||菜单名称|
|path|string|true|none||路由路径|
|pid|string|true|none||父级ID|
|redirect|string|false|none||重定向|
|type|string|true|none||菜单类型|
|meta|[MetaReq](#schemametareq)|false|none||菜单元数据|

<h2 id="tocS_OpenRtpServerReq">OpenRtpServerReq</h2>

<a id="schemaopenrtpserverreq"></a>
<a id="schema_OpenRtpServerReq"></a>
<a id="tocSopenrtpserverreq"></a>
<a id="tocsopenrtpserverreq"></a>

```json
{
  "port": 0,
  "tcp_mode": 0,
  "stream_id": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|port|integer|false|none||接收端口，0则为随机端口。|
|tcp_mode|integer|false|none||0 udp 模式，1 tcp 被动模式, 2 tcp 主动模式。 (兼容enable_tcp 为0/1)。|
|stream_id|string|false|none||该端口绑定的流ID，该端口只能创建这一个流(而不是根据ssrc创建多个)。|

<h2 id="tocS_AjaxResultLivePlayVO">AjaxResultLivePlayVO</h2>

<a id="schemaajaxresultliveplayvo"></a>
<a id="schema_AjaxResultLivePlayVO"></a>
<a id="tocSajaxresultliveplayvo"></a>
<a id="tocsajaxresultliveplayvo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ConnectRtpServerReq">ConnectRtpServerReq</h2>

<a id="schemaconnectrtpserverreq"></a>
<a id="schema_ConnectRtpServerReq"></a>
<a id="tocSconnectrtpserverreq"></a>
<a id="tocsconnectrtpserverreq"></a>

```json
{
  "dst_port": 0,
  "dst_url": 0,
  "stream_id": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|dst_port|integer|false|none||tcp主动模式时服务端端口|
|dst_url|integer|false|none||tcp主动模式时服务端地址|
|stream_id|string|false|none||该端口绑定的流ID，该端口只能创建这一个流(而不是根据ssrc创建多个)。|

<h2 id="tocS_LiveStartReq">LiveStartReq</h2>

<a id="schemalivestartreq"></a>
<a id="schema_LiveStartReq"></a>
<a id="tocSlivestartreq"></a>
<a id="tocslivestartreq"></a>

```json
{
  "deviceId": "string",
  "channelId": "string",
  "protocol": "FLV",
  "streamMode": "UDP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|true|none||none|
|channelId|string|true|none||none|
|protocol|string|false|none||none|
|streamMode|string|false|none||none|

<h2 id="tocS_CloseRtpServerResult">CloseRtpServerResult</h2>

<a id="schemaclosertpserverresult"></a>
<a id="schema_CloseRtpServerResult"></a>
<a id="tocSclosertpserverresult"></a>
<a id="tocsclosertpserverresult"></a>

```json
{
  "hit": "string",
  "code": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|hit|string|false|none||是否找到记录并关闭|
|code|string|false|none||none|

<h2 id="tocS_LiveStopReq">LiveStopReq</h2>

<a id="schemalivestopreq"></a>
<a id="schema_LiveStopReq"></a>
<a id="tocSlivestopreq"></a>
<a id="tocslivestopreq"></a>

```json
{
  "streamId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|streamId|string|true|none||none|

<h2 id="tocS_RtpServer">RtpServer</h2>

<a id="schemartpserver"></a>
<a id="schema_RtpServer"></a>
<a id="tocSrtpserver"></a>
<a id="tocsrtpserver"></a>

```json
{
  "port": "string",
  "streamId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|port|string|false|none||none|
|streamId|string|false|none||none|

<h2 id="tocS_AjaxResultListLivePlayVO">AjaxResultListLivePlayVO</h2>

<a id="schemaajaxresultlistliveplayvo"></a>
<a id="schema_AjaxResultListLivePlayVO"></a>
<a id="tocSajaxresultlistliveplayvo"></a>
<a id="tocsajaxresultlistliveplayvo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_ServerResponseListRtpServer">ServerResponseListRtpServer</h2>

<a id="schemaserverresponselistrtpserver"></a>
<a id="schema_ServerResponseListRtpServer"></a>
<a id="tocSserverresponselistrtpserver"></a>
<a id="tocsserverresponselistrtpserver"></a>

```json
{
  "code": 0,
  "data": [
    {
      "port": "string",
      "streamId": "string"
    }
  ],
  "msg": "success",
  "result": "success"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|integer|false|none||none|
|data|[[RtpServer](#schemartpserver)]|false|none||none|
|msg|string|false|none||none|
|result|string|false|none||none|

<h2 id="tocS_AjaxResult?">AjaxResult?</h2>

<a id="schemaajaxresult?"></a>
<a id="schema_AjaxResult?"></a>
<a id="tocSajaxresult?"></a>
<a id="tocsajaxresult?"></a>

```json
{
  "key": null
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|null|false|none||none|

<h2 id="tocS_StartSendRtpResult">StartSendRtpResult</h2>

<a id="schemastartsendrtpresult"></a>
<a id="schema_StartSendRtpResult"></a>
<a id="tocSstartsendrtpresult"></a>
<a id="tocsstartsendrtpresult"></a>

```json
{
  "code": "string",
  "local_port": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|code|string|false|none||none|
|local_port|string|false|none||none|

<h2 id="tocS_PlaybackStartReq">PlaybackStartReq</h2>

<a id="schemaplaybackstartreq"></a>
<a id="schema_PlaybackStartReq"></a>
<a id="tocSplaybackstartreq"></a>
<a id="tocsplaybackstartreq"></a>

```json
{
  "deviceId": "string",
  "channelId": "string",
  "startTime": "string",
  "endTime": "string",
  "streamMode": "UDP"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|true|none||none|
|channelId|string|true|none||none|
|startTime|string|true|none||none|
|endTime|string|true|none||none|
|streamMode|string|false|none||none|

<h2 id="tocS_StartSendRtpReq">StartSendRtpReq</h2>

<a id="schemastartsendrtpreq"></a>
<a id="schema_StartSendRtpReq"></a>
<a id="tocSstartsendrtpreq"></a>
<a id="tocsstartsendrtpreq"></a>

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "ssrc": 0,
  "dst_url": "string",
  "dst_port": 0,
  "is_udp": true,
  "src_port": 0,
  "pt": 0,
  "use_ps": 0,
  "only_audio": true
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schema|string|false|none||筛选协议，例如 rtsp或rtmp|
|vhost|string|false|none||筛选虚拟主机，例如__defaultVhost__|
|app|string|false|none||筛选应用名，例如 live|
|stream|string|false|none||筛选流id，例如 test|
|ssrc|integer|false|none||推流的rtp的ssrc,指定不同的ssrc可以同时推流到多个服务器。|
|dst_url|string|false|none||目标ip或域名。|
|dst_port|integer|false|none||目标端口。|
|is_udp|boolean|false|none||是否为udp模式,否则为tcp模式。|
|src_port|integer|false|none||使用的本机端口，为0或不传时默认为随机端口。|
|pt|integer|false|none||发送时，rtp的pt（uint8_t）,不传时默认为96。|
|use_ps|integer|false|none||发送时，rtp的负载类型。为1时，负载为ps；为0时，为es；不传时默认为1。|
|only_audio|boolean|false|none||当use_ps 为0时，有效。为1时，发送音频；为0时，发送视频；不传时默认为0。|

<h2 id="tocS_PlaybackControlReq">PlaybackControlReq</h2>

<a id="schemaplaybackcontrolreq"></a>
<a id="schema_PlaybackControlReq"></a>
<a id="tocSplaybackcontrolreq"></a>
<a id="tocsplaybackcontrolreq"></a>

```json
{
  "streamId": "string",
  "action": "string",
  "param": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|streamId|string|true|none||none|
|action|string|true|none||PLAY_RESUME / PLAY_RANGE / PLAY_SPEED / PLAY_NOW|
|param|string|false|none||none|

<h2 id="tocS_CloseSendRtpReq">CloseSendRtpReq</h2>

<a id="schemaclosesendrtpreq"></a>
<a id="schema_CloseSendRtpReq"></a>
<a id="tocSclosesendrtpreq"></a>
<a id="tocsclosesendrtpreq"></a>

```json
{
  "schema": "string",
  "vhost": "__defaultVhost__",
  "app": "string",
  "stream": "string",
  "ssrc": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|schema|string|false|none||筛选协议，例如 rtsp或rtmp|
|vhost|string|false|none||筛选虚拟主机，例如__defaultVhost__|
|app|string|false|none||筛选应用名，例如 live|
|stream|string|false|none||筛选流id，例如 test|
|ssrc|string|false|none||停止GB28181 ps-rtp推流|

<h2 id="tocS_RecordQueryReq">RecordQueryReq</h2>

<a id="schemarecordqueryreq"></a>
<a id="schema_RecordQueryReq"></a>
<a id="tocSrecordqueryreq"></a>
<a id="tocsrecordqueryreq"></a>

```json
{
  "deviceId": "string",
  "channelId": "string",
  "startTime": "string",
  "endTime": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|true|none||none|
|channelId|string|true|none||none|
|startTime|string|true|none||none|
|endTime|string|true|none||none|

<h2 id="tocS_ZlmNode">ZlmNode</h2>

<a id="schemazlmnode"></a>
<a id="schema_ZlmNode"></a>
<a id="tocSzlmnode"></a>
<a id="tocszlmnode"></a>

```json
{
  "serverId": "zlm",
  "host": "http://127.0.0.1:9092",
  "secret": "string",
  "enabled": true,
  "hookEnabled": true,
  "weight": 100
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|serverId|string|false|none||The id of this node.|
|host|string|false|none||The host of this node. eg: <a href="http://127.0.0.1:9092">node</a>|
|secret|string|false|none||The secret of this host.|
|enabled|boolean|false|none||Whether enable this host.|
|hookEnabled|boolean|false|none||Whether enable hook.|
|weight|integer|false|none||The weight of this host.|

<h2 id="tocS_AjaxResultListDeptResp">AjaxResultListDeptResp</h2>

<a id="schemaajaxresultlistdeptresp"></a>
<a id="schema_AjaxResultListDeptResp"></a>
<a id="tocSajaxresultlistdeptresp"></a>
<a id="tocsajaxresultlistdeptresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_DeptReq">DeptReq</h2>

<a id="schemadeptreq"></a>
<a id="schema_DeptReq"></a>
<a id="tocSdeptreq"></a>
<a id="tocsdeptreq"></a>

```json
{
  "name": "string",
  "remark": "string",
  "status": 0,
  "parentId": "string",
  "sortOrder": 0,
  "leader": "string",
  "phone": "string",
  "email": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|name|string|true|none||部门名称|
|remark|string|false|none||部门描述|
|status|integer|true|none||状态 1启用 0禁用|
|parentId|string|false|none||父部门ID|
|sortOrder|integer|false|none||排序|
|leader|string|false|none||部门负责人|
|phone|string|false|none||联系电话|
|email|string|false|none||邮箱|

<h2 id="tocS_AjaxResultLoginResp">AjaxResultLoginResp</h2>

<a id="schemaajaxresultloginresp"></a>
<a id="schema_AjaxResultLoginResp"></a>
<a id="tocSajaxresultloginresp"></a>
<a id="tocsajaxresultloginresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_LoginReq">LoginReq</h2>

<a id="schemaloginreq"></a>
<a id="schema_LoginReq"></a>
<a id="tocSloginreq"></a>
<a id="tocsloginreq"></a>

```json
{
  "username": "string",
  "password": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|username|string|true|none||用户名|
|password|string|true|none||密码|

<h2 id="tocS_AjaxResultPushProxyVO">AjaxResultPushProxyVO</h2>

<a id="schemaajaxresultpushproxyvo"></a>
<a id="schema_AjaxResultPushProxyVO"></a>
<a id="tocSajaxresultpushproxyvo"></a>
<a id="tocsajaxresultpushproxyvo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_PushProxyExtendReq">PushProxyExtendReq</h2>

<a id="schemapushproxyextendreq"></a>
<a id="schema_PushProxyExtendReq"></a>
<a id="tocSpushproxyextendreq"></a>
<a id="tocspushproxyextendreq"></a>

```json
{
  "vhost": "__defaultVhost__",
  "retryCount": -1,
  "rtpType": 0,
  "timeoutSec": 10,
  "autoReconnect": true,
  "retryInterval": 5,
  "enableMonitor": true,
  "qualityThreshold": 0.8,
  "maxBitrate": 5000,
  "minBitrate": 500,
  "enableAuth": false,
  "authUser": "user",
  "authPassword": "password",
  "autoStop": false,
  "autoStopDelay": 300,
  "priority": 1,
  "enableEncrypt": false,
  "encryptKey": "secret_key"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|vhost|string|false|none||虚拟主机|
|retryCount|integer|false|none||推流重试次数，-1表示无限重试|
|rtpType|integer|false|none||rtsp推流方式，0:tcp，1:udp|
|timeoutSec|integer|false|none||推流超时时间，单位秒|
|autoReconnect|boolean|false|none||是否启用自动重连|
|retryInterval|integer|false|none||推流失败重试间隔，单位秒|
|enableMonitor|boolean|false|none||是否启用推流状态监控|
|qualityThreshold|number|false|none||推流质量监控阈值|
|maxBitrate|integer|false|none||最大推流码率，单位kbps|
|minBitrate|integer|false|none||最小推流码率，单位kbps|
|enableAuth|boolean|false|none||是否启用推流认证|
|authUser|string|false|none||推流认证用户名|
|authPassword|string|false|none||推流认证密码|
|autoStop|boolean|false|none||无人观看是否自动停止推流|
|autoStopDelay|integer|false|none||自动停止延迟时间，单位秒|
|priority|integer|false|none||推流优先级，数值越大优先级越高|
|enableEncrypt|boolean|false|none||是否启用推流加密|
|encryptKey|string|false|none||推流加密密钥|

<h2 id="tocS_PushProxyCreateReq">PushProxyCreateReq</h2>

<a id="schemapushproxycreatereq"></a>
<a id="schema_PushProxyCreateReq"></a>
<a id="tocSpushproxycreatereq"></a>
<a id="tocspushproxycreatereq"></a>

```json
{
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|app|string|true|none||应用名称|
|stream|string|true|none||流名称|
|dstUrl|string|true|none||推流目标地址|
|schema|string|false|none||推流协议|
|status|integer|false|none||代理状态|
|serverId|string|true|none||节点ID|
|description|string|false|none||代理描述|
|pushProxyExtendReq|[PushProxyExtendReq](#schemapushproxyextendreq)|false|none||ZLM推流扩展参数|

<h2 id="tocS_PushProxyUpdateReq">PushProxyUpdateReq</h2>

<a id="schemapushproxyupdatereq"></a>
<a id="schema_PushProxyUpdateReq"></a>
<a id="tocSpushproxyupdatereq"></a>
<a id="tocspushproxyupdatereq"></a>

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "serverId": "zlm-node-1",
  "description": "测试推流代理",
  "pushProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "autoReconnect": true,
    "retryInterval": 5,
    "enableMonitor": true,
    "qualityThreshold": 0.8,
    "maxBitrate": 5000,
    "minBitrate": 500,
    "enableAuth": false,
    "authUser": "user",
    "authPassword": "password",
    "autoStop": false,
    "autoStopDelay": 300,
    "priority": 1,
    "enableEncrypt": false,
    "encryptKey": "secret_key"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|true|none||推流代理ID|
|app|string|false|none||应用名称|
|stream|string|false|none||流名称|
|dstUrl|string|false|none||推流目标地址|
|schema|string|false|none||推流协议|
|status|integer|false|none||代理状态|
|serverId|string|false|none||节点ID|
|description|string|false|none||代理描述|
|pushProxyExtendReq|[PushProxyExtendReq](#schemapushproxyextendreq)|false|none||ZLM推流扩展参数|

<h2 id="tocS_PushProxyQueryReq">PushProxyQueryReq</h2>

<a id="schemapushproxyqueryreq"></a>
<a id="schema_PushProxyQueryReq"></a>
<a id="tocSpushproxyqueryreq"></a>
<a id="tocspushproxyqueryreq"></a>

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "dstUrl": "rtmp://push.example.com/live/test",
  "schema": "rtmp",
  "status": 1,
  "onlineStatus": 1,
  "proxyKey": "push_proxy_key",
  "serverId": "zlm-node-1",
  "enabled": 1,
  "description": "测试推流代理"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||推流代理ID|
|app|string|false|none||应用名称|
|stream|string|false|none||流名称|
|dstUrl|string|false|none||推流目标地址|
|schema|string|false|none||推流协议|
|status|integer|false|none||代理状态|
|onlineStatus|integer|false|none||在线状态|
|proxyKey|string|false|none||代理密钥|
|serverId|string|false|none||节点ID|
|enabled|integer|false|none||是否启用|
|description|string|false|none||代理描述|

<h2 id="tocS_AjaxResultPushProxyListResp">AjaxResultPushProxyListResp</h2>

<a id="schemaajaxresultpushproxylistresp"></a>
<a id="schema_AjaxResultPushProxyListResp"></a>
<a id="tocSajaxresultpushproxylistresp"></a>
<a id="tocsajaxresultpushproxylistresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_AjaxResultStreamProxyVO">AjaxResultStreamProxyVO</h2>

<a id="schemaajaxresultstreamproxyvo"></a>
<a id="schema_AjaxResultStreamProxyVO"></a>
<a id="tocSajaxresultstreamproxyvo"></a>
<a id="tocsajaxresultstreamproxyvo"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_StreamProxyExtendReq">StreamProxyExtendReq</h2>

<a id="schemastreamproxyextendreq"></a>
<a id="schema_StreamProxyExtendReq"></a>
<a id="tocSstreamproxyextendreq"></a>
<a id="tocsstreamproxyextendreq"></a>

```json
{
  "vhost": "__defaultVhost__",
  "retryCount": -1,
  "rtpType": 0,
  "timeoutSec": 10,
  "schema": "rtsp",
  "enableHls": true,
  "enableHlsFmp4": false,
  "enableMp4": false,
  "enableRtsp": true,
  "enableRtmp": true,
  "enableTs": false,
  "enableFmp4": false,
  "hlsDemand": false,
  "rtspDemand": false,
  "rtmpDemand": false,
  "tsDemand": false,
  "fmp4Demand": false,
  "enableAudio": true,
  "addMuteAudio": false,
  "mp4SavePath": "string",
  "mp4MaxSecond": 300,
  "mp4AsPlayer": false,
  "hlsSavePath": "string",
  "modifyStamp": 0,
  "autoClose": false
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|vhost|string|false|none||虚拟主机|
|retryCount|integer|false|none||拉流重试次数，默认-1无限重试|
|rtpType|integer|false|none||RTSP拉流方式：0=TCP，1=UDP，2=组播|
|timeoutSec|integer|false|none||拉流超时时间(秒)|
|schema|string|false|none||拉流方式|
|enableHls|boolean|false|none||是否转换成HLS-MPEGTS协议|
|enableHlsFmp4|boolean|false|none||是否转换成HLS-FMP4协议|
|enableMp4|boolean|false|none||是否允许MP4录制|
|enableRtsp|boolean|false|none||是否转RTSP协议|
|enableRtmp|boolean|false|none||是否转RTMP/FLV协议|
|enableTs|boolean|false|none||是否转HTTP-TS/WS-TS协议|
|enableFmp4|boolean|false|none||是否转HTTP-FMP4/WS-FMP4协议|
|hlsDemand|boolean|false|none||HLS是否按需生成(有人观看才生成)|
|rtspDemand|boolean|false|none||RTSP是否按需生成(有人观看才生成)|
|rtmpDemand|boolean|false|none||RTMP是否按需生成(有人观看才生成)|
|tsDemand|boolean|false|none||TS是否按需生成(有人观看才生成)|
|fmp4Demand|boolean|false|none||FMP4是否按需生成(有人观看才生成)|
|enableAudio|boolean|false|none||转协议时是否开启音频|
|addMuteAudio|boolean|false|none||无音频时是否添加静音AAC音频|
|mp4SavePath|string|false|none||MP4录制文件保存根目录，置空使用默认|
|mp4MaxSecond|integer|false|none||MP4录制切片大小(秒)|
|mp4AsPlayer|boolean|false|none||MP4录制是否当作观看者参与播放人数计数|
|hlsSavePath|string|false|none||HLS文件保存根目录，置空使用默认|
|modifyStamp|integer|false|none||时间戳覆盖模式：0=绝对时间戳，1=系统时间戳，2=相对时间戳|
|autoClose|boolean|false|none||无人观看是否自动关闭流(不触发无人观看hook)|

<h2 id="tocS_StreamProxyCreateReq">StreamProxyCreateReq</h2>

<a id="schemastreamproxycreatereq"></a>
<a id="schema_StreamProxyCreateReq"></a>
<a id="tocSstreamproxycreatereq"></a>
<a id="tocsstreamproxycreatereq"></a>

```json
{
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|app|string|true|none||应用名称|
|stream|string|true|none||流ID|
|url|string|true|none||拉流地址|
|description|string|false|none||代理描述|
|status|integer|false|none||是否启用|
|serverId|string|false|none||节点ID，指定创建代理的ZLM节点|
|streamProxyExtendReq|[StreamProxyExtendReq](#schemastreamproxyextendreq)|false|none||================================<br />ZLM扩展参数对象|

<h2 id="tocS_StreamProxyUpdateReq">StreamProxyUpdateReq</h2>

<a id="schemastreamproxyupdatereq"></a>
<a id="schema_StreamProxyUpdateReq"></a>
<a id="tocSstreamproxyupdatereq"></a>
<a id="tocsstreamproxyupdatereq"></a>

```json
{
  "id": 1,
  "app": "live",
  "stream": "test",
  "url": "rtmp://live.hkstv.hk.lxdns.com/live/hks2",
  "description": "string",
  "status": 1,
  "serverId": "zlm-node-1",
  "extend": "string",
  "streamProxyExtendReq": {
    "vhost": "__defaultVhost__",
    "retryCount": -1,
    "rtpType": 0,
    "timeoutSec": 10,
    "schema": "rtsp",
    "enableHls": true,
    "enableHlsFmp4": false,
    "enableMp4": false,
    "enableRtsp": true,
    "enableRtmp": true,
    "enableTs": false,
    "enableFmp4": false,
    "hlsDemand": false,
    "rtspDemand": false,
    "rtmpDemand": false,
    "tsDemand": false,
    "fmp4Demand": false,
    "enableAudio": true,
    "addMuteAudio": false,
    "mp4SavePath": "string",
    "mp4MaxSecond": 300,
    "mp4AsPlayer": false,
    "hlsSavePath": "string",
    "modifyStamp": 0,
    "autoClose": false
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|true|none||代理ID|
|app|string|false|none||应用名称|
|stream|string|false|none||流ID|
|url|string|false|none||拉流地址|
|description|string|false|none||代理描述|
|status|integer|false|none||代理状态 1启用 0禁用|
|serverId|string|false|none||节点ID，指定创建代理的ZLM节点|
|extend|string|false|none||扩展字段|
|streamProxyExtendReq|[StreamProxyExtendReq](#schemastreamproxyextendreq)|false|none||================================<br />ZLM扩展参数对象|

<h2 id="tocS_StreamProxyQueryReq">StreamProxyQueryReq</h2>

<a id="schemastreamproxyqueryreq"></a>
<a id="schema_StreamProxyQueryReq"></a>
<a id="tocSstreamproxyqueryreq"></a>
<a id="tocsstreamproxyqueryreq"></a>

```json
{
  "id": 0,
  "app": "live",
  "stream": "test",
  "proxyKey": "string",
  "url": "string",
  "description": "string",
  "status": 0,
  "onlineStatus": 0,
  "serverId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|id|integer(int64)|false|none||代理ID|
|app|string|false|none||应用名称|
|stream|string|false|none||流ID|
|proxyKey|string|false|none||代理Key|
|url|string|false|none||拉流地址|
|description|string|false|none||代理描述|
|status|integer|false|none||状态 1启用 0禁用|
|onlineStatus|integer|false|none||在线状态 1在线 0离线|
|serverId|string|false|none||服务器ID|

<h2 id="tocS_AjaxResultStreamProxyListResp">AjaxResultStreamProxyListResp</h2>

<a id="schemaajaxresultstreamproxylistresp"></a>
<a id="schema_AjaxResultStreamProxyListResp"></a>
<a id="tocSajaxresultstreamproxylistresp"></a>
<a id="tocsajaxresultstreamproxylistresp"></a>

```json
{
  "key": {}
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|key|object|false|none||none|

<h2 id="tocS_Handler">Handler</h2>

<a id="schemahandler"></a>
<a id="schema_Handler"></a>
<a id="tocShandler"></a>
<a id="tocshandler"></a>

```json
{}

```

### 属性

*None*

<h2 id="tocS_MediaType">MediaType</h2>

<a id="schemamediatype"></a>
<a id="schema_MediaType"></a>
<a id="tocSmediatype"></a>
<a id="tocsmediatype"></a>

```json
{
  "type": "string",
  "subtype": "string",
  "parameters": {
    "key": "string"
  },
  "toStringValue": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|type|string|false|none||none|
|subtype|string|false|none||none|
|parameters|[MapString](#schemamapstring)|false|none||none|
|toStringValue|string¦null|false|none||none|

<h2 id="tocS_DataWithMediaType">DataWithMediaType</h2>

<a id="schemadatawithmediatype"></a>
<a id="schema_DataWithMediaType"></a>
<a id="tocSdatawithmediatype"></a>
<a id="tocsdatawithmediatype"></a>

```json
{
  "data": {},
  "mediaType": {
    "type": "string",
    "subtype": "string",
    "parameters": {
      "key": "string"
    },
    "toStringValue": "string"
  }
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|data|object|false|none||none|
|mediaType|[MediaType](#schemamediatype)|false|none||none|

<h2 id="tocS_Throwable">Throwable</h2>

<a id="schemathrowable"></a>
<a id="schema_Throwable"></a>
<a id="tocSthrowable"></a>
<a id="tocsthrowable"></a>

```json
{
  "detailMessage": "string",
  "cause": "this",
  "stackTrace": "new StackTraceElement[0]",
  "suppressedExceptions": "Collections.emptyList()"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|detailMessage|string|false|none||Specific details about the Throwable.  For example, for<br />{@code FileNotFoundException}, this contains the name of<br />the file that could not be found.|
|cause|[Throwable](#schemathrowable)|false|none||The throwable that caused this throwable to get thrown, or null if this<br />throwable was not caused by another throwable, or if the causative<br />throwable is unknown.  If this field is equal to this throwable itself,<br />it indicates that the cause of this throwable has not yet been<br />initialized.|
|stackTrace|[[StackTraceElement](#schemastacktraceelement)]|false|none||The stack trace, as returned by{@link #getStackTrace()}.<br /><br />The field is initialized to a zero-length array.  A{@code<br />    * null} value of this field indicates subsequent calls to{@link<br />    * #setStackTrace(StackTraceElement[])} and{@link<br />    * #fillInStackTrace()} will be no-ops.|
|suppressedExceptions|[[Throwable](#schemathrowable)]|false|none||The list of suppressed exceptions, as returned by{@link<br />    * #getSuppressed()}.  The list is initialized to a zero-element<br />unmodifiable sentinel list.  When a serialized Throwable is<br />read in, if the{@code suppressedExceptions} field points to a<br />zero-element list, the field is reset to the sentinel value.|

<h2 id="tocS_StackTraceElement">StackTraceElement</h2>

<a id="schemastacktraceelement"></a>
<a id="schema_StackTraceElement"></a>
<a id="tocSstacktraceelement"></a>
<a id="tocsstacktraceelement"></a>

```json
{
  "classLoaderName": "string",
  "moduleName": "string",
  "moduleVersion": "string",
  "declaringClass": "string",
  "methodName": "string",
  "fileName": "string",
  "lineNumber": 0,
  "format": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|classLoaderName|string|false|none||The name of the class loader.|
|moduleName|string|false|none||The module name.|
|moduleVersion|string|false|none||The module version.|
|declaringClass|string|false|none||The declaring class.|
|methodName|string|false|none||The method name.|
|fileName|string|false|none||The source file name.|
|lineNumber|integer|false|none||The source line number.|
|format|integer|false|none||Control to show full or partial module, package, and class names.|

<h2 id="tocS_Runnable">Runnable</h2>

<a id="schemarunnable"></a>
<a id="schema_Runnable"></a>
<a id="tocSrunnable"></a>
<a id="tocsrunnable"></a>

```json
{}

```

### 属性

*None*

<h2 id="tocS_DefaultCallback">DefaultCallback</h2>

<a id="schemadefaultcallback"></a>
<a id="schema_DefaultCallback"></a>
<a id="tocSdefaultcallback"></a>
<a id="tocsdefaultcallback"></a>

```json
{
  "delegates": "new ArrayList<>(1)"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|delegates|[[Runnable](#schemarunnable)]|false|none||none|

<h2 id="tocS_ErrorCallback">ErrorCallback</h2>

<a id="schemaerrorcallback"></a>
<a id="schema_ErrorCallback"></a>
<a id="tocSerrorcallback"></a>
<a id="tocserrorcallback"></a>

```json
{}

```

### 属性

*None*

<h2 id="tocS_SseEmitter">SseEmitter</h2>

<a id="schemasseemitter"></a>
<a id="schema_SseEmitter"></a>
<a id="tocSsseemitter"></a>
<a id="tocssseemitter"></a>

```json
{
  "timeout": 0,
  "handler": {},
  "earlySendAttempts": "new LinkedHashSet<>(8)",
  "complete": true,
  "failure": {
    "detailMessage": "string",
    "cause": "this",
    "stackTrace": "new StackTraceElement[0]",
    "suppressedExceptions": "Collections.emptyList()"
  },
  "timeoutCallback": "new DefaultCallback()",
  "errorCallback": "new ErrorCallback()",
  "completionCallback": "new DefaultCallback()"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|timeout|integer(int64)¦null|false|none||none|
|handler|[Handler](#schemahandler)|false|none||none|
|earlySendAttempts|[[DataWithMediaType](#schemadatawithmediatype)]|false|none||Store send data before handler is initialized.|
|complete|boolean|false|none||Store successful completion before the handler is initialized.|
|failure|[Throwable](#schemathrowable)|false|none||Store an error before the handler is initialized.|
|timeoutCallback|[DefaultCallback](#schemadefaultcallback)|false|none||none|
|errorCallback|[ErrorCallback](#schemaerrorcallback)|false|none||none|
|completionCallback|[DefaultCallback](#schemadefaultcallback)|false|none||none|

<h2 id="tocS_PtzControlReq">PtzControlReq</h2>

<a id="schemaptzcontrolreq"></a>
<a id="schema_PtzControlReq"></a>
<a id="tocSptzcontrolreq"></a>
<a id="tocsptzcontrolreq"></a>

```json
{
  "deviceId": "string",
  "channelId": "string",
  "command": "string",
  "speed": 128
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|true|none||none|
|channelId|string|true|none||none|
|command|string|true|none||UP/DOWN/LEFT/RIGHT/UP_LEFT/UP_RIGHT/DOWN_LEFT/DOWN_RIGHT/ZOOM_IN/ZOOM_OUT/STOP (PTZControlEnum)|
|speed|integer|false|none||none|

<h2 id="tocS_PtzStopReq">PtzStopReq</h2>

<a id="schemaptzstopreq"></a>
<a id="schema_PtzStopReq"></a>
<a id="tocSptzstopreq"></a>
<a id="tocsptzstopreq"></a>

```json
{
  "deviceId": "string",
  "channelId": "string"
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|true|none||none|
|channelId|string|true|none||none|

<h2 id="tocS_PresetReq">PresetReq</h2>

<a id="schemapresetreq"></a>
<a id="schema_PresetReq"></a>
<a id="tocSpresetreq"></a>
<a id="tocspresetreq"></a>

```json
{
  "deviceId": "string",
  "channelId": "string",
  "action": "string",
  "presetId": 0
}

```

### 属性

|名称|类型|必选|约束|中文名|说明|
|---|---|---|---|---|---|
|deviceId|string|true|none||none|
|channelId|string|true|none||none|
|action|string|true|none||SET / GOTO / DEL|
|presetId|integer|false|none||none|

