# 前后端请求关联排查

仅在 `diagnose-error-logs` 已将问题分诊到页面/API/Web 请求链路后加载。目标是用同一条请求的证据把浏览器、前端请求封装、HTTP 传输、Spring 参数绑定和后端业务串起来。

核心规则：**先证明请求在线上实际长什么样，再判断哪一端有错。源码中的对象、单元测试 mock 和预期契约都不能代替线上报文。**

除另有说明，以下命令从 voglander 后端仓库根目录执行。

## 1. 锁定当前请求

先记录五项事实：当前实例启动时间、错误时间、URI、异常类型、重复次数。不要从整份历史日志里直接挑一条栈开始改代码。

```bash
ls -lt ~/logs/voglander/
rg -n "Started ApplicationWeb|profiles are active" ~/logs/voglander/voglander-info.log* | tail -20
ps -ww -eo lstart,pid,args | rg "[v]oglander-web|[A]pplicationWeb"

# 用真实 URI 或异常短语替换关键词；同时看上下文和出现次数
rg -n -C 12 "请求URI|异常短语" ~/logs/voglander/voglander-{error,info}.log
rg -c "异常短语" ~/logs/voglander/voglander-error.log*
```

判读：

- 错误早于当前进程启动：旧实例残留，不能证明当前版本仍失败。
- 同一 URI 短时间重复：区分用户重试、前端自动重试、双击提交和组件重复触发。
- 后端没有同刻记录：请求可能被浏览器、CORS、前端代理、网关或错误 baseURL 挡住，也可能命中了另一实例。

## 2. 提取后端接口契约

从日志 URI 反查 Controller，不从前端类型猜后端契约。

```bash
rg -n "接口路径片段|@RequestMapping|@GetMapping|@PostMapping|@PutMapping|@DeleteMapping" \
  voglander-web/src/main/java --glob '*.java'
rg -n "RequestBody|RequestParam|RequestHeader|PathVariable|MultipartFile|consumes" \
  voglander-web/src/main/java --glob '*.java'
```

逐项写出契约：

- HTTP method 和完整 path。
- `consumes` / 请求 `Content-Type`。
- 必需与可选 header。
- path、query、form、multipart part、JSON body 的参数位置。
- 字段名、类型、时间单位、枚举和值域。
- HTTP status、`AjaxResult.code`、响应结构和二进制响应类型。

### Spring 绑定异常速查

- `MultipartException: Current request is not a multipart request`：整个请求不是 multipart；不是“文件内容不合法”。
- `MissingServletRequestPartException` 或缺少 `MultipartFile` 参数：请求可能是 multipart，但 part 名称或内容缺失。
- `HttpMediaTypeNotSupportedException`：`Content-Type` 与 `consumes` 不匹配。
- `HttpMessageNotReadableException`：JSON 语法、body 为空或 DTO 反序列化失败。
- `MissingServletRequestParameterException`：query/form 参数缺失。
- `MissingRequestHeaderException`：必需 header 缺失。
- `MethodArgumentTypeMismatchException`：path/query 字符串无法转换为目标类型。

栈停在 `HandlerMethodArgumentResolver`、消息转换器或 multipart resolver，且没有 Controller 业务方法、Service 方法入栈时，请求尚未进入业务逻辑。

## 3. 追前端调用链

voglander 前端通常位于后端仓库的相邻目录 `../voglander-vben-frontend`。先确认实际路径，不假设旧目录名。

```bash
FRONTEND_ROOT="../voglander-vben-frontend"
test -d "$FRONTEND_ROOT" && echo "frontend found"

rg -n "接口路径片段" "$FRONTEND_ROOT/apps/web-antd/src" --glob '*.ts' --glob '*.vue'
rg -n "调用函数名" "$FRONTEND_ROOT/apps/web-antd/src" --glob '*.ts' --glob '*.vue'
rg -n "Content-Type|FormData|requestClient|requestWithErrorMeta|interceptor|transformRequest" \
  "$FRONTEND_ROOT/apps/web-antd/src" "$FRONTEND_ROOT/packages/effects/request/src" --glob '*.ts'
```

按顺序核对：调用组件 → API 函数 → 通用请求 helper → RequestClient 默认配置 → 请求拦截器 → Axios/Fetch 转换器。

重点问题：

- API 是否使用正确 method、path 和参数位置。
- 字段名、时间单位、枚举和空值处理是否镜像 Controller。
- 通用客户端是否默认设置 JSON `Content-Type`。
- 局部 header 是合并、覆盖还是被后续拦截器重写。
- retry、token refresh 或错误处理是否重新构造了请求体。
- 上传、下载、SSE 是否误走普通 JSON 请求 helper。

## 4. 区分源码对象与实际传输

Axios 会根据合并后的 header 和 data 执行 `transformRequest`。例如 `FormData` 遇到显式 JSON Content-Type 时，可能先转为 JSON；文件字段随后表现为 `{}`。所以“调用前 `data instanceof FormData`”只能证明输入，不能证明发送结果。

先检查项目实际安装的 Axios 实现，不凭版本记忆：

```bash
FRONTEND_ROOT="../voglander-vben-frontend"
rg -n "isFormData|hasJSONContentType|formDataToJSON|transformRequest" \
  "$FRONTEND_ROOT/packages/effects/request/node_modules/axios/lib" \
  "$FRONTEND_ROOT/packages/effects/request/node_modules/axios/dist" \
  --glob '*.js' | head -40
```

需要最小复现时，使用当前项目安装的 Axios 和自定义 adapter 捕获**转换后的** `config.data` 与 `config.headers`；只打印类型、长度和非敏感 header，不打印 token、Cookie 或文件正文。

multipart 判据：

- 转换后 data 仍是 `FormData`，而不是 JSON 字符串。
- 浏览器请求头是 `multipart/form-data; boundary=...`。boundary 应由浏览器/Axios adapter 生成。
- Request Payload/Form Data 中 part 名与 Controller 的 `@RequestParam`/`@RequestPart` 一致。

不要仅凭单元测试里 mock 收到 `FormData` 就下结论；mock 往往位于 Axios 转换之前。

## 5. 浏览器 Network 是传输事实

在失败请求的 Network 详情中核对：

1. Request URL、method、status 和是否发生 redirect。
2. Request Headers 的 Content-Type、Accept、必要业务 header；Authorization/Cookie 只确认存在，不复制值。
3. Query String、Request Payload 或 Form Data 的参数位置和字段名。
4. Response Headers、HTTP status、响应体业务码和 message。
5. Initiator、重试次数和是否由 service worker/代理改写。

常见判读：

- 浏览器没有请求：组件校验、事件绑定或前端异常阻断。
- OPTIONS 失败：CORS/preflight，不是 Controller 业务失败。
- 404 且后端无日志：baseURL、代理前缀、静态服务或错误实例。
- 400/415 且绑定异常：传输契约问题。
- 401/403：认证/权限；确认 header 存在后再查后端鉴权。
- 5xx 且有业务栈：追最底层 `Caused by` 和外部依赖。
- HTTP 200 但 `AjaxResult.code != 0`：业务拒绝；检查前端响应拦截器如何解包。
- HTTP 与业务码都成功但页面异常：类型映射、字段名、状态管理、缓存或渲染逻辑。

## 6. 双向对照与根因归属

用同一张清单对照三份事实：Controller 契约、前端请求配置、浏览器实际请求。

| 断点 | 典型证据 | 根因方向 |
|---|---|---|
| 前端构造 | API method/path/字段已与 Controller 不同 | 前端 API 封装 |
| 请求转换 | 调用前对象正确，转换后 header/body 错 | RequestClient、Axios/Fetch、拦截器 |
| 代理/网关 | 浏览器请求与后端命中 URI/实例不一致 | baseURL、dev proxy、反向代理、CORS |
| Spring 绑定 | 线上请求与 Controller 的 consumes/参数位置不同 | 前后端传输契约 |
| 后端业务 | Controller/Service 已入栈，最底层异常明确 | voglander 业务或外部依赖 |
| 前端消费 | 后端响应正确，页面解包/映射/状态错误 | 响应拦截器、类型、store、组件 |
| 部署版本 | 源码正确，线上请求仍是旧格式 | 前端 dist、后端 JAR/进程、浏览器缓存 |

不要把“异常由 Spring logger 打出”直接归为后端 bug，也不要把“浏览器发错请求”直接归为页面组件 bug；公共请求封装和部署产物是独立断点。

## 7. 核对运行版本

前后端源码都正确但现象不变时，排查实际运行物：

```bash
# 后端进程、启动时间和产物时间
ps -ww -eo lstart,pid,args | rg "[v]oglander-web|[A]pplicationWeb"
stat voglander-web/target/voglander.jar

# 前端源码版本与构建产物时间
FRONTEND_ROOT="../voglander-vben-frontend"
git -C "$FRONTEND_ROOT" rev-parse --short HEAD
stat "$FRONTEND_ROOT/apps/web-antd/dist/index.html" \
  "$FRONTEND_ROOT/apps/web-antd/dist.zip"
```

再确认静态服务器实际目录、部署时间、浏览器缓存和 service worker。构建成功不等于新产物已经部署。

## 8. 最小复现与验证门禁

诊断结论至少满足以下一种强证据：

- 捕获到转换后的错误 header/body，并在调整后保持正确类型。
- 浏览器 Network 明确显示契约不一致。
- curl/MockMvc 用正确与错误 Content-Type 得到可区分的结果。
- 同一请求在修复前稳定失败、修复后稳定成功，且后端不再产生该异常。

修复请求只在用户明确要求时实施。修复后按影响范围执行：

```bash
# 前端 API 契约专项测试
pnpm -C ../voglander-vben-frontend exec vitest run --dom \
  apps/web-antd/src/api/__tests__/<target>.test.ts

# 前端静态门禁
pnpm -C ../voglander-vben-frontend --filter @vben/web-antd typecheck
pnpm -C ../voglander-vben-frontend exec eslint <changed-files>
pnpm -C ../voglander-vben-frontend --filter @vben/web-antd build

# 后端 Controller/契约专项测试（按实际类替换）
mvn test -Dtest=<ControllerContractTest>
```

最终再用真实浏览器请求确认 method、Content-Type、boundary、payload、status 和业务码，并检查当前实例日志没有新增同类异常。

## 9. 输出结论模板

报告保持证据优先：

1. **结论**：故障位于哪一边、哪一层。
2. **时间线**：实例启动、请求失败、重复次数。
3. **契约差异**：Controller、前端配置、线上请求三者哪里不同。
4. **机制解释**：为什么该差异会产生当前异常，而不是只复述堆栈。
5. **验证证据**：最小复现、测试和 Network 结果。
6. **剩余不确定性**：无法读取浏览器或部署环境时明确说明，不把推断写成事实。

全程禁止输出 Authorization、Cookie、JWT、refresh token、secret、上传文件内容、Base64 或带敏感 query 的内部 URL。
