# Voglander 前端开发规则

## 页面开发原则

### 1. 页面聚焦原则

- 不要创建过于发散的页面，只需要关注与当前页面相关的功能
- 每个页面应该有明确的业务边界和职责划分
- 避免在单个页面中混合多个不相关的业务逻辑

### 2. 后端接口优先原则

- 所有的页面都要严格按照后端接口实现和字段定义来开发
- 前端不应该自主创新字段或接口，必须与后端保持一致
- 字段名称、数据类型、接口路径等必须完全匹配后端定义
- 如果需要新增字段或修改接口，必须先在此规则文件中说明需求

## 字段扩展规则

### 当前已确认的字段扩展

*（暂无，如有新增需求会在此处记录）*

### 字段扩展申请流程

1. 在此文件中记录字段扩展需求
2. 说明业务场景和必要性
3. 定义字段类型和约束
4. 等待后端实现后再进行前端开发

## 开发规范

### 1. API调用规范

- 使用已定义的API接口，不允许自定义接口路径
- 严格按照后端返回的数据结构进行数据处理
- 错误处理必须按照后端定义的错误码和错误信息

### 2. 数据模型规范

- 前端数据模型必须与后端DTO/VO保持一致
- 不允许在前端临时添加不存在于后端的字段
- 数据验证规则必须与后端保持同步

### 3. 页面功能规范

- 每个页面的CRUD操作必须对应后端的具体接口
- 查询条件、分页参数等必须与后端接口参数保持一致
- 表单字段必须与后端实体字段完全匹配

### 4. 枚举数据规范 ✅ 新增

- 所有枚举值必须通过后端接口动态获取，禁止前端硬编码
- 使用统一的枚举接口 `/api/v1/enum/all` 获取所有枚举数据
- 枚举数据格式统一为：`{value: Integer, code: String, desc: String}`
- 关联字段自动计算：如协议类型根据设备种类+协议自动计算

## 已完成的页面开发

### 1. 设备管理页面 (Device.vue)

**页面位置:** `voglander-frontend/src/views/Device.vue`

**对接的后端接口:**

- `DeviceController` - 设备管理控制器
- `DeviceVO` - 设备视图对象
- `DeviceCreateReq` - 设备创建请求对象
- `DeviceUpdateReq` - 设备更新请求对象

**实现的功能:**

- ✅ 设备列表分页查询 (`/api/v1/device/pageListByEntity/{page}/{size}`)
- ✅ 设备统计信息 (`/api/v1/device/count`, `/api/v1/device/countByEntity`)
- ✅ 设备详情查看 (`/api/v1/device/get/{id}`)
- ✅ 设备添加 (`/api/v1/device/insert`)
- ✅ 设备编辑 (`/api/v1/device/update`)
- ✅ 设备删除 (`/api/v1/device/delete/{id}`)
- ✅ 批量添加设备 (`/api/v1/device/insertBatch`)
- ✅ 批量删除设备 (`/api/v1/device/deleteIds`)
- ✅ 条件搜索 (按设备ID、名称、IP、状态)
- ✅ 动态枚举加载 (`/api/v1/enum/all`)
- ✅ 智能协议类型计算 (根据设备种类+协议自动计算)

**数据字段对应关系:**

```
DeviceVO字段 -> 前端显示
├── id -> ID
├── deviceId -> 设备ID
├── name -> 设备名称
├── typeName -> 设备类型 (直接显示后端返回值)
├── ip -> IP地址
├── port -> 端口
├── status -> 状态值 (1=在线, 0=离线)
├── statusName -> 状态名称 (直接显示后端返回值)
├── serverIp -> 注册节点
├── registerTime -> 注册时间
├── keepaliveTime -> 心跳时间
├── createTime -> 创建时间
├── updateTime -> 更新时间
└── extendInfo -> 扩展信息
    ├── serialNumber -> 序列号
    ├── transport -> 传输协议
    ├── expires -> 注册有效期
    ├── password -> 密码
    ├── streamMode -> 数据流模式
    ├── charset -> 编码
    └── deviceInfo -> 设备信息
```

## 当前系统接口梳理

### 设备管理接口 (DeviceController)

基础路径: `/api/v1/device/`

**查询接口:**

- `GET /get/{id}` - 根据ID获取设备详情
- `GET /get` - 根据设备实体获取单个设备
- `GET /list` - 获取设备列表
- `GET /pageListByEntity/{page}/{size}` - 分页查询设备（带条件）✅
- `GET /pageList/{page}/{size}` - 分页查询设备
- `GET /count` - 获取设备总数 ✅
- `GET /countByEntity` - 根据条件获取设备总数 ✅

**操作接口:**

- `POST /insert` - 插入单个设备 ✅
- `POST /insertBatch` - 批量插入设备 ✅
- `PUT /update` - 更新设备 ✅
- `PUT /updateBatch` - 批量更新设备
- `DELETE /delete/{id}` - 根据ID删除单个设备 ✅
- `DELETE /deleteByEntity` - 根据设备实体删除设备
- `DELETE /deleteIds` - 批量删除设备 ✅ (注意:不是delete接口)

### 枚举接口 (EnumController) ✅ 新增

基础路径: `/api/v1/enum/`

**枚举查询接口:**

- `GET /device-sub-types` - 获取设备种类枚举 ✅
- `GET /device-protocols` - 获取设备协议枚举 ✅
- `GET /device-agreement-types` - 获取设备协议类型枚举 ✅
- `GET /device-agreement-type?subType=&protocol=` - 根据种类和协议计算协议类型 ✅
- `GET /all` - 获取所有枚举数据 ✅

## 前端技术栈规范

### 1. UI组件库

- 使用 Element Plus 作为主要UI组件库
- 统一的组件使用规范和样式风格

### 2. 状态管理

- 使用 Vue 3 Composition API
- 页面状态使用 reactive/ref 管理

### 3. 网络请求

- 统一使用 axios 进行HTTP请求
- 请求/响应拦截器统一处理
- 错误处理统一在拦截器中处理

### 4. 时间处理

- 统一的时间格式化函数 `formatDateTime`
- 格式：YYYY-MM-DD HH:mm:ss

## 页面开发任务清单

- [x] 设备管理页面 (Device.vue) - 已完成
- [ ] 设备配置页面 (DeviceConfig.vue)
- [ ] 设备通道管理页面 (DeviceChannel.vue)
- [ ] 导出管理页面

## 待确认接口

- 设备配置相关接口 (DeviceConfigController)
- 设备通道相关接口 (DeviceChannelController)
- 导出相关接口 (ExportController)

---

**注意：此规则文件会随着开发进度持续更新，所有前端开发都应该遵循此规则。**