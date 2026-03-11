#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
from dotenv import load_dotenv
from openai import OpenAI

# 加载环境变量
load_dotenv()

# 设置标准输出编码为UTF-8
if sys.platform == 'win32':
    import codecs
    sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'strict')
    sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'strict')

def generate_learning_report(topic_file: str) -> str:
    """
    基于学习数据生成学习报告（使用AI模型）
    
    Args:
        topic_file: 包含学习主题和数据的文件路径
        
    Returns:
        生成的学习报告（Markdown格式）
    """
    
    try:
        with open(topic_file, 'r', encoding='utf-8') as f:
            learning_data = f.read()
        
        print(f"开始生成学习报告...")
        print(f"学习数据：\n{learning_data}\n")
        
        report = generate_report_with_ai(learning_data)
        
        print("学习报告生成完成！")
        return report
        
    except FileNotFoundError:
        error_msg = f"错误：找不到文件 {topic_file}"
        print(error_msg, file=sys.stderr)
        return error_msg
    except Exception as e:
        error_msg = f"生成报告时发生错误：{str(e)}"
        print(error_msg, file=sys.stderr)
        return error_msg

def generate_report_with_ai(learning_data: str) -> str:
    """
    使用AI模型生成学习报告
    
    Args:
        learning_data: 学习数据字符串
        
    Returns:
        Markdown格式的学习报告
    """
    
    # 初始化通义千问模型
    api_key = os.getenv('QWEN_API_KEY')
    base_url = os.getenv('QWEN_BASE_URL', 'https://dashscope.aliyuncs.com/compatible-mode/v1')
    model = os.getenv('QWEN_MODEL', 'qwen-plus')
    
    if not api_key:
        raise ValueError("未找到QWEN_API_KEY环境变量")
    
    client = OpenAI(
        api_key=api_key,
        base_url=base_url
    )
    
    # 构建系统提示词
    system_prompt = """你是一位专业的学习顾问和教育专家，擅长分析学习数据并提供个性化的学习建议。

你的任务是基于用户提供的学习数据，生成一份详细、个性化的学习报告。

报告要求：
1. 使用Markdown格式
2. 内容要具体、有针对性，不要泛泛而谈
3. 基于实际的学习数据进行分析
4. 提供可操作的学习建议
5. 语言要鼓励性和启发性

报告结构：
# 学习报告

## 1. 学习成果总结
- 总结学习的主要成果
- 强调进步和成就
- 提及学习的关键里程碑

## 2. 知识掌握情况分析
### 2.1 整体掌握水平
- 分析知识点的整体掌握情况
- 识别优势和薄弱环节

### 2.2 学习进度评估
- 评估学习的广度和深度
- 分析学习的持续性

## 3. 学习数据分析
[这里会插入实际的学习数据]

## 4. 学习建议和改进方向
### 4.1 复习策略优化
- 基于知识点的重要程度和掌握程度，提供具体的复习建议
- 建议使用间隔重复等科学学习方法

### 4.2 学习方法建议
- 根据学习内容特点，推荐合适的学习方法
- 建议使用费曼技巧、主动回忆等高效学习策略

### 4.3 知识管理优化
- 建议如何更好地组织和管理知识
- 推荐知识关联和标签策略

## 5. 未来学习计划推荐
### 5.1 短期目标（1-2周）
- 具体的、可执行的短期目标
- 优先级排序

### 5.2 中期目标（1-2个月）
- 中期的学习目标和里程碑
- 技能提升计划

### 5.3 长期目标（3-6个月）
- 长期的学习愿景
- 能力提升方向

## 6. 总结
- 鼓励性的总结
- 强调持续学习的重要性
- 提供动力和启发"""
    
    # 构建用户提示词
    user_prompt = f"""请基于以下学习数据，生成一份详细的学习报告：

{learning_data}

请确保报告内容具体、有针对性，并且基于实际的学习数据进行分析。"""
    
    # 调用AI生成报告
    print("正在调用AI模型生成报告...")
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt}
        ],
        temperature=0.7,
        max_tokens=4000
    )
    
    return response.choices[0].message.content

def main():
    """
    主函数
    """
    if len(sys.argv) < 2:
        print("用法：python generate_report.py <学习数据文件>", file=sys.stderr)
        print("示例：python generate_report.py research_data.txt", file=sys.stderr)
        sys.exit(1)
    
    topic_file = sys.argv[1]
    
    if not os.path.exists(topic_file):
        print(f"错误：文件 '{topic_file}' 不存在", file=sys.stderr)
        sys.exit(1)
    
    try:
        report = generate_learning_report(topic_file)
        print(report)
        sys.exit(0)
    except Exception as e:
        print(f"生成报告失败：{str(e)}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
