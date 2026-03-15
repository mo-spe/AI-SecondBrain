#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
高质量学习路径生成服务
基于主题和水平生成个性化、详细的学习路径
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import re
from datetime import datetime

app = Flask(__name__)
CORS(app)

def get_level_description(level, topic):
    """获取水平描述"""
    level_descriptions = {
        'beginner': {
            'title': '零基础入门',
            'description': f'从零开始学习{topic}，适合完全没有编程经验或对{topic}不熟悉的学员',
            'prerequisites': '无需任何前置知识',
            'expected_outcome': f'能够编写简单的{topic}程序，理解基本概念'
        },
        'intermediate': {
            'title': '进阶提升',
            'description': f'在掌握{topic}基础后，深入学习核心概念和高级特性',
            'prerequisites': f'熟悉{topic}基本语法和编程概念',
            'expected_outcome': f'能够独立开发{topic}项目，理解高级特性'
        },
        'advanced': {
            'title': '高级精通',
            'description': f'深入掌握{topic}的高级特性、性能优化和架构设计',
            'prerequisites': f'熟练掌握{topic}，有实际项目经验',
            'expected_outcome': f'能够进行{topic}架构设计、性能调优和技术选型'
        }
    }
    return level_descriptions.get(level, level_descriptions['beginner'])

def get_topic_specific_content(topic):
    """获取主题特定的内容"""
    topic_content = {
        'python': {
            'core_concepts': ['数据类型和变量', '控制流程', '函数和模块', '面向对象编程', '异常处理', '文件操作'],
            'advanced_topics': ['装饰器', '生成器', '上下文管理器', '元编程', '并发编程', '性能优化'],
            'popular_frameworks': ['Django', 'Flask', 'FastAPI', 'PyTorch', 'Pandas'],
            'best_practices': ['PEP 8编码规范', '类型注解', '单元测试', '文档编写', '虚拟环境管理']
        },
        'java': {
            'core_concepts': ['Java语法基础', '面向对象编程', '集合框架', '异常处理', '多线程', 'IO流'],
            'advanced_topics': ['JVM原理', '并发编程', '设计模式', '性能调优', '微服务架构', 'Spring框架'],
            'popular_frameworks': ['Spring Boot', 'Spring Cloud', 'MyBatis', 'Hibernate', 'Netty'],
            'best_practices': ['代码规范', '异常处理', '内存管理', '并发安全', '设计模式应用']
        },
        'javascript': {
            'core_concepts': ['ES6+语法', 'DOM操作', '事件处理', '异步编程', 'Promise/Async', '模块化'],
            'advanced_topics': ['闭包和原型链', '性能优化', 'Web Workers', 'Service Workers', 'PWA开发', 'TypeScript'],
            'popular_frameworks': ['React', 'Vue.js', 'Angular', 'Node.js', 'Express'],
            'best_practices': ['模块化开发', '代码规范', '性能优化', '安全编码', '测试驱动开发']
        }
    }
    
    topic_lower = topic.lower()
    for key in topic_content:
        if key in topic_lower:
            return topic_content[key]
    
    return topic_content['python']

def generate_learning_path(topic, current_level, target_level):
    """生成高质量学习路径"""
    level_map = {
        'beginner': 'beginner',
        'intermediate': 'intermediate', 
        'advanced': 'advanced'
    }
    
    current_level_key = level_map.get(current_level, 'beginner')
    target_level_key = level_map.get(target_level, 'advanced')
    
    current_info = get_level_description(current_level_key, topic)
    target_info = get_level_description(target_level_key, topic)
    topic_content = get_topic_specific_content(topic)
    
    path = f"""# 🐍 {topic} 系统化学习路径

> **个性化定制**：基于您的当前水平（{current_info['title']}）和目标水平（{target_info['title']}）精心设计
> **设计理念**：循序渐进、理论与实践深度结合、项目驱动、持续反馈、可量化评估
> **总周期建议**：根据学习强度，建议4-8个月完成整个学习路径

---

## 📊 学习现状分析

### 当前水平评估
**水平等级**: {current_info['title']}
**详细描述**: {current_info['description']}
**前置要求**: {current_info['prerequisites']}
**预期成果**: {current_info['expected_outcome']}

### 目标水平设定
**目标等级**: {target_info['title']}
**详细描述**: {target_info['description']}
**前置要求**: {target_info['prerequisites']}
**预期成果**: {target_info['expected_outcome']}

### 学习差距分析
从{current_info['title']}到{target_info['title']}，您需要重点提升以下方面：
1. **理论知识深度**：从基础概念深入到原理和底层机制
2. **实践能力**：从简单练习到复杂项目开发
3. **问题解决**：从依赖帮助到独立调试和优化
4. **架构思维**：从功能实现到系统设计

---

## 🎯 核心学习目标

### 技术能力目标
- 掌握{topic}的核心概念和编程范式
- 理解{topic}的设计思想和最佳实践
- 能够独立完成{topic}相关项目开发
- 具备代码优化和问题调试能力

### 项目经验目标
- 完成3-5个完整的{topic}项目
- 参与至少1个开源项目
- 建立个人{topic}项目作品集

### 职业发展目标
- 能够胜任{topic}开发工程师岗位
- 具备技术选型和架构设计能力
- 形成持续学习和技术成长的习惯

---

## 📚 分阶段学习规划

### 第一阶段：基础夯实（2-3周）

#### 🎯 阶段目标
- 建立{topic}的完整知识框架
- 掌握核心语法和编程概念
- 能够独立编写基础程序

#### 📖 学习内容

**1.1 环境搭建与工具配置**
- 开发环境安装和配置
- IDE选择和配置（推荐：PyCharm/VS Code/IntelliJ IDEA）
- 版本控制工具（Git）
- 包管理器使用

**1.2 核心语法与数据结构**
"""

    for i, concept in enumerate(topic_content['core_concepts'][:3], 1):
        path += f"- {concept}\n"
    
    path += f"""
**1.3 编程基础实践**
- 变量、运算符和表达式
- 条件语句和循环结构
- 函数定义和调用
- 基本算法实现

#### 🛠️ 实践项目
- **项目1**: Hello World程序
- **项目2**: 简单计算器
- **项目3**: 基础数据管理工具

#### 📊 学习评估
- [ ] 能独立搭建开发环境
- [ ] 理解基本语法和概念
- [ ] 能编写简单程序
- [ ] 通过基础练习测试

#### 📚 学习资源
- 官方文档和教程
- 在线编程平台（LeetCode、牛客网）
- 视频课程（慕课网、极客时间）
- 技术博客和社区

---

### 第二阶段：核心深化（3-4周）

#### 🎯 阶段目标
- 深入理解{topic}核心概念
- 掌握面向对象编程思想
- 能够开发中等复杂度应用

#### 📖 学习内容

**2.1 面向对象编程**
- 类和对象的概念
- 封装、继承、多态
- 抽象类和接口
- 设计模式基础

**2.2 数据结构与算法**
"""

    for i, concept in enumerate(topic_content['core_concepts'][3:6], 1):
        path += f"- {concept}\n"
    
    path += f"""
- 常用算法（排序、查找）
- 时间复杂度和空间复杂度
- 算法优化技巧

**2.3 异常处理与调试**
- 异常类型和处理机制
- try-catch-finally结构
- 自定义异常
- 调试技巧和工具

#### 🛠️ 实践项目
- **项目4**: 学生成绩管理系统
- **项目5**: 简易博客系统
- **项目6**: 数据可视化工具

#### 📊 学习评估
- [ ] 理解面向对象编程概念
- [ ] 掌握常用数据结构
- [ ] 能处理程序异常
- [ ] 完成中等复杂度项目

#### 📚 学习资源
- 《设计模式：可复用面向对象软件的基础》
- 《算法导论》
- GitHub开源项目学习
- 技术社区讨论

---

### 第三阶段：进阶提升（4-6周）

#### 🎯 阶段目标
- 掌握{topic}高级特性
- 学习主流框架和库
- 提升代码质量和性能

#### 📖 学习内容

**3.1 高级语言特性**
"""

    for i, topic in enumerate(topic_content['advanced_topics'][:4], 1):
        path += f"- {topic}\n"
    
    path += f"""
**3.2 主流框架学习**
"""

    for i, framework in enumerate(topic_content['popular_frameworks'][:3], 1):
        path += f"- {framework}框架基础\n"
    
    path += f"""
**3.3 数据库操作**
- 关系型数据库（MySQL、PostgreSQL）
- NoSQL数据库（MongoDB、Redis）
- ORM框架使用
- 数据库设计和优化

**3.4 API开发**
- RESTful API设计
- 接口文档编写
- 身份认证和授权
- 接口测试

#### 🛠️ 实践项目
- **项目7**: 在线商城后端
- **项目8**: 任务管理系统
- **项目9**: 数据分析平台

#### 📊 学习评估
- [ ] 掌握高级语言特性
- [ ] 能使用主流框架开发
- [ ] 理解数据库操作
- [ ] 能设计和实现API

#### 📚 学习资源
- 框架官方文档
- 《高性能{topic}》
- 技术大会视频
- 开源项目源码分析

---

### 第四阶段：实战项目（6-8周）

#### 🎯 阶段目标
- 通过实际项目巩固所学知识
- 学习项目管理和协作
- 培养解决实际问题的能力

#### 📖 学习内容

**4.1 项目需求分析**
- 需求收集和分析
- 功能模块划分
- 技术选型
- 架构设计

**4.2 项目开发流程**
- 敏捷开发方法
- 版本控制策略
- 代码审查
- 持续集成

**4.3 测试和部署**
- 单元测试
- 集成测试
- 性能测试
- 部署上线

#### 🛠️ 实践项目
- **项目10**: 个人博客系统（完整版）
- **项目11**: 在线教育平台
- **项目12**: 电商系统

#### 📊 学习评估
- [ ] 能独立完成完整项目
- [ ] 掌握版本控制
- [ ] 理解部署流程
- [ ] 具备团队协作能力

#### 📚 学习资源
- 《代码整洁之道》
- 《重构：改善既有代码的设计》
- 项目实战课程
- 技术社区和论坛

---

### 第五阶段：专业精通（4-6周）

#### 🎯 阶段目标
- 学习{topic}最佳实践
- 掌握设计模式和架构原则
- 提升系统设计和性能优化能力

#### 📖 学习内容

**5.1 设计模式与架构**
"""

    for i, practice in enumerate(topic_content['best_practices'][:3], 1):
        path += f"- {practice}\n"
    
    path += f"""
- 系统架构模式
- 微服务架构
- 分布式系统设计

**5.2 性能优化**
- 代码性能分析
- 内存管理
- 并发优化
- 数据库优化

**5.3 安全与可靠性**
- 常见安全漏洞
- 安全编码实践
- 数据保护措施
- 系统监控和日志

#### 🛠️ 实践项目
- **项目13**: 分布式任务调度系统
- **项目14**: 高并发API服务
- **项目15**: 大数据处理平台

#### 📊 学习评估
- [ ] 能设计合理系统架构
- [ ] 理解常见设计模式
- [ ] 能进行性能优化
- [ ] 具备安全意识

#### 📚 学习资源
- 《系统设计面试》
- 《高性能MySQL》
- 技术论文和专利
- 行业技术博客

---

## 🛠️ 学习方法和工具

### 高效学习策略

#### 1. 费曼学习法
- 用简单语言解释复杂概念
- 通过教学加深理解
- 发现知识盲点并补充

#### 2. 项目驱动学习
- 每个阶段完成实际项目
- 在项目中学习新知识
- 通过实践巩固理论

#### 3. 间隔重复法
- 使用Anki等工具记忆知识点
- 定期复习重要概念
- 建立长期记忆

#### 4. 代码阅读法
- 阅读优秀开源项目代码
- 学习最佳实践
- 提升代码质量

### 推荐学习工具

#### 开发工具
- **IDE**: PyCharm Professional / IntelliJ IDEA Ultimate / VS Code
- **版本控制**: Git + GitHub / GitLab
- **数据库工具**: Navicat / DBeaver
- **API测试**: Postman / Apifox

#### 学习平台
- **在线编程**: LeetCode / 牛客网 / HackerRank
- **技术文档**: 官方文档 / MDN / Stack Overflow
- **视频学习**: 慕课网 / 极客时间 / B站技术区
- **社区交流**: GitHub / 掘金 / 知乎技术专栏

#### 效率工具
- **笔记工具**: Notion / Obsidian / Typora
- **时间管理**: Todoist / Microsoft To Do
- **代码片段**: Snipaste / Gist
- **思维导图**: XMind / MindManager

### 常见问题解决

#### 学习困难时
1. **分解问题**：将复杂问题拆解为小问题
2. **寻求帮助**：在社区提问或请教导师
3. **换个角度**：尝试不同的学习资源和方法
4. **休息调整**：适当休息，避免过度疲劳

#### 项目卡顿时
1. **回顾基础**：检查是否遗漏了基础知识
2. **参考示例**：查看类似项目的实现
3. **逐步调试**：使用调试工具定位问题
4. **记录经验**：将解决方案记录下来

#### 面试准备时
1. **算法练习**：每天刷1-2道算法题
2. **项目总结**：准备项目介绍和亮点
3. **系统设计**：学习常见系统设计问题
4. **模拟面试**：和朋友进行模拟面试

---

## 📈 学习进度跟踪

### 每周检查点
- [ ] 完成本周学习目标
- [ ] 完成练习项目
- [ ] 总结学习笔记
- [ ] 制定下周计划
- [ ] 复习重要概念

### 阶段性评估
- [ ] 第一阶段：基础夯实
- [ ] 第二阶段：核心深化
- [ ] 第三阶段：进阶提升
- [ ] 第四阶段：实战项目
- [ ] 第五阶段：专业精通

### 技能自评表
| 技能维度 | 初级 | 中级 | 高级 | 专家 |
|---------|------|------|------|------|
| 编程能力 | ⬜ | ⬜ | ⬜ | ⬜ |
| 理论理解 | ⬜ | ⬜ | ⬜ | ⬜ |
| 项目经验 | ⬜ | ⬜ | ⬜ | ⬜ |
| 问题解决 | ⬜ | ⬜ | ⬜ | ⬜ |
| 架构设计 | ⬜ | ⬜ | ⬜ | ⬜ |

---

## 💡 个性化学习建议

### 基于当前水平
由于您当前处于{current_info['title']}水平，建议：
1. **重视基础**：不要急于求成，打好基础很重要
2. **多练习**：编程是实践性技能，多写代码
3. **建立习惯**：每天至少学习1-2小时
4. **记录笔记**：建立自己的知识体系

### 基于目标水平
为了达到{target_info['title']}水平，需要：
1. **深入学习**：理解原理和底层机制
2. **项目实践**：通过项目积累经验
3. **持续学习**：关注技术发展趋势
4. **分享交流**：参与技术社区，分享经验

### 学习时间规划
- **工作日**: 每天1-2小时（晚上或早晨）
- **周末**: 每天3-4小时（集中学习）
- **节假日**: 每天4-6小时（深度学习）
- **总计**: 每周约15-20小时

---

## 🎯 学习成功标准

### 技术能力
- [ ] 能独立完成{topic}项目开发
- [ ] 理解{topic}的核心原理和设计思想
- [ ] 掌握主流框架和工具的使用
- [ ] 具备代码优化和问题调试能力

### 项目经验
- [ ] 完成5个以上{topic}项目
- [ ] 项目代码质量达到生产环境标准
- [ ] 有项目部署和运维经验
- [ ] 参与过开源项目贡献

### 职业发展
- [ ] 能胜任{topic}开发工程师岗位
- [ ] 具备技术选型和架构设计能力
- [ ] 有良好的编程习惯和代码规范
- [ ] 形成持续学习和技术成长的体系

---

## 🌟 总结与鼓励

### 学习成果预期
按照这个学习路径，您将获得：
- **扎实的技术基础**：从零基础到精通的完整知识体系
- **丰富的项目经验**：15个实战项目，涵盖各个难度级别
- **解决问题的能力**：从简单问题到复杂系统设计
- **职业发展机会**：具备胜任相关岗位的能力

### 学习心态建议
1. **保持耐心**：编程学习需要时间，不要急于求成
2. **持续练习**：每天编码，保持手感
3. **不怕犯错**：错误是学习的机会，从错误中学习
4. **享受过程**：编程是创造性的工作，享受解决问题的乐趣

### 最后的话
> 学习编程就像建造一座大厦，需要一砖一瓦地积累。每一个概念、每一个项目、每一次练习，都是在为你的技术大厦添砖加瓦。保持学习的热情，坚持下去，你一定能成为{topic}领域的专家！

**祝你学习顺利，前程似锦！** 🚀

---

*本学习路径生成时间：{datetime.now().strftime('%Y年%m月%d日 %H:%M:%S')}*
*个性化定制：基于当前水平{current_info['title']} → 目标水平{target_info['title']}*
"""
    
    return path

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'healthy',
        'service': 'Enhanced Learning Service'
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

if __name__ == '__main__':
    print("增强版学习服务启动在 http://localhost:8001")
    app.run(host='0.0.0.0', port=8001, debug=False)