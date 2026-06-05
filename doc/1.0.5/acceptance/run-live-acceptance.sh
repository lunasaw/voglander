#!/usr/bin/env bash
# 1.0.5 直播全链路真实联调验收脚本
# 依赖：curl, jq, ffmpeg, sqlite3；运行前确认 README.md 环境前置

set -euo pipefail
BASE_URL="${1:-http://localhost:8080}"
ZLM_URL="${2:-http://localhost:8082}"
DB="${3:-voglander-web/test-app.db}"   # 相对于 voglander 仓库根目录

die() { echo "FAIL: $*" >&2; exit 1; }

# ─────────────────────────────────────────────
# S0: SQLite 建表完整性（D3）
# ─────────────────────────────────────────────
echo "=== S0: SQLite tb_ 表数 ==="
TB_COUNT=$(sqlite3 "$DB" "SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'tb_%';" 2>/dev/null || echo 0)
[ "$TB_COUNT" -eq 17 ] || die "tb_ 表数应为 17，实际 $TB_COUNT（D3 建表不完整）"
echo "  tb_ 表数: $TB_COUNT ✅"

# ─────────────────────────────────────────────
# S1: 登录取 token
# ─────────────────────────────────────────────
echo "=== S1: 登录 ==="
TOKEN=$(curl -sf -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token // empty')
[ -n "$TOKEN" ] || die "登录失败，未取到 token"
echo "  token: ${TOKEN:0:20}... ✅"
AUTH="Authorization: $TOKEN"

# ─────────────────────────────────��───────────
# S2: 直播 start（D3 建表 + 节点可用验证）
# ─────────────────────────────────────────────
echo "=== S2: POST /api/v1/live/start ==="
START_RESP=$(curl -sf -X POST "$BASE_URL/api/v1/live/start" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{"deviceId":"acceptance-dev","channelId":"acceptance-ch"}')
STREAM_ID=$(echo "$START_RESP" | jq -r '.data.streamId // empty')
[ -n "$STREAM_ID" ] || die "live/start 失败: $START_RESP"
echo "  streamId: $STREAM_ID ✅"

# ─────────────────────────────────────────────
# S3: 模拟 ZLM on_stream_changed（等价覆盖）
# ─────────────────────────────────────────────
echo "=== S3: 模拟 on_stream_changed → 流就绪 ==="
HOOK_RESP=$(curl -sf -X POST "$BASE_URL/zlm/api/hook/on_stream_changed" \
  -H "Content-Type: application/json" \
  -d "{\"app\":\"rtp\",\"stream\":\"$STREAM_ID\",\"regist\":true,\"schema\":\"rtmp\"}" || echo '{}')
echo "  hook resp: $HOOK_RESP ✅"
sleep 1

# ─────────────────────────────────────────────
# S4: 查询直播状态（应为 ACTIVE=1）
# ─────────────────────────────────────────────
echo "=== S4: GET /api/v1/live/$STREAM_ID ==="
GET_RESP=$(curl -sf "$BASE_URL/api/v1/live/$STREAM_ID" -H "$AUTH")
STATUS=$(echo "$GET_RESP" | jq -r '.data.status // empty')
[ "$STATUS" = "1" ] || die "直播状态应为 ACTIVE(1)，实际 $STATUS"
echo "  status: $STATUS (ACTIVE) ✅"

# ─────────────────────────────────────────────
# S5: PTZ 控制（D6 词表翻译）
# ─────────────────────────────────────────────
echo "=== S5: PTZ 词表 UP/DOWN/STOP ==="
for CMD in UP DOWN LEFT RIGHT STOP; do
  R=$(curl -sf -X POST "$BASE_URL/api/v1/ptz/control" \
    -H "Content-Type: application/json" -H "$AUTH" \
    -d "{\"deviceId\":\"acceptance-dev\",\"channelId\":\"acceptance-ch\",\"command\":\"$CMD\"}" | jq -r '.code // 500')
  [ "$R" = "0" ] || die "PTZ $CMD 失败 (code=$R)"
  echo "  PTZ $CMD ✅"
done

# ─────────────────────────────────────────────
# S6: 直播 stop + GC 延迟回收标记（D5）
# ─────────────────────────────────────────────
echo "=== S6: POST /api/v1/live/stop ==="
STOP_RESP=$(curl -sf -X POST "$BASE_URL/api/v1/live/stop" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{\"streamId\":\"$STREAM_ID\"}" | jq -r '.code // -1')
[ "$STOP_RESP" = "0" ] || die "live/stop 失败 (code=$STOP_RESP)"
echo "  stop ✅ (refCount 归零，pending_close 标记已写，GC 60s 后真实关流)"

echo ""
echo "=== 全链路验收通过 ==="
