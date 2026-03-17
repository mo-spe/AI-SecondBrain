#!/bin/bash

# ============================================================
# AI-SecondBrain 阿里云服务器部署脚本
# ============================================================
# 使用说明：
# 1. 上传项目到服务器
# 2. 修改 .env.production 配置文件
# 3. 运行此脚本：bash deploy.sh
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        log_error "$1 未安装，请先安装：$2"
        exit 1
    fi
}

# 检查 Docker 是否安装
check_docker() {
    log_info "检查 Docker 安装..."
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装"
        log_info "请运行以下命令安装 Docker："
        echo "  yum install -y yum-utils"
        echo "  yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo"
        echo "  yum install -y docker-ce docker-ce-cli containerd.io"
        echo "  systemctl start docker"
        echo "  systemctl enable docker"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装"
        log_info "请运行以下命令安装 Docker Compose："
        echo "  curl -L \"https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-\$(uname -s)-\$(uname -m)\" -o /usr/local/bin/docker-compose"
        echo "  chmod +x /usr/local/bin/docker-compose"
        exit 1
    fi
    
    log_info "Docker 版本：$(docker --version)"
    log_info "Docker Compose 版本：$(docker-compose --version)"
}

# 检查配置文件
check_config() {
    log_info "检查配置文件..."
    
    if [ ! -f ".env.production" ]; then
        log_error ".env.production 文件不存在"
        log_info "请复制 .env.example 并修改为 .env.production"
        exit 1
    fi
    
    # 检查必要的环境变量
    if grep -q "your_qwen_api_key_here" .env.production; then
        log_warn "请修改 QWEN_API_KEY 配置"
    fi
    
    if grep -q "YourSecureMySQLRootPassword123!" .env.production; then
        log_warn "请修改 MySQL 密码为强密码"
    fi
    
    log_info "配置文件检查完成"
}

# 创建必要的目录
create_directories() {
    log_info "创建必要的目录..."
    
    mkdir -p logs/mysql
    mkdir -p logs/backend
    mkdir -p logs/nginx
    mkdir -p ssl
    mkdir -p redis
    
    log_info "目录创建完成"
}

# 停止旧服务
stop_services() {
    log_info "停止旧服务..."
    
    if docker-compose ps | grep -q "Up"; then
        docker-compose -f docker-compose.prod.yml down
        log_info "旧服务已停止"
    else
        log_info "没有运行中的服务"
    fi
}

# 清理旧容器（可选）
cleanup_containers() {
    log_info "清理停止的容器..."
    docker container prune -f
    log_info "容器清理完成"
}

# 构建服务
build_services() {
    log_info "开始构建服务..."
    
    # 构建前端
    log_info "构建前端..."
    docker-compose -f docker-compose.prod.yml build frontend-builder
    
    # 构建后端
    log_info "构建后端..."
    docker-compose -f docker-compose.prod.yml build backend
    
    # 构建 deerflow
    log_info "构建 deerflow..."
    docker-compose -f docker-compose.prod.yml build deerflow
    
    log_info "服务构建完成"
}

# 启动服务
start_services() {
    log_info "启动服务..."
    
    # 使用 -d 后台运行
    docker-compose -f docker-compose.prod.yml up -d
    
    log_info "服务启动完成"
}

# 检查服务状态
check_status() {
    log_info "检查服务状态..."
    
    sleep 10  # 等待服务启动
    
    docker-compose -f docker-compose.prod.yml ps
    
    log_info "服务状态检查完成"
}

# 查看日志
view_logs() {
    log_info "查看服务日志..."
    
    echo "按 Ctrl+C 退出日志查看"
    docker-compose -f docker-compose.prod.yml logs -f
}

# 备份数据
backup_data() {
    log_info "备份数据..."
    
    BACKUP_DIR="backup/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    
    # 备份 MySQL 数据
    if docker ps | grep -q mysql; then
        log_info "备份 MySQL 数据库..."
        MYSQL_CONTAINER=$(docker ps -q -f name=mysql)
        docker exec $MYSQL_CONTAINER mysqldump -u root -p${MYSQL_ROOT_PASSWORD} ${MYSQL_DATABASE} > "$BACKUP_DIR/mysql_backup.sql"
    fi
    
    # 备份 Redis 数据
    if docker ps | grep -q redis; then
        log_info "备份 Redis 数据..."
        cp -r redis-data "$BACKUP_DIR/" 2>/dev/null || true
    fi
    
    # 备份 Elasticsearch 数据
    if docker ps | grep -q elasticsearch; then
        log_info "备份 Elasticsearch 数据..."
        cp -r elasticsearch-data "$BACKUP_DIR/" 2>/dev/null || true
    fi
    
    log_info "数据备份完成：$BACKUP_DIR"
}

# 显示帮助信息
show_help() {
    echo "AI-SecondBrain 部署脚本"
    echo ""
    echo "用法：$0 [命令]"
    echo ""
    echo "命令:"
    echo "  deploy      完整部署流程（默认）"
    echo "  build       仅构建服务"
    echo "  start       启动服务"
    echo "  stop        停止服务"
    echo "  restart     重启服务"
    echo "  status      查看服务状态"
    echo "  logs        查看服务日志"
    echo "  backup      备份数据"
    echo "  clean       清理容器和镜像"
    echo "  help        显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 deploy   # 完整部署"
    echo "  $0 logs     # 查看日志"
    echo "  $0 backup   # 备份数据"
}

# 主函数
main() {
    case "${1:-deploy}" in
        deploy)
            check_docker
            check_config
            create_directories
            stop_services
            cleanup_containers
            build_services
            start_services
            check_status
            log_info "🎉 部署完成！"
            log_info "访问地址：http://你的服务器 IP"
            log_info "API 文档：http://你的服务器 IP/api/doc.html"
            ;;
        build)
            check_docker
            build_services
            ;;
        start)
            check_docker
            start_services
            check_status
            ;;
        stop)
            check_docker
            stop_services
            ;;
        restart)
            check_docker
            stop_services
            start_services
            check_status
            ;;
        status)
            check_docker
            check_status
            ;;
        logs)
            check_docker
            view_logs
            ;;
        backup)
            backup_data
            ;;
        clean)
            check_docker
            docker-compose -f docker-compose.prod.yml down
            docker container prune -f
            docker image prune -f
            log_info "清理完成"
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "未知命令：$1"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
