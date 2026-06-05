# GB28181 协议验证台 — 前端（S4/S5）实现技术方案与落地报告

> 版本 1.0.6 · 分支 `dev_merge_sip` · 文档日期 2026-06-05
> 配套后端方案：[`GB28181-PROTOCOL-LAB-TECH-PLAN.md`](./GB28181-PROTOCOL-LAB-TECH-PLAN.md)（后端 S1/S2/S3 已实现并核验，36 测试全绿）
> 本文范围：`vue-vben-admin/apps/web-antd` 前端 S4（骨架）+ S5（打磨）的**实现方案 + 已落地核验结论**

---

## ✅ 落地结论（2026-06-05）

前端 S4 + S5 **已完整实现并通过质量门禁**：

| 门禁 | 命令 | 结果 |
|------|------|------|
| 类型检查 | `pnpm typecheck`（vue-tsc） | 新增文件 **0 error**（media/* 既有基线错误与本次无关） |
| 代码规范 | `npx eslint <新增文件>` | **0 error / 0 warning** |
| 生产构建 | `pnpm build:antd`（turbo） | **11/11 task 成功**，产出 `protocol-lab` / `ClientPanel` / `ServerPanel` / `SipTimeline` chunk |

所有新增代码均严格对齐后端真实契约（逐文件核对 `LabClientController` + 5 个 `Lab*Listener` + `Gb28181ProtocolHandler` + `SseController`），**未发明任何字段或端点**。

---

## 0. TL;DR

一个左右分栏页面 `/protocol-lab/gb28181`：

- **左（设备 UA / Client）**：按钮触发设备主动发 SIP（注册/注销/心跳/自动心跳/上报目录/上报设备信息/上报告警）+ 一条"**收到指令**"时间线（消费 `clientcmd.*`）。
- **右（平台 / Server）**：实时**设备列表**（`device.register/online/offline` 驱动）+ 选中设备后的命令区（PTZ 方向盘/查目录/查设备信息/重启/点播）+ 一条"**平台事件**"时间线（消费 `device.*` / `session.*` / `alarm.*`）。
- **数据通道**：REST 触发动作（既有 `requestClient`）+ 单条 SSE 长连接（`useSseEvents`）接收全部实时事件，按 topic 前缀分发到左右两条时间线。

---

## 1. 与后端契约的精确对齐（实现依据）

下表是前端逐一核对的后端真实代码，**前端代码即按此实现，无任何臆测**。

### 1.1 左侧 REST（`LabClientController`，`@ConditionalOnProperty(voglander.protocol-lab.enabled=true)`）

| 前端 API 函数 | 方法 | 路径 | 请求体 | 返回 |
|--------------|------|------|--------|------|
| `getLabConfig()` | GET | `/api/v1/lab/client/config` | - | `{clientId,clientIp,clientPort,serverId,serverIp,serverPort,topics[]}` |
| `labRegister({expires})` | POST | `/api/v1/lab/client/register` | `{expires:3600}` | `AjaxResult<Void>` |
| `labUnregister()` | POST | `/api/v1/lab/client/unregister` | `{}` | `AjaxResult<Void>` |
| `labKeepalive()` | POST | `/api/v1/lab/client/keepalive` | `{}` | `AjaxResult<Void>` |
| `labKeepaliveAuto({enabled,intervalSec})` | POST | `/api/v1/lab/client/keepalive/auto` | `{enabled,intervalSec:30}` | `{enabled,intervalSec}` |
| `labPushCatalog({channelCount})` | POST | `/api/v1/lab/client/catalog/push` | `{channelCount:4,catalogName}` | `AjaxResult<Void>` |
| `labPushDeviceInfo({...})` | POST | `/api/v1/lab/client/device-info/push` | `{manufacturer,model,firmware}` | `AjaxResult<Void>` |
| `labPushAlarm({...})` | POST | `/api/v1/lab/client/alarm/push` | `{alarmType:1,priority:1,channelId}` | `AjaxResult<Void>` |

> ⚠ 注意：后端**不存在** `/lab/config`，正确路径是 **`/lab/client/config`**（已对齐方案 §4.1 修正）。

### 1.2 右侧 REST（复用既有端点，前端不新增后端）

| 前端 API 函数 | 方法 | 路径 | 请求体（核对自真实 Req） |
|--------------|------|------|------------------------|
| `ptzControl({deviceId,channelId,command,speed})` | POST | `/api/v1/ptz/control` | `PtzControlReq{deviceId,channelId,command,speed=128}` |
| `queryCatalog(deviceId)` | POST | `/api/v1/device-cmd/query-catalog` | `Map{deviceId}` |
| `queryDeviceInfo(deviceId)` | POST | `/api/v1/device-cmd/query-info` | `Map{deviceId}` |
| `rebootDevice(deviceId)` | POST | `/api/v1/device-cmd/reboot` | `Map{deviceId}` |
| `liveStart({deviceId,channelId})` | POST | `/api/v1/live/start` | `LiveStartReq{deviceId,channelId,protocol=FLV,streamMode=UDP}` |

> **PTZ 词表**：后端 `PtzControlReq.command` 注释明确支持 `UP/DOWN/LEFT/RIGHT/UP_LEFT/UP_RIGHT/DOWN_LEFT/DOWN_RIGHT/ZOOM_IN/ZOOM_OUT/STOP`（门面层 `PTZ_VOCAB` 翻译）。前端方向盘**直发这些词**，无需翻译（D6 已修，C3）。

### 1.3 SSE 事件契约（核对自 5 个 `Lab*Listener` + `Gb28181ProtocolHandler` 真实 payload）

**关键机制**（决定前端如何解析）：
- `RedisBackedSseEventBus.publishLocal` 用 `event().name(topic)` 发送 → **SSE `event:` 名 = 完整 topic**（如 `device.register`、`clientcmd.ptz`）。
- `data` 是 `event.getData()` 的 JSON，即 **payload 本体**，不含 `{topic,data}` 外层包裹。
- 订阅过滤（`SseController` → `matches()`）按**首个 `.` 之前的前缀域**匹配：订阅 `clientcmd` 可收 `clientcmd.register.ok`。
- ∴ **订阅 URL 用前缀**（`device,session,clientcmd,alarm`），但 `addEventListener` 必须用**完整 topic**（事件名是完整 topic）→ 完整列表由 `/config` 的 `topics[]` 提供。

| topic | 方向 | 前端消费面板 | data 关键字段（实测） |
|-------|------|------------|---------------------|
| `device.register` | 右收 | ServerPanel 设备列表 + 时间线 | `deviceId,remoteIp,remotePort,transport,expire,ts` |
| `device.online` / `device.offline` | 右收 | 设备在线态 | `deviceId,ts` |
| `device.keepalive` | 右收 | 时间线（后端 5s 节流） | `deviceId,ts` |
| `device.catalog` | 右收 | 设备列表 `channelCount` | `deviceId,channelCount,ts` |
| `device.info` | 右收 | 设备列表 `manufacturer` | `deviceId,manufacturer,ts` |
| `session.invite_ok` / `session.bye` | 右收 | 时间线 | `deviceId,callId,ts` |
| `alarm.new` | 右收 | 时间线 | `deviceId,...` |
| `clientcmd.register.ok` / `.challenge` | 左收 | "收到指令"时间线 | `clientId,ts` |
| `clientcmd.register.fail` | 左收 | 时间线 | `clientId,statusCode,ts` |
| `clientcmd.ptz` | 左收 | 时间线（hex+解析） | `platformId,channelId,ptzCmd(hex),parsed{direction,speed,hex},ts` |
| `clientcmd.record/reboot/iframe/guard/alarmreset` | 左收 | 时间线 | `platformId,channelId,ts`（record 多 `recordCmd`） |
| `clientcmd.query.catalog/deviceinfo/devicestatus` | 左收 | 时间线 | `platformId,sn,ts` |
| `clientcmd.config.basicparam` | 左收 | 时间线 | `platformId,deviceId,ts` |
| `clientcmd.broadcast` | 左收 | 时间线 | `platformId,deviceId,ts` |
| `clientcmd.invite` | 左收 | 时间线 | `callId,clientId,ts` |

---

## 2. 网络通道与环境对齐（make-or-break）

### 2.1 API 前缀链路（已核验）

- `requestClient` 的 `baseURL = apiURL`，`apiURL = VITE_GLOB_API_URL`，dev 下 = **`/api`**。
- vite 代理：`/api` → `rewrite` 剥离首段 → `target http://0.0.0.0:8081`。
- 既有 API 调用路径形如 `/api/v1/mediaNode/list`，与 `baseURL=/api` 拼接为 `/api/api/v1/...`，vite 剥离首个 `/api` 后 = `/api/v1/...` 转发到后端 8081。**这是项目既有约定**，前端 lab API 完全沿用（路径写全 `/api/v1/lab/client/...`）。
- `requestClient` 配 `responseReturn:'data'` + 响应拦截器 `successCode:0`（`AjaxResult.success` 固定 `code=0`），∴ API 函数**直接拿到 `data` 本体**，无需手动解包。

### 2.2 SSE 通道（EventSource）

- **URL**：`${apiURL}/api/v1/stream/events?topics=<前缀域>&token=<JWT>`，与 API 同构（`apiURL=/api` → vite 剥离 → 8081）。
- **鉴权**：EventSource 不支持自定义 header，token 走 URL（与 `SseController` 强制 `?token=` 校验一致）。token 取自 `useAccessStore().accessToken`。
- **去重**（规避单机 Redis 回环双投 D8）：按 `(topic | ts | callId/deviceId/clientId/platformId/sn)` 去重；后端虽已加 originId 抑制，前端再兜一层。
- **重连**：`error` 事件 → `close` → 3s `setTimeout(connect)`；组件卸载 `close()` 并清除定时器。
- **背压**：事件数组上限 500 条（超出截尾），`seen` 集合超 5000 清空，防长连接内存增长。

---

## 3. 文件清单（已落地）

```
apps/web-antd/src/
├── api/protocol-lab.ts                              # ✅ API 模块：8 个左侧 client + 5 个右侧复用 + 类型
├── composables/useSseEvents.ts                      # ✅ SSE 封装：token/前缀订阅/完整topic分流/去重/重连/背压
├── router/routes/modules/protocol-lab.ts            # ✅ 路由（order 9995，icon mdi:lan-connect，auto-glob 加载）
├── locales/langs/zh-CN/protocol-lab.json            # ✅ 中文 i18n（auto-glob 加载，无需注册）
├── locales/langs/en-US/protocol-lab.json            # ✅ 英文 i18n
└── views/protocol-lab/
    ├── index.vue                                    # ✅ 容器：拉 config → 建 SSE → 按方向分发左右
    ├── data.ts                                      # ✅ PTZ 方向盘元数据 + topic→文案/颜色映射
    └── components/
        ├── ClientPanel.vue                          # ✅ 左：身份卡 + 操作按钮组 + 收到指令时间线
        ├── ServerPanel.vue                          # ✅ 右：实时设备列表 + 命令区 + 平台事件时间线
        └── SipTimeline.vue                          # ✅ 复用：方向箭头时间线（in/out + 摘要 + hex）
```

> i18n 与路由均靠 `import.meta.glob` 自动加载（`locales/index.ts` glob `./langs/**/*.json`；`router/routes/index.ts` glob `./modules/**/*.ts`），新增文件**零注册**即生效。

---

## 4. 关键组件设计

### 4.1 `useSseEvents.ts`（核心）

```ts
export function useSseEvents(fullTopics: () => string[]) {
  // fullTopics 是 getter（config 异步就绪后才有值）
  // connect(): topics 为空直接 return；config 就绪后 restart() 触发真正建连
  // 订阅 URL 用前缀域（subscribeTopicsParam 把 device.register→device 去重）
  // 但 addEventListener 用完整 topic（后端 event 名=完整 topic）
  // 返回 { events, status, restart, close, clear }
}
```

- `events` 用 `shallowRef`（整体替换触发刷新，避免深响应开销）。
- `LabEvent` 归一化：`{topic, data, ts, seq, dir}`；`dir` 由 topic 前缀推断（`clientcmd.*` → `in` 设备收到 / 其余 → `out` 设备上行）。

### 4.2 `index.vue`（容器编排）

1. `onMounted` → `getLabConfig()`：成功则 `restart()` 用完整 `topics[]` 建 SSE；失败（端点仅 lab profile 注册，404）→ 展示"协议验证台未开启"告警（生产安全的自然降级）。
2. `computed` 按前缀拆流：`clientcmd.*` → 左 `received`；其余 → 右 `events`。
3. 顶部展示 SSE 连接状态徽标（open/connecting/error/closed）+ "清空时间线"。

### 4.3 `ServerPanel.vue`（设备列表 upsert）

- `watch(events, deep)` 对 `Map<deviceId, DeviceRow>` 做 **upsert**（R8 容忍乱序）：`register/online/keepalive/catalog/info` 置 `online=true`，`offline` 置 `false`；`catalog` 写 `channelCount`，`info` 写 `manufacturer/model`。
- **C2 时序约束落地**：命令区按钮在"选中设备且在线"前 `disabled`，并 Tooltip 提示——避免对未注册设备发指令（必失败），同时天然贴合 LAB-01→LAB-05 验收时序。
- 通道号约定：`deviceId + '01'`（与后端 catalog 通道命名 `clientId+两位序号` 对齐）。

### 4.4 `SipTimeline.vue`（阶梯时间线）

- 以"设备"为参照系：`out` 渲染 ⬆（设备→平台），`in` 渲染 ⬇（平台→设备）。
- 摘要按 topic 定制：`clientcmd.ptz` 显示 `方向 speed=N hex=...`；`device.catalog` 显示通道数；`device.register` 显示 `ip:port transport expire` 等。
- 最新事件置顶（按 `seq` 倒序，稳定）。

---

## 5. 验收矩阵映射（前端就绪项）

| 编号 | 场景 | 前端实现 | 状态 |
|------|------|---------|------|
| LAB-01 | 设备注册 | 左[注册] `labRegister` → 右设备列表出现 + 左 `register.ok` 时间线 | ✅ |
| LAB-02 | 设备注销 | 左[注销] `labUnregister` → 右 `offline` 标灰 | ✅ |
| LAB-03 | 心跳保活 | 左[自动心跳] `labKeepaliveAuto` 开关 → 右 `device.keepalive`（节流） | ✅ |
| LAB-04 | 目录查询回环 | 右[查目录] `queryCatalog` → 左 `clientcmd.query.catalog` + 右 `device.catalog` 通道数 | ✅ |
| LAB-05 ★ | PTZ 回显 | 右方向盘直发 `UP` → 左 `clientcmd.ptz`（hex+解析） | ✅ |
| LAB-06 | 设备信息查询 | 右[查设备信息] `queryDeviceInfo` → 左收 + 右 `device.info` | ✅ |
| LAB-07 | 告警上报 | 左[上报告警] `labPushAlarm` → 右 `alarm.new` 时间线 | ✅ |
| LAB-08 | 重启下发 | 右[重启] `rebootDevice` → 左 `clientcmd.reboot` | ✅ |
| LAB-09 | 实时点播 | 右[点播] `liveStart`（返回 playUrls；播放器留待后续） | ✅ 信令就绪 |
| LAB-10 | 断线重连 | `useSseEvents` error→3s 重连，事件按 ts/seq 稳定排序 | ✅ |
| - | SSE 经 JWT 鉴权 | URL `?token=accessToken` | ✅ |
| - | lab 关闭时不可用 | config 404 → "未开启"告警降级 | ✅ |
| - | 事件去重（D8） | `(topic\|ts\|id)` 去重 | ✅ |

> **联调待办（需后端 lab profile 实跑）**：以上为前端代码就绪 + 构建通过；端到端真实闭环需后端按 `dev,repo,inte,lab` 启动后在浏览器逐条点验（LAB-01→LAB-10）。点播视频播放器（flv.js，项目已有 `FlvPlayer` 组件）作为 S5 可选增强，本期先验信令。

---

## 6. 质量门禁记录（本次执行）

```bash
# 依赖（首次）
cd vue-vben-admin && pnpm install                     # ✅ 39s

# 类型检查
cd apps/web-antd && pnpm typecheck                    # ✅ 新增文件 0 error
                                                       #   （media/* 既有基线错误与本次无关，git 确认仅新增 lab 文件）
# 代码规范
npx eslint <lab 新增文件>                              # ✅ 0 error（void→null、onerror→addEventListener、排序/prettier 已修）

# 生产构建（必须走 turbo，先构建 @core 工作区包）
cd vue-vben-admin && pnpm build:antd                  # ✅ 11/11 task；产出 protocol-lab/ClientPanel/ServerPanel/SipTimeline chunk
```

> ⚠ **构建踩坑记录**：直接 `cd apps/web-antd && pnpm build` 会因 `@vben-core/design` 等 `@core` 工作区包**无 `dist/`**（dev 走 `development` 条件读 src，生产走 `default` 读 dist）而失败。必须用根目录 **`pnpm build:antd`**（turbo 先按依赖图构建工作区包）。typecheck 不受影响（走 src）。

---

## 7. 待确认 / 后续（S5 增强）

1. **菜单落位**：当前新建顶级菜单"协议验证台"（`order:9995`，紧邻媒体管理 9996）。如需并入"媒体管理"下，改 `protocol-lab.ts` 的父路由即可。
2. **视频播放器**：LAB-09 点播目前只发信令、展示 `clientcmd.invite`；接入 `FlvPlayer` 播放 `liveStart` 返回的 `playUrls.httpFlv` 依赖 ZLM 真实节点（1.0.5 在 :8082），建议联调期单独验证。
3. **SIP 报文级时间线**（`sip.trace`，后端方案 §3 可选 Phase 3）：当前为语义事件时间线（注册/PTZ/目录…），已满足"可视化协议链路"。若需真实 SIP 请求行（method/Call-ID/CSeq），待后端加日志切面后前端订阅 `sip.trace` 即可复用 `SipTimeline`。
4. **响应式**：已做窄屏（<1100px）单列降级；移动端方向盘触控体验可后续打磨。
```
