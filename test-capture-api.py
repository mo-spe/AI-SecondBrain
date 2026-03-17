import requests
import json

# 1. 登录获取token
print("正在登录...")
login_data = {
    "username": "user2",
    "password": "123456"
}

response = requests.post("http://localhost:8080/api/auth/login", json=login_data)
print(f"登录响应状态码: {response.status_code}")
print(f"登录响应内容: {response.text}")

if response.status_code == 200:
    result = response.json()
    token = result['data']['token']
    print(f"Token: {token}")

    # 2. 测试笔记捕捉
    print("\n正在测试笔记捕捉...")
    note_data = {
        "title": "测试笔记",
        "content": "这是一个测试笔记，用于测试数据捕捉功能。Java是一种广泛使用的编程语言。",
        "userId": 11
    }

    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    note_response = requests.post("http://localhost:8080/api/capture/note", json=note_data, headers=headers)
    print(f"笔记捕捉响应状态码: {note_response.status_code}")
    print(f"笔记捕捉响应内容: {note_response.text}")
else:
    print("登录失败！")