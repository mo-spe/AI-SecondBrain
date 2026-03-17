@echo off
echo ========================================
echo AI-SecondBrain 自动化测试报告
echo ========================================
echo.

echo [测试1] 检查后端服务状态
echo ----------------------------------------
netstat -ano | findstr :8080
if %errorlevel% equ 0 (
    echo ✅ 后端服务正在运行 (端口8080)
) else (
    echo ❌ 后端服务未运行
)
echo.

echo [测试2] 检查数据库向量数据
echo ----------------------------------------
mysql -u root -p123456 -e "USE second_brain; SELECT '知识点总数' as type, COUNT(*) as count FROM knowledge_node WHERE deleted=0 UNION SELECT '向量数据总数', COUNT(*) FROM knowledge_embedding;"
echo.

echo [测试3] 检查缺少向量的知识点
echo ----------------------------------------
mysql -u root -p123456 -e "USE second_brain; SELECT COUNT(*) as missing_vectors FROM knowledge_node kn LEFT JOIN knowledge_embedding ke ON kn.id = ke.knowledge_id WHERE kn.deleted=0 AND ke.id IS NULL;"
echo.

echo [测试4] 检查知识图谱关系数据
echo ----------------------------------------
mysql -u root -p123456 -e "USE second_brain; SELECT COUNT(*) as relation_count FROM knowledge_relation;"
echo.

echo [测试5] 检查复习卡片数据
echo ----------------------------------------
mysql -u root -p123456 -e "USE second_brain; SELECT deleted, COUNT(*) as count FROM review_card GROUP BY deleted;"
echo.

echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 注意：后端服务正在运行，请查看后端日志了解详细执行情况
pause