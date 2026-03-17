# 测试笔记捕捉功能

# 1. 登录获取token
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"user2\",\"password\":\"123456\"}" > login_response.json

# 解析token（需要手动从login_response.json中获取token）
# 2. 测试笔记捕捉
# 需要将下面的TOKEN替换为实际的token值
curl -X POST http://localhost:8080/api/capture/note -H "Content-Type: application/json" -H "Authorization: Bearer TOKEN" -d "{\"title\":\"测试笔记\",\"content\":\"这是一个测试笔记，用于测试数据捕捉功能。Java是一种广泛使用的编程语言。\",\"userId\":11}" > note_capture_response.json