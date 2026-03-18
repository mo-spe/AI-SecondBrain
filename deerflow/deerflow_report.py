#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
from pathlib import Path

# 添加DeerFlow到Python路径
deerflow_path = Path(__file__).parent / "deer-flow" / "backend"
sys.path.insert(0, str(deerflow_path))

from src.client import DeerFlowClient

def generate_learning_report(learning_data: str) -> str:
    """
    使用DeerFlow生成学习报告
    
    Args:
        learning_data: 学习数据字符串
        
    Returns:
        生成的学习报告（Markdown格式）
    """
    
    try:
        # 设置配置文件路径
        config_path = Path(__file__).parent / "config.yaml"
        os.environ['DEER_FLOW_CONFIG_PATH'] = str(config_path)
        
        # 创建DeerFlow客户端
        client = DeerFlowClient(config_path=str(config_path))
        
        # 构建学习报告生成的提示词
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

**分析方法：**
- 识别学习模式：学习节奏、知识领域分布、掌握程度变化趋势
- 发现学习问题：哪些知识点反复出错、学习效率低的原因
- 挖掘学习优势：哪些领域掌握快、学习方法的有效性
- 提供个性化建议：基于具体问题给出解决方案

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
- **成长路径**：为你规划清晰的技能成长路径

**请记住：这是一份个性化报告，每一条建议都要基于具体的学习数据，不要使用任何通用模板或套话。**"""
        
        # 使用DeerFlow的chat方法生成报告
        response = client.chat(prompt, thread_id="learning-report")
        
        return response
        
    except Exception as e:
        error_msg = f"生成报告时发生错误：{str(e)}"
        print(error_msg, file=sys.stderr)
        import traceback
        traceback.print_exc()
        return error_msg

def main():
    """
    主函数
    """
    # 设置输出编码为UTF-8，解决Windows控制台中文乱码问题
    if sys.platform == 'win32':
        import codecs
        sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'strict')
        sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'strict')
    
    if len(sys.argv) < 2:
        print("用法：python deerflow_report.py <学习数据文件>", file=sys.stderr)
        print("示例：python deerflow_report.py research_data.txt", file=sys.stderr)
        sys.exit(1)
    
    topic_file = sys.argv[1]
    
    if not os.path.exists(topic_file):
        print(f"错误：文件 '{topic_file}' 不存在", file=sys.stderr)
        sys.exit(1)
    
    try:
        with open(topic_file, 'r', encoding='utf-8') as f:
            learning_data = f.read()
        
        report = generate_learning_report(learning_data)
        
        if report and not report.startswith("错误"):
            print(report)
            sys.exit(0)
        else:
            sys.exit(1)
            
    except Exception as e:
        print(f"处理文件时发生错误：{str(e)}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
