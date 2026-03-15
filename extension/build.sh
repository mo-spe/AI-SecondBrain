#!/bin/bash

echo "========================================"
echo "AI-SecondBrain 插件打包脚本"
echo "========================================"
echo ""

VERSION="1.0.0"
BUILD_DIR="build"
DIST_DIR="dist"
PACKAGE_NAME="ai-secondbrain-extension-v${VERSION}"

echo "[1/5] 清理构建目录..."
rm -rf ${BUILD_DIR}
mkdir -p ${BUILD_DIR}

echo "[2/5] 复制插件文件..."
cp manifest.json ${BUILD_DIR}/
cp background.js ${BUILD_DIR}/
cp content.js ${BUILD_DIR}/
cp popup.html ${BUILD_DIR}/
cp popup.js ${BUILD_DIR}/
cp -r icons ${BUILD_DIR}/

echo "[3/5] 创建版本信息..."
echo "{\"version\": \"${VERSION}\", \"buildDate\": \"$(date '+%Y-%m-%d %H:%M:%S')\"}" > ${BUILD_DIR}/version.json

echo "[4/5] 创建压缩包..."
rm -rf ${DIST_DIR}
mkdir -p ${DIST_DIR}

cd ${BUILD_DIR}
zip -r ../${DIST_DIR}/${PACKAGE_NAME}.zip *
cd ..

echo "[5/5] 清理临时文件..."
rm -rf ${BUILD_DIR}

echo ""
echo "========================================"
echo "打包完成！"
echo "========================================"
echo ""
echo "输出文件: ${DIST_DIR}/${PACKAGE_NAME}.zip"
echo "版本: ${VERSION}"
echo ""
echo "安装说明:"
echo "1. 下载 ${PACKAGE_NAME}.zip"
echo "2. 解压到任意目录"
echo "3. 打开Chrome浏览器"
echo "4. 访问 chrome://extensions/"
echo "5. 开启'开发者模式'"
echo "6. 点击'加载已解压的扩展程序'"
echo "7. 选择解压后的目录"
echo ""
