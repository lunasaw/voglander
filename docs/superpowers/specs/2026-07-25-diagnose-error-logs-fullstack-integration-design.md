# 前后端关联排查整合设计

日期：2026-07-25

## 背景

现有 `diagnose-error-logs` 能完成日志基线、实例时间线、logger 归因和跨应用分诊，但对 Web 请求故障只给出简短路由。页面/API 报错常跨越浏览器、前端 API 封装、Axios 转换、代理、Spring 参数绑定和后端业务层；仅看异常栈容易把请求契约错误误判为后端业务故障。

本次将前后端关联排查并入现有 skill，不创建第二个竞争入口。

## 目标

- 扩展现有 skill 的触发范围，覆盖页面/API 报错、上传失败、4xx/5xx、请求参数绑定和前后端契约问题。
- 固化证据链：日志实例与时间 → URI/异常 → 后端接口契约 → 前端调用 → 请求封装与转换 → 浏览器实际请求 → 归因与验证。
- 区分前端构造、请求客户端转换、代理、Spring 绑定、后端业务和部署版本六类故障。
- 保留 `diagnose-error-logs` 作为唯一入口，并继续把 SIP 专项问题路由到 `debug-sip-comm`。

## 非目标

- 不修改应用代码、日志配置或前端请求封装。
- 不替代浏览器自动化、SIP 专项排查或外部服务自身日志。
- 不把某一个 multipart 案例写成唯一判断规则。
- 不新增脚本；本流程依赖问题现场和代码结构，固定脚本的收益不足。

## 内容组织

采用“主 skill 路由 + reference 详细流程”。

### 主文件

更新 `.agents/skills/diagnose-error-logs/SKILL.md`：

- frontmatter 增加前端页面/API、上传、请求契约和参数绑定触发语句。
- 在前端链路章节加入统一的前后端证据顺序和 reference 路由。
- 决策树加入 Web 请求分支，先判断传输契约，再进入后端业务。
- 检查清单增加 Controller、前端 API、RequestClient、浏览器 Network 和部署产物核对。

主文件只保留分诊规则和必须先做的动作，避免继续膨胀。

### Reference

新增 `.agents/skills/diagnose-error-logs/references/fullstack-request.md`，包含：

1. 从日志锁定当前实例、时间、URI、异常类型和重复次数。
2. 从 Controller 注解提取 method/path/consumes/header/query/body/multipart 契约。
3. 在相邻前端仓库定位 API 函数、调用组件、RequestClient 默认配置和拦截器。
4. 核对 Axios/Fetch 实际转换，区分“源码中是 FormData”和“线上发送的是 multipart”。
5. 用浏览器 Network 确认 URL、method、status、Content-Type、payload 和响应体；不记录凭证或文件内容。
6. 处理 JSON、multipart、query/body、header、method、CORS、代理前缀、状态码和业务码不一致。
7. 用最小复现和测试门禁证明根因与修复，不凭相关性下结论。

## 归因规则

- Controller 参数绑定前抛错：先查传输契约，不追业务 service。
- 源码构造正确但实际请求错误：查默认请求头、拦截器、序列化和代理。
- 浏览器请求正确而 Spring 绑定失败：查 `consumes`、参数注解、multipart 配置和过滤器。
- 后端成功但页面仍失败：查响应解包、业务码、类型映射、缓存和部署版本。
- 前后端源码均正确但线上异常：核对前端静态产物、后端进程/JAR和请求实际命中的实例。

## 安全约束

- 不输出 Authorization、Cookie、JWT、refresh token、文件正文、Base64 或 secret。
- 请求头只保留排查所需的名称和值类型；敏感值必须脱敏。
- 不使用清理日志、重置仓库或删除构建产物等破坏性动作建立基线。

## 验证

- 使用 `skill-creator/scripts/quick_validate.py` 校验 skill 目录。
- 单独解析所有 SKILL.md YAML frontmatter。
- 检查主文件到 reference 的相对路径存在。
- 用以下触发场景人工审查路由覆盖：
  - “上传图片报 Current request is not a multipart request”
  - “前端接口 400/500，后端日志怎么看”
  - “页面调用成功但数据显示不出来”
  - “代码改了，线上请求还是旧格式”
  - “设备注册不上”仍路由 SIP 专项 skill

## 完成标准

- `diagnose-error-logs` 是唯一通用错误排查入口。
- 主文件能在一屏决策路径内把 Web 请求问题路由到 reference。
- reference 能从日志证据闭环到浏览器实际请求和代码契约。
- 校验脚本、YAML 解析和引用检查全部通过。
- 不改动现有 SIP 专项流程和应用源代码。
