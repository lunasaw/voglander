#!/bin/bash

# JaCoCo 覆盖率报告生成脚本
# 用途：运行所有模块的测试，然后生成聚合覆盖率报告

echo "🚀 开始生成 Voglander 项目的 JaCoCo 聚合覆盖率报告..."

# 1. 清理所有模块的target目录
echo "📦 清理项目..."
mvn clean

# 2. 运行有测试的模块的测试（生成覆盖率数据）
echo "🧪 运行测试以生成覆盖率数据..."

# 运行test模块的测试
mvn test

# 3. 生成聚合报告
echo "📊 生成聚合覆盖率报告...跳过失败用例"
mvn verify

# 4. 检查报告是否生成成功
if [ -f "voglander-coverage-report/target/site/jacoco-aggregate/index.html" ]; then
    echo "✅ JaCoCo 聚合覆盖率报告生成成功！"
    echo "📍 报告位置: voglander-coverage-report/target/site/jacoco-aggregate/index.html"
    echo "🌐 在浏览器中打开查看详细覆盖率报告"
    
    # 在macOS上自动打开报告
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "🖥️  正在打开报告..."
        open "voglander-coverage-report/target/site/jacoco-aggregate/index.html"
    fi
else
    echo "❌ 报告生成失败，请检查上述输出中的错误信息"
fi

echo "🏁 完成！"