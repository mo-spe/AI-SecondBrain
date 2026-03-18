#!/bin/bash

# ================================================
# AI-SecondBrain 快速启动脚本（Linux/macOS）
# ================================================

set -e

echo "========================================="
echo "  AI-SecondBrain 快速启动脚本"
echo "========================================="
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 错误：未检测到 Docker，请先安装 Docker"
    echo "   访问：https://docs.docker.com/get-docker/"
    exit 1
fi

echo "✅ Docker 已安装：$(docker --version)"

# 检查 Docker Compose
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ 错误：未检测到 Docker Compose"
    echo "   访问：https://docs.docker.com/compose/install/"
    exit 1
fi

echo "✅ Docker Compose 已安装"

# 检查环境变量文件
if [ ! -f .env ]; then
    echo "⚠️  未检测到 .env 文件，正在从 .env.example 复制..."
    cp .env.example .env
    echo ""
    echo "⚠️  请编辑 .env 文件，配置以下必需项："
    echo "   - MYSQL_ROOT_PASSWORD"
    echo "   - MYSQL_PASSWORD"
    echo "   - REDIS_PASSWORD"
    echo "   - QWEN_API_KEY (或其他 AI API 密钥)"
    echo ""
    read -p "配置完成后按回车继续..."
fi

# 检查目录
echo "📁 检查必要的目录..."
mkdir -p logs/mysql logs/backend logs/frontend
mkdir -p mysql-data redis-data elasticsearch-data

# 启动服务
echo ""
echo "🚀 启动所有服务..."
if docker compose version &> /dev/null; then
    docker compose up -d
else
    docker-compose up -d
fi

echo ""
echo "⏳ 等待服务启动（约 30 秒）..."
sleep 30

# 检查服务状态
echo ""
echo "📊 服务状态："
if docker compose version &> /dev/null; then
    docker compose ps
else
    docker-compose ps
fi

echo ""
echo "========================================="
echo "  🎉 启动完成！"
echo "========================================="
echo ""
echo "📱 访问地址："
echo "   - 前端界面：http://localhost"
echo "   - API 文档：http://localhost:8080/api/doc.html"
echo "   - 健康检查：http://localhost:8080/api/health"
echo ""
echo "🔐 默认登录："
echo "   - 用户名：admin"
echo "   - 密码：admin123"
echo ""
echo "⚠️  首次登录后请立即修改密码！"
echo ""
echo "📖 查看日志："
echo "   docker-compose logs -f"
echo ""
echo "🛑 停止服务："
echo "   docker-compose down"
echo ""
echo "========================================="
