<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { aiAPI } from '@/api/ai'
import { communityAPI } from '@/api/community'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const aiConfig = ref([])
const providers = ref([])
const introduction = ref('')
const expertiseTags = ref([])
const tagInput = ref('')
const profileSaving = ref(false)

const load = async () => {
  try {
    const [config, pv, profile] = await Promise.all([
      aiAPI.getUserAiConfig().catch(() => []),
      aiAPI.getProviders().catch(() => []),
      userStore.userInfo?.id
        ? communityAPI.getUserProfile(userStore.userInfo.id).catch(() => null)
        : Promise.resolve(null),
    ])
    aiConfig.value = config || []
    providers.value = pv || []
    introduction.value = profile?.introduction || ''
    expertiseTags.value = profile?.expertiseTags || []
  } catch (e) {
    /* request 已 toast */
  }
}

const addTag = () => {
  const tag = tagInput.value.trim()
  if (!tag || expertiseTags.value.length >= 8 || tag.length > 30) {
    uni.showToast({ title: expertiseTags.value.length >= 8 ? '最多设置 8 个领域' : '领域标签最多 30 个字', icon: 'none' })
    return
  }
  if (!expertiseTags.value.includes(tag)) expertiseTags.value.push(tag)
  tagInput.value = ''
}

const removeTag = (tag) => {
  expertiseTags.value = expertiseTags.value.filter((item) => item !== tag)
}

const saveProfile = async () => {
  profileSaving.value = true
  try {
    const profile = await communityAPI.updateMyProfile({
      introduction: introduction.value.trim(),
      expertiseTags: expertiseTags.value,
    })
    introduction.value = profile?.introduction || introduction.value.trim()
    expertiseTags.value = profile?.expertiseTags || expertiseTags.value
    uni.showToast({ title: '社区资料已保存', icon: 'success' })
  } catch (_) {
    /* request 已 toast */
  } finally {
    profileSaving.value = false
  }
}

const clearCache = () => {
  uni.showModal({
    title: '清除本地缓存',
    content: '将清除本地登录态与缓存数据，需重新登录',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.reLaunch({ url: '/pages/login/index' })
      }
    },
  })
}

onShow(load)
</script>

<template>
  <view class="settings-page">
    <view class="card section community-section">
      <view class="section-heading">
        <view>
          <text class="section-title">社区资料</text>
          <text class="section-hint">只展示给社区访客，不会修改邮箱、手机号或安全信息</text>
        </view>
        <AppIcon name="users" :size="20" color="#2b5f4b" />
      </view>
      <textarea
        v-model="introduction"
        class="profile-textarea"
        maxlength="500"
        placeholder="写一句你的学习方向或正在探索的主题"
        placeholder-class="profile-placeholder"
      />
      <view class="tag-editor">
        <view v-for="tag in expertiseTags" :key="tag" class="profile-tag">
          <text>#{{ tag }}</text>
          <text class="tag-remove" @tap="removeTag(tag)">×</text>
        </view>
        <input
          v-if="expertiseTags.length < 8"
          v-model="tagInput"
          class="tag-input"
          maxlength="30"
          placeholder="添加领域"
          placeholder-class="profile-placeholder"
          confirm-type="done"
          @confirm="addTag"
        />
      </view>
      <button class="btn-primary profile-save tap-target" :loading="profileSaving" :disabled="profileSaving" @tap="saveProfile">保存社区资料</button>
    </view>

    <view class="card section">
      <text class="section-title">AI 服务</text>
      <view class="config-item" v-for="c in aiConfig" :key="c.providerId || c.id">
        <text class="config-label">{{ c.providerName || c.providerId || '提供方' }}</text>
        <text class="config-value ellipsis">{{ c.model || c.apiKey ? (c.apiKey ? '已配置' : '未配置') : '—' }}</text>
      </view>
      <view v-if="aiConfig.length === 0" class="empty-text">尚未配置 AI 提供方</view>
    </view>

    <view class="card section">
      <text class="section-title">通用</text>
      <view class="setting-row tap-target" @tap="clearCache">
        <text class="row-label">清除本地缓存</text>
        <text class="row-arrow">›</text>
      </view>
    </view>

    <view class="version">AI · Second Brain v1.0.0</view>
  </view>
</template>

<style lang="scss" scoped>
.settings-page {
  padding: 24rpx 32rpx 48rpx;
}

.section {
  padding: 28rpx 32rpx;
  margin-bottom: 24rpx;

  .section-title {
    display: block;
    font-size: 28rpx;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 20rpx;
  }

  .section-heading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20rpx;
    margin-bottom: 20rpx;
  }

  .section-hint {
    display: block;
    margin-top: 6rpx;
    color: var(--text-placeholder);
    font-size: 22rpx;
    line-height: 1.45;
  }

  .config-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16rpx 0;

    .config-label {
      font-size: 28rpx;
      color: var(--text-regular);
    }

    .config-value {
      font-size: 26rpx;
      color: var(--text-secondary);
      max-width: 400rpx;
    }
  }

  .empty-text {
    font-size: 26rpx;
    color: var(--text-placeholder);
  }

  .setting-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12rpx 0;

    .row-label {
      font-size: 28rpx;
      color: var(--text-regular);
    }

    .row-arrow {
      font-size: 36rpx;
      color: var(--text-placeholder);
    }
  }
}

.community-section {
  border-color: rgba(43, 95, 75, 0.22);
  background: linear-gradient(145deg, #fdfcfa, #f4f8f1);
}

.profile-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 20rpx;
  box-sizing: border-box;
  border-radius: var(--radius-md);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 28rpx;
  line-height: 1.6;
}

.profile-placeholder { color: var(--text-placeholder); }

.tag-editor {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}

.profile-tag {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  color: var(--color-primary-dark);
  background: rgba(43, 95, 75, 0.1);
  font-size: 24rpx;
}

.tag-remove {
  width: 28rpx;
  height: 28rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 28rpx;
}

.tag-input {
  width: 150rpx;
  height: 56rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 24rpx;
}

.profile-save {
  width: 100%;
  margin-top: 24rpx;
}

.version {
  text-align: center;
  margin-top: 48rpx;
  font-size: 24rpx;
  color: var(--text-placeholder);
}
</style>
