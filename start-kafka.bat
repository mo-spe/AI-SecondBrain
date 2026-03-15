@echo off
echo ========================================
echo 启动Kafka服务
echo ========================================
echo.

echo 检查Kafka端口占用...
netstat -ano | findstr :9092
if errorlevel 0 (
    echo 警告：端口9092已被占用
    echo 请先停止占用该端口的进程
    pause
    exit /b 1
)

echo.
echo 启动Kafka服务...
echo 配置文件：d:\AI-SecondBrain\kafka-server-fixed.properties
echo 监听端口：9092
echo.

cd /d D:\kafka_2.13-4.2.0\bin\windows
kafka-server-start.bat d:\AI-SecondBrain\kafka-server-fixed.properties

pause
