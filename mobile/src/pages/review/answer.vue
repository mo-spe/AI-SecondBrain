<script setup>
import { ref, computed } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { reviewAPI } from '@/api/review'
import { parseQuestionText, parseChoiceOptions, getCardTypeText } from '@/utils/review-parser'

const list = ref([])
const index = ref(0)
const loading = ref(true)

// 答题态
const choiceSelected = ref('')
const textAnswer = ref('')
const judgeSelected = ref('') // 对 | 错

const showResult = ref(false)
const result = ref(null)
const submitting = ref(false)

// 每张卡片的结果缓存，供上一题/下一题回填
const answerMap = new Map()

// 计时
let startTime = 0
let timer = null
const duration = ref(0)

const currentCard = computed(() => list.value[index.value] || null)

const options = computed(() => (currentCard.value ? parseChoiceOptions(currentCard.value.question) : []))

const isChoice = computed(() => currentCard.value?.cardType === 'choice')
const isJudge = computed(() => currentCard.value?.cardType === 'judge')

const startTimer = () => {
  startTime = Date.now()
  duration.value = 0
  clearTimer()
  timer = setInterval(() => {
    duration.value = Math.floor((Date.now() - startTime) / 1000)
  }, 1000)
}

const clearTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const load = async (targetId) => {
  loading.value = true
  try {
    const cardList = await reviewAPI.getTodayReviewCards()
    list.value = (cardList || []).filter((c) => c && c.id != null && parseQuestionText(c.question))
    if (list.value.length === 0) {
      uni.redirectTo({ url: '/pages/review/done' })
      return
    }
    const idx = targetId != null ? list.value.findIndex((c) => String(c.id) === String(targetId)) : -1
    index.value = idx >= 0 ? idx : 0
    resetForm()
    startTimer()
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  choiceSelected.value = ''
  textAnswer.value = ''
  judgeSelected.value = ''
  showResult.value = false
  result.value = null
  const cached = answerMap.get(currentCard.value?.id)
  if (cached) {
    showResult.value = true
    result.value = cached
    if (isChoice.value) choiceSelected.value = cached.userAnswer || ''
    else if (isJudge.value) judgeSelected.value = cached.userAnswer || ''
    else textAnswer.value = cached.userAnswer || ''
  }
}

const selectChoice = (key) => {
  if (showResult.value) return
  choiceSelected.value = key
}

const selectJudge = (val) => {
  if (showResult.value) return
  judgeSelected.value = val
}

const currentUserAnswer = () => {
  if (isChoice.value) return choiceSelected.value
  if (isJudge.value) return judgeSelected.value
  return textAnswer.value
}

const submit = async () => {
  if (showResult.value) return next()
  const answer = currentUserAnswer()
  if (!answer) {
    uni.showToast({ title: '请先作答', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const res = await reviewAPI.submitReviewResult({
      cardId: currentCard.value.id,
      userAnswer: answer,
      duration: duration.value,
    })
    result.value = res
    showResult.value = true
    answerMap.set(currentCard.value.id, {
      isCorrect: res.isCorrect,
      userAnswer: answer,
      correctAnswer: res.correctAnswer,
    })
  } catch (e) {
    /* request 已 toast */
  } finally {
    submitting.value = false
  }
}

const next = () => {
  if (index.value >= list.value.length - 1) {
    finish()
    return
  }
  index.value += 1
  resetForm()
  startTimer()
}

const prev = () => {
  if (index.value <= 0) return
  index.value -= 1
  resetForm()
}

const finish = () => {
  clearTimer()
  const answered = Array.from(answerMap.keys())
  uni.redirectTo({ url: `/pages/review/done?total=${list.value.length}&answered=${answered.length}` })
}

const quit = () => {
  clearTimer()
  uni.navigateBack()
}

onLoad((query) => {
  load(query && query.id ? query.id : null)
})

onUnload(clearTimer)
</script>

<template>
  <view class="answer-page">
    <view v-if="loading" class="empty-state">加载中…</view>

    <template v-else-if="currentCard">
      <!-- 顶部进度与计时 -->
      <view class="answer-header">
        <text class="progress-index">{{ index + 1 }} / {{ list.length }}</text>
        <text class="card-type">{{ getCardTypeText(currentCard.cardType) }}</text>
        <text class="timer">{{ Math.floor(duration / 60).toString().padStart(2, '0') }}:{{ (duration % 60).toString().padStart(2, '0') }}</text>
      </view>

      <!-- 题干 -->
      <view class="question-box">
        <text class="question-text">{{ parseQuestionText(currentCard.question) }}</text>
      </view>

      <!-- 选择题选项 -->
      <view v-if="isChoice" class="option-list">
        <view
          v-for="opt in options"
          :key="opt.key"
          class="option-item tap-target"
          :class="{
            selected: choiceSelected === opt.key,
            correct: showResult && result && result.correctAnswer === opt.key,
            wrong: showResult && choiceSelected === opt.key && result && !result.isCorrect,
          }"
          @tap="selectChoice(opt.key)"
        >
          <text class="option-key">{{ opt.key }}</text>
          <text class="option-text">{{ opt.text }}</text>
        </view>
      </view>

      <!-- 判断题 -->
      <view v-else-if="isJudge" class="judge-row">
        <view
          class="judge-item tap-target"
          :class="{ selected: judgeSelected === '对', correct: showResult && result && result.correctAnswer === '对' }"
          @tap="selectJudge('对')"
        >对</view>
        <view
          class="judge-item tap-target"
          :class="{ selected: judgeSelected === '错', correct: showResult && result && result.correctAnswer === '错' }"
          @tap="selectJudge('错')"
        >错</view>
      </view>

      <!-- 简答/填空 -->
      <view v-else class="text-answer">
        <textarea
          v-model="textAnswer"
          class="answer-input"
          placeholder="在此输入你的答案"
          placeholder-class="answer-placeholder"
          :disabled="showResult"
        />
      </view>

      <!-- 作答结果反馈 -->
      <view v-if="showResult && result" class="result-box card">
        <view class="result-head" :class="result.isCorrect ? 'ok' : 'bad'">
          <text class="result-icon">{{ result.isCorrect ? '✓' : '✕' }}</text>
          <text class="result-text">{{ result.isCorrect ? '回答正确' : '回答错误' }}</text>
        </view>
        <view class="correct-answer">
          <text class="ca-label">正确答案</text>
          <text class="ca-value">{{ result.correctAnswer }}</text>
        </view>
        <view v-if="!result.isCorrect" class="your-answer">
          <text class="ca-label">你的答案</text>
          <text class="ca-value">{{ result.userAnswer }}</text>
        </view>
      </view>

      <!-- 底部操作 -->
      <view class="answer-footer">
        <view class="prev-btn tap-target" @tap="prev">
          <text>上一题</text>
        </view>
        <button class="submit-btn tap-target" :loading="submitting" :disabled="submitting" @tap="submit">
          {{ showResult ? (index >= list.length - 1 ? '完成复习' : '下一题') : '提交答案' }}
        </button>
      </view>
    </template>
  </view>
</template>

<style lang="scss" scoped>
.answer-page {
  min-height: 100vh;
  padding: 24rpx 32rpx 200rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.answer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;

  .progress-index {
    font-size: 26rpx;
    color: var(--text-secondary);
    font-weight: 500;
  }

  .card-type {
    font-size: 22rpx;
    color: var(--color-primary);
    background: rgba(43, 95, 75, 0.08);
    padding: 4rpx 16rpx;
    border-radius: 8rpx;
  }

  .timer {
    font-size: 24rpx;
    color: var(--text-placeholder);
    font-variant-numeric: tabular-nums;
  }
}

.question-box {
  background: var(--bg-card);
  border: 1rpx solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 36rpx 32rpx;
  margin-bottom: 32rpx;

  .question-text {
    font-size: 34rpx;
    line-height: 1.6;
    color: var(--text-primary);
    font-weight: 500;
  }
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.option-item {
  display: flex;
  align-items: center;
  background: var(--bg-card);
  border: 1rpx solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 24rpx 28rpx;
  transition: all 0.15s;

  &.selected {
    border-color: var(--color-primary);
    background: rgba(43, 95, 75, 0.04);
  }

  &.correct {
    border-color: var(--color-success);
    background: rgba(59, 125, 90, 0.1);
  }

  &.wrong {
    border-color: var(--color-danger);
    background: rgba(184, 68, 58, 0.08);
  }

  .option-key {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    background: var(--bg-input);
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    font-weight: 600;
    margin-right: 24rpx;
    flex-shrink: 0;
  }

  .option-text {
    font-size: 30rpx;
    color: var(--text-regular);
    line-height: 1.5;
  }
}

.judge-row {
  display: flex;
  gap: 24rpx;

  .judge-item {
    flex: 1;
    height: 120rpx;
    border-radius: var(--radius-md);
    border: 1rpx solid var(--border-light);
    background: var(--bg-card);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 36rpx;
    color: var(--text-regular);

    &.selected,
    &.correct {
      border-color: var(--color-primary);
      background: rgba(43, 95, 75, 0.08);
      color: var(--color-primary);
      font-weight: 600;
    }
  }
}

.text-answer {
  .answer-input {
    width: 100%;
    min-height: 320rpx;
    box-sizing: border-box;
    background: var(--bg-card);
    border: 1rpx solid var(--border-light);
    border-radius: var(--radius-lg);
    padding: 28rpx;
    font-size: 30rpx;
    color: var(--text-primary);
    line-height: 1.6;
  }
}

.answer-placeholder {
  color: var(--text-placeholder);
}

.result-box {
  margin-top: 32rpx;
  padding: 28rpx 32rpx;

  .result-head {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;

    .result-icon {
      width: 48rpx;
      height: 48rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28rpx;
      color: #fff;
      margin-right: 16rpx;
    }

    &.ok .result-icon {
      background: var(--color-success);
    }

    &.bad .result-icon {
      background: var(--color-danger);
    }

    .result-text {
      font-size: 30rpx;
      font-weight: 600;
      color: var(--text-primary);
    }
  }

  .correct-answer,
  .your-answer {
    display: flex;
    margin-top: 12rpx;

    .ca-label {
      font-size: 26rpx;
      color: var(--text-secondary);
      margin-right: 16rpx;
      flex-shrink: 0;
    }

    .ca-value {
      font-size: 28rpx;
      color: var(--text-regular);
      line-height: 1.5;
    }
  }
}

.answer-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: var(--bg-page);
  display: flex;
  align-items: center;
  gap: 24rpx;

  .prev-btn {
    width: 160rpx;
    height: 88rpx;
    border-radius: 44rpx;
    border: 1rpx solid var(--border-base);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28rpx;
    color: var(--text-regular);
    background: var(--bg-card);
    flex-shrink: 0;
  }

  .submit-btn {
    flex: 1;
    height: 88rpx;
    border-radius: 44rpx;
    background: var(--color-primary);
    color: #fdfcfa;
    font-size: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0;
  }
}
</style>