#!/bin/bash

# Redis测试启动脚本
# 用于Voglander项目的缓存集成测试

echo "🚀 启动Redis服务用于Voglander缓存测试..."

# 检查Docker是否可用
if command -v docker &> /dev/null; then
    echo "✅ 检测到Docker，使用Docker启动Redis..."

    # 停止可能存在的测试Redis容器
    docker stop voglander-test-redis 2>/dev/null
    docker rm voglander-test-redis 2>/dev/null

    # 启动Redis容器
    echo "📦 启动Redis容器..."
    docker run --name voglander-test-redis \
        -p 6379:6379 \
        -d redis:latest \
        redis-server --appendonly yes

    # 等待Redis启动
    echo "⏳ 等待Redis启动..."
    sleep 3

    # 验证连接
    if docker exec voglander-test-redis redis-cli ping | grep -q "PONG"; then
        echo "✅ Redis启动成功！"
        echo "📋 连接信息："
        echo "   Host: localhost"
        echo "   Port: 6379"
        echo "   Database: 15 (测试用)"
        echo ""
        echo "🔍 您现在可以运行缓存集成测试了："
        echo "   mvn test -Dtest=MediaNodeCacheIntegrationTest"
        echo ""
        echo "🛑 测试完成后停止Redis容器："
        echo "   docker stop voglander-test-redis"
        echo "   docker rm voglander-test-redis"
    else
        echo "❌ Redis启动失败"
        exit 1
    fi

elif command -v brew &> /dev/null && brew list redis &> /dev/null; then
    echo "✅ 检测到Homebrew Redis，启动本地Redis服务..."

    # 启动Redis服务
    brew services start redis

    # 等待服务启动
    echo "⏳ 等待Redis服务启动..."
    sleep 2

    # 验证连接
    if redis-cli ping | grep -q "PONG"; then
        echo "✅ Redis服务启动成功！"
        echo "📋 连接信息："
        echo "   Host: localhost"
        echo "   Port: 6379"
        echo "   Database: 15 (测试用)"
        echo ""
        echo "🔍 您现在可以运行缓存集成测试了："
        echo "   mvn test -Dtest=MediaNodeCacheIntegrationTest"
        echo ""
        echo "🛑 测试完成后停止Redis服务："
        echo "   brew services stop redis"
    else
        echo "❌ Redis服务启动失败"
        exit 1
    fi

elif command -v redis-server &> /dev/null; then
    echo "✅ 检测到Redis，启动服务..."

    # 在后台启动Redis
    redis-server --daemonize yes --port 6379

    # 等待服务启动
    echo "⏳ 等待Redis服务启动..."
    sleep 2

    # 验证连接
    if redis-cli ping | grep -q "PONG"; then
        echo "✅ Redis服务启动成功！"
        echo "📋 连接信息："
        echo "   Host: localhost"
        echo "   Port: 6379"
        echo "   Database: 15 (测试用)"
        echo ""
        echo "🔍 您现在可以运行缓存集成测试了："
        echo "   mvn test -Dtest=MediaNodeCacheIntegrationTest"
        echo ""
        echo "🛑 测试完成后停止Redis服务："
        echo "   redis-cli shutdown"
    else
        echo "❌ Redis服务启动失败"
        exit 1
    fi

else
    echo "❌ 未检测到Redis或Docker"
    echo ""
    echo "📥 请选择以下安装方式之一："
    echo ""
    echo "🐳 方式1: 使用Docker (推荐)"
    echo "   docker run --name voglander-test-redis -p 6379:6379 -d redis:latest"
    echo ""
    echo "🍺 方式2: 使用Homebrew (macOS)"
    echo "   brew install redis"
    echo "   brew services start redis"
    echo ""
    echo "📦 方式3: 使用包管理器"
    echo "   # Ubuntu/Debian"
    echo "   sudo apt-get install redis-server"
    echo "   # CentOS/RHEL"
    echo "   sudo yum install redis"
    echo ""
    echo "💡 或者您可以运行不依赖Redis的缓存测试："
    echo "   mvn test -Dtest=MediaNodeCacheIntegrationTest#testMemoryCacheWhenRedisUnavailable_Success"
    exit 1
fi