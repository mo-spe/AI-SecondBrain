@echo off
echo ========================================
echo 同步知识点到Elasticsearch
echo ========================================
echo.

echo 正在调用同步API...
curl -X POST http://localhost:8080/api/knowledge/sync-to-elasticsearch -H "Content-Type: application/json" -d "{}"

echo.
echo 同步完成！
echo 请检查Elasticsearch中的数据：
echo curl -X GET "localhost:9200/knowledge_nodes/_search?pretty"
echo.

pause
