<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { knowledgeAPI } from '@/api/knowledge'

const loading = ref(true)
const nodes = ref([])
const edges = ref([])

const nodeCount = computed(() => nodes.value.length)
const edgeCount = computed(() => edges.value.length)

// 构建节点的邻接关系，用于列表式呈现关系（小程序无 canvas 图库时仍可读）
const adjacency = computed(() => {
  const map = {}
  for (const n of nodes.value) {
    map[n.id] = { label: n.label, color: n.color, neighbors: [] }
  }
  for (const e of edges.value) {
    if (map[e.source]) map[e.source].neighbors.push({ id: e.target, label: e.label })
    if (map[e.target]) map[e.target].neighbors.push({ id: e.source, label: e.label })
  }
  return map
})

const load = async () => {
  loading.value = true
  try {
    const graph = await knowledgeAPI.getRelationGraph()
    nodes.value = graph.nodes || []
    edges.value = graph.edges || []
  } catch (e) {
    /* request 已 toast */
  } finally {
    loading.value = false
  }
}

const goDetail = (id) => {
  uni.navigateTo({ url: `/pages/knowledge/detail?id=${id}` })
}

onLoad(load)
</script>

<template>
  <view class="graph-page">
    <view class="stat-bar card">
      <view class="stat-item">
        <text class="stat-num">{{ nodeCount }}</text>
        <text class="stat-label">知识点</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ edgeCount }}</text>
        <text class="stat-label">关系</text>
      </view>
    </view>

    <view v-if="loading" class="empty-state">加载中…</view>
    <view v-else-if="nodes.length === 0" class="empty-state">暂无知识图谱</view>
    <view v-else class="node-list">
      <view
        v-for="node in nodes"
        :key="node.id"
        class="card node-card tap-target"
        @tap="goDetail(node.id)"
      >
        <view class="node-head">
          <view class="node-dot" :style="{ background: node.color || '#2b5f4b' }" />
          <text class="node-label">{{ node.label }}</text>
        </view>
        <view v-if="adjacency[node.id] && adjacency[node.id].neighbors.length" class="node-rel">
          <text class="rel-label">关联：</text>
          <text class="rel-names ellipsis">
            {{ adjacency[node.id].neighbors.map((nb) => adjacency[nb.id]?.label || nb.id).join('、') }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
.graph-page {
  padding: 24rpx 32rpx 48rpx;
}

.stat-bar {
  display: flex;
  padding: 28rpx 0;
  margin-bottom: 24rpx;

  .stat-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;

    .stat-num {
      font-size: 44rpx;
      font-weight: 600;
      color: var(--color-primary);
    }

    .stat-label {
      margin-top: 6rpx;
      font-size: 24rpx;
      color: var(--text-secondary);
    }
  }
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.node-card {
  padding: 28rpx 32rpx;

  .node-head {
    display: flex;
    align-items: center;
    margin-bottom: 12rpx;

    .node-dot {
      width: 20rpx;
      height: 20rpx;
      border-radius: 50%;
      margin-right: 16rpx;
      flex-shrink: 0;
    }

    .node-label {
      font-size: 30rpx;
      font-weight: 600;
      color: var(--text-primary);
    }
  }

  .node-rel {
    display: flex;
    align-items: center;

    .rel-label {
      font-size: 24rpx;
      color: var(--text-secondary);
      flex-shrink: 0;
    }

    .rel-names {
      font-size: 24rpx;
      color: var(--text-placeholder);
    }
  }
}
</style>