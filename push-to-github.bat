@echo off
chcp 65001 >nul
echo ========================================
echo   推送到 GitHub
echo ========================================
echo.

REM 检查 Git 是否安装
where git >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 未检测到 Git，请先安装 Git
    pause
    exit /b 1
)

REM 检查是否已经配置了远程仓库
git remote -v | findstr "origin" >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [信息] 已配置远程仓库：
    git remote -v
    echo.
    set /p CONFIRM="是否要修改远程仓库地址？(y/n): "
    if /i "%CONFIRM%"=="y" (
        goto SET_REMOTE
    )
) else (
    :SET_REMOTE
    echo 请输入您的 GitHub 仓库地址
    echo 格式：https://github.com/YOUR_USERNAME/AI-SecondBrain.git
    echo.
    set /p REMOTE_URL="远程仓库地址："
    git remote remove origin 2>nul
    git remote add origin %REMOTE_URL%
    echo 远程仓库配置完成 ✓
    echo.
)

echo 准备推送到 GitHub...
echo.

REM 推送代码
echo [1/2] 推送到远程仓库...
git push -u origin main

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 推送失败！
    echo.
    echo 可能的原因：
    echo 1. 远程仓库不存在 - 请先在 GitHub 上创建仓库
    echo 2. 认证失败 - 请检查用户名和密码/Token
    echo 3. 仓库已有内容 - 请确保仓库是空的
    echo.
    echo 解决方案：
    echo 1. 访问 https://github.com/new 创建新仓库
    echo 2. 仓库名称：AI-SecondBrain
    echo 3. 不要添加 README、.gitignore 或 license
    echo 4. 创建完成后重新运行此脚本
    echo.
) else (
    echo.
    echo ========================================
    echo   推送成功！🎉
    echo ========================================
    echo.
    echo 您的项目已成功上传到 GitHub！
    echo.
    echo 接下来可以：
    echo 1. 访问您的 GitHub 仓库查看代码
    echo 2. 配置 GitHub Pages（如果需要）
    echo 3. 设置 GitHub Actions CI/CD
    echo 4. 邀请协作者
    echo 5. 发布 Release
    echo.
    echo 查看项目状态：
    echo    git status
    echo.
)

pause
