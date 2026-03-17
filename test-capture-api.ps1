# 测试数据捕捉接口

# 1. 登录获取token
Write-Host "正在登录..."
$body = @{
    username = "user2"
    password = "123456"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
Write-Host "登录成功！"
Write-Host "Token: $($loginResponse.data.token)"

$token = $loginResponse.data.token

# 2. 测试笔记捕捉
Write-Host "`n正在测试笔记捕捉..."
$noteBody = @{
    title = "测试笔记"
    content = "这是一个测试笔记，用于测试数据捕捉功能。Java是一种广泛使用的编程语言。"
    userId = 11
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $token"
}

try {
    $noteResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/capture/note" -Method Post -Body $noteBody -ContentType "application/json" -Headers $headers
    Write-Host "笔记捕捉成功！"
    Write-Host ($noteResponse | ConvertTo-Json)
} catch {
    Write-Host "笔记捕捉失败：$($_.Exception.Message)"
    Write-Host "错误详情：$($_.ErrorDetails.Message)"
}