@echo off
REM ================================================
REM AI-SecondBrain 快速启动脚本（Windows）
REM ================================================

echo ================================================
echo   AI-SecondBrain 快速启动脚本
echo ================================================
echo.

REM 检查 Docker
where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误：未检测到 Docker，请先安装 Docker
    echo    访问：https://docs.docker.com/desktop/install/windows-install/
    pause
    exit /b 1
)

echo ✅ Docker 已安装
docker --version
echo.

REM 检查 Docker Compose
where docker-compose >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ 错误：未检测到 Docker Compose
    echo    访问：https://docs.docker.com/compose/install/
    pause
    exit /b 1
)

echo ✅ Docker Compose 已安装
echo.

REM 检查环境变量文件
if not exist .env (
    echo ⚠️  未检测到 .env 文件，正在从 .env.example 复制...
    copy .env.example .env
    echo.
    echo ⚠️  请编辑 .env 文件，配置以下必需项：
    echo    - MYSQL_ROOT_PASSWORD
    echo    - MYSQL_PASSWORD
    echo    - REDIS_PASSWORD
    echo    - QWEN_API_KEY ^(或其他 AI API 密钥^)
    echo.
    pause
)

REM 检查目录
echo 📁 检查必要的目录...
if not exist logs mkdir logs
if not exist logs\mysql mkdir logs\mysql
if not exist logs\backend mkdir logs\backend
if not exist logs\frontend mkdir logs\frontend

echo.
echo 🚀 启动所有服务...
docker-compose up -d

echo.
echo ⏳ 等待服务启动（约 30 秒）...
timeout /t 30 /nobreak

REM 检查服务状态
echo.
echo 📊 服务状态：
docker-compose ps

echo.
echo ================================================
echo   🎉 启动完成！
echo ================================================
echo.
echo 📱 访问地址：
echo    - 前端界面：http://localhost
echo    - API 文档：http://localhost:8080/api/doc.html
echo    - 健康检查：http://localhost:8080/api/health
echo.
echo 🔐 默认登录：
echo    - 用户名：admin
echo    - 密码：admin123
echo.
echo ⚠️  首次登录后请立即修改密码！
echo.
echo 📖 查看日志：
echo    docker-compose logs -f
echo.
echo 🛑 停止服务：
echo    docker-compose down
echo.
echo ================================================
pause
