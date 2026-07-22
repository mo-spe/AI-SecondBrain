<template>
  <div class="rag-qa-container" aria-label="RAG问答页面">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><ChatDotRound /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">RAG知识问答</h1>
            <p class="welcome-subtitle">基于您的知识库进行智能问答</p>
          </div>
        </div>
      </div>

      <div class="content-wrapper">
        <div class="left-panel">
          <div class="question-card">
            <div class="card-header">
              <h3>
                <el-icon><Edit /></el-icon>
                提问
              </h3>
            </div>
            <div class="card-body">
              <el-input
                v-model="question"
                type="textarea"
                :rows="6"
                placeholder="请输入您的问题..."
                @keyup.ctrl.enter="handleAsk"
              />
              <div class="button-group">
                <el-button
                  type="primary"
                  @click="handleAsk"
                  :loading="loading"
                  size="large"
                >
                  <el-icon><Promotion /></el-icon>
                  提问 (Ctrl+Enter)
                </el-button>
                <el-button
                  @click="handleGenerateVectors"
                  :loading="generatingVectors"
                  size="large"
                >
                  <el-icon><MagicStick /></el-icon>
                  生成向量
                </el-button>
                <el-button @click="handleClear" size="large">
                  <el-icon><Delete /></el-icon>
                  清空
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="answer" class="answer-card">
            <div class="card-header">
              <h3>
                <el-icon><ChatLineRound /></el-icon>
                AI回答
              </h3>
              <div class="time-info">
                <el-tag type="info" size="small">
                  <el-icon><Timer /></el-icon>
                  检索：{{ retrievalTime }}ms
                </el-tag>
                <el-tag type="success" size="small">
                  <el-icon><MagicStick /></el-icon>
                  生成：{{ generationTime }}ms
                </el-tag>
              </div>
            </div>
            <div class="card-body">
              <div class="answer-content" v-html="formattedAnswer"></div>
            </div>
          </div>
        </div>

        <div class="right-panel">
          <div v-if="references.length > 0" class="references-card">
            <div class="card-header">
              <h3>
                <el-icon><Document /></el-icon>
                参考知识
              </h3>
              <el-tag type="warning">{{ references.length }}条</el-tag>
            </div>
            <div class="card-body">
              <el-card
                v-for="(ref, index) in references"
                :key="ref.knowledgeId"
                class="reference-card"
                shadow="hover"
              >
                <div class="reference-header">
                  <span class="reference-title"
                    >【知识{{ index + 1 }}】{{ ref.title }}</span
                  >
                  <el-tag type="warning" size="small">
                    相关度：{{ (ref.similarity * 100).toFixed(1) }}%
                  </el-tag>
                </div>
                <div class="reference-summary">{{ ref.summary }}</div>
                <div class="reference-content">{{ ref.matchedContent }}</div>
              </el-card>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  ChatDotRound,
  Edit,
  Promotion,
  Delete,
  MagicStick,
  Document,
  Clock,
  ArrowRight,
} from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import request from "@/utils/request";

const router = useRouter();

const question = ref("");
const answer = ref("");
const references = ref([]);
const loading = ref(false);
const generatingVectors = ref(false);
const retrievalTime = ref(0);
const generationTime = ref(0);

const formattedAnswer = computed(() => {
  return answer.value.replace(/\n/g, "<br>");
});

const handleAsk = async () => {
  if (!question.value.trim()) {
    ElMessage.warning("请输入问题");
    return;
  }

  loading.value = true;
  try {
    const result = await request.post("/rag/answer", {
      question: question.value,
      topK: 3,
      includeReferences: true,
    });

    answer.value = result.answer;
    references.value = result.references || [];
    retrievalTime.value = result.retrievalTime;
    generationTime.value = result.generationTime;
    ElMessage.success("回答完成");
  } catch (error) {
    console.error("回答失败:", error);
    if (error.message && error.message.includes("API Key")) {
      ElMessageBox.alert(
        "AI服务不可用，请配置有效的API Key。\n\n请前往【个人设置】添加您的API Key，或联系管理员配置平台API Key。",
        "需要配置API Key",
        {
          confirmButtonText: "前往设置",
          type: "warning",
        },
      ).then(() => {
        router.push("/settings");
      });
    } else {
      ElMessage.error("回答失败：" + (error.message || "未知错误"));
    }
  } finally {
    loading.value = false;
  }
};

const handleClear = () => {
  question.value = "";
  answer.value = "";
  references.value = [];
};

const handleGenerateVectors = async () => {
  generatingVectors.value = true;
  try {
    const result = await request.post("/rag/generate-vectors");
    ElMessage.success(result);
  } catch (error) {
    ElMessage.error("生成向量失败：" + error.message);
  } finally {
    generatingVectors.value = false;
  }
};
</script>

<style scoped>
.rag-qa-container {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

.main-content {
  position: relative;
  z-index: 1;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header-section {
  margin-bottom: 20px;
}



.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 20px;
}

.left-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card,
.answer-card,
.references-card {
  background: white;
  border-radius: var(--radius-md);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-lighter);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-info {
  display: flex;
  gap: 8px;
}

.card-body {
  padding: 24px;
}

.button-group {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}

.answer-content {
  padding: 20px;
  background-color: var(--bg-input);
  border-radius: var(--radius-sm);
  line-height: 1.8;
  color: var(--text-primary);
  font-size: 15px;
  white-space: pre-wrap;
}

.reference-card {
  margin-bottom: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
}

.reference-card:last-child {
  margin-bottom: 0;
}

.reference-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.reference-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary);
  flex: 1;
  margin-right: 10px;
}

.reference-summary {
  color: var(--text-regular);
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 1.6;
}

.reference-content {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  max-height: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

@media (max-width: 1200px) {
  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .right-panel {
    order: -1;
  }
}

@media (max-width: 768px) {
  .rag-qa-container {
    padding: 20px;
  }

  .page-title {
    font-size: 24px;
  }

  .button-group {
    flex-direction: column;
  }

  .button-group .el-button {
    width: 100%;
  }
}
</style>
