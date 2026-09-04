<script setup>
import { ref } from 'vue'
import { ragAPI } from '@/api/rag'

const messages = ref([])
const input = ref('')
const sending = ref(false)

const scrollToBottom = () => {
  uni.pageScrollTo({ scrollTop: 99999, duration: 200 })
}

const send = async () => {
  const q = input.value.trim()
  if (!q || sending.value) return
  messages.value.push({ role: 'user', content: q })
  input.value = ''
  sending.value = true
  scrollToBottom()

  try {
    // 非流式问答：一次性返回答案 + 引用，符合移动端弱网/小程序场景
    const res = await ragAPI.ask({ question: q, topK: 3, includeReferences: true })
    messages.value.push({
      role: 'assistant',
      content: res.answer || '未能生成回答',
      references: res.references || [],
    })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '回答失败，请稍后重试' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}
</script>

<template>
  <view class="chat-page">
    <view v-if="messages.length === 0" class="welcome">
      <view class="welcome-mark">墨</view>
      <text class="welcome-title">基于你的知识库问答</text>
      <text class="welcome-sub">向你的第二大脑提问，它会从已沉淀的知识中寻找答案</text>
    </view>

    <view v-else class="message-list">
      <view
        v-for="(m, i) in messages"
        :key="i"
        class="message-row"
        :class="{ user: m.role === 'user' }"
      >
        <view class="bubble" :class="m.role">
          <text class="bubble-text">{{ m.content }}</text>
          <view v-if="m.references && m.references.length" class="references">
            <text class="ref-title">引用来源：</text>
            <text v-for="ref in m.references" :key="ref.knowledgeId" class="ref-item">
              · {{ ref.title }}
            </text>
          </view>
        </view>
      </view>
    </view>

    <view class="input-bar">
      <input
        v-model="input"
        class="chat-input"
        placeholder="输入你的问题…"
        placeholder-class="input-placeholder"
        confirm-type="send"
        :disabled="sending"
        @confirm="send"
      />
      <view class="send-btn tap-target" :class="{ disabled: sending }" @tap="send">
        {{ sending ? '…' : '发送' }}
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.chat-page {
  padding: 24rpx 32rpx;
  padding-bottom: 140rpx;
  box-sizing: border-box;
  min-height: 100vh;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;

  .welcome-mark {
    width: 120rpx;
    height: 120rpx;
    border-radius: 32rpx;
    background: var(--color-primary);
    color: #fdfcfa;
    font-size: 56rpx;
    font-family: 'Noto Serif SC', serif;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 28rpx;
  }

  .welcome-title {
    font-size: 34rpx;
    font-weight: 600;
    color: var(--text-primary);
  }

  .welcome-sub {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: var(--text-secondary);
    text-align: center;
  }
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.message-row {
  display: flex;

  &.user {
    justify-content: flex-end;
  }
}

.bubble {
  max-width: 80%;
  padding: 20rpx 28rpx;
  border-radius: var(--radius-md);
  font-size: 28rpx;
  line-height: 1.6;

  &.user {
    background: var(--color-primary);
    color: #fdfcfa;
    border-top-right-radius: 4rpx;
  }

  &.assistant {
    background: var(--bg-card);
    border: 1rpx solid var(--border-light);
    color: var(--text-regular);
    border-top-left-radius: 4rpx;
  }

  .bubble-text {
    white-space: pre-wrap;
    word-break: break-word;
  }

  .references {
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid var(--border-light);
    display: flex;
    flex-wrap: wrap;
    gap: 8rpx;

    .ref-title {
      font-size: 22rpx;
      color: var(--text-secondary);
    }

    .ref-item {
      font-size: 22rpx;
      color: var(--text-placeholder);
    }
  }
}

.input-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: var(--bg-page);
  display: flex;
  align-items: center;
  gap: 20rpx;

  .chat-input {
    flex: 1;
    height: 80rpx;
    background: var(--bg-card);
    border: 1rpx solid var(--border-light);
    border-radius: 40rpx;
    padding: 0 32rpx;
    font-size: 28rpx;
  }

  .send-btn {
    width: 128rpx;
    height: 80rpx;
    border-radius: 40rpx;
    background: var(--color-primary);
    color: #fdfcfa;
    font-size: 28rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    &.disabled {
      opacity: 0.6;
    }
  }
}

.input-placeholder {
  color: var(--text-placeholder);
}
</style>