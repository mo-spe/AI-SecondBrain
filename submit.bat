@echo off
chcp 65001 >nul
echo ========================================
echo   AI-SecondBrain GitHub 提交脚本
echo ========================================
echo.

REM 检查 Git 是否安装
where git >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未检测到 Git，请先安装 Git
    echo 下载地址：https://git-scm.com/download/win
    pause
    exit /b 1
)

echo [1/7] 检查 Git 安装... ✓
git --version
echo.

REM 检查是否在 Git 仓库中
if not exist ".git" (
    echo [2/7] 初始化 Git 仓库...
    git init
) else (
    echo [2/7] Git 仓库已存在 ✓
)
echo.

REM 检查是否配置了 Git 用户信息
git config user.name >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [3/7] 配置 Git 用户信息
    set /p GIT_USER_NAME="请输入您的 Git 用户名："
    git config user.name "%GIT_USER_NAME%"
    
    set /p GIT_USER_EMAIL="请输入您的 Git 邮箱："
    git config user.email "%GIT_USER_EMAIL%"
    echo 用户信息配置完成 ✓
) else (
    echo [3/7] Git 用户信息已配置 ✓
)
echo.

REM 添加所有文件
echo [4/7] 添加所有文件到暂存区...
git add .
echo 文件添加完成 ✓
echo.

REM 查看状态
echo [5/7] 检查文件状态...
git status --short
echo.

REM 提交代码
echo [6/7] 提交代码...
git commit -m "Initial commit: AI-SecondBrain V2.0

- 完整的智能第二大脑系统
- 基于 Docker 的一键部署
- 支持多平台 AI 对话采集
- 包含知识管理、智能复习、RAG 问答等功能
- 提供 Windows/Linux/macOS 跨平台启动脚本
- 包含浏览器插件和 AI 服务
- 完整的开发和部署文档"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [警告] 提交失败，可能是没有文件变更
    echo 如果看到 'nothing to commit'，说明文件已经提交过了
    echo.
) else (
    echo 代码提交成功 ✓
)
echo.

REM 设置默认分支
echo [7/7] 设置默认分支为 main...
git branch -M main
echo 分支设置完成 ✓
echo.

echo ========================================
echo   本地 Git 仓库创建完成！
echo ========================================
echo.
echo 下一步操作：
echo 1. 在 GitHub 上创建新仓库（不要添加 README、.gitignore 或 license）
echo 2. 复制仓库地址
echo 3. 运行以下命令关联远程仓库并推送：
echo.
echo    git remote add origin https://github.com/YOUR_USERNAME/AI-SecondBrain.git
echo    git push -u origin main
echo.
echo 或者运行：submit-to-github.bat
echo.
pause
