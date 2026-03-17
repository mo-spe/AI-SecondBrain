import urllib.request
import json

# 1. 登录获取token
print("正在登录...")
login_data = {
    "username": "user2",
    "password": "123456"
}

req = urllib.request.Request(
    "http://localhost:8080/api/auth/login",
    data=json.dumps(login_data).encode('utf-8'),
    headers={'Content-Type': 'application/json'}
)

with urllib.request.urlopen(req) as response:
    result = json.loads(response.read().decode('utf-8'))
    token = result['data']['token']
    print(f"Token: {token}")

# 2. 测试笔记捕捉
print("\n正在测试笔记捕捉...")
note_data = {
    "title": "测试笔记",
    "content": "这是一个测试笔记，用于测试数据捕捉功能。Java是一种广泛使用的编程语言。",
    "userId": 11
}

req = urllib.request.Request(
    "http://localhost:8080/api/capture/note",
    data=json.dumps(note_data).encode('utf-8'),
    headers={
        'Authorization': f'Bearer {token}',
        'Content-Type': 'application/json'
    }
)

try:
    with urllib.request.urlopen(req) as response:
        result = json.loads(response.read().decode('utf-8'))
        print(f"笔记捕捉成功！响应：{result}")
except urllib.error.HTTPError as e:
    print(f"笔记捕捉失败：{e.code} - {e.reason}")
    print(f"错误详情：{e.read().decode('utf-8')}")