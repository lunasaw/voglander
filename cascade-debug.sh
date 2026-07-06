#!/bin/bash
# 级联注册诊断脚本

echo "=== 级联注册诊断 ==="
echo ""

echo "1. 检查级联平台配置："
sqlite3 voglander-web/app.db "SELECT id, platform_id, local_client_id, platform_ip, platform_port, local_ip, local_port, transport, register_status FROM tb_cascade_platform WHERE enabled=1;"
echo ""

echo "2. 检查 SIP 监听端口："
lsof -i :5061 | grep java
echo ""

echo "3. 测试目标平台网络连通性："
TARGET_IP=$(sqlite3 voglander-web/app.db "SELECT platform_ip FROM tb_cascade_platform WHERE id=3;")
TARGET_PORT=$(sqlite3 voglander-web/app.db "SELECT platform_port FROM tb_cascade_platform WHERE id=3;")
echo "目标: $TARGET_IP:$TARGET_PORT"
nc -zv -w 3 $TARGET_IP $TARGET_PORT 2>&1 || echo "⚠️  目标平台不可达"
echo ""

echo "4. 检查最近的级联日志（需要应用日志文件）："
if [ -f ~/logs/voglander/voglander-info.log ]; then
    tail -100 ~/logs/voglander/voglander-info.log | grep -E "(级联|ClientCommandSender|REGISTER)" | tail -20
else
    echo "⚠️  日志文件不存在: ~/logs/voglander/voglander-info.log"
fi
echo ""

echo "5. 建议启用 SIP 调试日志："
echo "   在 application-dev.yml 添加："
echo "   logging:"
echo "     level:"
echo "       io.github.lunasaw.gbproxy: DEBUG"
echo "       io.github.lunasaw.sip: DEBUG"
echo ""

echo "6. 检查 sip.enable 配置："
echo "   确认 application.yml 或 application-dev.yml 中有："
echo "   sip:"
echo "     enable: true"
