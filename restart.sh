#!/bin/bash

# Voglander 应用重启脚本
# 功能：kill Java 应用进程 → 启动新进程

PORT=8181
PROFILES=prod,repo,inte
MAX_WAIT=10

echo "========================================="
echo "重启 Voglander Java 应用 (port=$PORT)"
echo "========================================="

# 1. 查找并 kill Java 应用进程（只 kill java 进程）
echo ""
echo "[1/2] 查找并 kill Java 进程..."

# 使用 pgrep 查找所有 Java 进程，过滤出监听指定端口的进程
JAVA_PIDS=$(pgrep -f "java.*voglander" 2>/dev/null || true)

if [ -n "$JAVA_PIDS" ]; then
    for PID in $JAVA_PIDS; do
        # 检查该 Java 进程是否监听目标端口
        if lsof -p $PID -i :$PORT >/dev/null 2>&1; then
            echo "找到 Voglander Java 进程 PID: $PID"
            CMDLINE=$(ps -p $PID -o args= 2>/dev/null | cut -c 1-100 || true)
            echo "命令行: $CMDLINE..."
            echo "正在终止..."

            kill -TERM $PID 2>/dev/null || true

            # 等待进程优雅退出
            echo "等待进程退出..."
            for i in $(seq 1 $MAX_WAIT); do
                if ! kill -0 $PID 2>/dev/null; then
                    echo "进程已成功退出 (PID: $PID)"
                    break
                fi
                sleep 1
                if [ $i -eq $MAX_WAIT ]; then
                    echo "进程未响应，强制 kill..."
                    kill -9 $PID 2>/dev/null || true
                    sleep 1
                    echo "进程已强制终止 (PID: $PID)"
                fi
            done
        fi
    done
else
    echo "未找到 Voglander Java 进程"
fi

# 确认端口已释放
echo "确认端口 $PORT 已释放..."
for i in $(seq 1 5); do
    if ! lsof -ti:$PORT >/dev/null 2>&1; then
        echo "端口 $PORT 已释放"
        break
    fi
    echo "等待端口释放... ($i/5)"
    sleep 1
    if [ $i -eq 5 ]; then
        echo "警告: 端口仍被占用，请手动检查"
        lsof -i :$PORT
    fi
done

# 2. 启动应用（使用 jar 包方式，非阻塞）
echo ""
echo "[2/2] 启动应用..."
echo "Profiles: $PROFILES"

# 检查是否有编译好的 jar 包
JAR_FILE=$(find voglander-web/target -name "voglander-web-*.jar" ! -name "*-sources.jar" 2>/dev/null | head -1)

if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
    echo "使用 JAR 包: $JAR_FILE"
    mkdir -p voglander-web/logs

    nohup java -jar "$JAR_FILE" \
        --server.port=$PORT \
        --spring.profiles.active=$PROFILES \
        > voglander-web/logs/app.log 2>&1 &

    NEW_PID=$!
    echo "应用已启动，PID: $NEW_PID"
    echo "查看日志: tail -f voglander-web/logs/app.log"
else
    echo "未找到 JAR 包，使用 mvn spring-boot:run 启动（需手动操作）"
    echo "执行命令："
    echo "  mvn spring-boot:run -pl voglander-web -Dspring-boot.run.profiles=$PROFILES"
fi

echo ""
echo "========================================="
echo "重启完成"
echo "========================================="
