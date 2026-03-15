#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
本地学习路径生成服务
不依赖外部API，使用模板和规则生成内容
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import re

app = Flask(__name__)
CORS(app)

def generate_learning_path(topic, current_level, target_level):
    """
    生成本地学习路径
    """
    level_map = {
        'beginner': '初学者',
        'intermediate': '中级',
        'advanced': '高级'
    }
    
    current_level_cn = level_map.get(current_level, '初学者')
    target_level_cn = level_map.get(target_level, '高级')
    
    path = f"""# {topic} 学习路径规划

## 📊 学习概况
- **学习主题**: {topic}
- **当前水平**: {current_level_cn}
- **目标水平**: {target_level_cn}
- **预计学习时间**: 3-6个月

## 🎯 学习目标
- 从{current_level_cn}提升到{target_level_cn}
- 掌握{topic}的核心概念和实践技能
- 能够独立完成{topic}相关项目

## 📚 学习阶段规划

### 第一阶段：基础入门（1-2周）
**学习目标**：
- 了解{topic}的基本概念和应用场景
- 搭建开发环境
- 编写第一个{topic}程序

**学习内容**：
1. {topic}简介和历史
2. 开发环境搭建
3. 基本语法和数据类型
4. 第一个程序：Hello World

**学习资源**：
- 官方文档
- 在线教程
- 视频课程

**评估标准**：
- 能够独立搭建开发环境
- 理解基本语法
- 能编写简单程序

### 第二阶段：核心概念（2-3周）
**学习目标**：
- 掌握{topic}的核心概念
- 理解面向对象编程（如适用）
- 掌握常用数据结构和算法

**学习内容**：
1. 变量和数据类型
2. 控制流程（条件、循环）
3. 函数和模块
4. 面向对象基础
5. 常用数据结构

**学习资源**：
- 官方教程
- 实战项目
- 练习题

**评估标准**：
- 能独立编写中等复杂度程序
- 理解面向对象概念
- 能解决常见编程问题

### 第三阶段：进阶技能（3-4周）
**学习目标**：
- 掌握{topic}的高级特性
- 学习常用框架和库
- 提升代码质量和性能

**学习内容**：
1. 高级语法特性
2. 常用框架介绍
3. 数据库操作
4. API开发
5. 代码优化技巧

**学习资源**：
- 框架官方文档
- 开源项目学习
- 技术博客

**评估标准**：
- 能使用主流框架开发
- 理解数据库操作
- 能进行简单的性能优化

### 第四阶段：实战项目（4-6周）
**学习目标**：
- 通过实际项目巩固所学知识
- 学习项目管理和协作
- 培养解决问题的能力

**学习内容**：
1. 项目需求分析
2. 架构设计
3. 功能实现
4. 测试和调试
5. 部署上线

**项目建议**：
- 个人博客系统
- 在线商城
- 任务管理应用
- 数据分析工具

**评估标准**：
- 能独立完成完整项目
- 掌握版本控制
- 理解部署流程

### 第五阶段：专业提升（2-4周）
**学习目标**：
- 学习{topic}的最佳实践
- 掌握设计模式
- 提升架构能力

**学习内容**：
1. 设计模式
2. 架构原则
3. 性能调优
4. 安全考虑
5. 微服务架构（如适用）

**学习资源**：
- 经典书籍
- 技术大会视频
- 开源项目源码

**评估标准**：
- 能设计合理架构
- 理解常见设计模式
- 能进行性能优化

## 🛠️ 学习方法和技巧

### 高效学习策略
1. **理论与实践结合**：每学一个概念就动手实践
2. **项目驱动学习**：通过实际项目巩固知识
3. **定期复习**：使用间隔重复法记忆重要概念
4. **记录笔记**：建立自己的知识体系

### 推荐学习工具
- **代码编辑器**: VS Code, PyCharm, IntelliJ IDEA
- **版本控制**: Git, GitHub
- **在线学习**: LeetCode, 牛客网
- **文档工具**: Markdown, Notion

### 常见问题解决
1. **遇到错误时**：先阅读错误信息，再搜索解决方案
2. **学习卡顿时**：尝试不同的学习资源或寻求帮助
3. **项目失败时**：分析失败原因，总结经验教训

## 📈 学习进度跟踪

### 每周检查点
- [ ] 完成本周学习目标
- [ ] 完成练习项目
- [ ] 总结学习笔记
- [ ] 制定下周计划

### 阶段性评估
- [ ] 第一阶段：基础入门
- [ ] 第二阶段：核心概念
- [ ] 第三阶段：进阶技能
- [ ] 第四阶段：实战项目
- [ ] 第五阶段：专业提升

## 💡 学习建议

1. **保持耐心**：编程学习需要时间，不要急于求成
2. **持续练习**：每天至少编码1小时
3. **参与社区**：加入技术社区，与他人交流
4. **关注趋势**：了解{topic}的最新发展
5. **享受过程**：编程是创造性的工作，享受解决问题的乐趣

---

**祝你学习顺利！** 🎉
"""
    
    return path

def generate_knowledge_gap_analysis(user_knowledge, target_topic):
    """
    生成本地知识盲区分析
    """
    knowledge_list = "、".join(user_knowledge) if user_knowledge else "暂无"
    
    analysis = f"""# {target_topic} 知识盲区分析

## 📊 当前知识状况

### 已掌握的知识点
{knowledge_list}

### 目标学习主题
{target_topic}

## 🔍 知识盲区识别

### 核心概念盲区
根据你当前掌握的知识点，在{target_topic}的以下核心概念方面可能存在盲区：

1. **基础理论**
   - {target_topic}的基本原理和设计思想
   - 相关的数学基础
   - 理论模型和算法基础

2. **架构设计**
   - 系统架构模式
   - 模块化设计原则
   - 可扩展性和可维护性

3. **性能优化**
   - 性能瓶颈识别
   - 优化策略和技巧
   - 监控和调优工具

4. **安全考虑**
   - 常见安全漏洞
   - 安全编码实践
   - 数据保护措施

### 技能层次盲区

#### 初级技能盲区
- {target_topic}环境配置和部署
- 基本调试技巧
- 常用工具的使用

#### 中级技能盲区
- 复杂问题的分析和解决
- 代码重构和优化
- 单元测试和集成测试

#### 高级技能盲区
- 系统架构设计
- 性能调优和扩展
- 技术选型和决策

## 📚 知识关联分析

### 知识图谱
基于你已掌握的知识点，与{target_topic}的关联关系：

```
已掌握知识点 → 相关概念 → {target_topic}核心内容
```

### 学习路径建议
1. **从已知到未知**：利用已掌握的知识点作为跳板
2. **填补中间环节**：识别知识断层，重点学习
3. **构建知识网络**：建立知识点之间的联系

## 🎯 学习建议

### 优先级排序

#### 高优先级（立即学习）
1. **核心概念**：{target_topic}的基础理论和原理
2. **实践技能**：环境搭建、基本操作
3. **常用工具**：开发工具、调试工具

#### 中优先级（1-2周内）
1. **进阶概念**：架构设计、性能优化
2. **实践项目**：完成1-2个完整项目
3. **问题解决**：学习常见问题的解决方案

#### 低优先级（1个月内）
1. **高级主题**：分布式、微服务（如适用）
2. **最佳实践**：设计模式、编码规范
3. **前沿技术**：了解最新发展趋势

### 学习方法推荐

1. **理论+实践**：每学一个概念就动手实践
2. **项目驱动**：通过实际项目学习新知识
3. **问题导向**：带着问题去学习，效果更好
4. **社区交流**：参与技术社区，获取经验分享

## 📖 推荐学习资源

### 官方资源
- 官方文档和教程
- 官方示例代码
- 社区论坛

### 在线课程
- 视频教程平台
- 在线编程练习
- 技术博客和文章

### 实践项目
- 开源项目参与
- 个人项目开发
- 代码库学习

## 📈 学习进度跟踪

### 短期目标（1-2周）
- [ ] 完成{target_topic}基础概念学习
- [ ] 搭建开发环境
- [ ] 完成第一个练习项目

### 中期目标（1-2个月）
- [ ] 掌握核心技能
- [ ] 完成2-3个实战项目
- [ ] 参与社区讨论

### 长期目标（3-6个月）
- [ ] 能够独立完成复杂项目
- [ ] 理解{target_topic}的高级概念
- [ ] 具备解决实际问题的能力

## 💡 学习建议

1. **循序渐进**：不要急于求成，按计划逐步学习
2. **多练多问**：实践是最好的学习方式
3. **建立体系**：将知识点系统化，形成知识网络
4. **持续更新**：技术发展快速，保持学习状态
5. **享受过程**：学习是成长的过程，享受每一点进步

---

**祝你学习顺利！** 🎉
"""
    
    return analysis

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'healthy',
        'service': 'Local Learning Service'
    })

@app.route('/api/research/learning-path', methods=['POST'])
def generate_learning_path_api():
    """
    生成学习路径
    """
    try:
        data = request.get_json()
        topic = data.get('topic', '')
        current_level = data.get('current_level', 'beginner')
        target_level = data.get('target_level', 'advanced')
        
        path = generate_learning_path(topic, current_level, target_level)
        
        return jsonify({
            'success': True,
            'data': path
        })
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
        
        analysis = generate_knowledge_gap_analysis(user_knowledge, target_topic)
        
        return jsonify({
            'success': True,
            'data': analysis
        })
    except Exception as e:
        print(f"分析知识盲区异常: {e}")
        return jsonify({
            'success': False,
            'message': str(e)
        }), 500

if __name__ == '__main__':
    print("本地学习服务启动在 http://localhost:8001")
    app.run(host='0.0.0.0', port=8001, debug=False)