@echo off
chcp 65001
echo ========================================
echo AI-SecondBrain 插件打包脚本
echo ========================================
echo.

set VERSION=1.0.0
set BUILD_DIR=build
set DIST_DIR=dist
set PACKAGE_NAME=ai-secondbrain-extension-v%VERSION%

echo [1/5] 清理构建目录...
if exist %BUILD_DIR% (
    rmdir /s /q %BUILD_DIR%
)
mkdir %BUILD_DIR%

echo [2/5] 复制插件文件...
xcopy manifest.json %BUILD_DIR%\ /Y
xcopy background.js %BUILD_DIR%\ /Y
xcopy content.js %BUILD_DIR%\ /Y
xcopy popup.html %BUILD_DIR%\ /Y
xcopy popup.js %BUILD_DIR%\ /Y
xcopy icons %BUILD_DIR%\icons\ /E /I /Y

echo [3/5] 创建版本信息...
echo {"version": "%VERSION%", "buildDate": "%DATE% %TIME%"} > %BUILD_DIR%\version.json

echo [4/5] 创建压缩包...
if exist %DIST_DIR% (
    rmdir /s /q %DIST_DIR%
)
mkdir %DIST_DIR%

powershell Compress-Archive -Path %BUILD_DIR%\* -DestinationPath %DIST_DIR%\%PACKAGE_NAME%.zip -Force

echo [5/5] 清理临时文件...
rmdir /s /q %BUILD_DIR%

echo.
echo ========================================
echo 打包完成！
echo ========================================
echo.
echo 输出文件: %DIST_DIR%\%PACKAGE_NAME%.zip
echo 版本: %VERSION%
echo.
echo 安装说明:
echo 1. 下载 %PACKAGE_NAME%.zip
echo 2. 解压到任意目录
echo 3. 打开Chrome浏览器
echo 4. 访问 chrome://extensions/
echo 5. 开启"开发者模式"
echo 6. 点击"加载已解压的扩展程序"
echo 7. 选择解压后的目录
echo.
pause
