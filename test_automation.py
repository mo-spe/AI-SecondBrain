import requests
import json
import time

BASE_URL = "http://localhost:8080/api"

def print_test_result(test_name, success, message=""):
    status = "✅ PASS" if success else "❌ FAIL"
    print(f"{status} - {test_name}")
    if message:
        print(f"    {message}")

def test_rag_qa():
    """测试RAG知识问答功能"""
    print("\n=== 测试RAG知识问答功能 ===")
    
    try:
        # 测试RAG问答
        response = requests.post(
            f"{BASE_URL}/rag/qa",
            json={"question": "JVM垃圾回收机制"},
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            result = response.json()
            print_test_result("RAG问答API调用", True, f"响应码: {response.status_code}")
            
            if result.get("code") == 200:
                print_test_result("RAG问答返回成功", True)
                data = result.get("data", {})
                answer = data.get("answer", "")
                sources = data.get("sources", [])
                
                if answer:
                    print_test_result("RAG问答返回答案", True, f"答案长度: {len(answer)}")
                else:
                    print_test_result("RAG问答返回答案", False, "答案为空")
                
                if sources:
                    print_test_result("RAG问答返回相关知识点", True, f"找到{len(sources)}个相关知识点")
                else:
                    print_test_result("RAG问答返回相关知识点", False, "未找到相关知识点")
            else:
                print_test_result("RAG问答返回成功", False, f"错误信息: {result.get('message')}")
        else:
            print_test_result("RAG问答API调用", False, f"响应码: {response.status_code}")
            
    except Exception as e:
        print_test_result("RAG问答测试", False, f"异常: {str(e)}")

def test_knowledge_graph():
    """测试知识图谱自动生成关系功能"""
    print("\n=== 测试知识图谱自动生成关系功能 ===")
    
    try:
        # 测试自动生成关系
        response = requests.post(
            f"{BASE_URL}/knowledge-graph/auto-generate",
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            result = response.json()
            print_test_result("知识图谱自动生成关系API调用", True, f"响应码: {response.status_code}")
            
            if result.get("code") == 200:
                print_test_result("知识图谱自动生成关系成功", True)
                data = result.get("data", {})
                generated_count = data.get("generatedCount", 0)
                
                if generated_count > 0:
                    print_test_result("知识图谱生成关系数量", True, f"生成了{generated_count}个关系")
                else:
                    print_test_result("知识图谱生成关系数量", False, "生成了0个关系")
            else:
                print_test_result("知识图谱自动生成关系成功", False, f"错误信息: {result.get('message')}")
        else:
            print_test_result("知识图谱自动生成关系API调用", False, f"响应码: {response.status_code}")
            
    except Exception as e:
        print_test_result("知识图谱自动生成关系测试", False, f"异常: {str(e)}")

def test_review_center():
    """测试复习中心功能"""
    print("\n=== 测试复习中心功能 ===")
    
    try:
        # 测试生成练习卡片
        response = requests.post(
            f"{BASE_URL}/review-cards/generate-all",
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            result = response.json()
            print_test_result("生成练习卡片API调用", True, f"响应码: {response.status_code}")
            
            if result.get("code") == 200:
                print_test_result("生成练习卡片成功", True)
                data = result.get("data", {})
                count = data.get("count", 0)
                print_test_result("生成练习卡片数量", True, f"生成了{count}张练习卡片")
            else:
                print_test_result("生成练习卡片成功", False, f"错误信息: {result.get('message')}")
        else:
            print_test_result("生成练习卡片API调用", False, f"响应码: {response.status_code}")
        
        # 等待一下
        time.sleep(1)
        
        # 测试清空卡片（软删除）
        response = requests.delete(
            f"{BASE_URL}/review-cards/all",
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            result = response.json()
            print_test_result("清空卡片API调用", True, f"响应码: {response.status_code}")
            
            if result.get("code") == 200:
                print_test_result("清空卡片成功", True)
            else:
                print_test_result("清空卡片成功", False, f"错误信息: {result.get('message')}")
        else:
            print_test_result("清空卡片API调用", False, f"响应码: {response.status_code}")
        
        # 等待一下
        time.sleep(1)
        
        # 测试生成复习卡片（恢复软删除的卡片）
        response = requests.post(
            f"{BASE_URL}/review-cards/restore",
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            result = response.json()
            print_test_result("生成复习卡片（恢复）API调用", True, f"响应码: {response.status_code}")
            
            if result.get("code") == 200:
                print_test_result("生成复习卡片（恢复）成功", True)
                data = result.get("data", {})
                restored_count = data.get("restoredCount", 0)
                print_test_result("恢复卡片数量", True, f"恢复了{restored_count}张卡片")
            else:
                print_test_result("生成复习卡片（恢复）成功", False, f"错误信息: {result.get('message')}")
        else:
            print_test_result("生成复习卡片（恢复）API调用", False, f"响应码: {response.status_code}")
            
    except Exception as e:
        print_test_result("复习中心测试", False, f"异常: {str(e)}")

def test_vector_generation():
    """测试知识点向量自动生成功能"""
    print("\n=== 测试知识点向量自动生成功能 ===")
    
    try:
        # 测试批量生成向量
        response = requests.post(
            f"{BASE_URL}/vector/batch-generate",
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            result = response.json()
            print_test_result("批量生成向量API调用", True, f"响应码: {response.status_code}")
            
            if result.get("code") == 200:
                print_test_result("批量生成向量任务提交成功", True)
                print("    注意：向量生成是异步执行的，请查看后端日志确认执行情况")
            else:
                print_test_result("批量生成向量任务提交成功", False, f"错误信息: {result.get('message')}")
        else:
            print_test_result("批量生成向量API调用", False, f"响应码: {response.status_code}")
            
    except Exception as e:
        print_test_result("知识点向量自动生成测试", False, f"异常: {str(e)}")

def main():
    print("=" * 60)
    print("AI-SecondBrain 自动化测试")
    print("=" * 60)
    
    # 测试RAG知识问答
    test_rag_qa()
    
    # 测试知识图谱
    test_knowledge_graph()
    
    # 测试复习中心
    test_review_center()
    
    # 测试向量生成
    test_vector_generation()
    
    print("\n" + "=" * 60)
    print("测试完成！")
    print("=" * 60)

if __name__ == "__main__":
    main()