#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import requests
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)
CORS(app)

QWEN_API_KEY = os.getenv('QWEN_API_KEY', '')
QWEN_BASE_URL = os.getenv('QWEN_BASE_URL', 'https://dashscope.aliyuncs.com/compatible-mode/v1')
QWEN_MODEL = os.getenv('QWEN_MODEL', 'qwen-plus')

def call_qwen_api(prompt, user_api_key=None):
    """
    调用Qwen API生成内容
    """
    url = f"{QWEN_BASE_URL}/chat/completions"
    
    api_key = user_api_key if user_api_key else QWEN_API_KEY
    
    headers = {
        'Authorization': f'Bearer {api_key}',
        'Content-Type': 'application/json'
    }
    
    data = {
        'model': QWEN_MODEL,
        'messages': [
            {
                'role': 'user',
                'content': prompt
            }
        ],
        'temperature': 0.7,
        'max_tokens': 4000
    }
    
    try:
        api_key_source = "用户API Key" if user_api_key else "平台API Key"
        print(f"正在调用Qwen API，URL: {url}")
        print(f"请求参数: model={QWEN_MODEL}, max_tokens={data['max_tokens']}, API Key来源: {api_key_source}")
        
        response = requests.post(url, headers=headers, json=data, timeout=300)
        response.raise_for_status()
        
        result = response.json()
        print(f"API响应成功，状态码: {response.status_code}, API Key来源: {api_key_source}")
        
        if 'choices' in result and len(result['choices']) > 0:
            content = result['choices'][0]['message']['content']
            print(f"生成内容长度: {len(content)}")
            return content
        else:
            print(f"API响应格式错误: {result}")
            return None
    except requests.exceptions.Timeout:
        print(f"调用Qwen API超时（300秒）")
        return None
    except requests.exceptions.RequestException as e:
        print(f"调用Qwen API失败: {e}")
        return None
    except Exception as e:
        print(f"调用Qwen API异常: {e}")
        return None

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'healthy',
        'service': 'DeerFlow Research Service'
    })

@app.route('/api/research/learning-report', methods=['POST'])
def generate_learning_report():
    """
    生成学习报告
    """
    try:
        data = request.get_json()
        learning_data = data.get('learning_data', '')
        topic = data.get('topic', '')
        depth = data.get('depth', 'deep')
        user_api_key = data.get('api_key', None)
        
        prompt = f"""你是一位资深的个性化学习顾问和AI教育专家，擅长深度分析学习数据并提供精准的学习建议。

**重要指令：不要询问任何澄清问题，不要要求更多信息，直接基于现有数据生成报告。**

请仔细分析以下学习数据，生成一份**真正个性化、有深度**的学习报告：

{learning_data}

**重要要求：**
1. **拒绝模板化回答** - 不要使用通用套话，每条建议都要基于具体数据
2. **深度分析** - 不要只重复数据，要分析数据背后的学习模式和问题
3. **具体可操作** - 提供具体的行动建议，而不是泛泛而谈
4. **个性化洞察** - 基于具体知识点的内容和掌握情况，提供针对性建议
5. **数据驱动** - 所有结论都要有数据支撑

**报告结构：**
# 学习报告

## 1. 深度学习成果分析
- **学习模式识别**：分析你的学习节奏、时间分配模式
- **关键突破点**：识别你真正掌握的核心概念和技能
- **学习效率评估**：分析学习投入与掌握程度的关系
- **具体成就**：列出你真正理解和能应用的知识点

## 2. 知识掌握深度剖析
### 2.1 掌握程度分层分析
- **完全掌握层**：哪些知识点你已经能熟练应用
- **理解层**：哪些知识点你理解但不够熟练
- **模糊层**：哪些知识点你只是听说过或了解概念
- **未掌握层**：哪些知识点你完全不理解

### 2.2 知识关联分析
- **知识网络**：分析知识点之间的关联性
- **知识盲区**：识别学习中的知识断层
- **应用场景**：分析知识点在实际项目中的应用可能性

## 3. 学习行为模式分析
- **学习节奏**：分析你的学习时间分布和节奏特点
- **学习效率**：评估不同时间段的学习效果
- **学习偏好**：识别你擅长的学习方式和内容类型
- **学习障碍**：分析学习中遇到的困难和瓶颈

## 4. 个性化学习建议
### 4.1 针对性改进策略
- **薄弱环节突破**：针对掌握程度低的知识点，提供具体的学习方案
- **优势发挥**：如何利用你的学习优势来提升整体效果
- **学习效率优化**：基于你的学习模式，提供效率提升建议

### 4.2 个性化学习计划
- **短期目标**：基于当前掌握情况，制定1-2周的具体目标
- **中期规划**：1-2个月的系统性提升计划
- **长期发展**：3-6个月的技能发展路径

### 4.3 学习方法优化
- **适合你的学习方法**：基于你的学习特点，推荐最适合的学习方法
- **学习工具建议**：推荐能提升你学习效率的具体工具
- **学习资源推荐**：基于你的知识盲区，推荐具体的学习资源

## 5. 激励与展望
- **进步肯定**：基于具体数据，肯定你的真实进步
- **潜力分析**：分析你的学习潜力和发展方向
- **未来展望**：基于当前进展，展望未来的学习成果

请基于以上结构和要求，生成一份详细的、个性化的学习报告。"""

        report = call_qwen_api(prompt, user_api_key)
        
        if report:
            return jsonify({
                'success': True,
                'data': report
            })
        else:
            return jsonify({
                'success': False,
                'message': '生成学习报告失败'
            }), 500
            
    except Exception as e:
        print(f"生成学习报告异常: {e}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

@app.route('/api/research/learning-path', methods=['POST'])
def generate_learning_path():
    """
    生成学习路径
    """
    try:
        data = request.get_json()
        topic = data.get('topic', '')
        current_level = data.get('current_level', 'beginner')
        target_level = data.get('target_level', 'advanced')
        user_api_key = data.get('api_key', None)
        
        prompt = f"""你是一位资深的学习规划专家，擅长制定个性化的学习路径。

请为以下学习需求制定详细的学习路径：

**学习主题：** {topic}
**当前水平：** {current_level}
**目标水平：** {target_level}

**要求：**
1. 制定详细的学习阶段划分（至少5个阶段）
2. 每个阶段包含具体的学习目标和内容
3. 提供每个阶段的学习时间建议
4. 推荐相关的学习资源
5. 提供学习方法和技巧建议
6. 给出每个阶段的评估标准

请以Markdown格式输出详细的学习路径。"""

        path = call_qwen_api(prompt, user_api_key)
        
        if path:
            return jsonify({
                'success': True,
                'data': path
            })
        else:
            return jsonify({
                'success': False,
                'message': '生成学习路径失败'
            }), 500
            
    except Exception as e:
        print(f"生成学习路径异常: {e}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

@app.route('/api/research/knowledge-gap', methods=['POST'])
def analyze_knowledge_gap():
    """
    分析知识盲区
    """
    try:
        data = request.get_json()
        user_knowledge = data.get('user_knowledge', [])
        target_topic = data.get('target_topic', '')
        user_api_key = data.get('api_key', None)
        
        prompt = f"""你是一位知识分析专家，擅长识别学习中的知识盲区。

**用户已掌握的知识点：**
{', '.join(user_knowledge) if user_knowledge else '无'}

**目标学习主题：** {target_topic}

**要求：**
1. 分析用户在目标主题方面的知识盲区
2. 识别缺失的关键知识点
3. 分析知识点之间的关联性
4. 提供填补知识盲区的学习建议
5. 推荐学习资源和顺序

请以Markdown格式输出详细的知识盲区分析。"""

        analysis = call_qwen_api(prompt, user_api_key)
        
        if analysis:
            return jsonify({
                'success': True,
                'data': analysis
            })
        else:
            return jsonify({
                'success': False,
                'message': '分析知识盲区失败'
            }), 500
            
    except Exception as e:
        print(f"分析知识盲区异常: {e}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000, debug=False)
