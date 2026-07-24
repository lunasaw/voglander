#!/bin/bash

# Voglander 应用重启脚本
# 功能：构建应用 → 停止旧进程 → 启动新进程

PORT=8181
PROFILES=prod,repo,inte
MAX_WAIT=10
START_WAIT=60
STABLE_WAIT=3
LAB_PUSH_URL="rtmp://127.0.0.1:1935/live/lab_push"
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)

# 所有相对路径统一以仓库根目录解析，避免从其他目录调用脚本时路径漂移。
cd "$SCRIPT_DIR" || exit 1

is_running() {
    local pid=$1
    local state

    kill -0 "$pid" 2>/dev/null || return 1
    state=$(ps -o stat= -p "$pid" 2>/dev/null || true)
    [[ "$state" != Z* ]]
}

terminate_pid() {
    local pid=$1
    local label=$2
    local wait_seconds=${3:-$MAX_WAIT}

    is_running "$pid" || return 0
    echo "正在终止 $label (PID: $pid)..."
    kill -TERM "$pid" 2>/dev/null || true
    for ((i = 1; i <= wait_seconds; i++)); do
        if ! is_running "$pid"; then
            echo "$label 已退出 (PID: $pid)"
            return 0
        fi
        sleep 1
    done

    echo "$label 未在 ${wait_seconds}s 内退出，执行强制终止 (PID: $pid)"
    kill -KILL "$pid" 2>/dev/null || true
    sleep 1
}

echo "========================================="
echo "重启 Voglander Java 应用 (port=$PORT)"
echo "========================================="

# 1. 在停服前构建完整依赖链，构建失败时保留当前运行实例。
echo ""
echo "[1/4] 构建 Voglander 应用及其依赖..."
echo "执行命令: mvn -pl voglander-web -am package -DskipTests"
if ! mvn -pl voglander-web -am package -DskipTests; then
    echo "错误: Maven 构建失败，保留当前运行实例"
    exit 1
fi

JAR_FILE="voglander-web/target/voglander.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 构建成功但未找到可执行 JAR: $JAR_FILE"
    exit 1
fi

JAR_MANIFEST=$(unzip -p "$JAR_FILE" META-INF/MANIFEST.MF 2>/dev/null || true)
case "$JAR_MANIFEST" in
    *"Main-Class: org.springframework.boot.loader.launch.JarLauncher"*) ;;
    *)
        echo "错误: 构建产物不是可执行 Spring Boot JAR: $JAR_FILE"
        exit 1
        ;;
esac
echo "构建完成: $JAR_FILE"

# 2. 只停止实际监听应用端口的 Voglander JVM
echo ""
echo "[2/4] 停止监听端口 $PORT 的 Voglander 应用..."

LISTENER_PIDS=$(lsof -nP -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null | sort -u || true)
MAVEN_PIDS=""

if [ -n "$LISTENER_PIDS" ]; then
    for PID in $LISTENER_PIDS; do
        CMDLINE=$(tr '\0' ' ' < "/proc/$PID/cmdline" 2>/dev/null || true)
        case "$CMDLINE" in
            *voglander*|*ApplicationWeb*) ;;
            *)
                echo "错误: 端口 $PORT 由非 Voglander 进程占用，拒绝终止。"
                echo "PID: $PID"
                echo "命令行: $CMDLINE"
                exit 1
                ;;
        esac

        PARENT_PID=$(ps -o ppid= -p "$PID" 2>/dev/null | tr -d ' ' || true)
        if [[ "$PARENT_PID" =~ ^[0-9]+$ ]]; then
            PARENT_CMD=$(tr '\0' ' ' < "/proc/$PARENT_PID/cmdline" 2>/dev/null || true)
            case "$PARENT_CMD" in
                *org.codehaus.plexus.classworlds.launcher.Launcher*|*mvn*)
                    MAVEN_PIDS="$MAVEN_PIDS $PARENT_PID"
                    ;;
            esac
        fi

        echo "找到 Voglander 应用 PID: $PID"
        echo "命令行: ${CMDLINE:0:160}"
        terminate_pid "$PID" "Voglander 应用" "$MAX_WAIT"
    done
else
    echo "端口 $PORT 当前没有监听进程"
fi

# Spring 应用先退出，确保 @PreDestroy 有机会回收 ffmpeg；随后再清理 Maven 父进程。
for MAVEN_PID in $(printf '%s\n' $MAVEN_PIDS | sort -u); do
    terminate_pid "$MAVEN_PID" "Maven 父进程" 3
done

# 确认端口已释放
echo "确认端口 $PORT 已释放..."
for i in $(seq 1 5); do
    if ! lsof -nP -tiTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
        echo "端口 $PORT 已释放"
        break
    fi
    echo "等待端口释放... ($i/5)"
    sleep 1
    if [ $i -eq 5 ]; then
        echo "错误: 端口 $PORT 仍被占用"
        lsof -nP -iTCP:"$PORT" -sTCP:LISTEN
        exit 1
    fi
done

# 3. 仅清理使用 Lab 保留推流 URL 的孤儿 ffmpeg，避免影响其他媒体任务。
echo ""
echo "[3/4] 清理 Lab 残留 ffmpeg..."
LAB_FFMPEG_FOUND=false
for PID in $(pgrep -x ffmpeg 2>/dev/null || true); do
    CMDLINE=$(tr '\0' ' ' < "/proc/$PID/cmdline" 2>/dev/null || true)
    case "$CMDLINE" in
        *"$LAB_PUSH_URL"*)
            LAB_FFMPEG_FOUND=true
            terminate_pid "$PID" "Lab ffmpeg" 3
            ;;
    esac
done
if [ "$LAB_FFMPEG_FOUND" = false ]; then
    echo "未发现 Lab 残留 ffmpeg"
fi

# 4. 启动应用（使用 jar 包方式，非阻塞）
echo ""
echo "[4/4] 启动应用..."
echo "Profiles: $PROFILES"
echo "使用 JAR 包: $JAR_FILE"
mkdir -p voglander-web/logs

# 独立会话可避免调用脚本的终端/远程会话结束时连带终止应用。
nohup setsid java -jar "$JAR_FILE" \
    --server.port=$PORT \
    --spring.profiles.active=$PROFILES \
    > voglander-web/logs/app.log 2>&1 &

NEW_PID=$!
echo "应用已启动，PID: $NEW_PID"
echo "查看日志: tail -f voglander-web/logs/app.log"

echo "等待应用监听端口 $PORT（最多 ${START_WAIT}s）..."
STARTED=false
for ((i = 1; i <= START_WAIT; i++)); do
    if lsof -nP -tiTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
        LISTENER_PID=$(lsof -nP -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null | head -1)
        STABLE=true
        for ((j = 1; j <= STABLE_WAIT; j++)); do
            sleep 1
            if ! is_running "$NEW_PID" \
                || ! lsof -nP -tiTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
                STABLE=false
                break
            fi
        done
        if [ "$STABLE" = true ]; then
            echo "应用启动成功，监听 PID: $LISTENER_PID（已稳定 ${STABLE_WAIT}s）"
            STARTED=true
            break
        fi
    fi
    if ! is_running "$NEW_PID"; then
        echo "错误: 启动进程已提前退出，最近日志如下:"
        tail -n 80 voglander-web/logs/app.log 2>/dev/null || true
        exit 1
    fi
    sleep 1
done

if [ "$STARTED" = false ]; then
    echo "错误: 应用未在 ${START_WAIT}s 内监听端口 $PORT，最近日志如下:"
    tail -n 80 voglander-web/logs/app.log 2>/dev/null || true
    exit 1
fi

echo ""
echo "========================================="
echo "重启完成"
echo "========================================="
