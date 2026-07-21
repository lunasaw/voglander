#!/bin/bash

# Voglander 应用重启脚本
# 功能：kill 旧进程 → 启动新进程（端口 8181）

set -e

PORT=8181
PROFILES=dev,repo,inte  # 必须同时激活 dev, repo, inte 三个 profiles
MAX_WAIT=10

echo "========================================="
echo "重启 Voglander 应用 (port=$PORT, profile=$PROFILE)"
echo "========================================="

# 1. 查找并 kill 占用端口的进程
echo ""
echo "[1/3] 查找端口 $PORT 上的进程..."
PID=$(lsof -ti:$PORT 2>/dev/null || true)

if [ -n "$PID" ]; then
    echo "找到进程 PID: $PID，正在终止..."
    kill -TERM $PID 2>/dev/null || true

    # 等待进程优雅退出
    echo "等待进程退出..."
    for i in $(seq 1 $MAX_WAIT); do
        if ! kill -0 $PID 2>/dev/null; then
            echo "进程已退出"
            break
        fi
        sleep 1
        if [ $i -eq $MAX_WAIT ]; then
            echo "进程未响应，强制 kill..."
            kill -9 $PID 2>/dev/null || true
            sleep 1
        fi
    done
else
    echo "端口 $PORT 未被占用"
fi

# 2. 确认端口已释放
echo ""
echo "[2/3] 确认端口已释放..."
for i in $(seq 1 5); do
    if ! lsof -ti:$PORT >/dev/null 2>&1; then
        echo "端口 $PORT 已释放"
        break
    fi
    echo "等待端口释放... ($i/5)"
    sleep 1
done

# 3. 启动应用
echo ""
echo "[3/3] 启动应用..."
echo "命令: mvn spring-boot:run -pl voglander-web -Dspring-boot.run.jvmArguments=\"-Dserver.port=$PORT\" -Dspring-boot.run.profiles=$PROFILES"
echo ""

mvn spring-boot:run \
    -pl voglander-web \
    -Dspring-boot.run.jvmArguments="-Dserver.port=$PORT" \
    -Dspring-boot.run.profiles=$PROFILES
