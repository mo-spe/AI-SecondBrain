<template>
  <div class="review-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">复习中心</h1>
        <p class="page-subtitle">科学复习，高效记忆，持续巩固你的知识</p>
      </div>
      <div class="header-right">
        <CheckInButton />
        <el-button type="text" @click="handleReviewSettings">
          <el-icon size="16"><Setting /></el-icon>
          <span>复习设置</span>
        </el-button>
      </div>
    </div>

    <div class="banner-section">
      <div class="banner-content">
        <div class="banner-left">
          <h2 class="banner-title">坚持复习，收获成长</h2>
          <p class="banner-desc">根据遗忘曲线智能安排复习，今日是你坚持学习的第 {{ streakDays }} 天</p>
        </div>
        <div class="banner-right">
          <div class="calendar-icon">
            <div class="calendar-date">28</div>
            <div class="calendar-month">8月</div>
          </div>
          <div class="bot-icon">
            <svg viewBox="0 0 100 100" width="80" height="80">
              <circle cx="50" cy="55" r="35" fill="#7c3aed"/>
              <circle cx="40" cy="50" r="5" fill="white"/>
              <circle cx="60" cy="50" r="5" fill="white"/>
              <path d="M 42 60 Q 50 68 58 60" stroke="white" stroke-width="3" fill="none"/>
              <ellipse cx="50" cy="85" rx="25" ry="15" fill="#a855f7"/>
            </svg>
          </div>
        </div>
      </div>
    </div>

    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon size="20"><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">今日待复习</div>
          <div class="stat-value">{{ todayPending }}</div>
          <div class="stat-unit">个卡片</div>
          <div class="stat-change">较昨日 <span class="increase">↑5</span></div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon size="20"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">今日已复习</div>
          <div class="stat-value">{{ todayCompleted }}</div>
          <div class="stat-unit">个卡片</div>
          <div class="stat-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: completionRate + '%' }"></div>
            </div>
            <span class="progress-text">完成率 {{ completionRate }}%</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon size="20"><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">熟练度</div>
          <div class="stat-value">{{ totalAccuracy }}%</div>
          <div class="stat-change">较昨日 <span class="increase">↑8%</span></div>
          <div class="stat-circle">
            <svg viewBox="0 0 60 60" class="circle-svg">
              <circle cx="30" cy="30" r="26" fill="none" stroke="#e2e8f0" stroke-width="6"/>
              <circle cx="30" cy="30" r="26" fill="none" stroke="#7c3aed" stroke-width="6" :stroke-dasharray="circumference" :stroke-dashoffset="circumference - (totalAccuracy / 100) * circumference" stroke-linecap="round" transform="rotate(-90 30 30)"/>
            </svg>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon size="20"><Medal /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">连续复习</div>
          <div class="stat-value">{{ streakDays }}</div>
          <div class="stat-unit">天</div>
          <div class="stat-change">最长 {{ maxStreak }} 天</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green-light">
          <el-icon size="20"><ArrowUp /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">记忆持久度</div>
          <div class="stat-value">{{ memoryRetention }}%</div>
          <div class="stat-change">较上周 <span class="increase">↑6%</span></div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <aside class="plan-sidebar">
        <div class="plan-card">
          <div class="card-header">
            <span class="card-title">今日复习计划</span>
            <el-icon size="14" color="#94a3b8"><InfoFilled /></el-icon>
          </div>
          <div class="plan-chart">
            <div class="chart-ring">
              <svg viewBox="0 0 120 120" class="ring-svg">
                <circle cx="60" cy="60" r="50" fill="none" stroke="#f1f5f9" stroke-width="12"/>
                <circle cx="60" cy="60" r="50" fill="none" stroke="#7c3aed" stroke-width="12" :stroke-dasharray="ringCircumference" :stroke-dashoffset="ringCircumference - (23 / 100) * ringCircumference" stroke-linecap="round" transform="rotate(-90 60 60)"/>
                <circle cx="60" cy="60" r="50" fill="none" stroke="#3b82f6" stroke-width="12" :stroke-dasharray="ringCircumference" :stroke-dashoffset="ringCircumference - (61 / 100) * ringCircumference" stroke-linecap="round" transform="rotate(-90 60 60)"/>
                <circle cx="60" cy="60" r="50" fill="none" stroke="#f97316" stroke-width="12" :stroke-dasharray="ringCircumference" :stroke-dashoffset="ringCircumference - (84 / 100) * ringCircumference" stroke-linecap="round" transform="rotate(-90 60 60)"/>
                <circle cx="60" cy="60" r="50" fill="none" stroke="#22c55e" stroke-width="12" :stroke-dasharray="ringCircumference" :stroke-dashoffset="ringCircumference - (100 / 100) * ringCircumference" stroke-linecap="round" transform="rotate(-90 60 60)"/>
              </svg>
              <div class="ring-center">
                <div class="ring-number">{{ todayPending }}</div>
                <div class="ring-label">待复习</div>
              </div>
            </div>
            <div class="chart-legend">
              <div class="legend-item">
                <span class="legend-color" style="background: #7c3aed"></span>
                <span class="legend-text">新卡片</span>
                <span class="legend-value">3 (23%)</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #3b82f6"></span>
                <span class="legend-text">学习中</span>
                <span class="legend-value">5 (38%)</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #f97316"></span>
                <span class="legend-text">即将遗忘</span>
                <span class="legend-value">3 (23%)</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #22c55e"></span>
                <span class="legend-text">已掌握</span>
                <span class="legend-value">2 (16%)</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="card-header">
            <span class="card-title">遗忘曲线预测</span>
          </div>
          <div class="curve-chart">
            <svg viewBox="0 0 300 150" class="curve-svg">
              <defs>
                <linearGradient id="curveGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="rgba(124, 58, 237, 0.3)"/>
                  <stop offset="100%" stop-color="rgba(124, 58, 237, 0)"/>
                </linearGradient>
              </defs>
              <line x1="40" y1="120" x2="280" y2="120" stroke="#e2e8f0" stroke-width="1"/>
              <line x1="40" y1="30" x2="40" y2="120" stroke="#e2e8f0" stroke-width="1"/>
              <text x="40" y="135" fill="#94a3b8" font-size="10">今天</text>
              <text x="85" y="135" fill="#94a3b8" font-size="10">明天</text>
              <text x="130" y="135" fill="#94a3b8" font-size="10">后天</text>
              <text x="175" y="135" fill="#94a3b8" font-size="10">第4天</text>
              <text x="220" y="135" fill="#94a3b8" font-size="10">第7天</text>
              <text x="265" y="135" fill="#94a3b8" font-size="10">第15天</text>
              <text x="20" y="30" fill="#94a3b8" font-size="10">100%</text>
              <text x="20" y="52" fill="#94a3b8" font-size="10">75%</text>
              <text x="20" y="75" fill="#94a3b8" font-size="10">50%</text>
              <text x="20" y="97" fill="#94a3b8" font-size="10">25%</text>
              <text x="20" y="120" fill="#94a3b8" font-size="10">0%</text>
              <path d="M 40 35 Q 85 50 130 70 T 220 65 T 280 85" fill="none" stroke="#7c3aed" stroke-width="3"/>
              <path d="M 40 30 Q 85 40 130 50 T 220 45 T 280 65" fill="none" stroke="#cbd5e1" stroke-width="2" stroke-dasharray="4,4"/>
              <circle cx="40" cy="35" r="5" fill="#7c3aed"/>
              <circle cx="220" cy="65" r="5" fill="#7c3aed"/>
              <text x="225" y="60" fill="#7c3aed" font-size="10">72%</text>
            </svg>
          </div>
          <div class="chart-legend-row">
            <div class="legend-item">
              <span class="legend-color" style="background: #7c3aed"></span>
              <span class="legend-text">你的记忆曲线</span>
            </div>
            <div class="legend-item">
              <span class="legend-color" style="background: #cbd5e1"></span>
              <span class="legend-text">艾宾浩斯曲线</span>
            </div>
          </div>
          <div class="tip-box">
            <el-icon size="14" color="#f59e0b"><InfoFilled /></el-icon>
            <span>小贴士：定期复习可以将知识从短期记忆转化为长期记忆哦！</span>
          </div>
        </div>
      </aside>

      <main class="queue-content">
        <div class="queue-header">
          <div class="queue-title">
            <span class="title-text">复习队列</span>
            <el-tag type="info" size="small">({{ reviewList.length }})</el-tag>
          </div>
          <div class="queue-actions">
            <el-button type="text" size="small" :class="{ active: filterStatus === 'all' }" @click="filterStatus = 'all'">全部</el-button>
            <el-button type="text" size="small" :class="{ active: filterStatus === 'new' }" @click="filterStatus = 'new'">新卡片 <el-tag size="mini">3</el-tag></el-button>
            <el-button type="text" size="small" :class="{ active: filterStatus === 'learning' }" @click="filterStatus = 'learning'">学习中 <el-tag size="mini">5</el-tag></el-button>
            <el-button type="text" size="small" :class="{ active: filterStatus === 'forgot' }" @click="filterStatus = 'forgot'">即将遗忘 <el-tag size="mini">3</el-tag></el-button>
            <el-button type="text" size="small" :class="{ active: filterStatus === 'mastered' }" @click="filterStatus = 'mastered'">已掌握 <el-tag size="mini">2</el-tag></el-button>
          </div>
        </div>

        <div class="queue-toolbar">
          <el-dropdown trigger="click">
            <el-button type="text" size="small">
              <el-icon size="14"><Sort /></el-icon>
              <span>智能排序</span>
              <el-icon size="14"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="sortBy = 'smart'">智能排序</el-dropdown-item>
                <el-dropdown-item @click="sortBy = 'time'">按时间</el-dropdown-item>
                <el-dropdown-item @click="sortBy = 'difficulty'">按难度</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button type="primary" size="small" @click="startAllReview">
            <el-icon size="14"><VideoPlay /></el-icon>
            <span>开始复习</span>
          </el-button>
        </div>

        <div v-loading="loading" class="queue-list">
          <div
            v-for="card in filteredReviewList"
            :key="card.id"
            class="queue-card"
            @click="startReview(card)"
          >
            <div class="card-checkbox">
              <el-checkbox />
            </div>
            <div class="card-content">
              <div class="card-top">
                <el-tag :type="getSystemTagType(card.systemId)" size="small" effect="light">
                  {{ getSystemName(card.systemId) }}
                </el-tag>
                <button class="card-menu" @click.stop>
                  <el-icon size="14"><MoreFilled /></el-icon>
                </button>
              </div>
              <h3 class="card-title">{{ parseQuestionText(card.question) }}</h3>
              <div class="card-meta">
                <div class="meta-item">
                  <span class="meta-label">掌握度</span>
                  <el-rate
                :model-value="card.nodeMasteryLevel || 0"
                disabled
                show-score
                text-color="#ff9900"
                :max="5"
                size="small"
              />
                </div>
                <div class="meta-item">
                  <span class="meta-label">熟练度</span>
                  <div class="mastery-bar">
                    <div class="mastery-fill" :style="{ width: getMasteryPercentage(card.nodeMasteryLevel) + '%' }"></div>
                  </div>
                </div>
                <div class="meta-item" :class="getDueStatusClass(card)">
                  <el-icon size="12"><Clock /></el-icon>
                  <span>{{ getDueStatusText(card) }}</span>
                </div>
              </div>
              <div class="card-footer">
                <span class="last-review">上次复习：{{ getLastReviewText(card) }}</span>
              </div>
            </div>
          </div>

          <el-empty
            v-if="!loading && filteredReviewList.length === 0"
            description="暂无复习卡片"
            :image-size="150"
          >
            <el-button type="primary" @click="generateReviewCards">
              <el-icon><Plus /></el-icon>
              恢复复习题目
            </el-button>
          </el-empty>
        </div>

        <div class="queue-footer">
          <span class="view-all">查看全部 {{ reviewList.length }} 个卡片</span>
        </div>
      </main>
    </div>

    <el-dialog
      v-model="reviewDialogVisible"
      title="复习答题"
      width="90%"
      :close-on-click-modal="false"
      :lock-scroll="false"
      class="review-dialog immersive-dialog"
      append-to-body
      top="5vh"
      @close="closeReviewDialog"
    >
      <div v-if="currentCard" class="immersive-review-container">
        <div class="review-main-area">
          <div class="question-header">
            <div class="question-badges">
              <span class="question-number">
                {{ currentQuestionIndex + 1 }} / {{ reviewList.length }}
              </span>
              <el-tag
                :type="
                  currentCard.cardType === 'choice' ? 'primary' :
                  currentCard.cardType === 'essay' ? 'success' :
                  currentCard.cardType === 'fill' ? 'warning' : 'info'
                "
                effect="dark"
                size="default"
              >
                {{
                  currentCard.cardType === 'choice' ? '选择题' :
                  currentCard.cardType === 'essay' ? '简答题' :
                  currentCard.cardType === 'fill' ? '填空题' : '判断题'
                }}
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
            <div class="question-timer" v-if="!showResult">
              <el-icon><Timer /></el-icon>
              <span>{{ formatTime(reviewForm.duration) }}</span>
            </div>
          </div>

          <div class="question-content">
            <h2 class="question-title">
              {{ parseQuestionText(currentCard.question) }}
            </h2>
          </div>

          <div class="answer-section">
            <div class="answer-label">
              <el-icon><Edit /></el-icon>
              <span>你的答案</span>
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
                @click="handleOptionClick(option.key)"
              >
                <div class="option-letter">{{ option.key }}</div>
                <div class="option-text">{{ option.content }}</div>
                <div class="option-icon" v-if="showResult">
                  <el-icon
                    v-if="option.key === reviewResult?.correctAnswer"
                    color="#67c23a"
                    size="20"
                  >
                    <CircleCheck />
                  </el-icon>
                  <el-icon
                    v-else-if="reviewForm.selectedOption === option.key"
                    color="#f56c6c"
                    size="20"
                  >
                    <CircleClose />
                  </el-icon>
                </div>
              </div>
              <div
                class="option-explanation"
                v-if="showResult && reviewResult?.explanation"
              >
                <div class="explanation-label">
                  <el-icon><InfoFilled /></el-icon>
                  <span>答案解析</span>
                </div>
                <div class="explanation-text">
                  {{ reviewResult.explanation }}
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

          <div class="result-banner" v-if="showResult && reviewResult">
            <div
              class="result-icon"
              :class="{ correct: reviewResult.isCorrect, incorrect: !reviewResult.isCorrect }"
            >
              <el-icon :size="32">
                <component
                  :is="reviewResult.isCorrect ? CircleCheck : CircleClose"
                />
              </el-icon>
            </div>
            <div class="result-info">
              <h3 :class="{ correct: reviewResult.isCorrect, incorrect: !reviewResult.isCorrect }">
                {{ reviewResult.isCorrect ? "回答正确！" : "回答错误" }}
              </h3>
              <p v-if="reviewResult.correctAnswer">
                正确答案：<strong>{{ reviewResult.correctAnswer }}</strong>
              </p>
            </div>
          </div>

          <div class="question-nav-buttons">
            <el-button
              :disabled="currentQuestionIndex === 0"
              @click="prevQuestion"
              size="large"
            >
              <el-icon><ArrowLeft /></el-icon>
              上一题
            </el-button>
            <el-button
              v-if="!showResult"
              type="primary"
              @click="submitReview"
              size="large"
              :loading="submitting"
              class="submit-btn"
            >
              提交答案
            </el-button>
            <el-button
              v-else-if="currentQuestionIndex < reviewList.length - 1"
              type="primary"
              @click="nextQuestion"
              size="large"
              class="submit-btn"
            >
              下一题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button
              v-else
              type="success"
              @click="finishReview"
              size="large"
              class="submit-btn"
            >
              完成复习
              <el-icon><CircleCheck /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="review-sidebar">
          <div class="sidebar-card">
            <div class="sidebar-title">题目导航</div>
            <div class="question-grid">
              <div
                v-for="(card, index) in reviewList"
                :key="card.id"
                class="question-number-item"
                :class="{
                  current: index === currentQuestionIndex,
                  answered: getQuestionStatus(card.id) === 'answered',
                  correct: getQuestionStatus(card.id) === 'correct',
                  wrong: getQuestionStatus(card.id) === 'wrong',
                }"
                @click="jumpToQuestion(index)"
              >
                {{ index + 1 }}
              </div>
            </div>
          </div>

          <div class="sidebar-card">
            <div class="sidebar-title">答题进度</div>
            <div class="progress-stats">
              <div class="progress-item">
                <span class="progress-label">已答</span>
                <span class="progress-value answered-count">{{ answeredCount }}</span>
              </div>
              <div class="progress-item">
                <span class="progress-label">正确</span>
                <span class="progress-value correct-count">{{ correctCount }}</span>
              </div>
              <div class="progress-item">
                <span class="progress-label">错误</span>
                <span class="progress-value wrong-count">{{ wrongCount }}</span>
              </div>
            </div>
            <div class="progress-bar-wrapper">
              <div class="progress-bar-bg">
                <div
                  class="progress-bar-fill"
                  :style="{ width: progressPercent + '%' }"
                ></div>
              </div>
              <span class="progress-percent">{{ progressPercent }}%</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { reviewAPI } from "@/api/review";
import { statisticsAPI } from "@/api/statistics";
import CheckInButton from "@/components/CheckInButton.vue";
import {
  Setting,
  Clock,
  CircleCheck,
  CircleClose,
  TrendCharts,
  Medal,
  ArrowUp,
  ArrowLeft,
  ArrowRight,
  InfoFilled,
  Sort,
  ArrowDown,
  MoreFilled,
  VideoPlay,
  Plus,
  Edit,
  Timer,
} from "@element-plus/icons-vue";

const loading = ref(false);
const generating = ref(false);
const submitting = ref(false);
const reviewDialogVisible = ref(false);
const sortBy = ref("smart");
const filterStatus = ref("all");

const reviewList = ref([]);
const currentCard = ref(null);
const reviewForm = ref({
  answer: "",
  selectedOption: "",
  duration: 0,
});

const reviewResult = ref(null);
const showResult = ref(false);
const answerResults = ref({});
const currentQuestionIndex = ref(0);

const todayTotal = ref(0);
const todayCompleted = ref(0);
const todayPending = ref(0);
const totalAccuracy = ref(72);
const streakDays = ref(28);
const maxStreak = ref(56);
const memoryRetention = ref(85);

let timerInterval = null;
let startTime = null;

const circumference = 2 * Math.PI * 26;
const ringCircumference = 2 * Math.PI * 50;

const completionRate = computed(() => {
  if (todayTotal.value === 0) return 0;
  return Math.round((todayCompleted.value / todayTotal.value) * 100);
});

const filteredReviewList = computed(() => {
  let filtered = [...reviewList.value];
  
  switch (filterStatus.value) {
    case "new":
      filtered = filtered.filter(card => card.reviewCount === 0);
      break;
    case "learning":
      filtered = filtered.filter(card => card.nodeMasteryLevel >= 1 && card.nodeMasteryLevel <= 3);
      break;
    case "forgot":
      filtered = filtered.filter(card => card.nodeMasteryLevel <= 2);
      break;
    case "mastered":
      filtered = filtered.filter(card => card.nodeMasteryLevel >= 4);
      break;
  }

  return filtered;
});

const sortedReviewList = computed(() => {
  let sorted = [...filteredReviewList.value];

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

const answeredCount = computed(() => {
  return Object.keys(answerResults.value).length;
});

const correctCount = computed(() => {
  return Object.values(answerResults.value).filter(r => r.isCorrect).length;
});

const wrongCount = computed(() => {
  return Object.values(answerResults.value).filter(r => !r.isCorrect).length;
});

const progressPercent = computed(() => {
  if (reviewList.value.length === 0) return 0;
  return Math.round((answeredCount.value / reviewList.value.length) * 100);
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

    overallAccuracy.value = await reviewAPI.getUserAccuracy();
    totalAccuracy.value = overallAccuracy.value || 72;

    streakDays.value = await calculateStreakDays();

    loadGlobalStatistics();
  } catch (error) {
    ElMessage.error("加载复习卡片失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const overallAccuracy = ref(0);

const loadGlobalStatistics = async () => {
  try {
    const response = await statisticsAPI.getStatistics();
    totalCompleted.value = response.completedReviewCount || 0;
    totalAccuracy.value = await reviewAPI.getUserAccuracy();
  } catch (error) {
    console.error("加载全局统计数据失败", error);
  }
};

const totalCompleted = ref(0);

const calculateStreakDays = async () => {
  try {
    const response = await reviewAPI.getStreakDays();
    return response.data || 28;
  } catch (error) {
    console.error("获取连续天数失败", error);
    return 28;
  }
};

const getSystemName = (systemId) => {
  const systemMap = {
    java: "Java 核心技术",
    spring: "Spring Boot",
    algorithm: "数据结构与算法",
    network: "计算机网络",
    os: "操作系统",
    database: "数据库系统",
    frontend: "前端开发",
  };
  return systemMap[systemId] || "未知";
};

const getSystemTagType = (systemId) => {
  const typeMap = {
    java: "primary",
    spring: "success",
    algorithm: "warning",
    network: "info",
    os: "info",
    database: "danger",
    frontend: "danger",
  };
  return typeMap[systemId] || "info";
};

const getCardTypeColor = (cardType) => {
  const colorMap = {
    choice: "primary",
    essay: "success",
    fill: "warning",
    truefalse: "info",
  };
  return colorMap[cardType] || "primary";
};

const getCardTypeText = (cardType) => {
  const textMap = {
    choice: "选择题",
    essay: "简答题",
    fill: "填空题",
    truefalse: "判断题",
  };
  return textMap[cardType] || "未知题型";
};

const getMasteryStars = (level) => {
  return level || 0;
};

const getMasteryPercentage = (level) => {
  return (level / 5) * 100;
};

const getDueStatusClass = (card) => {
  const mastery = card.nodeMasteryLevel || 0;
  if (mastery <= 2) return "status-danger";
  if (mastery <= 3) return "status-warning";
  return "status-success";
};

const getDueStatusText = (card) => {
  const mastery = card.nodeMasteryLevel || 0;
  if (mastery <= 2) return "今天到期";
  if (mastery <= 3) return "明天到期";
  return "2天后到期";
};

const getLastReviewText = (card) => {
  if (!card.reviewCount || card.reviewCount === 0) {
    return "从未复习";
  }
  const daysAgo = Math.floor(Math.random() * 7) + 1;
  if (daysAgo === 1) return "1天前";
  return `${daysAgo}天前`;
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

const startAllReview = () => {
  if (reviewList.value.length === 0) {
    ElMessage.warning("暂无待复习卡片");
    return;
  }
  answerResults.value = {};
  currentQuestionIndex.value = 0;
  startReview(reviewList.value[0]);
};

const startReview = (card) => {
  const index = reviewList.value.findIndex(c => c.id === card.id);
  currentQuestionIndex.value = index >= 0 ? index : 0;
  currentCard.value = card;
  reviewForm.value = {
    answer: "",
    selectedOption: "",
    duration: 0,
  };
  showResult.value = false;
  reviewResult.value = null;
  reviewDialogVisible.value = true;

  startTime = Date.now();
  if (timerInterval) {
    clearInterval(timerInterval);
  }
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
  answerResults.value = {};
  currentQuestionIndex.value = 0;
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
};

const handleOptionClick = (optionKey) => {
  if (showResult.value) return;
  reviewForm.value.selectedOption = optionKey;
};

const getQuestionStatus = (cardId) => {
  if (answerResults.value[cardId]) {
    return answerResults.value[cardId].isCorrect ? 'correct' : 'wrong';
  }
  return 'unanswered';
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
    
    answerResults.value[currentCard.value.id] = {
      isCorrect: result.isCorrect,
      userAnswer: answer,
      correctAnswer: result.correctAnswer,
    };

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

const prevQuestion = () => {
  if (currentQuestionIndex.value === 0) return;
  
  const prevIndex = currentQuestionIndex.value - 1;
  const prevCard = reviewList.value[prevIndex];
  
  currentQuestionIndex.value = prevIndex;
  currentCard.value = prevCard;
  
  const savedResult = answerResults.value[prevCard.id];
  if (savedResult) {
    showResult.value = true;
    reviewResult.value = savedResult;
    reviewForm.value.selectedOption = savedResult.userAnswer || "";
    reviewForm.value.answer = savedResult.userAnswer || "";
  } else {
    showResult.value = false;
    reviewResult.value = null;
    reviewForm.value = {
      answer: "",
      selectedOption: "",
      duration: 0,
    };
    startTime = Date.now();
    if (timerInterval) {
      clearInterval(timerInterval);
    }
    timerInterval = setInterval(() => {
      reviewForm.value.duration = Math.floor((Date.now() - startTime) / 1000);
    }, 1000);
  }
};

const nextQuestion = () => {
  if (currentQuestionIndex.value >= reviewList.value.length - 1) return;
  
  const nextIndex = currentQuestionIndex.value + 1;
  const nextCard = reviewList.value[nextIndex];
  
  currentQuestionIndex.value = nextIndex;
  currentCard.value = nextCard;
  
  const savedResult = answerResults.value[nextCard.id];
  if (savedResult) {
    showResult.value = true;
    reviewResult.value = savedResult;
    reviewForm.value.selectedOption = savedResult.userAnswer || "";
    reviewForm.value.answer = savedResult.userAnswer || "";
  } else {
    showResult.value = false;
    reviewResult.value = null;
    reviewForm.value = {
      answer: "",
      selectedOption: "",
      duration: 0,
    };
    startTime = Date.now();
    if (timerInterval) {
      clearInterval(timerInterval);
    }
    timerInterval = setInterval(() => {
      reviewForm.value.duration = Math.floor((Date.now() - startTime) / 1000);
    }, 1000);
  }
};

const jumpToQuestion = (index) => {
  if (index < 0 || index >= reviewList.value.length) return;
  
  const targetCard = reviewList.value[index];
  currentQuestionIndex.value = index;
  currentCard.value = targetCard;
  
  const savedResult = answerResults.value[targetCard.id];
  if (savedResult) {
    showResult.value = true;
    reviewResult.value = savedResult;
    reviewForm.value.selectedOption = savedResult.userAnswer || "";
    reviewForm.value.answer = savedResult.userAnswer || "";
  } else {
    showResult.value = false;
    reviewResult.value = null;
    reviewForm.value = {
      answer: "",
      selectedOption: "",
      duration: 0,
    };
    startTime = Date.now();
    if (timerInterval) {
      clearInterval(timerInterval);
    }
    timerInterval = setInterval(() => {
      reviewForm.value.duration = Math.floor((Date.now() - startTime) / 1000);
    }, 1000);
  }
};

const finishReview = () => {
  reviewDialogVisible.value = false;
  loadReviewCards();
  loadGlobalStatistics();
  
  const total = reviewList.value.length;
  const correct = correctCount.value;
  const accuracy = total > 0 ? Math.round((correct / total) * 100) : 0;
  
  ElMessage.success(`复习完成！共 ${total} 题，正确 ${correct} 题，正确率 ${accuracy}%`);
};

const nextReview = () => {
  nextQuestion();
};

const handleReviewSettings = () => {
  ElMessage.info("复习设置功能开发中");
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
.review-page {
  min-height: 100%;
  background: var(--bg-page);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--spacing-xl) 0;
  margin-bottom: var(--spacing-lg);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--text-muted);
  margin: 0;
}

.header-right .el-button {
  color: var(--text-secondary);
}

.banner-section {
  background: linear-gradient(135deg, #7c3aed 0%, #8b5cf6 50%, #a78bfa 100%);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  margin-bottom: var(--spacing-lg);
  position: relative;
  overflow: hidden;
}

.banner-section::before {
  content: "";
  position: absolute;
  top: -50%;
  right: -20%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.banner-left {
  flex: 1;
}

.banner-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: white;
  margin: 0 0 var(--spacing-sm) 0;
}

.banner-desc {
  font-size: var(--font-size-base);
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

.banner-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.calendar-icon {
  background: white;
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
  text-align: center;
  box-shadow: var(--shadow-md);
}

.calendar-date {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: #7c3aed;
  line-height: 1;
}

.calendar-month {
  font-size: var(--font-size-xs);
  color: #94a3b8;
  margin-top: 4px;
}

.bot-icon {
  position: relative;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.stat-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.blue {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.stat-icon.green {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.stat-icon.purple {
  background: rgba(124, 58, 237, 0.1);
  color: #7c3aed;
}

.stat-icon.orange {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.stat-icon.green-light {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-bottom: 4px;
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-unit {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.stat-change {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-top: 4px;
}

.stat-change .increase {
  color: #22c55e;
  font-weight: var(--font-weight-medium);
}

.stat-progress {
  margin-top: 8px;
}

.progress-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: 4px;
}

.progress-fill {
  height: 100%;
  background: #22c55e;
  border-radius: var(--radius-full);
  transition: width var(--transition-base);
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.stat-circle {
  position: absolute;
  right: var(--spacing-lg);
  top: 50%;
  transform: translateY(-50%);
}

.circle-svg {
  width: 40px;
  height: 40px;
}

.main-content {
  display: flex;
  gap: var(--spacing-lg);
}

.plan-sidebar {
  width: 380px;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.plan-card,
.chart-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.card-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.plan-chart {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart-ring {
  position: relative;
  margin-bottom: var(--spacing-lg);
}

.ring-svg {
  width: 140px;
  height: 140px;
}

.ring-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.ring-number {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  line-height: 1;
}

.ring-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-top: 4px;
}

.chart-legend {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.legend-text {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.legend-value {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.curve-chart {
  margin-bottom: var(--spacing-md);
}

.curve-svg {
  width: 100%;
  height: 120px;
}

.chart-legend-row {
  display: flex;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
}

.tip-box {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  background: rgba(245, 158, 11, 0.1);
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.queue-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.queue-header {
  margin-bottom: var(--spacing-md);
}

.queue-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.title-text {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.queue-actions {
  display: flex;
  gap: var(--spacing-xs);
}

.queue-actions .el-button {
  padding: 4px 12px;
}

.queue-actions .el-button.active {
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
}

.queue-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-lighter);
}

.queue-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.queue-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  display: flex;
  gap: var(--spacing-md);
}

.queue-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.card-checkbox {
  flex-shrink: 0;
}

.card-content {
  flex: 1;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.card-menu {
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-base), color var(--transition-base);
}

.card-menu:hover {
  background: var(--bg-input);
  color: var(--text-primary);
}

.card-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  margin: 0 0 var(--spacing-md) 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
}

.meta-label {
  color: var(--text-muted);
  flex-shrink: 0;
}

.mastery-bar {
  width: 100px;
  height: 6px;
  background: #e2e8f0;
  border-radius: var(--radius-full);
  overflow: hidden;
}

.mastery-fill {
  height: 100%;
  background: #7c3aed;
  border-radius: var(--radius-full);
}

.meta-item.status-danger {
  color: #ef4444;
}

.meta-item.status-warning {
  color: #f97316;
}

.meta-item.status-success {
  color: #22c55e;
}

.card-footer {
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-lighter);
}

.last-review {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.queue-footer {
  text-align: center;
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-lighter);
  margin-top: var(--spacing-lg);
}

.view-all {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  cursor: pointer;
}

.view-all:hover {
  color: var(--color-primary);
}

.review-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  background: var(--gradient-primary);
  border-bottom: none;
}

.review-dialog :deep(.el-dialog__title) {
  color: white;
  font-size: var(--font-size-xl);
  font-weight: 600;
}

.review-dialog :deep(.el-dialog__body) {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.review-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid var(--border-lighter);
}

.review-dialog-content {
  color: var(--text-primary);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--border-lighter);
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
  font-size: var(--font-size-xl);
  color: var(--text-primary);
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
  color: var(--color-primary);
  font-size: var(--font-size-lg);
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
  border: 2px solid var(--border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base), background var(--transition-base), border-color var(--transition-base), color var(--transition-base);
  background: white;
}

.choice-option:hover:not(.correct-answer):not(.wrong-answer) {
  border-color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.choice-option.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.choice-option.correct-answer {
  border-color: var(--color-success);
  background: rgba(103, 194, 58, 0.1);
}

.choice-option.wrong-answer {
  border-color: var(--color-danger);
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
  border-radius: var(--radius-full);
  background: var(--border-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: var(--text-primary);
  flex-shrink: 0;
}

.choice-option.correct-answer .option-letter {
  background: var(--color-success);
  color: white;
}

.choice-option.wrong-answer .option-letter {
  background: var(--color-danger);
  color: white;
}

.option-content {
  flex: 1;
  color: var(--text-regular);
  font-size: var(--font-size-md);
  line-height: 1.5;
}

.option-explanation {
  margin-top: 8px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary);
}

.explanation-label {
  font-size: var(--font-size-sm);
  font-weight: bold;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.explanation-text {
  font-size: var(--font-size-base);
  color: var(--text-regular);
  line-height: 1.5;
}

.dialog-result-summary {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: var(--radius-md);
  border: 2px solid var(--border-light);
}

.result-summary-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.result-summary-header h3 {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: bold;
}

.result-summary-header.correct h3 {
  color: var(--color-success);
}

.result-summary-header.incorrect h3 {
  color: var(--color-danger);
}

.result-summary-content {
  padding: 12px;
  background: white;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-primary);
}

.result-summary-label {
  font-size: var(--font-size-base);
  font-weight: bold;
  color: var(--color-primary);
  margin-bottom: 8px;
}

.result-summary-text {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  line-height: 1.6;
}

.dialog-timer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: var(--bg-list-item);
  border-radius: 4px;
}

.timer-display {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-lg);
  font-weight: bold;
  color: var(--color-primary);
}

.timer-info {
  color: var(--text-muted);
  font-size: var(--font-size-xs);
  display: flex;
  align-items: center;
  gap: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-lighter);
}

:deep(.el-empty) {
  background: transparent;
}

:deep(.el-empty__description p) {
  color: var(--text-muted);
}

:deep(.el-button--primary) {
  background: var(--gradient-primary);
  border: none;
}

.immersive-dialog :deep(.el-dialog__header) {
  display: none;
}

.immersive-dialog :deep(.el-dialog__body) {
  padding: 0;
  max-height: 90vh;
  overflow: hidden;
}

.immersive-dialog :deep(.el-dialog__footer) {
  display: none;
}

.immersive-review-container {
  display: flex;
  height: 90vh;
  background: var(--bg-page);
}

.review-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 32px 40px;
  overflow-y: auto;
  background: white;
  border-right: 1px solid var(--border-lighter);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--border-lighter);
}

.question-badges {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.question-number {
  font-size: var(--font-size-lg);
  font-weight: bold;
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
  padding: 6px 12px;
  border-radius: var(--radius-md);
}

.question-timer {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-lg);
  font-weight: bold;
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
  padding: 8px 16px;
  border-radius: var(--radius-md);
}

.question-content {
  margin-bottom: 32px;
}

.question-title {
  font-size: var(--font-size-2xl);
  color: var(--text-primary);
  line-height: 1.6;
  margin: 0;
  font-weight: 600;
}

.answer-section {
  margin-bottom: 24px;
}

.result-banner {
  margin-bottom: 24px;
  padding: 20px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  gap: 16px;
}

.result-banner.correct {
  background: rgba(103, 194, 58, 0.1);
  border: 2px solid var(--color-success);
}

.result-banner.wrong {
  background: rgba(245, 108, 108, 0.1);
  border: 2px solid var(--color-danger);
}

.result-banner-icon {
  font-size: 32px;
}

.result-banner.correct .result-banner-icon {
  color: var(--color-success);
}

.result-banner.wrong .result-banner-icon {
  color: var(--color-danger);
}

.result-banner-text {
  flex: 1;
}

.result-banner-title {
  font-size: var(--font-size-xl);
  font-weight: bold;
  margin-bottom: 4px;
}

.result-banner.correct .result-banner-title {
  color: var(--color-success);
}

.result-banner.wrong .result-banner-title {
  color: var(--color-danger);
}

.result-banner-answer {
  font-size: var(--font-size-base);
  color: var(--text-regular);
}

.question-nav-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 24px;
  border-top: 2px solid var(--border-lighter);
}

.question-nav-buttons .submit-btn {
  min-width: 140px;
}

.review-sidebar {
  width: 300px;
  display: flex;
  flex-direction: column;
  padding: 24px;
  gap: 20px;
  background: var(--bg-page);
  overflow-y: auto;
}

.sidebar-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
}

.sidebar-title {
  font-size: var(--font-size-lg);
  font-weight: bold;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.question-number-item {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  background: var(--bg-list-item);
  color: var(--text-secondary);
  border: 2px solid transparent;
}

.question-number-item:hover {
  transform: scale(1.05);
}

.question-number-item.current {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.question-number-item.answered {
  background: var(--border-light);
  color: var(--text-primary);
}

.question-number-item.correct {
  background: var(--color-success);
  color: white;
  border-color: var(--color-success);
}

.question-number-item.wrong {
  background: var(--color-danger);
  color: white;
  border-color: var(--color-danger);
}

.progress-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.progress-item {
  text-align: center;
  padding: 12px;
  background: var(--bg-list-item);
  border-radius: var(--radius-md);
}

.progress-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  display: block;
  margin-bottom: 4px;
}

.progress-value {
  font-size: var(--font-size-xl);
  font-weight: bold;
}

.answered-count {
  color: var(--color-primary);
}

.correct-count {
  color: var(--color-success);
}

.wrong-count {
  color: var(--color-danger);
}

.progress-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar-bg {
  flex: 1;
  height: 8px;
  background: var(--border-light);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: var(--radius-full);
  transition: width var(--transition-base);
}

.progress-percent {
  font-size: var(--font-size-sm);
  font-weight: bold;
  color: var(--color-primary);
  min-width: 40px;
  text-align: right;
}

@media (max-width: 1200px) {
  .stats-section {
    grid-template-columns: repeat(3, 1fr);
  }
  .main-content {
    flex-direction: column;
  }
  .plan-sidebar {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .banner-content {
    flex-direction: column;
    text-align: center;
  }
  .banner-right {
    margin-top: var(--spacing-lg);
  }
}
</style>