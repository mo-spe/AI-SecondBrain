#!/bin/bash

# ============================================================
# AI-SecondBrain 从 1.0 升级到 2.0 快速脚本
# ============================================================
# 使用说明：
# 1. 上传到服务器
# 2. 运行：bash upgrade_v1_to_v2.sh
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查是否在正确的目录
if [ ! -f "docker-compose.prod.yml" ]; then
    log_error "请在项目根目录执行此脚本"
    exit 1
fi

log_step "=========================================="
log_step "开始从 1.0 升级到 2.0"
log_step "=========================================="
echo ""

# 步骤 1：备份数据
log_step "步骤 1/8: 备份数据..."
BACKUP_DIR="backup/upgrade_$(date +%Y%m%d_%H%M%S)"
mkdir -p "${BACKUP_DIR}"

# 获取当前环境变量
if [ -f ".env" ]; then
    cp .env "${BACKUP_DIR}/"
    log_info "✓ 已备份 .env"
fi

if [ -f ".env.production" ]; then
    cp .env.production "${BACKUP_DIR}/"
    log_info "✓ 已备份 .env.production"
fi

# 备份 MySQL 数据库
if docker ps | grep -q ai-second-brain-mysql; then
    log_info "正在备份 MySQL 数据库..."
    MYSQL_CONTAINER=$(docker ps -q -f name=ai-second-brain-mysql)
    
    # 获取 MySQL root 密码
    MYSQL_ROOT_PWD=$(docker exec ${MYSQL_CONTAINER} env | grep MYSQL_ROOT_PASSWORD | cut -d'=' -f2)
    MYSQL_DB=$(docker exec ${MYSQL_CONTAINER} env | grep MYSQL_DATABASE | cut -d'=' -f2)
    
    if [ -n "${MYSQL_ROOT_PWD}" ] && [ -n "${MYSQL_DB}" ]; then
        docker exec ${MYSQL_CONTAINER} mysqldump \
            -u root -p${MYSQL_ROOT_PWD} ${MYSQL_DB} \
            > "${BACKUP_DIR}/mysql_backup.sql"
        log_info "✓ MySQL 数据库已备份：${BACKUP_DIR}/mysql_backup.sql"
    else
        log_warn "无法获取 MySQL 密码，跳过备份"
    fi
fi

# 备份 Redis 数据（可选）
if docker ps | grep -q ai-second-brain-redis; then
    log_info "正在备份 Redis 数据..."
    docker exec ai-second-brain-redis redis-cli BGSAVE 2>/dev/null || true
    log_info "✓ Redis 数据已保存"
fi

log_info "备份完成：${BACKUP_DIR}"
ls -lh "${BACKUP_DIR}/"
echo ""

# 步骤 2：停止旧服务
log_step "步骤 2/8: 停止旧服务..."
docker-compose -f docker-compose.prod.yml down
log_info "✓ 旧服务已停止"
echo ""

# 步骤 3：清理旧容器
log_step "步骤 3/8: 清理旧容器..."
docker container prune -f
log_info "✓ 旧容器已清理"
echo ""

# 步骤 4：更新代码
log_step "步骤 4/8: 更新代码..."
if [ -d ".git" ]; then
    log_info "使用 Git 更新代码..."
    git pull origin main || log_warn "Git 更新失败，请手动更新"
else
    log_warn "未发现 Git 仓库，请手动上传新代码"
fi
log_info "✓ 代码已更新"
echo ""

# 步骤 5：配置环境
log_step "步骤 5/8: 配置环境..."
if [ ! -f ".env.production" ]; then
    cp .env.example .env.production
    log_info "✓ 已创建 .env.production"
    log_warn "请编辑 .env.production 配置必要的环境变量"
    log_warn "必须配置：MYSQL_ROOT_PASSWORD, REDIS_PASSWORD, QWEN_API_KEY"
    echo ""
    read -p "按回车键继续..."
else
    log_info "✓ .env.production 已存在"
    log_warn "请检查配置是否需要更新"
fi
echo ""

# 步骤 6：运行环境检查
log_step "步骤 6/8: 运行环境检查..."
if [ -f "check_deployment.sh" ]; then
    chmod +x check_deployment.sh
    bash check_deployment.sh
    log_info "✓ 环境检查完成"
else
    log_warn "check_deployment.sh 不存在，跳过检查"
fi
echo ""

# 步骤 7：执行部署
log_step "步骤 7/8: 执行部署..."
if [ -f "deploy.sh" ]; then
    chmod +x deploy.sh
    bash deploy.sh
    log_info "✓ 部署完成"
else
    log_error "deploy.sh 不存在，无法部署"
    exit 1
fi
echo ""

# 步骤 8：恢复数据
log_step "步骤 8/8: 恢复数据..."
BACKUP_FILE="${BACKUP_DIR}/mysql_backup.sql"
if [ -f "${BACKUP_FILE}" ]; then
    log_info "正在恢复 MySQL 数据库..."
    
    # 等待 MySQL 启动
    log_info "等待 MySQL 启动..."
    sleep 30
    
    # 获取 MySQL root 密码
    MYSQL_ROOT_PWD=$(grep MYSQL_ROOT_PASSWORD .env.production | cut -d'=' -f2)
    MYSQL_DB=$(grep MYSQL_DATABASE .env.production | cut -d'=' -f2)
    
    if [ -z "${MYSQL_ROOT_PWD}" ]; then
        MYSQL_ROOT_PWD="123456"  # 默认密码
    fi
    
    if [ -z "${MYSQL_DB}" ]; then
        MYSQL_DB="second_brain"  # 默认数据库
    fi
    
    # 恢复数据库
    docker exec -i ai-second-brain-mysql mysql \
        -u root -p${MYSQL_ROOT_PWD} ${MYSQL_DB} \
        < "${BACKUP_FILE}"
    
    log_info "✓ MySQL 数据库已恢复"
    
    # 验证数据
    log_info "验证数据..."
    USER_COUNT=$(docker exec ai-second-brain-mysql mysql \
        -u root -p${MYSQL_ROOT_PWD} -N -e \
        "SELECT COUNT(*) FROM ${MYSQL_DB}.user" 2>/dev/null || echo "0")
    
    log_info "用户表记录数：${USER_COUNT}"
else
    log_warn "未找到备份文件，跳过数据恢复"
fi
echo ""

# 验证部署
log_step "验证部署..."
docker-compose -f docker-compose.prod.yml ps
echo ""

# 显示访问信息
log_step "=========================================="
log_step "升级完成！"
log_step "=========================================="
echo ""
log_info "前端地址：http://你的服务器 IP"
log_info "API 文档：http://你的服务器 IP/api/doc.html"
log_info "健康检查：http://你的服务器 IP/api/actuator/health"
echo ""
log_warn "请记得："
log_warn "1. 检查 .env.production 配置是否正确"
log_warn "2. 测试核心功能是否正常"
log_warn "3. 验证数据是否完整"
echo ""
log_info "备份文件位置：${BACKUP_DIR}"
echo ""

# 完成
echo -e "${GREEN}🎉 升级完成！${NC}"
