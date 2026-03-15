@echo off
echo ========================================
echo 清理Kafka日志文件
echo ========================================
echo.

echo 停止可能运行的Kafka进程...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq kafka*" 2>nul

echo.
echo 清理Kafka日志文件...
if exist "D:\kafka_2.13-4.2.0\logs" (
    rd /s /q "D:\kafka_2.13-4.2.0\logs"
    echo 日志目录已删除
)

echo.
echo 清理Kafka数据文件...
if exist "D:\kafka_2.13-4.2.0\data" (
    rd /s /q "D:\kafka_2.13-4.2.0\data"
    echo 数据目录已删除
)

echo.
echo 重新创建目录...
mkdir "D:\kafka_2.13-4.2.0\logs"
mkdir "D:\kafka_2.13-4.2.0\data"

echo.
echo 清理完成！
echo 现在可以启动Kafka了
echo.
pause
