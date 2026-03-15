@echo off
echo ========================================
echo 启动DeerFlow服务
echo ========================================
echo.

cd /d "%~dp0deerflow"

echo 检查Python环境...
python --version
if errorlevel 1 (
    echo 错误：未找到Python环境
    pause
    exit /b 1
)

echo.
echo 安装Python依赖...
pip install flask flask-cors requests python-dotenv
if errorlevel 1 (
    echo 警告：依赖安装可能失败，尝试继续...
)

echo.
echo 启动DeerFlow服务...
echo 服务地址：http://localhost:8000
echo 按 Ctrl+C 停止服务
echo.

python app.py

pause
