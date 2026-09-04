<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { knowledgeAPI } from '@/api/knowledge'

const id = ref(null)
const loading = ref(false)
const saving = ref(false)

const form = ref({
  title: '',
  summary: '',
  contentMd: '',
  importance: 1,
})

const isEdit = ref(false)

const load = async () => {
  if (!id.value) return
  loading.value = true
  try {
    const data = await knowledgeAPI.getById(id.value)
    form.value = {
      title: data.title || '',
      summary: data.summary || '',
      contentMd: data.contentMd || data.content || '',
      importance: data.importance || 1,
    }
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (!form.value.title.trim()) {
    uni.showToast({ title: '标题不能为空', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.value.title.trim(),
      summary: form.value.summary.trim(),
      contentMd: form.value.contentMd,
      importance: Number(form.value.importance) || 1,
    }
    if (isEdit.value) {
      await knowledgeAPI.updateKnowledge(id.value, payload)
      uni.showToast({ title: '保存成功', icon: 'success' })
    } else {
      await knowledgeAPI.createKnowledge(payload)
      uni.showToast({ title: '创建成功', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 500)
  } catch (e) {
    /* request 已 toast */
  } finally {
    saving.value = false
  }
}

onLoad((query) => {
  if (query.id) {
    id.value = query.id
    isEdit.value = true
    uni.setNavigationBarTitle({ title: '编辑知识' })
  }
  load()
})
</script>

<template>
  <view class="create-page">
    <view class="field">
      <text class="field-label">标题</text>
      <input
        v-model="form.title"
        class="field-input"
        placeholder="输入标题"
        placeholder-class="field-placeholder"
        maxlength="200"
      />
    </view>

    <view class="field">
      <text class="field-label">摘要</text>
      <input
        v-model="form.summary"
        class="field-input"
        placeholder="一句话概括（可选）"
        placeholder-class="field-placeholder"
      />
    </view>

    <view class="field">
      <text class="field-label">正文（Markdown）</text>
      <textarea
        v-model="form.contentMd"
        class="content-input"
        placeholder="支持 Markdown 语法"
        placeholder-class="field-placeholder"
        maxlength="-1"
      />
    </view>

    <view class="field">
      <text class="field-label">重要度（1-5）</text>
      <view class="importance-row">
        <view
          v-for="n in 5"
          :key="n"
          class="star tap-target"
          :class="{ active: form.importance >= n }"
          @tap="form.importance = n"
        >★</view>
      </view>
    </view>

    <button class="btn-primary save-btn tap-target" :loading="saving" :disabled="saving" @tap="save">
      {{ isEdit ? '保存修改' : '创建知识' }}
    </button>
  </view>
</template>

<style lang="scss" scoped>
.create-page {
  padding: 24rpx 32rpx 48rpx;
}

.field {
  margin-bottom: 36rpx;

  .field-label {
    display: block;
    font-size: 26rpx;
    color: var(--text-secondary);
    margin-bottom: 14rpx;
  }

  .field-input {
    height: 88rpx;
    background: var(--bg-card);
    border: 1rpx solid var(--border-light);
    border-radius: var(--radius-md);
    padding: 0 28rpx;
    font-size: 30rpx;
    color: var(--text-primary);
    box-sizing: border-box;
  }

  .content-input {
    width: 100%;
    min-height: 400rpx;
    box-sizing: border-box;
    background: var(--bg-card);
    border: 1rpx solid var(--border-light);
    border-radius: var(--radius-md);
    padding: 28rpx;
    font-size: 30rpx;
    color: var(--text-primary);
    line-height: 1.6;
  }
}

.field-placeholder {
  color: var(--text-placeholder);
}

.importance-row {
  display: flex;
  gap: 16rpx;

  .star {
    font-size: 48rpx;
    color: var(--border-base);

    &.active {
      color: var(--color-accent);
    }
  }
}

.save-btn {
  margin-top: 16rpx;
}
</style>