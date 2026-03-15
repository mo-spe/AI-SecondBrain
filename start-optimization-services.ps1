# AI-SecondBrain 系统优化快速启动脚本

# 检查Docker是否运行
Write-Host "检查Docker状态..." -ForegroundColor Green
$dockerStatus = docker ps 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker未运行，请先启动Docker" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Docker运行正常" -ForegroundColor Green

# 启动Elasticsearch
Write-Host "`n🚀 启动Elasticsearch服务..." -ForegroundColor Cyan
docker-compose up -d elasticsearch
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Elasticsearch启动成功" -ForegroundColor Green
    Write-Host "   端口: 9200" -ForegroundColor Yellow
    Write-Host "   状态: http://localhost:9200/_cluster/health" -ForegroundColor Yellow
} else {
    Write-Host "❌ Elasticsearch启动失败" -ForegroundColor Red
}

# 启动Kafka和Zookeeper
Write-Host "`n🚀 启动Kafka和Zookeeper服务..." -ForegroundColor Cyan
docker-compose up -d kafka zookeeper
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Kafka和Zookeeper启动成功" -ForegroundColor Green
    Write-Host "   Kafka端口: 9092" -ForegroundColor Yellow
    Write-Host "   Zookeeper端口: 2181" -ForegroundColor Yellow
} else {
    Write-Host "❌ Kafka和Zookeeper启动失败" -ForegroundColor Red
}

# 等待服务就绪
Write-Host "`n⏳ 等待服务就绪..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 检查服务状态
Write-Host "`n📊 服务状态检查:" -ForegroundColor Cyan
$services = @("elasticsearch", "kafka", "zookeeper")
foreach ($service in $services) {
    $status = docker ps --filter "name=ai-second-brain-$service" --format "{{.Status}}"
    if ($status -match "Up") {
        Write-Host "✅ $service : 运行中" -ForegroundColor Green
    } else {
        Write-Host "❌ $service : 未运行" -ForegroundColor Red
    }
}

# 检查Elasticsearch健康状态
Write-Host "`n🏥 检查Elasticsearch健康状态..." -ForegroundColor Cyan
try {
    $esHealth = curl -s http://localhost:9200/_cluster/health
    if ($esHealth) {
        $healthStatus = ($esHealth | ConvertFrom-Json).status
        if ($healthStatus -eq "green" -or $healthStatus -eq "yellow") {
            Write-Host "✅ Elasticsearch健康状态: $healthStatus" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Elasticsearch健康状态: $healthStatus" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ 无法连接到Elasticsearch" -ForegroundColor Red
}

# 检查Kafka连接
Write-Host "`n📡 检查Kafka连接..." -ForegroundColor Cyan
try {
    $kafkaTopics = docker exec ai-second-brain-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Kafka连接正常" -ForegroundColor Green
        Write-Host "   可用主题: $kafkaTopics" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ 无法连接到Kafka" -ForegroundColor Red
}

# 配置更新提示
Write-Host "`n⚙️  配置更新提示:" -ForegroundColor Cyan
Write-Host "请确保 application.yml 中以下配置已启用:" -ForegroundColor Yellow
Write-Host "spring:" -ForegroundColor White
Write-Host "  elasticsearch:" -ForegroundColor White
Write-Host "    enabled: true  # 确保为true" -ForegroundColor Green
Write-Host "  kafka:" -ForegroundColor White
Write-Host "    enabled: true  # 确保为true" -ForegroundColor Green

# 后续步骤
Write-Host "`n📋 后续步骤:" -ForegroundColor Cyan
Write-Host "1. 重启后端服务: mvn spring-boot:run" -ForegroundColor White
Write-Host "2. 生成向量数据: 调用 POST /api/vector/batch-generate" -ForegroundColor White
Write-Host "3. 测试语义搜索: 调用 GET /api/knowledge/search/semantic" -ForegroundColor White
Write-Host "4. 测试异步任务: 调用 POST /api/deerflow/research/learning-path-async" -ForegroundColor White

Write-Host "`n🎉 优化服务启动完成！" -ForegroundColor Green
Write-Host "详细文档请查看: SYSTEM_OPTIMIZATION_GUIDE.md" -ForegroundColor Yellow