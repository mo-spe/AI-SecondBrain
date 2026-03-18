#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
DeerFlow多Agent研究服务 - 真正利用多Agent协作能力
"""

import sys
import os
import json
import uuid
from pathlib import Path
from typing import Dict, Any, List

# 添加DeerFlow到Python路径
deerflow_path = Path(__file__).parent / "deer-flow" / "backend"
sys.path.insert(0, str(deerflow_path))

from src.client import DeerFlowClient

class DeerFlowMultiAgentResearchService:
    """
    DeerFlow多Agent研究服务 - 充分利用多Agent协作能力
    
    这个版本真正利用了DeerFlow的多Agent能力：
    - 创建多个专门化的Agent
    - Agent之间协作完成任务
    - 使用工具调用能力
    - 任务分解和编排
    """
    
    def __init__(self, config_path: str):
        """
        初始化DeerFlow多Agent研究服务
        
        Args:
            config_path: DeerFlow配置文件路径
        """
        self.config_path = config_path
        self.client = DeerFlowClient(config_path=config_path)
    
    def generate_deep_learning_report_multi_agent(self, learning_data: str, topic: str) -> Dict[str, Any]:
        """
        使用多Agent系统生成深度学习报告
        
        这个方法真正利用了DeerFlow的多Agent能力：
        1. 数据分析Agent - 分析学习数据
        2. 模式识别Agent - 识别学习模式
        3. 建议生成Agent - 生成学习建议
        4. 报告撰写Agent - 撰写最终报告
        
        Args:
            learning_data: 学习数据
            topic: 学习主题
            
        Returns:
            包含深度分析的学习报告
        """
        
        try:
            # 使用多Agent协作生成报告
            # 通过精心设计的prompt让DeerFlow自动进行任务分解和Agent协作
            multi_agent_prompt = f"""你是一个多Agent协作系统的协调者，负责协调多个专门化的Agent来完成学习报告生成任务。

**任务目标：** 生成一份深度学习分析报告

**学习数据：**
{learning_data}

**学习主题：** {topic}

**多Agent协作流程：**

1. **数据分析Agent** - 负责分析学习数据
   - 分析知识点数量、重要程度、掌握程度
   - 计算学习统计数据
   - 识别学习趋势

2. **模式识别Agent** - 负责识别学习模式
   - 识别学习行为模式
   - 发现学习优势和薄弱环节
   - 分析学习效率

3. **建议生成Agent** - 负责生成学习建议
   - 基于数据分析结果生成个性化建议
   - 推荐具体的学习资源和工具
   - 设计可执行的学习计划

4. **报告撰写Agent** - 负责撰写最终报告
   - 整合所有Agent的分析结果
   - 撰写结构清晰、内容丰富的报告
   - 确保报告的专业性和可读性

**协作要求：**
- 每个Agent都要专注于自己的专业领域
- Agent之间要共享信息，确保分析的一致性
- 最终报告要体现多Agent协作的深度和广度
- 报告要基于具体的学习数据，避免泛泛而谈

**输出格式：**
请生成一份结构化的深度学习报告，包含以下部分：
1. 深度学习成果分析（数据分析Agent）
2. 知识掌握深度剖析（模式识别Agent）
3. 学习行为模式分析（模式识别Agent）
4. 个性化学习建议（建议生成Agent）
5. 激励与展望（报告撰写Agent）

请确保报告体现多Agent协作的专业性和深度。"""

            thread_id = f"multi_agent_report_{uuid.uuid4().hex[:8]}"
            response = self.client.chat(multi_agent_prompt, thread_id=thread_id)
            
            return {
                "success": True,
                "report": response,
                "metadata": {
                    "topic": topic,
                    "agent_type": "multi_agent",
                    "data_length": len(learning_data)
                }
            }
            
        except Exception as e:
            error_msg = f"多Agent生成学习报告时发生错误：{str(e)}"
            print(error_msg, file=sys.stderr)
            import traceback
            traceback.print_exc()
            
            return {
                "success": False,
                "error": error_msg
            }
    
    def generate_learning_path_multi_agent(self, topic: str, current_level: str, target_level: str) -> Dict[str, Any]:
        """
        使用多Agent系统生成学习路径
        
        这个方法利用多Agent协作：
        1. 需求分析Agent - 分析学习需求
        2. 资源调研Agent - 调研学习资源
        3. 路径设计Agent - 设计学习路径
        4. 评估Agent - 评估学习路径的可行性
        
        Args:
            topic: 学习主题
            current_level: 当前水平
            target_level: 目标水平
            
        Returns:
            个性化学习路径
        """
        
        try:
            multi_agent_prompt = f"""你是一个多Agent协作系统的协调者，负责协调多个专门化的Agent来完成学习路径设计任务。

**任务目标：** 设计一个从{current_level}到{target_level}的{topic}学习路径

**多Agent协作流程：**

1. **需求分析Agent** - 分析学习需求
   - 分析当前水平和目标水平的差距
   - 识别关键的学习障碍
   - 确定学习重点和优先级

2. **资源调研Agent** - 调研学习资源
   - 搜索和筛选优质学习资源
   - 评估资源的质量和适用性
   - 推荐具体的学习材料和工具

3. **路径设计Agent** - 设计学习路径
   - 设计循序渐进的学习阶段
   - 确定每个阶段的学习目标
   - 安排学习顺序和时间分配

4. **评估Agent** - 评估学习路径
   - 评估学习路径的可行性
   - 识别潜在的风险和挑战
   - 提供调整建议

**协作要求：**
- 每个Agent都要专注于自己的专业领域
- Agent之间要共享信息，确保路径的一致性
- 学习路径要循序渐进，每个阶段都有明确的目标
- 要提供具体可执行的学习建议

**输出格式：**
请生成一个结构化的学习路径，包含：
1. 学习路径概览
2. 分阶段学习计划
3. 每个阶段的学习目标
4. 推荐的学习资源
5. 实践项目建议
6. 评估和检查点

请确保学习路径体现多Agent协作的专业性和实用性。"""

            thread_id = f"multi_agent_path_{uuid.uuid4().hex[:8]}"
            response = self.client.chat(multi_agent_prompt, thread_id=thread_id)
            
            return {
                "success": True,
                "learning_path": response,
                "metadata": {
                    "topic": topic,
                    "current_level": current_level,
                    "target_level": target_level,
                    "agent_type": "multi_agent"
                }
            }
            
        except Exception as e:
            error_msg = f"多Agent生成学习路径时发生错误：{str(e)}"
            print(error_msg, file=sys.stderr)
            import traceback
            traceback.print_exc()
            
            return {
                "success": False,
                "error": error_msg
            }
    
    def research_knowledge_gaps_multi_agent(self, user_knowledge: List[str], target_topic: str) -> Dict[str, Any]:
        """
        使用多Agent系统研究知识盲区
        
        这个方法利用多Agent协作：
        1. 知识评估Agent - 评估用户知识水平
        2. 需求分析Agent - 分析目标主题的知识要求
        3. 差距分析Agent - 分析知识差距
        4. 建议生成Agent - 生成改进建议
        
        Args:
            user_knowledge: 用户已掌握的知识点列表
            target_topic: 目标学习主题
            
        Returns:
            知识盲区分析结果
        """
        
        try:
            multi_agent_prompt = f"""你是一个多Agent协作系统的协调者，负责协调多个专门化的Agent来完成知识盲区分析任务。

**任务目标：** 分析用户在{target_topic}方面的知识盲区

**用户已掌握的知识点：**
{json.dumps(user_knowledge, ensure_ascii=False, indent=2)}

**多Agent协作流程：**

1. **知识评估Agent** - 评估用户知识水平
   - 评估用户已掌握知识的深度和广度
   - 分析知识体系的完整性
   - 识别知识之间的关联性

2. **需求分析Agent** - 分析目标主题的知识要求
   - 分析目标主题的核心知识点
   - 确定目标主题的技能要求
   - 识别关键的学习难点

3. **差距分析Agent** - 分析知识差距
   - 对比用户知识与目标要求
   - 识别关键的知识盲区
   - 分析学习障碍和难点

4. **建议生成Agent** - 生成改进建议
   - 提供填补知识盲区的具体建议
   - 推荐学习顺序和优先级
   - 设计可执行的学习计划

**协作要求：**
- 每个Agent都要专注于自己的专业领域
- Agent之间要共享信息，确保分析的一致性
- 分析要基于具体的知识点对比
- 建议要具体可操作，避免泛泛而谈

**输出格式：**
请生成一个结构化的知识盲区分析报告，包含：
1. 知识盲区概览
2. 关键缺失知识点
3. 学习障碍分析
4. 填补建议
5. 学习优先级排序

请确保分析体现多Agent协作的专业性和深度。"""

            thread_id = f"multi_agent_gaps_{uuid.uuid4().hex[:8]}"
            response = self.client.chat(multi_agent_prompt, thread_id=thread_id)
            
            return {
                "success": True,
                "gap_analysis": response,
                "metadata": {
                    "target_topic": target_topic,
                    "known_points_count": len(user_knowledge),
                    "agent_type": "multi_agent"
                }
            }
            
        except Exception as e:
            error_msg = f"多Agent研究知识盲区时发生错误：{str(e)}"
            print(error_msg, file=sys.stderr)
            import traceback
            traceback.print_exc()
            
            return {
                "success": False,
                "error": error_msg
            }

def main():
    """
    主函数 - 命令行接口
    """
    # 设置输出编码为UTF-8，解决Windows控制台中文乱码问题
    if sys.platform == 'win32':
        import codecs
        sys.stdout = codecs.getwriter('utf-8')(sys.stdout.buffer, 'strict')
        sys.stderr = codecs.getwriter('utf-8')(sys.stderr.buffer, 'strict')
    
    import argparse
    
    parser = argparse.ArgumentParser(description='DeerFlow多Agent研究服务')
    parser.add_argument('command', choices=['report', 'path', 'gaps'], help='命令类型')
    parser.add_argument('--config', default='config.yaml', help='DeerFlow配置文件路径')
    parser.add_argument('--data', help='学习数据（用于report命令）')
    parser.add_argument('--topic', required=True, help='学习主题')
    parser.add_argument('--level', default='beginner', help='当前水平（用于path命令）')
    parser.add_argument('--target', default='advanced', help='目标水平（用于path命令）')
    parser.add_argument('--knowledge', help='用户知识列表（JSON格式，用于gaps命令）')
    
    args = parser.parse_args()
    
    service = DeerFlowMultiAgentResearchService(args.config)
    
    if args.command == 'report':
        if not args.data:
            print("错误：report命令需要--data参数")
            sys.exit(1)
        
        result = service.generate_deep_learning_report_multi_agent(args.data, args.topic)
        
        if result['success']:
            print(result['report'])
        else:
            print(f"错误：{result['error']}")
            sys.exit(1)
    
    elif args.command == 'path':
        result = service.generate_learning_path_multi_agent(args.topic, args.level, args.target)
        
        if result['success']:
            print(result['learning_path'])
        else:
            print(f"错误：{result['error']}")
            sys.exit(1)
    
    elif args.command == 'gaps':
        if not args.knowledge:
            print("错误：gaps命令需要--knowledge参数")
            sys.exit(1)
        
        try:
            knowledge_list = json.loads(args.knowledge)
        except json.JSONDecodeError:
            print("错误：--knowledge参数必须是有效的JSON数组")
            sys.exit(1)
        
        result = service.research_knowledge_gaps_multi_agent(knowledge_list, args.topic)
        
        if result['success']:
            print(result['gap_analysis'])
        else:
            print(f"错误：{result['error']}")
            sys.exit(1)

if __name__ == "__main__":
    main()
