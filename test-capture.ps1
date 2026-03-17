# 测试数据捕捉功能

# 1. 登录获取token
$loginResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"user2","password":"123456"}'
Write-Host "登录响应："
$loginResponse | ConvertTo-Json

# 解析token
$token = $loginResponse.data.token
Write-Host "Token: $token"

# 2. 测试笔记捕捉
$noteCaptureResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/capture/note" -Method POST -Headers @{"Authorization"="Bearer $token"} -ContentType "application/json" -Body '{"title":"测试笔记","content":"这是一个测试笔记，用于测试数据捕捉功能。Java是一种广泛使用的编程语言。","userId":11}'
Write-Host "笔记捕捉响应："
$noteCaptureResponse | ConvertTo-Json

# 3. 测试文档捕捉
$filePath = "D:\AI-SecondBrain\test.txt"
$fileBytes = [System.IO.File]::ReadAllBytes($filePath)
$fileBase64 = [System.Convert]::ToBase64String($fileBytes)

$multipartContent = @{
    file = @{
        FileName = "test.txt"
        Bytes = $fileBytes
    }
    userId = "11"
}

$documentCaptureResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/capture/document" -Method POST -Headers @{"Authorization"="Bearer $token"} -Form $multipartContent
Write-Host "文档捕捉响应："
$documentCaptureResponse | ConvertTo-Json