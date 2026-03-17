#!/bin/bash
# ============================================================
# AI-SecondBrain 2.0 数据库重建脚本
# ============================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 加载环境变量
set -a
source /home/.env
set +a

DB_USER="root"
DB_NAME="${MYSQL_DATABASE:-second_brain}"
SQL_FILE="/home/database-v2.0.sql"
MYSQL_CONTAINER="ai-second-brain-mysql"

log_info "=========================================="
log_info "  AI-SecondBrain 2.0 数据库重建"
log_info "=========================================="

# 1. 检查前置条件
log_info "[1/8] 检查前置条件..."
BACKUP_DIR=$(ls -td /home/backups/v1.0_before_v2.0_* | head -1)
if [ -z "$BACKUP_DIR" ]; then
    log_error "未找到备份目录！请先执行备份脚本"
    exit 1
fi
if [ ! -f "$SQL_FILE" ]; then
    log_error "建表语句文件不存在：$SQL_FILE"
    exit 1
fi

# 2. 确认操作
log_warn "[2/8] 确认操作..."
log_warn "⚠️ 此操作将删除现有数据库并重建！"
read -p "确定要继续吗？(输入 YES 确认): " confirm
if [ "$confirm" != "YES" ]; then
    log_info "操作已取消"
    exit 1
fi

# 3. 停止后端服务
log_info "[3/8] 停止后端服务..."
cd /home && docker compose stop backend

# 4. 删除旧数据库
log_warn "[4/8] 删除旧数据库..."
docker exec $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  -e "DROP DATABASE IF EXISTS ${DB_NAME};"

# 5. 创建新数据库
log_info "[5/8] 创建新数据库..."
docker exec $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  -e "CREATE DATABASE ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 6. 执行 2.0 建表语句
log_info "[6/8] 执行 2.0 建表语句..."
docker exec -i $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  ${DB_NAME} < $SQL_FILE

# 7. 验证表结构
log_info "[7/8] 验证表结构..."
TABLE_COUNT=$(docker exec $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';" \
  ${DB_NAME})
if [ "$TABLE_COUNT" -ge 12 ]; then
    log_info "  ✅ 表结构验证通过 (数量：$TABLE_COUNT)"
else
    log_error "  ❌ 表数量不足！预期 12+，实际：$TABLE_COUNT"
    exit 1
fi

# 8. 恢复业务数据
log_info "[8/8] 恢复业务数据..."
# 解压备份以便恢复数据 (如果之前是 tar.gz)
# 这里假设备份目录已存在，直接读取 sql 文件
for table in user knowledge_node knowledge_tag knowledge_node_tag_relation raw_chat_record review_card review_log learning_report async_task research_history; do
    DATA_FILE="$BACKUP_DIR/data_${table}.sql"
    if [ -f "$DATA_FILE" ] && [ -s "$DATA_FILE" ]; then
        docker exec -i $MYSQL_CONTAINER mysql \
          -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
          ${DB_NAME} < $DATA_FILE
        log_info "  ✅ 恢复表：$table"
    fi
done

log_info "=========================================="
log_info "  🎉 数据库重建完成！"
log_info "=========================================="
