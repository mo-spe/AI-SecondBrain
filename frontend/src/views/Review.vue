<template>
  <div class="review-container">
    <div class="background-gradient"></div>

    <div class="main-content">
      <div class="header-section">
        <div class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="50"><Trophy /></el-icon>
          </div>
          <div class="welcome-content">
            <h1 class="welcome-title">复习中心</h1>
            <p class="welcome-subtitle">今日学习进度追踪</p>
          </div>
        </div>
      </div>

      <div class="stats-section">
        <div class="stats-grid">
          <div class="stat-card total">
            <div class="stat-header">
              <div class="stat-icon-wrapper total">
                <el-icon :size="32"><Document /></el-icon>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ todayPending }}</div>
              <div class="stat-label">待复习</div>
            </div>
          </div>

          <div class="stat-card completed">
            <div class="stat-header">
              <div class="stat-icon-wrapper completed">
                <el-icon :size="32"><CircleCheck /></el-icon>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ totalCompleted }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>

          <div class="stat-card accuracy">
            <div class="stat-header">
              <div class="stat-icon-wrapper accuracy">
                <el-icon :size="32"><TrendCharts /></el-icon>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ totalAccuracy }}%</div>
              <div class="stat-label">准确率</div>
            </div>
          </div>

          <div class="stat-card streak">
            <div class="stat-header">
              <div class="stat-icon-wrapper streak">
                <el-icon :size="32"><Medal /></el-icon>
              </div>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ streakDays }}</div>
              <div class="stat-label">连续天数</div>
            </div>
          </div>
        </div>
      </div>

      <div class="progress-section">
        <div class="progress-card">
          <div class="progress-header">
            <span class="progress-label">今日进度</span>
            <el-tag :type="getProgressType()" size="large">
              {{ progressText }}
            </el-tag>
          </div>
          <el-progress
            :percentage="todayProgress"
            :color="progressColor"
            :stroke-width="12"
            :show-text="true"
            :format="progressFormat"
          />
        </div>
      </div>

      <div class="toolbar-section">
        <div class="toolbar-card">
          <div class="toolbar-left">
            <el-radio-group v-model="viewMode" size="large">
              <el-radio-button value="list">
                <el-icon><List /></el-icon>
                列表
              </el-radio-button>
              <el-radio-button value="card">
                <el-icon><Document /></el-icon>
                卡片
              </el-radio-button>
            </el-radio-group>

            <el-select
              v-model="sortBy"
              placeholder="排序方式"
              size="large"
              style="width: 150px"
            >
              <el-option label="智能推荐" value="smart" />
              <el-option label="按时间" value="time" />
              <el-option label="按难度" value="difficulty" />
            </el-select>
          </div>

          <div class="toolbar-right">
            <el-button
              type="primary"
              size="large"
              @click="generateAllCards"
              :loading="generating"
            >
              <el-icon><Plus /></el-icon>
              生成练习卡片
            </el-button>
            <el-button type="success" size="large" @click="generateReviewCards">
              <el-icon><Document /></el-icon>
              恢复复习题目
            </el-button>
            <el-button type="danger" size="large" @click="deleteAllCards">
              <el-icon><Delete /></el-icon>
              清空
            </el-button>
          </div>
        </div>
      </div>

      <div class="content-section">
        <div class="content-card">
          <div class="card-header">
            <h3>
              <el-icon><Document /></el-icon>
              复习卡片列表
            </h3>
            <el-tag type="info">{{ reviewList.length }}张</el-tag>
          </div>

          <div class="card-body">
            <transition name="fade" mode="out-in">
              <div v-if="viewMode === 'list'" key="list" class="list-view">
                <el-table
                  :data="sortedReviewList"
                  stripe
                  :row-style="getRowStyle"
                  @row-click="startReview"
                  style="cursor: pointer"
                  v-loading="loading"
                >
                  <el-table-column
                    prop="question"
                    label="问题"
                    min-width="350"
                    show-overflow-tooltip
                  >
                    <template #default="{ row }">
                      <div class="question-cell">
                        <el-icon class="question-icon"
                          ><QuestionFilled
                        /></el-icon>
                        <span>{{ parseQuestionText(row.question) }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column
                    prop="cardType"
                    label="类型"
                    width="100"
                    align="center"
                  >
                    <template #default="{ row }">
                      <el-tag
                        :type="getCardTypeColor(row.cardType)"
                        effect="dark"
                      >
                        {{ getCardTypeText(row.cardType) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column
                    prop="difficulty"
                    label="难度"
                    width="130"
                    align="center"
                  >
                    <template #default="{ row }">
                      <el-rate
                        v-model="row.difficulty"
                        disabled
                        show-score
                        text-color="#ff9900"
                        :max="3"
                        size="small"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column
                    prop="nodeReviewCount"
                    label="知识点复习次数"
                    width="120"
                    align="center"
                  >
                    <template #default="{ row }">
                      <el-badge
                        :value="row.nodeReviewCount"
                        :max="10"
                        type="warning"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column
                    prop="nodeMasteryLevel"
                    label="知识点掌握程度"
                    width="120"
                    align="center"
                  >
                    <template #default="{ row }">
                      <el-tag
                        :type="getMasteryLevelColor(row.nodeMasteryLevel)"
                        effect="dark"
                        size="large"
                      >
                        {{ getMasteryLevelText(row.nodeMasteryLevel) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column
                    prop="createTime"
                    label="生成时间"
                    width="160"
                    align="center"
                  >
                    <template #default="{ row }">
                      <div class="time-cell">
                        <el-icon><Clock /></el-icon>
                        <span>{{ row.createTime }}</span>
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column
                    label="操作"
                    width="180"
                    align="center"
                    fixed="right"
                  >
                    <template #default="{ row }">
                      <el-button-group>
                        <el-button
                          type="primary"
                          size="small"
                          @click.stop="startReview(row)"
                        >
                          <el-icon><VideoPlay /></el-icon>
                          复习
                        </el-button>
                        <el-button
                          type="danger"
                          size="small"
                          @click.stop="deleteCard(row)"
                        >
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </el-button-group>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <div v-else-if="viewMode === 'card'" key="card" class="card-view">
                <transition-group name="card-fade" tag="div" class="card-grid">
                  <div
                    v-for="card in sortedReviewList"
                    :key="card.id"
                    class="review-card-item"
                    @click="startReview(card)"
                  >
                    <div class="card-inner">
                      <div class="card-header">
                        <div class="card-badges">
                          <el-tag
                            :type="getCardTypeColor(card.cardType)"
                            effect="dark"
                          >
                            {{ getCardTypeText(card.cardType) }}
                          </el-tag>
                          <el-tag
                            type="danger"
                            effect="dark"
                            v-if="
                              card.reviewCount === 0 && card.isRestored === 0
                            "
                          >
                            新卡片
                          </el-tag>
                          <el-tag
                            :type="
                              card.generationType === 'manual'
                                ? 'warning'
                                : 'success'
                            "
                            effect="dark"
                          >
                            {{
                              card.generationType === "manual"
                                ? "手动练习"
                                : "正式复习"
                            }}
                          </el-tag>
                        </div>
                        <div class="card-difficulty">
                          <el-rate
                            v-model="card.difficulty"
                            disabled
                            show-score
                            text-color="#ff9900"
                            :max="3"
                            size="small"
                          />
                        </div>
                      </div>

                      <div class="card-body">
                        <div class="card-question">
                          <el-icon class="question-icon-large"
                            ><QuestionFilled
                          /></el-icon>
                          <p>{{ parseQuestionText(card.question) }}</p>
                        </div>

                        <div class="card-stats">
                          <div class="stat-row">
                            <el-icon><Document /></el-icon>
                            <span
                              >知识点复习 {{ card.nodeReviewCount }} 次</span
                            >
                          </div>
                          <div class="stat-row">
                            <el-icon><Medal /></el-icon>
                            <el-tag
                              :type="
                                getMasteryLevelColor(card.nodeMasteryLevel)
                              "
                              effect="dark"
                              size="small"
                            >
                              {{ getMasteryLevelText(card.nodeMasteryLevel) }}
                            </el-tag>
                          </div>
                          <div class="stat-row">
                            <el-icon><Clock /></el-icon>
                            <span>生成：{{ card.createTime }}</span>
                          </div>
                        </div>
                      </div>

                      <div class="card-footer">
                        <div class="card-time">
                          <el-icon><Clock /></el-icon>
                          <span>{{ formatDate(card.nextReviewTime) }}</span>
                        </div>
                        <el-button
                          type="primary"
                          size="large"
                          @click.stop="startReview(card)"
                        >
                          <el-icon><VideoPlay /></el-icon>
                          开始复习
                        </el-button>
                      </div>
                    </div>
                  </div>
                </transition-group>
              </div>
            </transition>

            <el-empty
              v-if="!loading && reviewList.length === 0"
              description="暂无待复习卡片"
              :image-size="150"
            >
              <el-button type="primary" @click="generateReviewCards">
                <el-icon><Plus /></el-icon>
                恢复复习题目
              </el-button>
            </el-empty>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="reviewDialogVisible"
      title="复习答题"
      width="800px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="review-dialog"
      append-to-body
      @close="closeReviewDialog"
    >
      <div v-if="currentCard" class="review-dialog-content">
        <div class="dialog-header">
          <div class="dialog-badges">
            <el-tag
              :type="getCardTypeColor(currentCard.cardType)"
              effect="dark"
              size="default"
            >
              {{ getCardTypeText(currentCard.cardType) }}
            </el-tag>
            <el-tag
              :type="
                currentCard.generationType === 'manual' ? 'warning' : 'success'
              "
              effect="dark"
              size="default"
            >
              {{
                currentCard.generationType === "manual"
                  ? "手动练习"
                  : "正式复习"
              }}
            </el-tag>
            <el-rate
              v-model="currentCard.difficulty"
              disabled
              show-score
              text-color="#ff9900"
              :max="5"
              size="small"
            />
          </div>
        </div>

        <div class="dialog-question">
          <h3>{{ parseQuestionText(currentCard.question) }}</h3>
        </div>

        <div class="dialog-answer-section">
          <div class="answer-header">
            <el-icon><Edit /></el-icon>
            <h4>你的答案</h4>
          </div>

          <div v-if="currentCard.cardType === 'choice'" class="choice-options">
            <div
              v-for="option in parseChoiceOptions(currentCard.question)"
              :key="option.key"
              class="choice-option"
              :class="{
                selected: reviewForm.selectedOption === option.key,
                'correct-answer':
                  showResult && option.key === reviewResult?.correctAnswer,
                'wrong-answer':
                  showResult &&
                  reviewForm.selectedOption === option.key &&
                  option.key !== reviewResult?.correctAnswer,
              }"
              @click="!showResult && (reviewForm.selectedOption = option.key)"
            >
              <div class="option-content-wrapper">
                <div class="option-header">
                  <div class="option-letter">{{ option.key }}</div>
                  <div class="option-status" v-if="showResult">
                    <el-icon
                      v-if="option.key === reviewResult?.correctAnswer"
                      color="#67c23a"
                    >
                      <CircleCheck />
                    </el-icon>
                    <el-icon
                      v-else-if="reviewForm.selectedOption === option.key"
                      color="#f56c6c"
                    >
                      <CircleClose />
                    </el-icon>
                  </div>
                </div>
                <div class="option-content">{{ option.content }}</div>
                <div
                  class="option-explanation"
                  v-if="showResult && getOptionExplanation(option.key)"
                >
                  <div class="explanation-label">解析：</div>
                  <div class="explanation-text">
                    {{ getOptionExplanation(option.key) }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <el-input
            v-else
            v-model="reviewForm.answer"
            type="textarea"
            :rows="6"
            placeholder="请输入你的答案..."
            size="large"
            :disabled="showResult"
          />
        </div>

        <div class="dialog-result-summary" v-if="showResult && reviewResult">
          <div class="result-summary-header">
            <el-icon
              :size="24"
              :color="reviewResult.isCorrect ? '#67c23a' : '#f56c6c'"
            >
              <component
                :is="reviewResult.isCorrect ? CircleCheck : CircleClose"
              />
            </el-icon>
            <h3
              :class="{
                correct: reviewResult.isCorrect,
                incorrect: !reviewResult.isCorrect,
              }"
            >
              {{ reviewResult.isCorrect ? "回答正确！" : "回答错误" }}
            </h3>
          </div>

          <div class="result-summary-content" v-if="reviewResult.explanation">
            <div class="result-summary-label">整体解析</div>
            <div class="result-summary-text">
              {{ reviewResult.explanation }}
            </div>
          </div>
        </div>

        <div class="dialog-timer" v-if="!showResult">
          <div class="timer-display">
            <el-icon><Timer /></el-icon>
            <span>{{ formatTime(reviewForm.duration) }}</span>
          </div>
          <div class="timer-info">
            <el-icon><InfoFilled /></el-icon>
            <span>请认真思考后选择答案</span>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeReviewDialog" size="default">
            取消
          </el-button>
          <el-button
            v-if="!showResult"
            type="primary"
            @click="submitReview"
            size="default"
            :loading="submitting"
          >
            提交答案
          </el-button>
          <el-button v-else type="primary" @click="nextReview" size="default">
            下一题
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { reviewAPI } from "@/api/review";
import { statisticsAPI } from "@/api/statistics";
import {
  Trophy,
  Document,
  CircleCheck,
  CircleClose,
  TrendCharts,
  Medal,
  List,
  Plus,
  Delete,
  QuestionFilled,
  Clock,
  VideoPlay,
  Edit,
  Timer,
  Check,
  Close,
} from "@element-plus/icons-vue";

const loading = ref(false);
const generating = ref(false);
const submitting = ref(false);
const reviewDialogVisible = ref(false);
const viewMode = ref("list");
const sortBy = ref("smart");

const reviewList = ref([]);
const currentCard = ref(null);
const reviewForm = ref({
  answer: "",
  selectedOption: "",
  duration: 0,
});

const reviewResult = ref(null);
const showResult = ref(false);

const todayTotal = ref(0);
const todayCompleted = ref(0);
const todayPending = ref(0);
const overallAccuracy = ref(0);
const streakDays = ref(0);
const totalCompleted = ref(0);
const totalAccuracy = ref(0);

let timerInterval = null;
let startTime = null;

watch(sortBy, () => {
  loadReviewCards();
});

const todayProgress = computed(() => {
  if (todayTotal.value === 0) return 0;
  return Math.round((todayCompleted.value / todayTotal.value) * 100);
});

const progressText = computed(() => {
  if (todayProgress.value === 100) return "已完成";
  if (todayProgress.value >= 75) return "即将完成";
  if (todayProgress.value >= 50) return "进行中";
  return "刚开始";
});

const progressType = computed(() => {
  if (todayProgress.value === 100) return "success";
  if (todayProgress.value >= 75) return "warning";
  if (todayProgress.value >= 50) return "info";
  return "primary";
});

const progressColor = computed(() => {
  if (todayProgress.value === 100) return "#67c23a";
  if (todayProgress.value >= 75) return "#e6a23c";
  if (todayProgress.value >= 50) return "#409eff";
  return "#667eea";
});

const sortedReviewList = computed(() => {
  let sorted = [...reviewList.value];

  switch (sortBy.value) {
    case "smart":
      sorted.sort((a, b) => {
        const aScore =
          (a.nodeMasteryLevel || 0) * 0.6 + (a.difficulty || 1) * 0.4;
        const bScore =
          (b.nodeMasteryLevel || 0) * 0.6 + (b.difficulty || 1) * 0.4;
        return bScore - aScore;
      });
      break;
    case "time":
      sorted.sort((a, b) => {
        const aTime = a.nextReviewTime
          ? new Date(a.nextReviewTime).getTime()
          : 0;
        const bTime = b.nextReviewTime
          ? new Date(b.nextReviewTime).getTime()
          : 0;
        return aTime - bTime;
      });
      break;
    case "difficulty":
      sorted.sort((a, b) => (b.difficulty || 1) - (a.difficulty || 1));
      break;
  }

  return sorted;
});

const loadReviewCards = async () => {
  try {
    loading.value = true;
    const data = await reviewAPI.getTodayReviewCards(sortBy.value);
    reviewList.value = data || [];

    todayTotal.value = reviewList.value.length;
    todayCompleted.value = reviewList.value.filter(
      (card) => card.reviewCount > 0,
    ).length;
    todayPending.value = todayTotal.value - todayCompleted.value;

    // 从后端获取用户全局准确率
    overallAccuracy.value = await reviewAPI.getUserAccuracy();

    streakDays.value = await calculateStreakDays();

    loadGlobalStatistics();
  } catch (error) {
    ElMessage.error("加载复习卡片失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const loadGlobalStatistics = async () => {
  try {
    const response = await statisticsAPI.getStatistics();
    totalCompleted.value = response.completedReviewCount || 0;

    // 从后端获取用户全局准确率
    totalAccuracy.value = await reviewAPI.getUserAccuracy();
  } catch (error) {
    console.error("加载全局统计数据失败", error);
  }
};

const calculateStreakDays = async () => {
  try {
    const response = await reviewAPI.getStreakDays();
    return response.data || 0;
  } catch (error) {
    console.error("获取连续天数失败", error);
    return 0;
  }
};

const getProgressType = () => progressType.value;

const progressFormat = (percentage) => {
  return `${percentage}%`;
};

const getRowStyle = ({ row }) => {
  if (row.reviewCount === 0) {
    return { backgroundColor: "rgba(102, 126, 234, 0.1)" };
  }
  return {};
};

const getCardTypeColor = (type) => {
  const colorMap = {
    choice: "primary",
    fill: "success",
    essay: "warning",
  };
  return colorMap[type] || "info";
};

const getCardTypeText = (type) => {
  const textMap = {
    choice: "选择题",
    fill: "填空题",
    essay: "问答题",
  };
  return textMap[type] || "未知";
};

const getAccuracyColor = (accuracy) => {
  if (accuracy >= 0.8) return "#67c23a";
  if (accuracy >= 0.6) return "#e6a23c";
  return "#f56c6c";
};

const getMasteryLevelColor = (level) => {
  const colorMap = {
    0: "danger",
    1: "warning",
    2: "info",
    3: "primary",
    4: "success",
    5: "success",
  };
  return colorMap[level] || "info";
};

const getMasteryLevelText = (level) => {
  const textMap = {
    0: "未掌握",
    1: "入门",
    2: "熟悉",
    3: "掌握",
    4: "精通",
    5: "专家",
  };
  return textMap[level] || "未知";
};

const parseQuestionText = (question) => {
  if (!question) return "";
  const lines = question.split("\n");
  let questionText = "";

  for (const line of lines) {
    const trimmedLine = line.trim();
    if (
      trimmedLine &&
      !trimmedLine.startsWith("A.") &&
      !trimmedLine.startsWith("B.") &&
      !trimmedLine.startsWith("C.") &&
      !trimmedLine.startsWith("D.") &&
      !trimmedLine.startsWith("A、") &&
      !trimmedLine.startsWith("B、") &&
      !trimmedLine.startsWith("C、") &&
      !trimmedLine.startsWith("D、") &&
      !trimmedLine.startsWith("正确答案：") &&
      !trimmedLine.startsWith("解析：") &&
      !trimmedLine.startsWith("【正确答案】") &&
      !trimmedLine.startsWith("【解析】")
    ) {
      if (questionText) {
        questionText += " ";
      }
      questionText += trimmedLine;
    }
  }

  return questionText || question;
};

const parseChoiceOptions = (question) => {
  const options = [];
  const lines = question.split("\n");
  let inOptionsSection = false;

  for (const line of lines) {
    const trimmedLine = line.trim();

    if (
      trimmedLine.startsWith("选项解析：") ||
      trimmedLine.startsWith("解析：")
    ) {
      break;
    }

    const match = trimmedLine.match(/^([A-D])\.\s*(.+)$/);
    if (match) {
      options.push({ key: match[1], content: match[2].trim() });
      inOptionsSection = true;
    }
  }
  return options;
};

const getOptionExplanation = (optionKey) => {
  if (!currentCard.value || !currentCard.value.question) {
    return null;
  }

  const lines = currentCard.value.question.split("\n");
  let foundOptionExplanation = false;
  let explanation = "";

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    if (line.startsWith("选项解析：")) {
      foundOptionExplanation = true;
      continue;
    }

    if (foundOptionExplanation) {
      const optionMatch = line.match(/^([A-D])\.\s*(.+)$/);
      if (optionMatch) {
        if (optionMatch[1] === optionKey) {
          explanation = optionMatch[2].trim();
          break;
        } else if (explanation) {
          break;
        }
      }
    }
  }

  return explanation || null;
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

const formatTime = (seconds) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
};

const startReview = (card) => {
  currentCard.value = card;
  reviewForm.value = {
    answer: "",
    selectedOption: "",
    duration: 0,
  };
  reviewDialogVisible.value = true;

  startTime = Date.now();
  timerInterval = setInterval(() => {
    reviewForm.value.duration = Math.floor((Date.now() - startTime) / 1000);
  }, 1000);
};

const closeReviewDialog = () => {
  reviewDialogVisible.value = false;
  showResult.value = false;
  reviewResult.value = null;
  reviewForm.value = {
    answer: "",
    selectedOption: "",
    duration: 0,
  };
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
};

const submitReview = async () => {
  try {
    submitting.value = true;

    let answer = reviewForm.value.answer;
    if (currentCard.value.cardType === "choice") {
      answer = reviewForm.value.selectedOption;
    }

    if (!answer) {
      submitting.value = false;
      ElMessage.warning("请先填写答案");
      return;
    }

    const result = await reviewAPI.submitReviewResult({
      cardId: currentCard.value.id,
      userAnswer: answer,
      duration: reviewForm.value.duration,
    });

    reviewResult.value = result;
    showResult.value = true;
    ElMessage.success("提交成功");

    loadReviewCards();
    loadGlobalStatistics();
  } catch (error) {
    ElMessage.error("提交失败：" + error.message);
  } finally {
    submitting.value = false;
    if (timerInterval) {
      clearInterval(timerInterval);
      timerInterval = null;
    }
  }
};

const nextReview = () => {
  const currentIndex = reviewList.value.findIndex(
    (card) => card.id === currentCard.value.id,
  );
  const nextIndex = currentIndex + 1;

  if (nextIndex < reviewList.value.length) {
    const nextCard = reviewList.value[nextIndex];
    currentCard.value = nextCard;
    reviewForm.value = {
      answer: "",
      selectedOption: "",
      duration: 0,
    };
    showResult.value = false;
    reviewResult.value = null;

    startTime = Date.now();
    if (timerInterval) {
      clearInterval(timerInterval);
    }
    timerInterval = setInterval(() => {
      reviewForm.value.duration = Math.floor((Date.now() - startTime) / 1000);
    }, 1000);
  } else {
    reviewDialogVisible.value = false;
    loadReviewCards();
    ElMessage.info("已经是最后一题了");
  }
};

const generateAllCards = async () => {
  try {
    await ElMessageBox.confirm(
      "确定要为所有知识点生成练习卡片吗？这些卡片与复习规律无关，仅用于练习。",
      "确认生成练习卡片",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    generating.value = true;
    const count = await reviewAPI.generateAllReviewCards();
    ElMessage.success(`成功生成 ${count} 张练习卡片`);
    loadReviewCards();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("生成练习卡片失败：" + error.message);
    }
  } finally {
    generating.value = false;
  }
};

const generateReviewCards = async () => {
  try {
    await ElMessageBox.confirm(
      "确定要恢复复习题目吗？这将把之前清空（软删除）的题目重新显示在复习中心。",
      "确认恢复复习题目",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "info",
      },
    );

    generating.value = true;
    const restoredCount = await reviewAPI.restoreReviewCards();
    ElMessage.success(`成功恢复 ${restoredCount} 张复习卡片`);
    loadReviewCards();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("恢复复习题目失败：" + error.message);
    }
  } finally {
    generating.value = false;
  }
};

const deleteCard = async (card) => {
  try {
    await ElMessageBox.confirm(`确定要删除这张复习卡片吗？`, "确认删除", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });

    await reviewAPI.deleteReviewCard(card.id);
    ElMessage.success("删除成功");
    loadReviewCards();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除失败：" + error.message);
    }
  }
};

const deleteAllCards = async () => {
  try {
    await ElMessageBox.confirm(
      "确定要清空所有复习题目吗？清空后题目会被隐藏，但不会删除，可以通过'恢复复习题目'按钮重新显示。",
      "确认清空",
      {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      },
    );

    await reviewAPI.deleteAllReviewCards();
    ElMessage.success("清空成功，题目已隐藏");
    loadReviewCards();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("清空失败：" + error.message);
    }
  }
};

onMounted(() => {
  loadReviewCards();
});

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval);
  }
});
</script>

<style scoped>
.review-container {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

.review-dialog {
  border-radius: 12px;
  overflow: hidden;
}

.review-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom: none;
}

.review-dialog :deep(.el-dialog__title) {
  color: white;
  font-size: 18px;
  font-weight: 600;
}

.review-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.review-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
}

.review-dialog-content {
  color: #303133;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.dialog-badges {
  display: flex;
  gap: 12px;
  align-items: center;
}

.dialog-question {
  margin-bottom: 24px;
}

.dialog-question h3 {
  font-size: 18px;
  color: #303133;
  line-height: 1.5;
  margin: 0;
}

.dialog-answer-section {
  margin-bottom: 24px;
}

.answer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #667eea;
  font-size: 16px;
  font-weight: bold;
}

.choice-options {
  display: grid;
  gap: 12px;
}

.choice-option {
  display: flex;
  gap: 12px;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.choice-option:hover:not(.correct-answer):not(.wrong-answer) {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.choice-option.selected {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.choice-option.correct-answer {
  border-color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
}

.choice-option.wrong-answer {
  border-color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}

.option-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-letter {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: #303133;
  flex-shrink: 0;
}

.choice-option.correct-answer .option-letter {
  background: #67c23a;
  color: white;
}

.choice-option.wrong-answer .option-letter {
  background: #f56c6c;
  color: white;
}

.option-status {
  flex-shrink: 0;
}

.option-content {
  flex: 1;
  color: #606266;
  font-size: 15px;
  line-height: 1.5;
}

.option-explanation {
  margin-top: 8px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #667eea;
}

.explanation-label {
  font-size: 13px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 4px;
}

.explanation-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.dialog-result-summary {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 2px solid #e0e0e0;
}

.result-summary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.result-summary-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: bold;
}

.result-summary-header.correct h3 {
  color: #67c23a;
}

.result-summary-header.incorrect h3 {
  color: #f56c6c;
}

.result-summary-content {
  padding: 12px;
  background: white;
  border-radius: 8px;
  border-left: 3px solid #667eea;
}

.result-summary-label {
  font-size: 14px;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 8px;
}

.result-summary-text {
  font-size: 15px;
  color: #303133;
  line-height: 1.6;
}

.dialog-timer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 4px;
}

.timer-display {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: bold;
  color: #667eea;
}

.timer-info {
  color: #909399;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.background-gradient {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 0;
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

.welcome-card {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 30px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInDown 0.6s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-icon {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.welcome-content {
  flex: 1;
}

.welcome-title {
  font-size: 32px;
  font-weight: bold;
  color: white;
  margin: 0 0 8px 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.welcome-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.stats-section {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.3);
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-header {
  margin-bottom: 12px;
}

.stat-icon-wrapper {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-icon-wrapper.total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon-wrapper.completed {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stat-icon-wrapper.accuracy {
  background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);
}

.stat-icon-wrapper.streak {
  background: linear-gradient(135deg, #f56c6c 0%, #f89898 100%);
}

.stat-body {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.progress-section {
  margin-bottom: 20px;
}

.progress-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.progress-label {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.toolbar-section {
  margin-bottom: 20px;
}

.toolbar-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  animation: fadeInUp 0.6s ease-out;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.content-section {
  margin-bottom: 20px;
}

.content-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.6s ease-out;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.card-header h3 {
  font-size: 18px;
  color: #303133;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h3 .el-icon {
  color: #667eea;
}

.card-body {
  min-height: 400px;
}

.list-view {
  width: 100%;
}

.question-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-icon {
  color: #667eea;
  font-size: 18px;
}

.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

.card-view {
  width: 100%;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 16px;
}

.review-card-item {
  background: white;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
}

.review-card-item:hover {
  border-color: #667eea;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  transform: translateY(-4px);
}

.card-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  background: #f9f9f9;
  border-bottom: 1px solid #f0f0f0;
}

.card-badges {
  display: flex;
  gap: 8px;
}

.card-difficulty {
  display: flex;
  align-items: center;
}

.card-body {
  flex: 1;
  padding: 16px;
}

.card-question {
  margin-bottom: 16px;
}

.question-icon-large {
  color: #667eea;
  font-size: 24px;
  margin-bottom: 8px;
}

.card-question p {
  color: #303133;
  font-size: 15px;
  line-height: 1.5;
  margin: 0;
}

.card-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

.stat-row .el-icon {
  color: #667eea;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.card-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
}

:deep(.el-empty) {
  background: transparent;
  color: #909399;
}

:deep(.el-empty__description p) {
  color: #909399;
}

:deep(.el-input__wrapper) {
  box-shadow: none;
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover) {
  border-color: #667eea;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

:deep(.el-textarea__inner) {
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
}

:deep(.el-textarea__inner:hover) {
  border-color: #667eea;
}

:deep(.el-textarea__inner:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

:deep(.el-table) {
  background: white;
}

:deep(.el-table th) {
  background: #f9f9f9;
  color: #303133;
  font-weight: bold;
}

:deep(.el-table tr:hover > td) {
  background: rgba(102, 126, 234, 0.05) !important;
}

:deep(.el-progress-bar__outer) {
  background: #f0f0f0;
}

:deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.card-fade-enter-active {
  transition: all 0.3s;
}

.card-fade-leave-active {
  transition: all 0.3s;
}

.card-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.card-fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    text-align: center;
  }

  .welcome-title {
    font-size: 24px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .toolbar-card {
    flex-direction: column;
    gap: 16px;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
