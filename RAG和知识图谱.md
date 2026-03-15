# 🎓 欢迎来到 AI-SecondBrain 进阶开发课堂！

同学你好！很高兴看到你想深入学习 RAG 和知识图谱的开发。我会像老师一样，**手把手带你理解每个步骤**，不仅告诉你"怎么做"，更告诉你"为什么这么做"。

---

## 📚 课程大纲

```
┌─────────────────────────────────────────────────────────────┐
│  第一部分：RAG 知识问答系统（约 2-3 天）                       │
├─────────────────────────────────────────────────────────────┤
│  第 1 课：理解 RAG 原理                                       │
│  第 2 课：数据库设计                                         │
│  第 3 课：向量存储实现                                       │
│  第 4 课：检索服务实现                                       │
│  第 5 课：LLM 集成与答案生成                                  │
│  第 6 课：前端问答界面                                       │
├─────────────────────────────────────────────────────────────┤
│  第二部分：知识图谱系统（约 2-3 天）                         │
├─────────────────────────────────────────────────────────────┤
│  第 7 课：理解知识图谱                                       │
│  第 8 课：关系数据库设计                                     │
│  第 9 课：关系管理后端                                       │
│  第 10 课：图谱可视化前端                                    │
│  第 11 课：智能关系推荐                                      │
└─────────────────────────────────────────────────────────────┘
```

---

# 📖 第一部分：RAG 知识问答系统

## 第 1 课：理解 RAG 原理（30 分钟）

### 🤔 什么是 RAG？

**RAG = Retrieval-Augmented Generation（检索增强生成）**

让我用个比喻帮你理解：

```
┌─────────────────────────────────────────────────────────────┐
│  传统 LLM 问答 vs RAG 问答                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  传统 LLM（没有 RAG）：                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  学生考试（只能靠记忆）                               │   │
│  │  老师问："你们公司的人事政策是什么？"                 │   │
│  │  学生："呃...我记不清了..." ❌                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  RAG 问答（有知识库）：                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  学生开卷考试（可以查资料）                           │   │
│  │  老师问："你们公司的人事政策是什么？"                 │   │
│  │  学生：                                              │   │
│  │    1. 先查员工手册 📖                                │   │
│  │    2. 找到相关章节                                   │   │
│  │    3. 根据手册内容回答 ✅                             │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 🔄 RAG 工作流程

```
用户提问
   │
   ▼
┌─────────────────┐
│  1. 问题理解     │  把用户问题转换成向量
│     (Embedding) │
└────────┬────────
         │
         ▼
┌─────────────────┐
│  2. 向量检索     │  在知识库中找相似内容
│     (Search)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  3. 知识片段     │  返回最相关的知识
│     (Context)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  4. LLM 生成答案  │  结合知识生成回答
│     (Generate)  │
└────────┬────────┘
         │
         ▼
    返回答案给用户
```

### 💡 核心概念解释

| 概念 | 通俗理解 | 技术实现 |
|------|----------|----------|
| **Embedding** | 把文字变成数字向量 | 使用 embedding 模型 |
| **向量检索** | 找相似的内容 | 计算向量距离（余弦相似度） |
| **Context** | 相关的知识片段 | 从数据库取出的知识 |
| **Prompt** | 给 LLM 的指令 | 包含问题 + 知识的模板 |

---

## 第 2 课：数据库设计（1 小时）

### 📊 需要新增的表

**让我们看看需要加什么表：**

```sql
-- 1. 知识向量表（存储知识的向量表示）
CREATE TABLE knowledge_embedding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_id BIGINT NOT NULL COMMENT '知识节点 ID',
    content TEXT NOT NULL COMMENT '用于向量化的内容',
    embedding VECTOR(1536) COMMENT '向量表示（OpenAI 1536 维）',
    model VARCHAR(50) DEFAULT 'text-embedding-3-small' COMMENT 'embedding 模型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_knowledge_id (knowledge_id),
    FOREIGN KEY (knowledge_id) REFERENCES knowledge_node(id)
);

-- 2. 问答记录表（存储用户的问答历史）
CREATE TABLE qa_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    question TEXT NOT NULL COMMENT '用户问题',
    answer TEXT COMMENT 'AI 回答',
    source_knowledge_ids VARCHAR(500) COMMENT '参考的知识 ID 列表',
    confidence_score DECIMAL(5,4) COMMENT '答案置信度',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
);

-- 3. 问答反馈表（收集用户对答案的反馈）
CREATE TABLE qa_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '问答会话 ID',
    rating TINYINT COMMENT '评分 1-5',
    feedback TEXT COMMENT '文字反馈',
    is_helpful TINYINT COMMENT '是否有用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES qa_session(id)
);
```

### 🗺️ 表关系图

```
┌──────────────────┐       ┌──────────────────┐
│  knowledge_node  │       │   qa_session     │
├──────────────────┤       ├──────────────────┤
│ id               │◄──────│ knowledge_id     │
│ title            │       │ user_id          │
│ content          │       │ question         │
│ user_id          │       │ answer           │
└────────┬─────────┘       └────────┬─────────┘
         │                          │
         │ 1:1                      │ 1:N
         ▼                          ▼
┌──────────────────┐       ┌──────────────────┐
│knowledge_embedding│      │   qa_feedback    │
├──────────────────┤       ├──────────────────┤
│ knowledge_id     │       │ session_id       │
│ embedding        │       │ rating           │
│ content          │       │ feedback         │
└──────────────────┘       └──────────────────┘
```

---

## 第 3 课：向量存储实现（2 小时）

### 🎯 目标

**把知识内容转换成向量，存到数据库**

### 步骤 1：添加 Embedding 服务

**创建 `EmbeddingService.java`：**

```java
package com.secondbrain.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    
    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    /**
     * 将文本转换为向量
     * @param text 输入文本
     * @return 向量数组（float[]）
     */
    public float[] embed(String text) {
        // 调用 Spring AI 的 embedding 模型
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        
        // 获取向量结果
        return response.getResult().getOutput();
    }
    
    /**
     * 批量转换
     */
    public List<float[]> embedAll(List<String> texts) {
        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        return response.getResults().stream()
            .map(r -> r.getOutput())
            .toList();
    }
}
```

### 步骤 2：创建知识向量化服务

**创建 `KnowledgeVectorService.java`：**

```java
package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeEmbedding;
import com.secondbrain.mapper.KnowledgeEmbeddingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeVectorService {
    
    private final EmbeddingService embeddingService;
    private final KnowledgeEmbeddingMapper embeddingMapper;
    
    public KnowledgeVectorService(
        EmbeddingService embeddingService,
        KnowledgeEmbeddingMapper embeddingMapper
    ) {
        this.embeddingService = embeddingService;
        this.embeddingMapper = embeddingMapper;
    }
    
    /**
     * 为知识节点创建向量
     */
    @Transactional
    public void createEmbedding(KnowledgeNode knowledge) {
        // 1. 准备要向量化内容（标题 + 内容）
        String contentForEmbedding = knowledge.getTitle() + "\n" + knowledge.getContent();
        
        // 2. 调用 embedding 服务生成向量
        float[] vector = embeddingService.embed(contentForEmbedding);
        
        // 3. 保存到数据库
        KnowledgeEmbedding embedding = new KnowledgeEmbedding();
        embedding.setKnowledgeId(knowledge.getId());
        embedding.setContent(contentForEmbedding);
        embedding.setEmbedding(vector);
        embedding.setModel("text-embedding-3-small");
        
        embeddingMapper.insert(embedding);
    }
    
    /**
     * 批量向量化（用于初始化历史数据）
     */
    @Transactional
    public void batchEmbedding(List<KnowledgeNode> knowledgeList) {
        for (KnowledgeNode knowledge : knowledgeList) {
            try {
                createEmbedding(knowledge);
                System.out.println("已向量化：" + knowledge.getTitle());
            } catch (Exception e) {
                System.err.println("向量化失败：" + knowledge.getTitle());
            }
        }
    }
}
```

### 步骤 3：创建 Mapper

**创建 `KnowledgeEmbeddingMapper.java`：**

```java
package com.secondbrain.mapper;

import com.secondbrain.entity.KnowledgeEmbedding;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeEmbeddingMapper {
    
    @Insert("INSERT INTO knowledge_embedding (knowledge_id, content, embedding, model) " +
            "VALUES (#{knowledgeId}, #{content}, #{embedding}, #{model})")
    int insert(KnowledgeEmbedding embedding);
    
    @Select("SELECT * FROM knowledge_embedding WHERE knowledge_id = #{knowledgeId}")
    KnowledgeEmbedding getByKnowledgeId(Long knowledgeId);
    
    @Delete("DELETE FROM knowledge_embedding WHERE knowledge_id = #{knowledgeId}")
    int deleteByKnowledgeId(Long knowledgeId);
}
```

### 步骤 4：实体类

**创建 `KnowledgeEmbedding.java`：**

```java
package com.secondbrain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeEmbedding {
    private Long id;
    private Long knowledgeId;
    private String content;
    private float[] embedding;  // 向量数组
    private String model;
    private LocalDateTime createTime;
}
```

---

## 第 4 课：检索服务实现（3 小时）

### 🎯 目标

**用户提问时，从知识库中找到最相关的知识**

### 核心思路

```
用户问题 → 转成向量 → 计算相似度 → 返回最相似的知识
```

### 步骤 1：向量相似度计算

**MySQL 8.0+ 支持向量运算，我们可以用余弦相似度：**

```java
package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.mapper.KnowledgeEmbeddingMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {
    
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;
    
    public VectorSearchService(
        EmbeddingService embeddingService,
        JdbcTemplate jdbcTemplate
    ) {
        this.embeddingService = embeddingService;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * 向量检索 - 找最相似的知识
     * @param question 用户问题
     * @param topK 返回多少条结果
     * @return 相关知识列表
     */
    public List<KnowledgeNode> searchSimilar(String question, int topK) {
        // 1. 把问题转成向量
        float[] questionVector = embeddingService.embed(question);
        
        // 2. 向量转成字符串格式（用于 SQL）
        String vectorStr = vectorToString(questionVector);
        
        // 3. 执行向量相似度查询
        String sql = """
            SELECT kn.id, kn.title, kn.content, kn.user_id,
                   (1 - (ke.embedding <=> CAST(? AS VECTOR(1536)))) AS similarity
            FROM knowledge_node kn
            JOIN knowledge_embedding ke ON kn.id = ke.knowledge_id
            WHERE kn.deleted = 0
            ORDER BY similarity DESC
            LIMIT ?
            """;
        
        // 4. 执行查询（这里简化了，实际需要用 RowMapper）
        return jdbcTemplate.query(sql, new KnowledgeNodeRowMapper(), vectorStr, topK);
    }
    
    /**
     * 向量数组转字符串
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
```

### 步骤 2：创建 RAG 问答服务

**这是核心服务！创建 `RagService.java`：**

```java
package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.QaSession;
import com.secondbrain.mapper.QaSessionMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {
    
    private final VectorSearchService vectorSearchService;
    private final ChatClient chatClient;
    private final QaSessionMapper qaSessionMapper;
    
    // RAG Prompt 模板
    private static final String RAG_PROMPT = """
        你是一个专业的知识库助手。请根据以下参考知识回答用户的问题。
        
        【参考知识】
        {context}
        
        【用户问题】
        {question}
        
        【回答要求】
        1. 基于参考知识回答，不要编造信息
        2. 如果参考知识不足以回答问题，请诚实告知
        3. 回答要清晰、准确、有条理
        4. 必要时可以引用知识的标题
        
        【你的回答】
        """;
    
    public RagService(
        VectorSearchService vectorSearchService,
        ChatClient.Builder chatClientBuilder,
        QaSessionMapper qaSessionMapper
    ) {
        this.vectorSearchService = vectorSearchService;
        this.chatClient = chatClientBuilder.build();
        this.qaSessionMapper = qaSessionMapper;
    }
    
    /**
     * RAG 问答 - 核心方法
     */
    public String ask(String question, Long userId) {
        // 1. 向量检索 - 找相关知识
        List<KnowledgeNode> similarKnowledge = vectorSearchService.searchSimilar(question, 5);
        
        // 2. 构建上下文
        String context = buildContext(similarKnowledge);
        
        // 3. 构建 Prompt
        String prompt = RAG_PROMPT
            .replace("{context}", context)
            .replace("{question}", question);
        
        // 4. 调用 LLM 生成答案
        String answer = chatClient.prompt()
            .user(prompt)
            .call()
            .content();
        
        // 5. 保存问答记录
        saveQaSession(question, answer, similarKnowledge, userId);
        
        return answer;
    }
    
    /**
     * 构建上下文（把多个知识片段拼接起来）
     */
    private String buildContext(List<KnowledgeNode> knowledgeList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < knowledgeList.size(); i++) {
            KnowledgeNode k = knowledgeList.get(i);
            sb.append("【知识").append(i + 1).append("】\n");
            sb.append("标题：").append(k.getTitle()).append("\n");
            sb.append("内容：").append(k.getContent()).append("\n\n");
        }
        return sb.toString();
    }
    
    /**
     * 保存问答记录
     */
    private void saveQaSession(String question, String answer, 
                               List<KnowledgeNode> knowledgeList, Long userId) {
        QaSession session = new QaSession();
        session.setUserId(userId);
        session.setQuestion(question);
        session.setAnswer(answer);
        
        // 记录参考了哪些知识
        String knowledgeIds = knowledgeList.stream()
            .map(k -> k.getId().toString())
            .collect(java.util.stream.Collectors.joining(","));
        session.setSourceKnowledgeIds(knowledgeIds);
        
        qaSessionMapper.insert(session);
    }
}
```

---

## 第 5 课：LLM 集成与答案生成（2 小时）

### 🎯 目标

**配置 Spring AI，连接大模型**

### 步骤 1：添加依赖

**`pom.xml` 中添加：**

```xml
<!-- Spring AI OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M6</version>
</dependency>

<!-- Spring AI Embedding -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

### 步骤 2：配置文件

**`application.yml` 中添加：**

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-3-small
```

### 步骤 3：创建 Controller

**`RagController.java`：**

```java
package com.secondbrain.controller;

import com.secondbrain.service.RagService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    
    private final RagService ragService;
    
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }
    
    @Data
    public static class AskRequest {
        private String question;
    }
    
    @PostMapping("/ask")
    public Map<String, Object> ask(
            @RequestBody AskRequest request,
            @RequestAttribute("userId") Long userId) {
        
        long startTime = System.currentTimeMillis();
        
        // 调用 RAG 服务
        String answer = ragService.ask(request.getQuestion(), userId);
        
        long costTime = System.currentTimeMillis() - startTime;
        
        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("answer", answer);
        result.put("costTime", costTime + "ms");
        
        return result;
    }
    
    @GetMapping("/history")
    public Map<String, Object> getHistory(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        
        // 实现获取问答历史
        // ...
        
        return new HashMap<>();
    }
}
```

---

## 第 6 课：前端问答界面（3 小时）

### 🎯 目标

**创建问答界面，用户可以提问并看到答案**

### 界面设计

```
┌─────────────────────────────────────────────────────────────┐
│  🧠 AI 知识库问答                                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  什么是 JVM 垃圾回收？                         [发送] │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  🤖 AI 助手                                          │   │
│  │  ─────────────────────────────────────────────────  │   │
│  │  JVM 垃圾回收（Garbage Collection）是 Java 虚拟机...  │   │
│  │                                                      │   │
│  │  参考知识：                                          │   │
│  │  📄 JVM 内存管理                                      │   │
│  │  📄 Java 垃圾回收机制                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  👤 我                                               │   │
│  │  ─────────────────────────────────────────────────  │   │
│  │  那有哪些垃圾回收器呢？                              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Vue 组件代码

**创建 `RagChat.vue`：**

```vue
<template>
  <div class="rag-chat-container">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <h2>🧠 AI 知识库问答</h2>
      <p>基于你的知识库智能回答问题</p>
    </div>
    
    <!-- 消息列表 -->
    <div class="message-list" ref="messageList">
      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        :class="['message', msg.role]"
      >
        <div class="message-avatar">
          {{ msg.role === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="message-content">
          <div class="message-text" v-html="formatMessage(msg.content)"></div>
          
          <!-- 参考知识 -->
          <div v-if="msg.sources && msg.sources.length > 0" class="sources">
            <h4>📚 参考知识</h4>
            <div class="source-list">
              <div 
                v-for="source in msg.sources" 
                :key="source.id"
                class="source-item"
                @click="viewKnowledge(source.id)"
              >
                📄 {{ source.title }}
              </div>
            </div>
          </div>
          
          <!-- 反馈按钮 -->
          <div v-if="msg.role === 'assistant'" class="feedback">
            <button @click="giveFeedback(msg.id, true)">👍 有用</button>
            <button @click="giveFeedback(msg.id, false)">👎 无用</button>
          </div>
        </div>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="message assistant">
        <div class="message-avatar">🤖</div>
        <div class="message-content">
          <div class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 输入区域 -->
    <div class="input-area">
      <textarea 
        v-model="question"
        placeholder="输入你的问题，例如：什么是 JVM 垃圾回收？"
        @keydown.enter.exact.prevent="sendQuestion"
        rows="3"
      ></textarea>
      <button 
        @click="sendQuestion" 
        :disabled="!question.trim() || loading"
        class="send-btn"
      >
        发送
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { request } from '@/utils/request'

const question = ref('')
const messages = ref([])
const loading = ref(false)
const messageList = ref(null)

// 发送问题
const sendQuestion = async () => {
  if (!question.value.trim() || loading.value) return
  
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: question.value
  })
  
  const userQuestion = question.value
  question.value = ''
  loading.value = true
  
  // 滚动到底部
  await scrollToBottom()
  
  try {
    // 调用 API
    const response = await request.post('/api/rag/ask', {
      question: userQuestion
    })
    
    // 添加 AI 回复
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      sources: response.sources || [],
      id: response.sessionId
    })
  } catch (error) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，回答失败了，请重试。',
      error: true
    })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (messageList.value) {
    messageList.value.scrollTop = messageList.value.scrollHeight
  }
}

// 格式化消息（支持 Markdown）
const formatMessage = (text) => {
  // 简单实现，可以用 marked.js
  return text.replace(/\n/g, '<br>')
}

// 查看知识详情
const viewKnowledge = (id) => {
  router.push(`/knowledge/${id}`)
}

// 反馈
const giveFeedback = async (sessionId, isHelpful) => {
  await request.post('/api/rag/feedback', {
    sessionId,
    isHelpful
  })
  ElMessage.success('感谢反馈！')
}
</script>

<style scoped>
.rag-chat-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.chat-header {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
}

.message {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 70%;
  padding: 15px;
  border-radius: 12px;
  background: #f5f7fa;
}

.message.user .message-content {
  background: #409EFF;
  color: white;
}

.input-area {
  display: flex;
  gap: 10px;
  padding: 20px 0;
  border-top: 1px solid #eee;
}

.input-area textarea {
  flex: 1;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  resize: none;
  font-family: inherit;
}

.send-btn {
  padding: 15px 30px;
  background: #409EFF;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.sources {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ddd;
}

.source-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.source-item {
  padding: 5px 12px;
  background: #ecf5ff;
  color: #409EFF;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

.source-item:hover {
  background: #409EFF;
  color: white;
}

.feedback {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.feedback button {
  padding: 5px 15px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}
</style>
```

---

# 📖 第二部分：知识图谱系统

## 第 7 课：理解知识图谱（30 分钟）

### 🤔 什么是知识图谱？

**用图的方式表示知识之间的关系**

```
传统知识管理（树状）：
└─ Java
   ├─ 基础语法
   └─ 集合框架

知识图谱（网状）：
     Java
    /  |  \
   /   |   \
Spring  JVM  MySQL
  |     |      |
 IOC  垃圾回收  索引
  |            |
 AOP         事务
```

### 💡 核心概念

| 概念 | 说明 | 例子 |
|------|------|------|
| **节点（Node）** | 知识实体 | "Spring"、"IOC" |
| **关系（Relation）** | 节点间的连接 | "包含"、"依赖"、"相关" |
| **属性（Property）** | 节点的属性 | 名称、描述、创建时间 |

### 常见关系类型

```
┌─────────────────────────────────────────────────────────────┐
│  关系类型                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  包含关系：Java ──包含──> 集合框架                          │
│  依赖关系：Spring ──依赖──> Java                            │
│  相关关系：MySQL ──相关──> 数据库                           │
│  继承关系：ArrayList ──继承──> List                         │
│  实现关系：HashMap ──实现──> Map                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 第 8 课：关系数据库设计（1 小时）

### 📊 表设计

```sql
-- 知识关系表
CREATE TABLE knowledge_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_knowledge_id BIGINT NOT NULL COMMENT '起始知识 ID',
    to_knowledge_id BIGINT NOT NULL COMMENT '目标知识 ID',
    relation_type VARCHAR(50) NOT NULL COMMENT '关系类型：contains/depends/related/inherits/implements',
    relation_name VARCHAR(100) COMMENT '关系名称（自定义）',
    weight DECIMAL(5,4) DEFAULT 1.0000 COMMENT '关系权重',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_user BIGINT COMMENT '创建人',
    deleted TINYINT DEFAULT 0,
    
    INDEX idx_from (from_knowledge_id),
    INDEX idx_to (to_knowledge_id),
    INDEX idx_type (relation_type),
    
    FOREIGN KEY (from_knowledge_id) REFERENCES knowledge_node(id),
    FOREIGN KEY (to_knowledge_id) REFERENCES knowledge_node(id)
);

-- 关系类型字典表
CREATE TABLE relation_type_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '类型代码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    description TEXT COMMENT '描述',
    color VARCHAR(20) DEFAULT '#409EFF' COMMENT '显示颜色',
    icon VARCHAR(50) DEFAULT 'link' COMMENT '图标'
);

-- 初始化关系类型
INSERT INTO relation_type_dict (type_code, type_name, description, color, icon) VALUES
('contains', '包含', '整体与部分的关系', '#409EFF', 'folder'),
('depends', '依赖', '一个知识依赖另一个', '#67C23A', 'link'),
('related', '相关', '两个知识有关联', '#E6A23C', 'connection'),
('inherits', '继承', '面向对象继承关系', '#F56C6C', 'arrow-up'),
('implements', '实现', '接口实现关系', '#909399', 'check');
```

### 🗺️ 表关系图

```
┌──────────────────┐       ┌──────────────────┐
│  knowledge_node  │       │ knowledge_relation│
├──────────────────┤       ├──────────────────┤
│ id               │◄──────│ from_knowledge_id│
│ title            │       │ to_knowledge_id  │
│ content          │       │ relation_type    │
└────────┬─────────       │ weight           │
         │                └────────┬─────────┘
         │ 1:N                     │
         │                         │ N:1
         │                ┌────────▼─────────┐
         └───────────────►│  knowledge_node  │
                          │  (目标知识)       │
                          └──────────────────┘
```

---

## 第 9 课：关系管理后端（3 小时）

### 步骤 1：实体类

```java
package com.secondbrain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeRelation {
    private Long id;
    private Long fromKnowledgeId;
    private Long toKnowledgeId;
    private String relationType;  // contains/depends/related/inherits/implements
    private String relationName;
    private Double weight;
    private LocalDateTime createTime;
    private Long createUser;
    private Integer deleted;
    
    // 关联对象（用于返回）
    private KnowledgeNode fromKnowledge;
    private KnowledgeNode toKnowledge;
}
```

### 步骤 2：Mapper

```java
package com.secondbrain.mapper;

import com.secondbrain.entity.KnowledgeRelation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeRelationMapper {
    
    @Insert("INSERT INTO knowledge_relation " +
            "(from_knowledge_id, to_knowledge_id, relation_type, relation_name, weight, create_user) " +
            "VALUES (#{fromKnowledgeId}, #{toKnowledgeId}, #{relationType}, #{relationName}, #{weight}, #{createUser})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeRelation relation);
    
    @Select("SELECT * FROM knowledge_relation WHERE from_knowledge_id = #{knowledgeId} AND deleted = 0")
    List<KnowledgeRelation> getFromRelations(Long knowledgeId);
    
    @Select("SELECT * FROM knowledge_relation WHERE to_knowledge_id = #{knowledgeId} AND deleted = 0")
    List<KnowledgeRelation> getToRelations(Long knowledgeId);
    
    @Select("""
        SELECT kr.*, 
               kn1.title as from_title, kn1.content as from_content,
               kn2.title as to_title, kn2.content as to_content
        FROM knowledge_relation kr
        JOIN knowledge_node kn1 ON kr.from_knowledge_id = kn1.id
        JOIN knowledge_node kn2 ON kr.to_knowledge_id = kn2.id
        WHERE kr.from_knowledge_id = #{knowledgeId} OR kr.to_knowledge_id = #{knowledgeId}
        AND kr.deleted = 0
        """)
    List<KnowledgeRelation> getAllRelations(Long knowledgeId);
    
    @Delete("UPDATE knowledge_relation SET deleted = 1 WHERE id = #{id}")
    int delete(Long id);
}
```

### 步骤 3：Service

```java
package com.secondbrain.service;

import com.secondbrain.entity.KnowledgeRelation;
import com.secondbrain.mapper.KnowledgeRelationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeRelationService {
    
    private final KnowledgeRelationMapper relationMapper;
    
    public KnowledgeRelationService(KnowledgeRelationMapper relationMapper) {
        this.relationMapper = relationMapper;
    }
    
    /**
     * 创建知识关系
     */
    @Transactional
    public KnowledgeRelation createRelation(Long fromId, Long toId, 
                                            String relationType, String relationName) {
        // 1. 检查是否已存在
        List<KnowledgeRelation> existing = relationMapper.getFromRelations(fromId);
        for (KnowledgeRelation r : existing) {
            if (r.getToKnowledgeId().equals(toId) && 
                r.getRelationType().equals(relationType)) {
                throw new RuntimeException("关系已存在");
            }
        }
        
        // 2. 创建关系
        KnowledgeRelation relation = new KnowledgeRelation();
        relation.setFromKnowledgeId(fromId);
        relation.setToKnowledgeId(toId);
        relation.setRelationType(relationType);
        relation.setRelationName(relationName);
        relation.setWeight(1.0);
        
        relationMapper.insert(relation);
        
        return relation;
    }
    
    /**
     * 获取知识的图谱数据（用于前端可视化）
     */
    public Map<String, Object> getGraphData(Long knowledgeId) {
        List<KnowledgeRelation> relations = relationMapper.getAllRelations(knowledgeId);
        
        // 构建图谱数据（符合 ECharts 格式）
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> links = new ArrayList<>();
        
        // 添加中心节点
        Map<String, Object> centerNode = new HashMap<>();
        centerNode.put("id", knowledgeId);
        centerNode.put("name", "中心知识");
        centerNode.put("symbolSize", 50);
        centerNode.put("category", 0);
        nodes.add(centerNode);
        
        // 添加关联节点和边
        for (KnowledgeRelation r : relations) {
            // 添加边
            Map<String, Object> link = new HashMap<>();
            link.put("source", r.getFromKnowledgeId());
            link.put("target", r.getToKnowledgeId());
            link.put("label", r.getRelationName() != null ? r.getRelationName() : r.getRelationType());
            links.add(link);
            
            // 添加节点（去重）
            // ... 实现节点添加逻辑
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        
        return result;
    }
}
```

### 步骤 4：Controller

```java
package com.secondbrain.controller;

import com.secondbrain.service.KnowledgeRelationService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge/relation")
public class KnowledgeRelationController {
    
    private final KnowledgeRelationService relationService;
    
    public KnowledgeRelationController(KnowledgeRelationService relationService) {
        this.relationService = relationService;
    }
    
    @PostMapping
    public Map<String, Object> createRelation(
            @RequestParam Long fromId,
            @RequestParam Long toId,
            @RequestParam String relationType,
            @RequestParam(required = false) String relationName) {
        
        relationService.createRelation(fromId, toId, relationType, relationName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "关系创建成功");
        
        return result;
    }
    
    @GetMapping("/graph")
    public Map<String, Object> getGraphData(@RequestParam Long knowledgeId) {
        return relationService.getGraphData(knowledgeId);
    }
    
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteRelation(@PathVariable Long id) {
        relationService.deleteRelation(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "关系删除成功");
        
        return result;
    }
}
```

---

## 第 10 课：图谱可视化前端（4 小时）

### 🎯 目标

**用 ECharts 展示知识关系图**

### Vue 组件代码

**创建 `KnowledgeGraph.vue`：**

```vue
<template>
  <div class="knowledge-graph-container">
    <div class="graph-header">
      <h2>🕸️ 知识图谱</h2>
      <div class="graph-controls">
        <button @click="refreshGraph">🔄 刷新</button>
        <button @click="exportImage">📷 导出图片</button>
      </div>
    </div>
    
    <!-- 图谱容器 -->
    <div ref="graphContainer" class="graph-container"></div>
    
    <!-- 节点详情面板 -->
    <div v-if="selectedNode" class="node-detail">
      <h3>{{ selectedNode.name }}</h3>
      <p>{{ selectedNode.content }}</p>
      <button @click="viewKnowledge(selectedNode.id)">查看详情</button>
      <button @click="selectedNode = null">关闭</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { request } from '@/utils/request'

const graphContainer = ref(null)
const selectedNode = ref(null)
let chartInstance = null

// 初始化图谱
const initGraph = async () => {
  await nextTick()
  
  // 创建 ECharts 实例
  chartInstance = echarts.init(graphContainer.value)
  
  // 加载数据
  await loadGraphData()
  
  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    chartInstance.resize()
  })
}

// 加载图谱数据
const loadGraphData = async () => {
  try {
    const response = await request.get('/api/knowledge/relation/graph', {
      params: { knowledgeId: route.params.id }
    })
    
    // 配置图谱选项
    const option = {
      tooltip: {
        trigger: 'item',
        formatter: '{b}'
      },
      series: [
        {
          type: 'graph',
          layout: 'force',
          data: response.nodes,
          links: response.links,
          categories: [
            { name: '中心知识' },
            { name: '关联知识' }
          ],
          roam: true,
          label: {
            show: true,
            position: 'right',
            formatter: '{b}'
          },
          force: {
            repulsion: 300,
            edgeLength: 100
          },
          lineStyle: {
            color: 'source',
            curveness: 0.3
          },
          emphasis: {
            focus: 'adjacency',
            lineStyle: {
              width: 4
            }
          }
        }
      ]
    }
    
    chartInstance.setOption(option)
    
    // 监听点击事件
    chartInstance.on('click', (params) => {
      if (params.dataType === 'node') {
        selectedNode.value = params.data
      }
    })
  } catch (error) {
    ElMessage.error('加载图谱失败')
  }
}

// 刷新图谱
const refreshGraph = () => {
  loadGraphData()
}

// 导出图片
const exportImage = () => {
  const url = chartInstance.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#fff'
  })
  
  const link = document.createElement('a')
  link.download = 'knowledge-graph.png'
  link.href = url
  link.click()
}

// 查看知识详情
const viewKnowledge = (id) => {
  router.push(`/knowledge/${id}`)
}

onMounted(() => {
  initGraph()
})
</script>

<style scoped>
.knowledge-graph-container {
  padding: 20px;
  height: calc(100vh - 120px);
}

.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.graph-controls button {
  margin-left: 10px;
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.graph-container {
  width: 100%;
  height: calc(100% - 80px);
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
}

.node-detail {
  position: absolute;
  right: 20px;
  top: 100px;
  width: 300px;
  padding: 20px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}
</style>
```

---

## 第 11 课：智能关系推荐（2 小时）

### 🎯 目标

**AI 自动推荐可能的知识关系**

### 实现思路

```
1. 分析知识内容（标题 + 描述）
2. 计算知识间的相似度
3. 推荐可能的关系类型
4. 用户确认后创建关系
```

### 代码实现

```java
@Service
public class RelationRecommendService {
    
    private final EmbeddingService embeddingService;
    private final KnowledgeNodeMapper knowledgeMapper;
    
    /**
     * 为知识推荐可能的关系
     */
    public List<RelationRecommendation> recommendRelations(Long knowledgeId) {
        // 1. 获取当前知识
        KnowledgeNode current = knowledgeMapper.selectById(knowledgeId);
        
        // 2. 获取当前知识的向量
        float[] currentVector = embeddingService.embed(
            current.getTitle() + " " + current.getContent()
        );
        
        // 3. 获取所有其他知识
        List<KnowledgeNode> allKnowledge = knowledgeMapper.selectAll();
        
        // 4. 计算相似度
        List<RelationRecommendation> recommendations = new ArrayList<>();
        for (KnowledgeNode k : allKnowledge) {
            if (k.getId().equals(knowledgeId)) continue;
            
            float[] otherVector = embeddingService.embed(
                k.getTitle() + " " + k.getContent()
            );
            
            double similarity = cosineSimilarity(currentVector, otherVector);
            
            if (similarity > 0.6) {  // 相似度阈值
                RelationRecommendation rec = new RelationRecommendation();
                rec.setTargetKnowledge(k);
                rec.setSimilarity(similarity);
                rec.setRecommendedType(guessRelationType(current, k));
                recommendations.add(rec);
            }
        }
        
        // 5. 按相似度排序
        recommendations.sort((a, b) -> 
            Double.compare(b.getSimilarity(), a.getSimilarity())
        );
        
        return recommendations.subList(0, Math.min(10, recommendations.size()));
    }
    
    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += Math.pow(a[i], 2);
            normB += Math.pow(b[i], 2);
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * 猜测关系类型
     */
    private String guessRelationType(KnowledgeNode a, KnowledgeNode b) {
        String titleA = a.getTitle().toLowerCase();
        String titleB = b.getTitle().toLowerCase();
        
        if (titleA.contains("包含") || titleA.contains("框架")) {
            return "contains";
        }
        if (titleB.contains("依赖") || titleB.contains("需要")) {
            return "depends";
        }
        return "related";
    }
}
```

---

# 🎓 学习总结

## 📊 开发路线图

```
┌─────────────────────────────────────────────────────────────┐
│  开发阶段                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  第 1 周：RAG 基础                                            │
│  ├── 第 1-2 天：数据库设计 + 向量存储                         │
│  ├── 第 3-4 天：检索服务 + RAG 核心                           │
│  └── 第 5 天：前端问答界面                                    │
│                                                             │
│  第 2 周：知识图谱                                            │
│  ├── 第 1-2 天：关系数据库 + 后端服务                         │
│  ├── 第 3-4 天：图谱可视化                                    │
│  └── 第 5 天：智能推荐 + 测试优化                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 💡 关键知识点

| 知识点 | 重要性 | 学习资源 |
|--------|--------|----------|
| **向量 Embedding** | ⭐⭐⭐⭐⭐ | Spring AI 官方文档 |
| **向量相似度计算** | ⭐⭐⭐⭐ | 余弦相似度算法 |
| **RAG Prompt 设计** | ⭐⭐⭐⭐⭐ | LangChain 文档 |
| **ECharts 图谱** | ⭐⭐⭐⭐ | ECharts 官方示例 |
| **图数据库概念** | ⭐⭐⭐ | Neo4j 文档 |

---

## 🎯 下一步行动

**同学，现在你可以：**

1. **先实现 RAG 基础功能**（第 1-6 课）
2. **测试问答效果**
3. **再实现知识图谱**（第 7-11 课）
4. **逐步优化体验**

**有任何问题随时问我！我会继续耐心解答！** 📚

想从哪一课开始深入？我可以提供更详细的代码和解释！