<template>
  <div class="review-page">
    <!-- ============ 列表视图（默认） ============ -->
    <template v-if="reviewMode === 'list'">
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
            <div class="calendar-date">{{ todayDate }}</div>
            <div class="calendar-month">{{ todayMonth }}</div>
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
          <div class="stat-change">
            较昨日
            <span v-if="pendingDiff > 0" class="increase">↑{{ pendingDiff }}</span>
            <span v-else-if="pendingDiff < 0" class="decrease">↓{{ Math.abs(pendingDiff) }}</span>
            <span v-else>持平</span>
          </div>
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
          <div class="stat-change">
            较昨日
            <span v-if="accuracyDiff > 0" class="increase">↑{{ accuracyDiff }}%</span>
            <span v-else-if="accuracyDiff < 0" class="decrease">↓{{ Math.abs(accuracyDiff) }}%</span>
            <span v-else>持平</span>
          </div>
          <div class="stat-circle">
            <svg viewBox="0 0 60 60" class="circle-svg">
              <circle cx="30" cy="30" r="26" fill="none" stroke="#e2e8f0" stroke-width="6"/>
              <circle cx="30" cy="30" r="26" fill="none" stroke="#7c3aed" stroke-width="6" :stroke-dasharray="circumference" :stroke-dashoffset="circumference - (totalAccuracy / 100) * circumference" :stroke-linecap="accuracyLinecap" transform="rotate(-90 30 30)"/>
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
          <div class="stat-change">
            较上周
            <span v-if="memoryRetentionDiff > 0" class="increase">↑{{ memoryRetentionDiff }}%</span>
            <span v-else-if="memoryRetentionDiff < 0" class="decrease">↓{{ Math.abs(memoryRetentionDiff) }}%</span>
            <span v-else>持平</span>
          </div>
        </div>
      </div>
    </div>

    <div class="review-tabs">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="我的复习计划" name="myPlan" />
        <el-tab-pane v-if="currentWorkspaceId" label="工作区题目池" name="pool" />
      </el-tabs>
    </div>

    <div v-if="activeTab === 'myPlan'" class="main-content">
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
                <circle
                  v-for="(seg, i) in ringSegments"
                  :key="i"
                  cx="60"
                  cy="60"
                  r="50"
                  fill="none"
                  :stroke="seg.color"
                  stroke-width="12"
                  :stroke-dasharray="ringCircumference"
                  :stroke-dashoffset="ringCircumference - (seg.cumPercent / 100) * ringCircumference"
                  stroke-linecap="round"
                  transform="rotate(-90 60 60)"
                />
              </svg>
              <div class="ring-center">
                <div class="ring-number">{{ reviewList.length }}</div>
                <div class="ring-label">待复习</div>
              </div>
            </div>
            <div class="chart-legend">
              <div class="legend-item">
                <span class="legend-color" style="background: #7c3aed"></span>
                <span class="legend-text">新卡片</span>
                <span class="legend-value">{{ categoryCounts.new }} ({{ calcPercent(categoryCounts.new) }}%)</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #3b82f6"></span>
                <span class="legend-text">学习中</span>
                <span class="legend-value">{{ categoryCounts.learning }} ({{ calcPercent(categoryCounts.learning) }}%)</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #f97316"></span>
                <span class="legend-text">即将遗忘</span>
                <span class="legend-value">{{ categoryCounts.forgot }} ({{ calcPercent(categoryCounts.forgot) }}%)</span>
              </div>
              <div class="legend-item">
                <span class="legend-color" style="background: #22c55e"></span>
                <span class="legend-text">已掌握</span>
                <span class="legend-value">{{ categoryCounts.mastered }} ({{ calcPercent(categoryCounts.mastered) }}%)</span>
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
            <el-button type="text" size="small" :class="{ active: filterStatus === 'new' }" @click="filterStatus = 'new'">新卡片 <el-tag v-if="categoryCounts.new > 0" size="mini">{{ categoryCounts.new }}</el-tag></el-button>
            <el-button type="text" size="small" :class="{ active: filterStatus === 'learning' }" @click="filterStatus = 'learning'">学习中 <el-tag v-if="categoryCounts.learning > 0" size="mini">{{ categoryCounts.learning }}</el-tag></el-button>
            <el-button type="text" size="small" :class="{ active: filterStatus === 'forgot' }" @click="filterStatus = 'forgot'">即将遗忘 <el-tag v-if="categoryCounts.forgot > 0" size="mini">{{ categoryCounts.forgot }}</el-tag></el-button>
            <el-button type="text" size="small" :class="{ active: filterStatus === 'mastered' }" @click="filterStatus = 'mastered'">已掌握 <el-tag v-if="categoryCounts.mastered > 0" size="mini">{{ categoryCounts.mastered }}</el-tag></el-button>
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
                <el-tag :type="getSystemTagType(card.systemId || card.nodeId)" size="small" effect="light">
                  {{ card.nodeTitle || getSystemName(card.systemId) || "未关联" }}
                </el-tag>
                <button
                  class="card-menu"
                  type="button"
                  aria-label="设置知识点复习提醒"
                  @click.stop="handleCardMenu(card)"
                >
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

    <!-- 工作区题目池 -->
    <div v-if="activeTab === 'pool'" class="pool-content" v-loading="poolLoading">
      <div class="pool-header">
        <div class="pool-title-row">
          <span class="pool-title">题目池</span>
          <span class="pool-count">{{ poolList.length }} 道题目</span>
        </div>
      </div>

      <div v-if="poolList.length > 0" class="pool-list">
        <div v-for="item in poolList" :key="item.id" class="pool-card">
          <div class="pool-card-body">
            <div class="pool-card-top">
              <el-tag size="small" type="info">{{ getCardTypeText(item.cardType) }}</el-tag>
              <el-rate :model-value="item.difficulty" disabled :max="5" size="small" />
              <span v-if="item.communityLabel" class="community-tag">
                <span :class="'community-dot ' + item.communityLabel"></span>
                {{ item.communityText }}
              </span>
              <span class="member-count">{{ item.memberCount || 0 }}人复习</span>
            </div>
            <div class="pool-question">{{ item.questionPreview }}</div>
            <div class="pool-card-bottom">
              <span class="pool-meta">来自: {{ item.nodeTitle || '未知知识点' }}</span>
              <div class="pool-actions">
                <el-button
                  v-if="!item.isJoined"
                  type="primary"
                  size="small"
                  @click="handleJoinPool(item)"
                >加入复习</el-button>
                <template v-else>
                  <el-tag type="success" size="small">已加入</el-tag>
                  <el-button
                    type="warning"
                    size="small"
                    text
                    @click="handleRejoinPool(item)"
                  >重新加入</el-button>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="题目池暂无题目，确认知识点入库并选择生成卡片后会自动加入池子" />
    </div>
    </template>

    <!-- ============ 沉浸式做题视图（取代弹窗） ============ -->
    <section v-else-if="reviewMode === 'focus' && currentCard" class="focus-view" @keydown="onFocusKeydown" tabindex="-1">
      <header class="focus-topbar">
        <button class="focus-exit" @click="closeReviewDialog" aria-label="退出复习">
          <el-icon size="18"><ArrowLeft /></el-icon>
          <span>退出</span>
        </button>
        <div class="focus-progress">
          <span class="focus-counter">
            <strong>{{ currentQuestionIndex + 1 }}</strong> / {{ reviewList.length }}
          </span>
          <div class="focus-progress-bar">
            <div class="focus-progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
        </div>
        <div class="focus-timer" v-if="!showResult">
          <el-icon><Timer /></el-icon>
          <span>{{ formatTime(reviewForm.duration) }}</span>
        </div>
      </header>

      <div class="focus-body">
        <main class="focus-main">
          <Transition name="q-slide" mode="out-in">
            <div :key="currentCard.id" class="focus-card">
              <div class="focus-badges">
                <span class="focus-badge" :class="`badge-${currentCard.cardType}`">
                  {{ currentCard.cardType === 'choice' ? '选择题' : currentCard.cardType === 'essay' ? '简答题' : currentCard.cardType === 'fill' ? '填空题' : '判断题' }}
                </span>
                <span class="focus-badge badge-gen" :class="currentCard.generationType === 'manual' ? 'badge-manual' : 'badge-formal'">
                  {{ currentCard.generationType === 'manual' ? '手动练习' : '正式复习' }}
                </span>
                <el-rate v-model="currentCard.difficulty" disabled show-score text-color="#B8723A" :max="5" size="small" />
              </div>

              <div class="focus-question">
                <span class="focus-quote">“</span>
                <h2 class="focus-question-title">{{ parseQuestionText(currentCard.question) }}</h2>
              </div>

              <div class="focus-answer">
                <div class="focus-answer-label">
                  <el-icon><Edit /></el-icon>
                  <span>你的答案</span>
                </div>

                <div v-if="currentCard.cardType === 'choice'" class="focus-options">
                  <button
                    v-for="option in parseChoiceOptions(currentCard.question)"
                    :key="option.key"
                    type="button"
                    class="focus-option"
                    :class="{
                      selected: reviewForm.selectedOption === option.key,
                      'is-correct': showResult && option.key === reviewResult?.correctAnswer,
                      'is-wrong': showResult && reviewForm.selectedOption === option.key && option.key !== reviewResult?.correctAnswer,
                    }"
                    @click="handleOptionClick(option.key)"
                  >
                    <span class="focus-option-letter">{{ option.key }}</span>
                    <span class="focus-option-text">{{ option.content }}</span>
                    <span class="focus-option-icon" v-if="showResult">
                      <el-icon v-if="option.key === reviewResult?.correctAnswer" color="#3B7D5A" size="18"><CircleCheck /></el-icon>
                      <el-icon v-else-if="reviewForm.selectedOption === option.key" color="#B8443A" size="18"><CircleClose /></el-icon>
                    </span>
                  </button>
                </div>

                <el-input
                  v-else
                  v-model="reviewForm.answer"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入你的答案..."
                  size="large"
                  :disabled="showResult"
                  class="focus-essay-input"
                />
              </div>

              <Transition name="result-pop">
                <div v-if="showResult && reviewResult" class="focus-result" :class="reviewResult.isCorrect ? 'result-correct' : 'result-wrong'">
                  <div class="focus-result-icon">
                    <el-icon :size="28">
                      <component :is="reviewResult.isCorrect ? CircleCheck : CircleClose" />
                    </el-icon>
                  </div>
                  <div class="focus-result-text">
                    <h3>{{ reviewResult.isCorrect ? '回答正确' : '回答错误' }}</h3>
                    <p v-if="reviewResult.correctAnswer">正确答案：<strong>{{ reviewResult.correctAnswer }}</strong></p>
                  </div>
                </div>
              </Transition>

              <Transition name="result-fade">
                <div v-if="showResult && reviewResult?.explanation" class="focus-explanation">
                  <div class="focus-explanation-label">
                    <el-icon><InfoFilled /></el-icon>
                    <span>答案解析</span>
                  </div>
                  <div class="focus-explanation-text">{{ reviewResult.explanation }}</div>
                </div>
              </Transition>
            </div>
          </Transition>

          <div class="focus-actions">
            <el-button :disabled="currentQuestionIndex === 0" @click="prevQuestion" size="large" class="focus-nav-btn">
              <el-icon><ArrowLeft /></el-icon>
              上一题
            </el-button>
            <el-button
              v-if="!showResult"
              type="primary"
              @click="submitReview"
              size="large"
              :loading="submitting"
              class="focus-submit-btn"
            >
              提交答案
            </el-button>
            <el-button
              v-else-if="currentQuestionIndex < reviewList.length - 1"
              type="primary"
              @click="nextQuestion"
              size="large"
              class="focus-submit-btn"
            >
              下一题
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button
              v-else
              type="success"
              @click="finishReview"
              size="large"
              class="focus-submit-btn"
            >
              完成复习
              <el-icon><CircleCheck /></el-icon>
            </el-button>
          </div>
        </main>

        <aside class="focus-sidebar">
          <div class="focus-side-card">
            <div class="focus-side-title">题目导航</div>
            <div class="focus-question-grid">
              <button
                v-for="(card, index) in reviewList"
                :key="card.id"
                type="button"
                class="focus-qnum"
                :class="{
                  current: index === currentQuestionIndex,
                  answered: getQuestionStatus(card.id) === 'answered',
                  correct: getQuestionStatus(card.id) === 'correct',
                  wrong: getQuestionStatus(card.id) === 'wrong',
                }"
                @click="jumpToQuestion(index)"
              >
                {{ index + 1 }}
              </button>
            </div>
          </div>

          <div class="focus-side-card">
            <div class="focus-side-title">答题进度</div>
            <div class="focus-stats">
              <div class="focus-stat">
                <span class="focus-stat-label">已答</span>
                <span class="focus-stat-value stat-answered">{{ answeredCount }}</span>
              </div>
              <div class="focus-stat">
                <span class="focus-stat-label">正确</span>
                <span class="focus-stat-value stat-correct">{{ correctCount }}</span>
              </div>
              <div class="focus-stat">
                <span class="focus-stat-label">错误</span>
                <span class="focus-stat-value stat-wrong">{{ wrongCount }}</span>
              </div>
            </div>
            <div class="focus-percent-row">
              <span class="focus-percent">{{ progressPercent }}%</span>
            </div>
          </div>
        </aside>
      </div>
    </section>
    <ReviewPreferencesDialog v-model="showReviewSettings" />
    <KnowledgeReviewReminderDialog
      v-model="showReminderDialog"
      :node="reminderNode"
    />
    </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { reviewAPI } from "@/api/review";
import { statisticsAPI } from "@/api/statistics";
import CheckInButton from "@/components/CheckInButton.vue";
import ReviewPreferencesDialog from "@/components/ReviewPreferencesDialog.vue";
import KnowledgeReviewReminderDialog from "@/components/KnowledgeReviewReminderDialog.vue";
import { useWorkspaceStore } from "@/stores/workspace";
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
// 复习模式：'list' 列表视图 / 'focus' 沉浸式做题视图（取代原弹窗）
const reviewMode = ref("list");
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
const totalAccuracy = ref(0);
const streakDays = ref(0);
const maxStreak = ref(0);
const memoryRetention = ref(0);

// 紫框右上角日历图标真实日期
const todayDate = ref(new Date().getDate());
const todayMonth = ref(`${new Date().getMonth() + 1}月`);

// 统计卡片的增减差值
const pendingDiff = ref(0);
const completedDiff = ref(0);
const accuracyDiff = ref(0);
const memoryRetentionDiff = ref(0);

// 队列分类数量（新卡片/学习中/即将遗忘/已掌握）
const categoryCounts = ref({ new: 0, learning: 0, forgot: 0, mastered: 0 });

// 工作区题目池
const workspaceStore = useWorkspaceStore();
const activeTab = ref("myPlan");
const poolList = ref([]);
const poolLoading = ref(false);
const currentWorkspaceId = computed(() => workspaceStore.currentId);
const showReviewSettings = ref(false);
const showReminderDialog = ref(false);
const reminderNode = ref(null);

let timerInterval = null;
let startTime = null;

const circumference = 2 * Math.PI * 26;
const ringCircumference = 2 * Math.PI * 50;

// 满圈或空圈时用 butt 避免 round 端点造成的凸起/圆点
const accuracyLinecap = computed(() => {
  const a = Number(totalAccuracy.value) || 0;
  if (a <= 0.1 || a >= 99.9) return 'butt';
  return 'round';
});

const completionRate = computed(() => {
  const total = (todayPending.value + todayCompleted.value);
  if (total <= 0) return 0;
  return Math.round((todayCompleted.value / total) * 100);
});

// 计算分类数量：前端从真实 reviewList 实时派生，overview 给了也做前端再重算一次（保证与后端 categoryCounts 和前端真实值 + 再校验）
const computeCategoryCounts = () => {
  const list = reviewList.value;
  const c = { new: 0, learning: 0, forgot: 0, mastered: 0 };
  for (const card of list) {
    const mastery = card.nodeMasteryLevel ?? (card.masteryLevel != null ? card.masteryLevel : (card.reviewCount ? 1 : 0));
    if (!card.reviewCount) c.new++;
    if (mastery >= 1 && mastery <= 3) c.learning++;
    if (mastery >= 4) c.mastered++;
    if (mastery <= 2 && card.reviewCount) c.forgot++;
  }
  return c;
};

// 饼图分段（按比例换算成累计百分比画 SVG 圆环）
const ringSegments = computed(() => {
  const cc = computeCategoryCounts();
  const total = Math.max(1, reviewList.value.length);
  const order = [
    { key: "new", color: "#7c3aed" },
    { key: "learning", color: "#3b82f6" },
    { key: "forgot", color: "#f97316" },
    { key: "mastered", color: "#22c55e" },
  ];
  let cum = 0;
  return order.map(o => {
    const value = cc[o.key] || 0;
    const percent = (value / total) * 100;
    cum += percent;
    return { color: o.color, cumPercent: cum };
  });
});

const calcPercent = (value) => {
  const total = Math.max(1, reviewList.value.length);
  return Math.round(((value || 0) / total) * 100);
};

// 真实分类数量（优先用后端 overview 返回值；当页面内用户交互后列表变化时重算覆盖）
const realCategoryCounts = computed(() => {
  const front = computeCategoryCounts();
  const back = categoryCounts.value || {};
  return {
    new: front.new || back.new || 0,
    learning: front.learning || back.learning || 0,
    forgot: front.forgot || back.forgot || 0,
    mastered: front.mastered || back.mastered || 0,
  };
});

// 给 template 用：统一走 realCategoryCounts 响应式别名
// NOTE: 由于模板直接使用 categoryCounts，这里通过 watch 保持两者一致
watch(
  () => reviewList.value.length,
  () => {
    if (reviewList.value && reviewList.value.length > 0) {
      categoryCounts.value = computeCategoryCounts();
    }
  },
  { immediate: true }
);

const filteredReviewList = computed(() => {
  let filtered = [...reviewList.value];

  switch (filterStatus.value) {
    case "new":
      filtered = filtered.filter(card => !card.reviewCount);
      break;
    case "learning": {
      filtered = filtered.filter(card => {
        const mastery = card.nodeMasteryLevel ?? (card.masteryLevel != null ? card.masteryLevel : (card.reviewCount ? 1 : 0));
        return mastery >= 1 && mastery <= 3;
      });
      break;
    }
    case "forgot": {
      filtered = filtered.filter(card => {
        const mastery = card.nodeMasteryLevel ?? (card.masteryLevel != null ? card.masteryLevel : (card.reviewCount ? 1 : 0));
        return mastery <= 2 && card.reviewCount;
      });
      break;
    }
    case "mastered": {
      filtered = filtered.filter(card => {
        const mastery = card.nodeMasteryLevel ?? (card.masteryLevel != null ? card.masteryLevel : (card.reviewCount ? 1 : 0));
        return mastery >= 4;
      });
      break;
    }
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

    // 并行请求 overview + 今日卡片（避免原串行多次请求）
    const [cards, overview] = await Promise.all([
      reviewAPI.getTodayReviewCards(sortBy.value),
      reviewAPI.getOverview().catch((e) => {
        console.warn("load_overview_failed fallback", e);
        return null;
      }),
    ]);

    reviewList.value = cards || [];

    // 先按真实 cards 填充基础计数
    const totalFromCards = reviewList.value.length;
    const completedFromCards = reviewList.value.filter(
      (card) => card.reviewCount > 0,
    ).length;
    todayTotal.value = totalFromCards;
    todayCompleted.value = completedFromCards;
    todayPending.value = totalFromCards - completedFromCards;

    // 再用 overview 覆盖更权威的数据
    if (overview) {
      const o = overview || {};
      // 日期图标
      if (o.todayDate != null) todayDate.value = o.todayDate;
      if (o.todayMonth) todayMonth.value = o.todayMonth;

      if (o.todayPending != null) todayPending.value = Number(o.todayPending) || 0;
      if (o.todayCompleted != null) todayCompleted.value = Number(o.todayCompleted) || 0;

      if (o.todayAccuracy != null) totalAccuracy.value = Number(o.todayAccuracy) || 0;
      if (o.currentStreak != null) streakDays.value = Number(o.currentStreak) || 0;
      if (o.maxStreak != null) maxStreak.value = Number(o.maxStreak) || 0;
      if (o.memoryRetention != null) memoryRetention.value = Number(o.memoryRetention) || 0;

      if (o.pendingDiff != null) pendingDiff.value = Number(o.pendingDiff) || 0;
      if (o.completedDiff != null) completedDiff.value = Number(o.completedDiff) || 0;
      if (o.accuracyDiff != null) accuracyDiff.value = Number(o.accuracyDiff) || 0;
      if (o.memoryRetentionDiff != null) memoryRetentionDiff.value = Number(o.memoryRetentionDiff) || 0;

      if (o.categoryCounts && typeof o.categoryCounts === "object") {
        categoryCounts.value = {
          new: Number(o.categoryCounts.new) || 0,
          learning: Number(o.categoryCounts.learning) || 0,
          forgot: Number(o.categoryCounts.forgot) || 0,
          mastered: Number(o.categoryCounts.mastered) || 0,
        };
      }
    } else {
      // overview 失败时兜底：调用旧接口
      try {
        totalAccuracy.value = (await reviewAPI.getUserAccuracy()) || 0;
        streakDays.value = (await reviewAPI.getStreakDays()) || 0;
      } catch (_) {
        /* ignore */
      }
      try {
        const response = await statisticsAPI.getStatistics();
        if (response && response.completedReviewCount != null) {
          /* 仅用于未来扩展 */
        }
      } catch (_) {
        /* ignore */
      }
    }
  } catch (error) {
    ElMessage.error("加载复习卡片失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const overallAccuracy = ref(0);
const totalCompleted = ref(0);

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
  // 基于后端返回的 lastReviewTime（真实数据，不再随机）
  if (!card.lastReviewTime) {
    return `${card.reviewCount}次复习`;
  }
  const t = new Date(card.lastReviewTime).getTime();
  if (isNaN(t)) return `${card.reviewCount}次复习`;
  const diffMs = Date.now() - t;
  const diffDay = Math.floor(diffMs / 86400000);
  if (diffDay <= 0) {
    const diffHour = Math.floor(diffMs / 3600000);
    if (diffHour <= 0) {
      const diffMin = Math.max(1, Math.floor(diffMs / 60000));
      return `${diffMin}分钟前`;
    }
    return `${diffHour}小时前`;
  }
  if (diffDay === 1) return "1天前";
  if (diffDay < 30) return `${diffDay}天前`;
  const diffMon = Math.floor(diffDay / 30);
  return `${diffMon}个月前`;
};

const handleCardMenu = (card) => {
  if (!card?.nodeId) {
    ElMessage.warning("这张卡片没有可设置提醒的知识点");
    return;
  }
  reminderNode.value = { id: card.nodeId, title: card.nodeTitle || "关联知识点" };
  showReminderDialog.value = true;
};

const parseQuestionText = (question) => {
  if (!question) return "";
  const raw = typeof question === "string" ? question : String(question ?? "");
  const lines = raw.split("\n");
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

  const cleaned = questionText.trim();
  if (cleaned) return cleaned;
  // 兜底：如果只剩选项没有题干，展示前 80 字符避免卡片标题为空
  return raw.trim().slice(0, 80) || "";
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
  const valid = getValidReviewList();
  if (valid.length === 0) {
    ElMessage.warning("暂无待复习卡片");
    return;
  }
  answerResults.value = {};
  currentQuestionIndex.value = 0;
  startReview(valid[0]);
};

// 从展示的 reviewList 中再做一次安全过滤（避免脏数据即使漏到前端也点击无响应）
const getValidReviewList = () => {
  return reviewList.value.filter(card => {
    if (!card || card.id == null) return false;
    const q = parseQuestionText(card.question);
    return !!q;
  });
};

const startReview = (card) => {
  if (!card || card.id == null) {
    ElMessage.warning("此卡片内容不完整，暂时无法复习");
    return;
  }
  if (!parseQuestionText(card.question)) {
    ElMessage.warning("此卡片题目为空，暂时无法复习");
    return;
  }
  const valid = getValidReviewList();
  const index = valid.findIndex(c => c.id === card.id);
  // 用 valid 做索引，避免被过滤掉的脏卡占位置导致 index 错乱
  currentQuestionIndex.value = index >= 0 ? index : 0;
  currentCard.value = card;
  reviewForm.value = {
    answer: "",
    selectedOption: "",
    duration: 0,
  };
  showResult.value = false;
  reviewResult.value = null;
  // 进入沉浸式做题视图（不再使用弹窗）
  reviewMode.value = "focus";

  startTime = Date.now();
  if (timerInterval) {
    clearInterval(timerInterval);
  }
  timerInterval = setInterval(() => {
    reviewForm.value.duration = Math.floor((Date.now() - startTime) / 1000);
  }, 1000);
};

const closeReviewDialog = () => {
  // 退出沉浸式做题视图，回到列表
  reviewMode.value = "list";
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

// 沉浸式做题视图键盘导航：1-4 选选项 / Enter 提交 / ←→ 切换题目
const onFocusKeydown = (e) => {
  const card = currentCard.value;
  if (!card) return;
  const key = e.key;

  if (card.cardType === "choice") {
    if (["1", "2", "3", "4"].includes(key)) {
      const opts = parseChoiceOptions(card.question);
      const idx = Number(key) - 1;
      if (opts[idx]) {
        handleOptionClick(opts[idx].key);
        e.preventDefault();
      }
    } else if (["a", "b", "c", "d", "A", "B", "C", "D"].includes(key)) {
      const opts = parseChoiceOptions(card.question);
      const target = opts.find(o => o.key === key.toUpperCase());
      if (target) {
        handleOptionClick(target.key);
        e.preventDefault();
      }
    }
  }

  if (key === "Enter" && !showResult.value && !submitting.value) {
    submitReview();
    e.preventDefault();
  } else if (key === "ArrowRight" && showResult.value) {
    if (currentQuestionIndex.value < reviewList.value.length - 1) {
      nextQuestion();
      e.preventDefault();
    }
  } else if (key === "ArrowLeft" && currentQuestionIndex.value > 0) {
    prevQuestion();
    e.preventDefault();
  }
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
  // 完成复习：回到列表视图
  reviewMode.value = "list";
  loadReviewCards();

  const total = reviewList.value.length;
  const correct = correctCount.value;
  const accuracy = total > 0 ? Math.round((correct / total) * 100) : 0;
  
  ElMessage.success(`复习完成！共 ${total} 题，正确 ${correct} 题，正确率 ${accuracy}%`);
};

const nextReview = () => {
  nextQuestion();
};

const handleReviewSettings = () => {
  showReviewSettings.value = true;
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

// ========== 题目池操作 ==========

const onTabChange = (tabName) => {
  if (tabName === "pool") {
    loadPoolList();
  } else {
    loadReviewCards();
  }
};

const loadPoolList = async () => {
  if (!currentWorkspaceId.value) return;
  try {
    poolLoading.value = true;
    const data = await reviewAPI.getPoolList(currentWorkspaceId.value);
    poolList.value = data || [];
  } catch (e) {
    ElMessage.error("加载题目池失败");
  } finally {
    poolLoading.value = false;
  }
};

const handleJoinPool = async (item) => {
  try {
    await reviewAPI.joinPool(item.id);
    ElMessage.success("已加入复习计划");
    item.isJoined = true;
    loadReviewCards();
  } catch (e) {
    ElMessage.error("加入失败");
  }
};

const handleRejoinPool = async (item) => {
  try {
    await ElMessageBox.confirm(
      "重新加入将创建一份全新的复习副本，旧进度会保留为历史记录。",
      "确认重新加入",
      { confirmButtonText: "确定", cancelButtonText: "取消", type: "info" }
    );
    await reviewAPI.joinPool(item.id);
    ElMessage.success("已重新加入");
  } catch (e) {
    if (e !== "cancel") ElMessage.error("操作失败");
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
  position: relative;
  min-width: 0;
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

.stat-change .decrease {
  color: #ef4444;
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
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

.card-menu:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
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

/* ============================================================
   沉浸式做题视图（取代 el-dialog）
   遵循 Scholar's Desk 设计系统：sage/brass/parchment + serif
   ============================================================ */
.focus-view {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
  outline: none;
  animation: focus-enter 0.28s ease-out;
}

@keyframes focus-enter {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* —— 顶部栏：退出 / 进度 / 计时 —— */
.focus-topbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-xl);
  padding: var(--spacing-md) var(--spacing-2xl);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-lighter);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}

.focus-exit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  min-height: 40px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.focus-exit:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.focus-exit:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
}

.focus-progress {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  min-width: 0;
}

.focus-counter {
  font-family: var(--font-family-ui);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  white-space: nowrap;
  flex-shrink: 0;
}

.focus-counter strong {
  font-size: var(--font-size-lg);
  color: var(--color-primary);
  font-weight: var(--font-weight-bold);
}

.focus-progress-bar {
  flex: 1;
  height: 6px;
  background: var(--border-lighter);
  border-radius: var(--radius-full);
  overflow: hidden;
  min-width: 80px;
}

.focus-progress-fill {
  height: 100%;
  background: var(--gradient-primary);
  border-radius: var(--radius-full);
  transition: width 0.4s ease;
}

.focus-timer {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

/* —— 主体：左主区 + 右侧栏 —— */
.focus-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.focus-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: var(--spacing-2xl) var(--spacing-3xl);
  overflow-y: auto;
  background: var(--bg-card);
  border-right: 1px solid var(--border-lighter);
}

.focus-card {
  max-width: 760px;
  margin: 0 auto;
  width: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* —— 题型徽章 —— */
.focus-badges {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-wrap: wrap;
  margin-bottom: var(--spacing-xl);
}

.focus-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.02em;
  border: 1px solid transparent;
}

.focus-badge.badge-choice  { background: var(--color-primary-alpha-10); color: var(--color-primary); border-color: var(--color-primary-alpha-20); }
.focus-badge.badge-essay   { background: var(--color-success-bg); color: var(--color-success); border-color: var(--color-success-border); }
.focus-badge.badge-fill    { background: var(--color-warning-bg); color: var(--color-warning); border-color: var(--color-warning-border); }
.focus-badge.badge-judge   { background: var(--color-info-bg); color: var(--color-info); border-color: var(--color-info-border); }
.focus-badge.badge-formal  { background: var(--color-primary-alpha-10); color: var(--color-primary); }
.focus-badge.badge-manual  { background: var(--color-accent-alpha-10); color: var(--color-accent); }

/* —— 题干：编辑风排版，serif display —— */
.focus-question {
  position: relative;
  margin-bottom: var(--spacing-2xl);
  padding-left: var(--spacing-xl);
}

.focus-quote {
  position: absolute;
  left: 0;
  top: -8px;
  font-family: var(--font-family-display);
  font-size: 56px;
  line-height: 1;
  color: var(--color-accent);
  opacity: 0.55;
  user-select: none;
}

.focus-question-title {
  font-family: var(--font-family-display);
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  line-height: 1.45;
  color: var(--text-primary);
  margin: 0;
}

/* —— 答题区 —— */
.focus-answer {
  margin-bottom: var(--spacing-xl);
}

.focus-answer-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: var(--spacing-md);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.focus-options {
  display: grid;
  gap: var(--spacing-md);
}

.focus-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  min-height: 56px;
  padding: 14px 18px;
  border: 1.5px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  text-align: left;
  cursor: pointer;
  font-family: var(--font-family-ui);
  transition: border-color var(--transition-fast), background var(--transition-fast), transform var(--transition-fast);
}

.focus-option:hover:not(.is-correct):not(.is-wrong) {
  border-color: var(--color-primary-light);
  background: var(--color-primary-alpha-10);
}

.focus-option:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
  border-color: var(--color-primary);
}

.focus-option.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.focus-option.is-correct {
  border-color: var(--color-success);
  background: var(--color-success-bg);
}

.focus-option.is-wrong {
  border-color: var(--color-danger);
  background: var(--color-danger-bg);
}

.focus-option-letter {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: var(--bg-list-item);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-sm);
  transition: all var(--transition-fast);
}

.focus-option.selected .focus-option-letter {
  background: var(--color-primary);
  color: var(--text-inverse);
}

.focus-option.is-correct .focus-option-letter {
  background: var(--color-success);
  color: var(--text-inverse);
}

.focus-option.is-wrong .focus-option-letter {
  background: var(--color-danger);
  color: var(--text-inverse);
}

.focus-option-text {
  flex: 1;
  font-size: var(--font-size-md);
  line-height: 1.55;
  color: var(--text-regular);
}

.focus-option-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.focus-essay-input :deep(.el-textarea__inner) {
  font-family: var(--font-family-body);
  font-size: var(--font-size-md);
  line-height: 1.7;
  border-radius: var(--radius-md);
}

/* —— 结果横幅 —— */
.focus-result {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-md);
  border: 1px solid transparent;
}

.focus-result.result-correct {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
}

.focus-result.result-wrong {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
}

.focus-result-icon { display: flex; }
.focus-result.result-correct .focus-result-icon { color: var(--color-success); }
.focus-result.result-wrong .focus-result-icon { color: var(--color-danger); }

.focus-result-text h3 {
  font-family: var(--font-family-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  margin: 0 0 2px;
}

.focus-result.result-correct .focus-result-text h3 { color: var(--color-success); }
.focus-result.result-wrong .focus-result-text h3 { color: var(--color-danger); }

.focus-result-text p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-regular);
}

/* —— 解析 —— */
.focus-explanation {
  padding: var(--spacing-lg) var(--spacing-xl);
  background: var(--bg-list-item);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-accent);
  margin-bottom: var(--spacing-xl);
}

.focus-explanation-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-family-ui);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-accent);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: var(--spacing-sm);
}

.focus-explanation-text {
  font-family: var(--font-family-body);
  font-size: var(--font-size-base);
  line-height: 1.75;
  color: var(--text-regular);
}

/* —— 底部操作栏 —— */
.focus-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-top: auto;
  padding-top: var(--spacing-xl);
  border-top: 1px solid var(--border-lighter);
}

.focus-nav-btn {
  min-width: 120px;
}

.focus-submit-btn {
  min-width: 150px;
  margin-left: auto;
}

/* —— 右侧栏 —— */
.focus-sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  padding: var(--spacing-xl);
  background: var(--bg-page);
  overflow-y: auto;
}

.focus-side-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  border: 1px solid var(--border-lighter);
}

.focus-side-title {
  font-family: var(--font-family-display);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--spacing-md);
  letter-spacing: 0.02em;
}

.focus-question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.focus-qnum {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  border: 1.5px solid transparent;
  background: var(--bg-list-item);
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.focus-qnum:hover { transform: scale(1.06); }

.focus-qnum:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
}

.focus-qnum.current {
  background: var(--color-primary);
  color: var(--text-inverse);
  border-color: var(--color-primary);
}

.focus-qnum.correct {
  background: var(--color-success);
  color: var(--text-inverse);
  border-color: var(--color-success);
}

.focus-qnum.wrong {
  background: var(--color-danger);
  color: var(--text-inverse);
  border-color: var(--color-danger);
}

.focus-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-md);
}

.focus-stat {
  text-align: center;
  padding: 10px 6px;
  background: var(--bg-list-item);
  border-radius: var(--radius-sm);
}

.focus-stat-label {
  display: block;
  font-family: var(--font-family-ui);
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-bottom: 2px;
}

.focus-stat-value {
  font-family: var(--font-family-display);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.focus-stat-value.stat-answered { color: var(--color-primary); }
.focus-stat-value.stat-correct  { color: var(--color-success); }
.focus-stat-value.stat-wrong    { color: var(--color-danger); }

.focus-percent-row {
  text-align: right;
}

.focus-percent {
  font-family: var(--font-family-ui);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

/* —— 过渡：题目切换（exit 快于 enter） —— */
.q-slide-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.q-slide-leave-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.q-slide-enter-from { opacity: 0; transform: translateX(12px); }
.q-slide-leave-to   { opacity: 0; transform: translateX(-12px); }

/* —— 过渡：结果横幅弹出 —— */
.result-pop-enter-active { transition: opacity 0.22s ease, transform 0.22s cubic-bezier(0.34, 1.2, 0.64, 1); }
.result-pop-leave-active { transition: opacity 0.15s ease; }
.result-pop-enter-from { opacity: 0; transform: scale(0.92); }
.result-pop-leave-to   { opacity: 0; }

/* —— 过渡：解析淡入 —— */
.result-fade-enter-active { transition: opacity 0.3s ease 0.05s; }
.result-fade-leave-active { transition: opacity 0.15s ease; }
.result-fade-enter-from, .result-fade-leave-to { opacity: 0; }

/* —— 响应式：窄屏隐藏侧栏 —— */
@media (max-width: 980px) {
  .focus-sidebar { display: none; }
  .focus-main { padding: var(--spacing-xl) var(--spacing-lg); }
  .focus-question-title { font-size: var(--font-size-2xl); }
}

@media (max-width: 640px) {
  .focus-topbar { gap: var(--spacing-md); padding: var(--spacing-sm) var(--spacing-md); }
  .focus-timer { display: none; }
  .focus-actions { flex-wrap: wrap; }
  .focus-submit-btn { margin-left: 0; flex: 1; }
  .focus-nav-btn { flex: 1; }
}

/* 尊重用户的减弱动效偏好 */
@media (prefers-reduced-motion: reduce) {
  .focus-view, .q-slide-enter-active, .q-slide-leave-active,
  .result-pop-enter-active, .result-pop-leave-active,
  .result-fade-enter-active, .result-fade-leave-active {
    animation: none !important;
    transition: none !important;
  }
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

/* ========== 题目池样式 ========== */

.review-tabs {
  max-width: 1200px;
  margin: 0 auto var(--spacing-lg);
  padding: 0 var(--spacing-lg);
}

.review-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.pool-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg) var(--spacing-2xl);
}

.pool-header {
  margin-bottom: var(--spacing-lg);
}

.pool-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pool-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.pool-count {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.pool-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.pool-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s;
}

.pool-card:hover {
  box-shadow: var(--shadow-md);
}

.pool-card-body {
  padding: var(--spacing-lg);
}

.pool-card-top {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-sm);
}

.community-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.community-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.community-dot.green {
  background: #22c55e;
}

.community-dot.yellow {
  background: #eab308;
}

.community-dot.red {
  background: #ef4444;
}

.member-count {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  margin-left: auto;
}

.pool-question {
  font-size: var(--font-size-base);
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: var(--spacing-sm);
}

.pool-card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pool-meta {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.pool-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}
</style>
