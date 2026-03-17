@echo off
REM ============================================================
REM AI-SecondBrain Windows 部署脚本
REM ============================================================
REM 使用说明：
REM 1. 确保已安装 Docker Desktop
REM 2. 修改 .env.production 配置文件
REM 3. 运行此脚本：deploy.bat
REM ============================================================

setlocal enabledelayedexpansion

REM 颜色定义（Windows 10+ 支持）
for /F "tokens=1,2 delims=#" %%a in ('"prompt #$H#$E# &echo on &for %%b in (1) do     rem"') do (
  set "DEL=%%a"
  set "COLOR_GREEN=%%b[32m"
  set "COLOR_YELLOW=%%b[33m"
  set "COLOR_RED=%%b[31m"
  set "COLOR_RESET=%%b[0m"
)

REM 日志函数
:log_info
echo %COLOR_GREEN%[INFO]%COLOR_RESET% %1
goto :eof

:log_warn
echo %COLOR_YELLOW%[WARN]%COLOR_RESET% %1
goto :eof

:log_error
echo %COLOR_RED%[ERROR]%COLOR_RESET% %1
goto :eof

REM 检查 Docker 是否安装
:check_docker
%log_info% 检查 Docker 安装...
docker --version >nul 2>&1
if errorlevel 1 (
    %log_error% Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    %log_error% Docker Compose 未安装
    pause
    exit /b 1
)

%log_info% Docker 已安装
goto :eof

REM 检查配置文件
:check_config
%log_info% 检查配置文件...
if not exist ".env.production" (
    %log_error% .env.production 文件不存在
    %log_info% 请复制 .env.example 并修改为 .env.production
    pause
    exit /b 1
)
%log_info% 配置文件检查完成
goto :eof

REM 创建必要的目录
:create_directories
%log_info% 创建必要的目录...
if not exist "logs\mysql" mkdir logs\mysql
if not exist "logs\backend" mkdir logs\backend
if not exist "logs\nginx" mkdir logs\nginx
if not exist "ssl" mkdir ssl
if not exist "redis" mkdir redis
%log_info% 目录创建完成
goto :eof

REM 停止旧服务
:stop_services
%log_info% 停止旧服务...
docker-compose -f docker-compose.prod.yml down
%log_info% 旧服务已停止
goto :eof

REM 清理旧容器
:cleanup_containers
%log_info% 清理停止的容器...
docker container prune -f
%log_info% 容器清理完成
goto :eof

REM 构建服务
:build_services
%log_info% 开始构建服务...
%log_info% 构建前端...
docker-compose -f docker-compose.prod.yml build frontend-builder
%log_info% 构建后端...
docker-compose -f docker-compose.prod.yml build backend
%log_info% 构建 deerflow...
docker-compose -f docker-compose.prod.yml build deerflow
%log_info% 服务构建完成
goto :eof

REM 启动服务
:start_services
%log_info% 启动服务...
docker-compose -f docker-compose.prod.yml up -d
%log_info% 服务启动完成
goto :eof

REM 检查服务状态
:check_status
%log_info% 检查服务状态...
timeout /t 10 /nobreak >nul
docker-compose -f docker-compose.prod.yml ps
%log_info% 服务状态检查完成
goto :eof

REM 显示帮助信息
:show_help
echo AI-SecondBrain 部署脚本
echo.
echo 用法：%0 [命令]
echo.
echo 命令:
echo   deploy      完整部署流程（默认）
echo   build       仅构建服务
echo   start       启动服务
echo   stop        停止服务
echo   restart     重启服务
echo   status      查看服务状态
echo   logs        查看服务日志
echo   clean       清理容器和镜像
echo   help        显示帮助信息
echo.
echo 示例:
echo   %0 deploy   完整部署
echo   %0 logs     查看日志
goto :eof

REM 主程序
:main
if "%~1"=="" set "CMD=deploy"
if "%~1"=="deploy" set "CMD=deploy"
if "%~1"=="build" set "CMD=build"
if "%~1"=="start" set "CMD=start"
if "%~1"=="stop" set "CMD=stop"
if "%~1"=="restart" set "CMD=restart"
if "%~1"=="status" set "CMD=status"
if "%~1"=="logs" set "CMD=logs"
if "%~1"=="clean" set "CMD=clean"
if "%~1"=="help" set "CMD=help"

if "%CMD%"=="deploy" (
    call :check_docker
    call :check_config
    call :create_directories
    call :stop_services
    call :cleanup_containers
    call :build_services
    call :start_services
    call :check_status
    %log_info% 部署完成！
    %log_info% 访问地址：http://localhost
    %log_info% API 文档：http://localhost/api/doc.html
    goto :end
)

if "%CMD%"=="build" (
    call :check_docker
    call :build_services
    goto :end
)

if "%CMD%"=="start" (
    call :check_docker
    call :start_services
    call :check_status
    goto :end
)

if "%CMD%"=="stop" (
    call :check_docker
    call :stop_services
    goto :end
)

if "%CMD%"=="restart" (
    call :check_docker
    call :stop_services
    call :start_services
    call :check_status
    goto :end
)

if "%CMD%"=="status" (
    call :check_docker
    call :check_status
    goto :end
)

if "%CMD%"=="logs" (
    call :check_docker
    docker-compose -f docker-compose.prod.yml logs -f
    goto :end
)

if "%CMD%"=="clean" (
    call :check_docker
    docker-compose -f docker-compose.prod.yml down
    docker container prune -f
    docker image prune -f
    %log_info% 清理完成
    goto :end
)

if "%CMD%"=="help" (
    call :show_help
    goto :end
)

%log_error% 未知命令：%1
call :show_help

:end
endlocal
pause
