#!/bin/bash
# ============================================================
# AI-SecondBrain 1.0 数据备份脚本
# ============================================================
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 加载环境变量 (获取密码)
set -a
source /home/.env
set +a

# 配置 (从环境变量获取)
DB_USER="root"
DB_NAME="${MYSQL_DATABASE:-second_brain}"
BACKUP_DIR="/home/backups/v1.0_before_v2.0_$(date +%Y%m%d_%H%M%S)"
MYSQL_CONTAINER="ai-second-brain-mysql" # 请根据实际容器名调整

log_info "=========================================="
log_info "  AI-SecondBrain 1.0 数据备份"
log_info "=========================================="
log_info "备份目录：$BACKUP_DIR"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 1. 完整数据库备份
log_info "[1/6] 备份完整数据库..."
docker exec $MYSQL_CONTAINER mysqldump \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  --single-transaction --routines --triggers --add-drop-database \
  ${DB_NAME} > ${BACKUP_DIR}/full_backup.sql

# 2. 导出业务数据 (示例：user, knowledge_node 等)
log_info "[2/6] 导出业务数据..."
TABLES="user knowledge_node knowledge_tag knowledge_node_tag_relation raw_chat_record review_card review_log learning_report async_task research_history"
for table in $TABLES; do
    docker exec $MYSQL_CONTAINER mysqldump \
      -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
      --no-create-info --complete-insert --skip-triggers \
      ${DB_NAME} $table > ${BACKUP_DIR}/data_${table}.sql
done

# 3. 备份配置文件
log_info "[3/6] 备份配置文件..."
cp /home/docker-compose.yml ${BACKUP_DIR}/ 2>/dev/null || true
cp /home/.env ${BACKUP_DIR}/ 2>/dev/null || true
cp -r /home/nginx ${BACKUP_DIR}/ 2>/dev/null || true

# 4. 压缩备份
log_info "[4/6] 压缩备份文件..."
cd /home/backups
tar -czf $(basename $BACKUP_DIR).tar.gz $(basename $BACKUP_DIR)

log_info "=========================================="
log_info "  🎉 备份完成！"
log_info "  文件：/home/backups/$(basename $BACKUP_DIR).tar.gz"
log_info "=========================================="
