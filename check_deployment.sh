#!/bin/bash

# ============================================================
# AI-SecondBrain 生产环境配置检查脚本
# ============================================================
# 使用说明：
# 1. 上传到服务器
# 2. 运行：bash check_deployment.sh
# 3. 根据检查结果修复问题
# ============================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 计数器
PASS=0
FAIL=0
WARN=0

# 日志函数
log_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASS++))
}

log_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAIL++))
}

log_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARN++))
}

log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_section() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 检查 Docker
check_docker() {
    log_section "检查 Docker 环境"
    
    if command -v docker &> /dev/null; then
        log_pass "Docker 已安装：$(docker --version)"
    else
        log_fail "Docker 未安装"
    fi
    
    if command -v docker-compose &> /dev/null; then
        log_pass "Docker Compose 已安装：$(docker-compose --version)"
    else
        log_fail "Docker Compose 未安装"
    fi
    
    if docker info &> /dev/null; then
        log_pass "Docker 服务正在运行"
    else
        log_fail "Docker 服务未运行"
    fi
}

# 检查配置文件
check_config_files() {
    log_section "检查配置文件"
    
    # 检查 .env.production
    if [ -f ".env.production" ]; then
        log_pass ".env.production 文件存在"
        
        # 检查必要的环境变量
        if grep -q "MYSQL_ROOT_PASSWORD=" .env.production && ! grep -q "MYSQL_ROOT_PASSWORD=YourSecureMySQLRootPassword123!" .env.production; then
            log_pass "MySQL root 密码已配置"
        else
            log_fail "MySQL root 密码未配置或使用默认值"
        fi
        
        if grep -q "REDIS_PASSWORD=" .env.production && ! grep -q "REDIS_PASSWORD=YourSecureRedisPassword789!" .env.production; then
            log_pass "Redis 密码已配置"
        else
            log_fail "Redis 密码未配置或使用默认值"
        fi
        
        if grep -q "QWEN_API_KEY=" .env.production && ! grep -q "QWEN_API_KEY=your_qwen_api_key_here" .env.production; then
            log_pass "Qwen API Key 已配置"
        else
            log_warn "Qwen API Key 未配置或使用默认值"
        fi
        
        if grep -q "DOMAIN_NAME=" .env.production && ! grep -q "DOMAIN_NAME=your-domain.com" .env.production; then
            log_pass "域名已配置"
        else
            log_warn "域名未配置或使用默认值"
        fi
    else
        log_fail ".env.production 文件不存在"
    fi
    
    # 检查 docker-compose.prod.yml
    if [ -f "docker-compose.prod.yml" ]; then
        log_pass "docker-compose.prod.yml 文件存在"
    else
        log_fail "docker-compose.prod.yml 文件不存在"
    fi
    
    # 检查 application-prod.yml
    if [ -f "src/main/resources/application-prod.yml" ]; then
        log_pass "application-prod.yml 文件存在"
    else
        log_fail "application-prod.yml 文件不存在"
    fi
    
    # 检查 nginx 配置
    if [ -f "nginx/nginx.conf" ]; then
        log_pass "nginx.conf 文件存在"
    else
        log_warn "nginx.conf 文件不存在"
    fi
    
    if [ -f "nginx/conf.d/default.conf" ]; then
        log_pass "default.conf 文件存在"
    else
        log_warn "default.conf 文件不存在"
    fi
}

# 检查 Docker 配置
check_docker_config() {
    log_section "检查 Docker 配置"
    
    # 检查 docker-compose.prod.yml 语法
    if docker-compose -f docker-compose.prod.yml config &> /dev/null; then
        log_pass "docker-compose.prod.yml 语法正确"
    else
        log_fail "docker-compose.prod.yml 语法错误"
    fi
    
    # 检查网络配置
    if grep -q "ai-second-brain-network" docker-compose.prod.yml; then
        log_pass "Docker 网络配置正确"
    else
        log_warn "Docker 网络配置可能不完整"
    fi
    
    # 检查数据卷配置
    if grep -q "mysql-data:" docker-compose.prod.yml && \
       grep -q "redis-data:" docker-compose.prod.yml && \
       grep -q "elasticsearch-data:" docker-compose.prod.yml; then
        log_pass "数据卷配置正确"
    else
        log_warn "数据卷配置可能不完整"
    fi
    
    # 检查健康检查配置
    if grep -q "healthcheck:" docker-compose.prod.yml; then
        log_pass "健康检查配置存在"
    else
        log_warn "健康检查配置缺失"
    fi
}

# 检查必要目录
check_directories() {
    log_section "检查目录结构"
    
    # 检查日志目录
    if [ -d "logs" ]; then
        log_pass "logs 目录存在"
    else
        log_info "创建 logs 目录"
        mkdir -p logs
    fi
    
    if [ -d "logs/mysql" ]; then
        log_pass "logs/mysql 目录存在"
    else
        mkdir -p logs/mysql
        log_info "创建 logs/mysql 目录"
    fi
    
    if [ -d "logs/backend" ]; then
        log_pass "logs/backend 目录存在"
    else
        mkdir -p logs/backend
        log_info "创建 logs/backend 目录"
    fi
    
    if [ -d "logs/nginx" ]; then
        log_pass "logs/nginx 目录存在"
    else
        mkdir -p logs/nginx
        log_info "创建 logs/nginx 目录"
    fi
    
    # 检查 SSL 目录
    if [ -d "ssl" ]; then
        log_pass "ssl 目录存在"
    else
        mkdir -p ssl
        log_info "创建 ssl 目录"
    fi
    
    # 检查 redis 配置目录
    if [ -d "redis" ]; then
        log_pass "redis 目录存在"
    else
        mkdir -p redis
        log_info "创建 redis 目录"
    fi
}

# 检查安全配置
check_security() {
    log_section "检查安全配置"
    
    # 检查密码强度
    if [ -f ".env.production" ]; then
        MYSQL_ROOT_PWD=$(grep "MYSQL_ROOT_PASSWORD=" .env.production | cut -d'=' -f2)
        
        if [ ${#MYSQL_ROOT_PWD} -ge 16 ]; then
            log_pass "MySQL root 密码长度符合要求 (${#MYSQL_ROOT_PWD} 位)"
        else
            log_fail "MySQL root 密码长度不足 16 位"
        fi
        
        # 检查密码复杂度
        if [[ $MYSQL_ROOT_PWD =~ [A-Z] ]] && \
           [[ $MYSQL_ROOT_PWD =~ [a-z] ]] && \
           [[ $MYSQL_ROOT_PWD =~ [0-9] ]] && \
           [[ $MYSQL_ROOT_PWD =~ [^A-Za-z0-9] ]]; then
            log_pass "MySQL root 密码复杂度符合要求"
        else
            log_warn "MySQL root 密码建议包含大小写字母、数字和特殊字符"
        fi
    fi
    
    # 检查 .gitignore
    if [ -f ".gitignore" ]; then
        if grep -q ".env" .gitignore; then
            log_pass ".env 文件已添加到 .gitignore"
        else
            log_warn ".env 文件未添加到 .gitignore"
        fi
    else
        log_warn ".gitignore 文件不存在"
    fi
}

# 检查系统资源
check_system_resources() {
    log_section "检查系统资源"
    
    # CPU 核心数
    CPU_CORES=$(nproc 2>/dev/null || echo "未知")
    log_info "CPU 核心数：$CPU_CORES"
    
    # 内存
    if [ -f /proc/meminfo ]; then
        TOTAL_MEM=$(grep MemTotal /proc/meminfo | awk '{print $2}')
        TOTAL_MEM_GB=$((TOTAL_MEM / 1024 / 1024))
        log_info "总内存：${TOTAL_MEM_GB}GB"
        
        if [ $TOTAL_MEM_GB -ge 4 ]; then
            log_pass "内存满足最低要求 (4GB)"
        else
            log_warn "内存可能不足 (建议至少 4GB)"
        fi
        
        if [ $TOTAL_MEM_GB -ge 8 ]; then
            log_pass "内存满足推荐配置 (8GB)"
        fi
    fi
    
    # 磁盘空间
    DISK_AVAILABLE=$(df -h . | tail -1 | awk '{print $4}')
    log_info "可用磁盘空间：$DISK_AVAILABLE"
    
    # Docker 资源
    if docker info &> /dev/null; then
        DOCKER_CONTAINERS=$(docker ps -a | wc -l)
        DOCKER_IMAGES=$(docker images | wc -l)
        log_info "Docker 容器数：$((DOCKER_CONTAINERS - 1))"
        log_info "Docker 镜像数：$((DOCKER_IMAGES - 1))"
    fi
}

# 检查端口占用
check_ports() {
    log_section "检查端口占用"
    
    # 检查常用端口
    PORTS=(80 443 3306 6379 8080 9200 9092)
    
    for PORT in "${PORTS[@]}"; do
        if netstat -tuln 2>/dev/null | grep -q ":$PORT "; then
            log_warn "端口 $PORT 已被占用"
        else
            log_pass "端口 $PORT 可用"
        fi
    done
}

# 检查服务状态
check_services() {
    log_section "检查服务状态"
    
    if docker-compose -f docker-compose.prod.yml ps &> /dev/null; then
        log_pass "Docker Compose 可以正常执行"
        
        # 检查各个服务
        SERVICES=("mysql" "redis" "elasticsearch" "kafka" "backend" "nginx")
        
        for SERVICE in "${SERVICES[@]}"; do
            if docker-compose -f docker-compose.prod.yml ps | grep -q "$SERVICE.*Up"; then
                log_pass "服务 $SERVICE 正在运行"
            else
                log_info "服务 $SERVICE 未运行"
            fi
        done
    else
        log_warn "Docker Compose 服务未启动"
    fi
}

# 显示总结
show_summary() {
    log_section "检查总结"
    
    echo -e "通过：${GREEN}$PASS${NC}"
    echo -e "失败：${RED}$FAIL${NC}"
    echo -e "警告：${YELLOW}$WARN${NC}"
    echo ""
    
    if [ $FAIL -eq 0 ] && [ $WARN -eq 0 ]; then
        echo -e "${GREEN}🎉 完美！所有检查项都通过了！${NC}"
        echo ""
        echo "可以执行部署："
        echo "  bash deploy.sh"
    elif [ $FAIL -eq 0 ]; then
        echo -e "${YELLOW}⚠️  存在警告项，但可以继续部署${NC}"
        echo ""
        echo "建议先处理警告项，然后执行部署："
        echo "  bash deploy.sh"
    else
        echo -e "${RED}❌ 存在失败项，请先修复后再部署${NC}"
        echo ""
        echo "修复完成后执行部署："
        echo "  bash deploy.sh"
    fi
    
    echo ""
}

# 主函数
main() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}AI-SecondBrain 生产环境配置检查${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    
    check_docker
    check_config_files
    check_docker_config
    check_directories
    check_security
    check_system_resources
    check_ports
    check_services
    
    show_summary
    
    # 返回错误码
    if [ $FAIL -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

# 执行主函数
main "$@"
