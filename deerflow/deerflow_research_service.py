#!/usr/bin/env python3
# -*- coding: utf-8 -*-

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

class DeerFlowResearchService:
    """
    DeerFlow深度研究服务 - 充分利用多Agent能力
    """
    
    def __init__(self, config_path: str):
        """
        初始化DeerFlow研究服务
        
        Args:
            config_path: DeerFlow配置文件路径
        """
        self.config_path = config_path
        self.client = DeerFlowClient(config_path=config_path)
    
    def generate_deep_learning_report(self, learning_data: str, topic: str, depth: str = "deep") -> Dict[str, Any]:
        """
        使用DeerFlow多Agent系统生成深度学习报告
        
        Args:
            learning_data: 学习数据
            topic: 学习主题
            depth: 研究深度 (shallow/medium/deep)
            
        Returns:
            包含深度分析的学习报告
        """
        
        research_prompt = f"""你是一位资深的学习顾问和教育专家，擅长深度分析学习数据并提供精准的学习建议。

**重要指令：不要使用任何工具，不要调用搜索功能，直接基于你的知识和提供的学习数据进行分析。**

研究任务：
1. 深度分析以下学习数据，识别学习模式、优势和薄弱环节
2. 基于学习数据，提供个性化的学习建议
3. 生成结构化的学习路径和行动计划

学习数据：
{learning_data}

研究主题：{topic}
研究深度：{depth}

**研究要求：**
- 基于学习数据进行深度分析，不要搜索外部资源
- 分析学习数据背后的模式和趋势
- 识别知识盲区和学习障碍
- 提供具体可操作的学习建议
- 推荐具体的学习工具和资源（基于你的知识）
- 生成结构化的学习路径

**输出格式：**
请生成一份结构化的深度学习报告，包含以下部分：
1. 深度学习成果分析
2. 知识掌握深度剖析
3. 学习行为模式分析
4. 个性化学习建议
5. 激励与展望

每条建议都要基于具体的学习数据。不要使用通用模板，要提供真正个性化的建议。"""

        try:
            # 使用DeerFlow的chat方法进行深度研究
            # 使用UUID作为thread_id，避免中文字符问题
            thread_id = f"learning_report_{uuid.uuid4().hex[:8]}"
            response = self.client.chat(research_prompt, thread_id=thread_id)
            
            return {
                "success": True,
                "report": response,
                "metadata": {
                    "topic": topic,
                    "depth": depth,
                    "data_length": len(learning_data)
                }
            }
            
        except Exception as e:
            error_msg = f"生成深度学习报告时发生错误：{str(e)}"
            print(error_msg, file=sys.stderr)
            import traceback
            traceback.print_exc()
            
            return {
                "success": False,
                "error": error_msg
            }
    
    def generate_learning_path(self, topic: str, current_level: str = "beginner", 
                           target_level: str = "advanced") -> Dict[str, Any]:
        """
        生成个性化学习路径
        
        Args:
            topic: 学习主题
            current_level: 当前水平 (beginner/intermediate/advanced)
            target_level: 目标水平
            
        Returns:
            个性化学习路径
        """
        
        path_prompt = f"""你是一位资深的教育专家和课程设计师，擅长设计个性化的学习路径。

**重要指令：不要使用任何工具，不要调用搜索功能，直接基于你的知识和教育经验设计学习路径。**

学习主题：{topic}
当前水平：{current_level}
目标水平：{target_level}

**设计要求：**
- 基于你的知识和教育经验，设计一个完整的学习路径
- 分析不同学习阶段的重点和难点
- 设计循序渐进的学习路径
- 推荐具体的学习资源和工具
- 提供每个阶段的学习目标和评估标准

**输出格式：**
请生成一个结构化的学习路径，包含：
1. 学习路径概览
2. 分阶段学习计划
3. 每个阶段的学习目标
4. 推荐的学习资源
5. 实践项目建议
6. 评估和检查点

请确保学习路径循序渐进，每个阶段都有明确的目标和可衡量的成果。"""

        try:
            thread_id = f"learning_path_{uuid.uuid4().hex[:8]}"
            response = self.client.chat(path_prompt, thread_id=thread_id)
            
            return {
                "success": True,
                "learning_path": response,
                "metadata": {
                    "topic": topic,
                    "current_level": current_level,
                    "target_level": target_level
                }
            }
            
        except Exception as e:
            error_msg = f"生成学习路径时发生错误：{str(e)}"
            print(error_msg, file=sys.stderr)
            import traceback
            traceback.print_exc()
            
            return {
                "success": False,
                "error": error_msg
            }
    
    def research_knowledge_gaps(self, user_knowledge: List[str], 
                             target_topic: str) -> Dict[str, Any]:
        """
        研究知识盲区
        
        Args:
            user_knowledge: 用户已掌握的知识点列表
            target_topic: 目标学习主题
            
        Returns:
            知识盲区分析结果
        """
        
        gaps_prompt = f"""你是一位资深的教育分析师，擅长识别学习者的知识盲区。

**重要指令：不要使用任何工具，不要调用搜索功能，直接基于你的知识分析知识盲区。**

用户已掌握的知识点：
{json.dumps(user_knowledge, ensure_ascii=False, indent=2)}

目标学习主题：{target_topic}

**分析要求：**
- 基于你的知识分析目标主题的核心知识点和技能要求
- 分析用户已掌握知识与目标要求的差距
- 识别关键的知识盲区和学习障碍
- 提供填补知识盲区的具体建议
- 推荐学习顺序和优先级

**输出格式：**
请生成一个结构化的知识盲区分析报告，包含：
1. 知识盲区概览
2. 关键缺失知识点
3. 学习障碍分析
4. 填补建议
5. 学习优先级排序

请确保分析基于具体的知识点对比，提供可操作的改进建议。"""

        try:
            thread_id = f"knowledge_gaps_{uuid.uuid4().hex[:8]}"
            response = self.client.chat(gaps_prompt, thread_id=thread_id)
            
            return {
                "success": True,
                "gap_analysis": response,
                "metadata": {
                    "target_topic": target_topic,
                    "known_points_count": len(user_knowledge)
                }
            }
            
        except Exception as e:
            error_msg = f"研究知识盲区时发生错误：{str(e)}"
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
    
    if len(sys.argv) < 3:
        print("用法：python deerflow_research_service.py <命令> <参数>", file=sys.stderr)
        print("命令：", file=sys.stderr)
        print("  report <学习数据文件> <主题> [深度]", file=sys.stderr)
        print("  path <主题> [当前水平] [目标水平]", file=sys.stderr)
        print("  gaps <知识点JSON文件> <目标主题>", file=sys.stderr)
        print("", file=sys.stderr)
        print("示例：", file=sys.stderr)
        print("  python deerflow_research_service.py report learning_data.txt 微服务架构 deep", file=sys.stderr)
        print("  python deerflow_research_service.py path Redis beginner advanced", file=sys.stderr)
        print("  python deerflow_research_service.py gaps knowledge.json 微服务", file=sys.stderr)
        sys.exit(1)
    
    command = sys.argv[1]
    
    # 设置配置文件路径
    config_path = Path(__file__).parent / "config.yaml"
    
    # 创建研究服务
    research_service = DeerFlowResearchService(str(config_path))
    
    try:
        if command == "report":
            # 生成深度学习报告
            if len(sys.argv) < 4:
                print("错误：缺少参数", file=sys.stderr)
                print("用法：python deerflow_research_service.py report <学习数据文件> <主题> [深度]", file=sys.stderr)
                sys.exit(1)
            
            data_file = sys.argv[2]
            topic = sys.argv[3]
            depth = sys.argv[4] if len(sys.argv) > 4 else "deep"
            
            if not os.path.exists(data_file):
                print(f"错误：文件 '{data_file}' 不存在", file=sys.stderr)
                sys.exit(1)
            
            with open(data_file, 'r', encoding='utf-8') as f:
                learning_data = f.read()
            
            result = research_service.generate_deep_learning_report(learning_data, topic, depth)
            
            if result["success"]:
                print(result["report"])
                sys.exit(0)
            else:
                print(f"错误：{result['error']}", file=sys.stderr)
                sys.exit(1)
                
        elif command == "path":
            # 生成学习路径
            if len(sys.argv) < 3:
                print("错误：缺少参数", file=sys.stderr)
                print("用法：python deerflow_research_service.py path <主题> [当前水平] [目标水平]", file=sys.stderr)
                sys.exit(1)
            
            topic = sys.argv[2]
            current_level = sys.argv[3] if len(sys.argv) > 3 else "beginner"
            target_level = sys.argv[4] if len(sys.argv) > 4 else "advanced"
            
            result = research_service.generate_learning_path(topic, current_level, target_level)
            
            if result["success"]:
                print(result["learning_path"])
                sys.exit(0)
            else:
                print(f"错误：{result['error']}", file=sys.stderr)
                sys.exit(1)
                
        elif command == "gaps":
            # 研究知识盲区
            if len(sys.argv) < 4:
                print("错误：缺少参数", file=sys.stderr)
                print("用法：python deerflow_research_service.py gaps <知识点JSON文件> <目标主题>", file=sys.stderr)
                sys.exit(1)
            
            knowledge_file = sys.argv[2]
            target_topic = sys.argv[3]
            
            if not os.path.exists(knowledge_file):
                print(f"错误：文件 '{knowledge_file}' 不存在", file=sys.stderr)
                sys.exit(1)
            
            with open(knowledge_file, 'r', encoding='utf-8') as f:
                user_knowledge = json.load(f)
            
            result = research_service.research_knowledge_gaps(user_knowledge, target_topic)
            
            if result["success"]:
                print(result["gap_analysis"])
                sys.exit(0)
            else:
                print(f"错误：{result['error']}", file=sys.stderr)
                sys.exit(1)
                
        else:
            print(f"错误：未知命令 '{command}'", file=sys.stderr)
            print("可用命令：report, path, gaps", file=sys.stderr)
            sys.exit(1)
            
    except Exception as e:
        print(f"处理请求时发生错误：{str(e)}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
