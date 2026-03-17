#!/bin/bash
# ============================================================
# AI-SecondBrain 2.0 数据库验证脚本
# ============================================================

set -a
source /home/.env
set +a

DB_USER="root"
DB_NAME="${MYSQL_DATABASE:-second_brain}"
MYSQL_CONTAINER="ai-second-brain-mysql"

echo "=========================================="
echo "  AI-SecondBrain 2.0 数据库验证"
echo "=========================================="

# 1. 表数量
echo "[1/4] 检查表数量..."
docker exec $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  -e "SELECT COUNT(*) AS '表数量' FROM information_schema.tables WHERE table_schema = '${DB_NAME}';" \
  ${DB_NAME}

# 2. 2.0 关键表验证
echo "[2/4] 验证 2.0 关键表 (embedding, relation)..."
docker exec $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  -e "SELECT table_name, table_comment FROM information_schema.tables WHERE table_schema = '${DB_NAME}' AND table_name IN ('knowledge_embedding', 'knowledge_relation', 'qa_session');" \
  ${DB_NAME}

# 3. 字符集验证
echo "[3/4] 验证字符集..."
docker exec $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  -e "SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '${DB_NAME}';" \
  ${DB_NAME}

# 4. 数据量检查
echo "[4/4] 检查用户数据是否恢复..."
docker exec $MYSQL_CONTAINER mysql \
  -u $DB_USER -p"${MYSQL_ROOT_PASSWORD}" \
  -e "SELECT COUNT(*) AS '用户数量' FROM ${DB_NAME}.user;" \
  ${DB_NAME}

echo "=========================================="
echo "  ✅ 验证完成！"
echo "=========================================="
