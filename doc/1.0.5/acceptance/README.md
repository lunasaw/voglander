# 1.0.5 真实联调验收清单

> 适用范围：本机/预发环境，不进 CI。自动化 E2E 在 `voglander-web/src/test/` 覆盖。

## 环境前置

| 依赖 | 配置要求 |
|------|---------|
| ZLM HTTP | 端口 **8082**（开发默认 9092，联调须覆盖：`-Dlocal.zlm.port=8082`） |
| Redis | `localhost:6379`，密码 `luna` |
| 媒体节点 | `tb_media_node` 至少一条 `enabled=1`，否则 `/live/start` 返 700002 |
| ffmpeg | PATH 可见（推流用） |

> ZLM `config.ini` 钩子重配被安全策略拦截；联调以模拟 `on_stream_changed` POST 等价覆盖（详见 run-live-acceptance.sh）。

## 启动

```bash
cd voglander
mvn spring-boot:run -pl voglander-web -Dlocal.zlm.port=8082
```

## 执行

```bash
cd doc/1.0.5/acceptance
bash run-live-acceptance.sh
```
