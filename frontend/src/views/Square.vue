<template>
  <div class="square-page">
    <!-- ============ 列表视图（默认） ============ -->
    <template v-if="viewMode === 'list'">
    <div class="main-content">
      <div class="page-header">
        <div class="header-info">
          <h1 class="page-title">知识广场</h1>
          <p class="page-subtitle">浏览和分享知识，发现精彩内容</p>
        </div>
        <el-button v-if="activeTab === 'square'" type="primary" size="large" @click="openPublishDialog">
          <el-icon><Plus /></el-icon>
          发布到广场
        </el-button>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="广场" name="square" />
        <el-tab-pane label="我的收藏" name="bookmarks" />
        <el-tab-pane label="我的点赞" name="likes" />
      </el-tabs>

      <div v-if="activeTab === 'square'" class="filter-bar">
        <div class="filter-left">
          <el-radio-group v-model="scope" size="default" @change="loadList">
            <el-radio-button value="global">全平台</el-radio-button>
            <el-radio-button value="workspace">工作区</el-radio-button>
          </el-radio-group>
          <el-radio-group v-model="sort" size="default" @change="loadList">
            <el-radio-button value="newest">最新</el-radio-button>
            <el-radio-button value="hottest">最热</el-radio-button>
          </el-radio-group>
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索推荐语..."
            clearable
            @keyup.enter="loadList"
            @clear="loadList"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="loadList">搜索</el-button>
        </div>
      </div>

      <div v-loading="loading" class="post-list">
        <div v-if="!loading && postList.length === 0" class="empty-state">
          <el-empty :description="emptyDescription" />
        </div>

        <TransitionGroup name="card-list" tag="div" class="post-list-inner">
          <article
            v-for="post in postList"
            :key="post.postId"
            class="post-card"
            @click="openDetail(post)"
          >
            <div class="post-card-header">
              <div class="post-author">
                <el-avatar :size="36" :src="post.authorAvatar">
                  {{ post.authorName?.charAt(0) }}
                </el-avatar>
                <span class="author-name">{{ post.authorName }}</span>
              </div>
              <span class="post-time">{{ formatDate(post.createdAt) }}</span>
            </div>

            <h3 class="post-title">{{ post.nodeTitle }}</h3>

            <p v-if="post.recommendText" class="post-recommend">
              "{{ post.recommendText }}"
            </p>

            <p v-if="post.nodeSummary" class="post-summary">
              {{ post.nodeSummary }}
            </p>

            <div class="post-actions" @click.stop>
              <button
                class="action-btn"
                :class="{ active: post.isLiked }"
                @click="handleToggleLike(post)"
              >
                <el-icon :size="16">
                  <component :is="post.isLiked ? StarFilled : Star" />
                </el-icon>
                <span>{{ post.likeCount || 0 }}</span>
              </button>

              <button class="action-btn" @click="openDetail(post)">
                <el-icon :size="16"><ChatLineSquare /></el-icon>
                <span>{{ post.commentCount || 0 }}</span>
              </button>

              <button
                class="action-btn"
                :class="{ active: post.isBookmarked }"
                @click="handleToggleBookmark(post)"
              >
                <el-icon :size="16">
                  <component :is="post.isBookmarked ? Collection : Collection" />
                </el-icon>
                <span>{{ post.bookmarkCount || 0 }}</span>
              </button>
            </div>
          </article>
        </TransitionGroup>
      </div>

      <div v-if="pagination.total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
    </template>

    <!-- ============ 沉浸式详情视图（取代弹窗） ============ -->
    <section v-else-if="viewMode === 'detail' && currentPost" class="detail-view" tabindex="-1">
      <header class="detail-topbar">
        <button class="detail-exit" @click="closeDetail" aria-label="返回广场">
          <el-icon size="18"><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <div class="detail-topbar-title">帖子详情</div>
      </header>

      <div class="detail-body">
        <main class="detail-main">
          <Transition name="d-fade" mode="out-in">
            <article :key="currentPost.postId" class="detail-article">
              <div class="detail-meta-row">
                <div class="detail-author">
                  <el-avatar :size="40" :src="currentPost.authorAvatar">
                    {{ currentPost.authorName?.charAt(0) }}
                  </el-avatar>
                  <div class="detail-author-info">
                    <span class="detail-author-name">{{ currentPost.authorName }}</span>
                    <span class="detail-time">{{ formatDate(currentPost.createdAt) }}</span>
                  </div>
                </div>
              </div>

              <h1 class="detail-title">{{ currentPost.nodeTitle }}</h1>

              <blockquote v-if="currentPost.recommendText" class="recommend-quote">
                <span class="quote-mark">"</span>
                {{ currentPost.recommendText }}
              </blockquote>

              <div v-if="currentPost.nodeSummary" class="node-summary">
                <div class="summary-label">
                  <el-icon><Document /></el-icon>
                  <span>内容摘要</span>
                </div>
                <p>{{ currentPost.nodeSummary }}</p>
              </div>

              <div class="detail-actions">
                <button
                  class="detail-action-btn"
                  :class="{ active: currentPost.isLiked }"
                  @click="handleToggleLike(currentPost)"
                >
                  <el-icon :size="18">
                    <component :is="currentPost.isLiked ? StarFilled : Star" />
                  </el-icon>
                  <span>{{ currentPost.isLiked ? '已点赞' : '点赞' }}</span>
                  <span class="detail-action-count">{{ currentPost.likeCount || 0 }}</span>
                </button>
                <button
                  class="detail-action-btn"
                  :class="{ active: currentPost.isBookmarked }"
                  @click="handleToggleBookmark(currentPost)"
                >
                  <el-icon :size="18"><Collection /></el-icon>
                  <span>{{ currentPost.isBookmarked ? '已收藏' : '收藏' }}</span>
                  <span class="detail-action-count">{{ currentPost.bookmarkCount || 0 }}</span>
                </button>
                <button class="detail-action-btn warn" @click="openReportDialog">
                  <el-icon :size="18"><Warning /></el-icon>
                  <span>举报</span>
                </button>
              </div>
            </article>
          </Transition>

          <section class="comment-section">
            <h3 class="comment-section-title">
              <el-icon><ChatLineSquare /></el-icon>
              <span>评论</span>
              <span class="comment-count">{{ currentPost.commentCount || 0 }}</span>
            </h3>

            <div class="comment-input-area">
              <el-input
                v-model="newComment"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="写下你的评论..."
                class="comment-textarea"
              />
              <div class="comment-input-actions">
                <el-button
                  type="primary"
                  :loading="commentLoading"
                  :disabled="!newComment.trim()"
                  @click="submitComment"
                >
                  发表评论
                </el-button>
              </div>
            </div>

            <TransitionGroup name="comment-list" tag="div" class="comment-list" v-if="comments.length > 0">
              <div
                v-for="comment in comments"
                :key="comment.id"
                class="comment-item"
              >
                <div class="comment-header">
                  <div class="comment-user">
                    <el-avatar :size="28" :src="comment.avatar">
                      {{ comment.username?.charAt(0) }}
                    </el-avatar>
                    <span class="comment-username">{{ comment.username }}</span>
                    <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                  </div>
                  <el-button
                    v-if="comment.userId === userStore.userInfo.id"
                    type="danger"
                    size="small"
                    text
                    @click="handleDeleteComment(comment.id)"
                  >
                    删除
                  </el-button>
                </div>
                <p class="comment-content">{{ comment.content }}</p>
              </div>
            </TransitionGroup>
            <div v-else class="comment-empty">
              <el-icon size="32" color="var(--text-muted)"><ChatLineSquare /></el-icon>
              <span>暂无评论，快来发表第一条吧</span>
            </div>
          </section>
        </main>
      </div>
    </section>
  </div>

  <!-- ============ 发布抽屉（取代弹窗） ============ -->
  <el-drawer
    v-model="showPublishDialog"
    title="发布到知识广场"
    direction="rtl"
    size="480px"
    @open="onPublishDialogOpened"
  >
    <el-form :model="publishForm" label-position="top" class="publish-form">
      <el-form-item label="发布范围" required>
        <el-radio-group v-model="publishForm.scope">
          <el-radio value="global">全平台可见</el-radio>
          <el-radio value="workspace">仅工作区可见</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        v-if="publishForm.scope === 'workspace'"
        label="选择工作区"
        required
      >
        <el-select
          v-model="publishForm.workspaceId"
          placeholder="请选择工作区"
          style="width: 100%"
        >
          <el-option
            v-for="ws in workspaces"
            :key="ws.id"
            :label="ws.name"
            :value="ws.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="选择知识节点" required>
        <el-select
          v-model="publishForm.nodeId"
          placeholder="请选择要分享的知识节点"
          filterable
          style="width: 100%"
        >
          <el-option
            v-for="node in userNodes"
            :key="node.id"
            :label="node.title"
            :value="node.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="推荐语">
        <el-input
          v-model="publishForm.recommendText"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          placeholder="写一段推荐语，告诉大家为什么值得阅读..."
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="drawer-footer">
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="handlePublish">
          发布
        </el-button>
      </div>
    </template>
  </el-drawer>

  <!-- ============ 举报抽屉（取代弹窗） ============ -->
  <el-drawer
    v-model="showReportDialog"
    title="举报内容"
    direction="rtl"
    size="420px"
  >
    <el-form :model="reportForm" label-position="top" class="report-form">
      <el-form-item label="举报原因" required>
        <el-input
          v-model="reportForm.reason"
          type="textarea"
          :rows="6"
          maxlength="500"
          show-word-limit
          placeholder="请详细描述举报原因..."
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="drawer-footer">
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="primary" :loading="reportLoading" @click="submitReport">
          提交举报
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Search,
  Star,
  StarFilled,
  ChatLineSquare,
  Collection,
  Warning,
  ArrowLeft,
  Document,
} from "@element-plus/icons-vue";
import { squareAPI } from "@/api/square";
import { knowledgeAPI } from "@/api/knowledge";
import { workspaceAPI } from "@/api/workspace";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();

const loading = ref(false);
const postList = ref([]);
const activeTab = ref("square");
const scope = ref("global");
const sort = ref("newest");
const keyword = ref("");
const pagination = reactive({ current: 1, size: 10, total: 0 });

// 发布对话框
const showPublishDialog = ref(false);
const publishLoading = ref(false);
const publishForm = reactive({ nodeId: null, recommendText: "", scope: "global", workspaceId: null });
const userNodes = ref([]);
const workspaces = ref([]);

// 视图模式：'list' 列表 / 'detail' 沉浸式详情（取代弹窗）
const viewMode = ref("list");
const currentPost = ref(null);
const comments = ref([]);
const newComment = ref("");
const commentLoading = ref(false);

// 举报对话框
const showReportDialog = ref(false);
const reportLoading = ref(false);
const reportForm = reactive({ reason: "" });

const emptyDescription = computed(() => {
  if (activeTab.value === "bookmarks") return "暂无收藏内容";
  if (activeTab.value === "likes") return "暂无点赞内容";
  return "暂无分享内容";
});

// ========== 列表 ==========

const loadList = async () => {
  loading.value = true;
  try {
    let data;
    const params = { current: pagination.current, size: pagination.size };

    if (activeTab.value === "bookmarks") {
      data = await squareAPI.getMyBookmarks(params);
    } else if (activeTab.value === "likes") {
      data = await squareAPI.getMyLikes(params);
    } else {
      data = await squareAPI.getList({
        scope: scope.value,
        sort: sort.value,
        keyword: keyword.value || undefined,
        ...params,
      });
    }
    postList.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载失败: " + (error.message || "未知错误"));
  } finally {
    loading.value = false;
  }
};

const onTabChange = () => {
  pagination.current = 1;
  loadList();
};

// ========== 发布 ==========

const openPublishDialog = () => {
  publishForm.nodeId = null;
  publishForm.recommendText = "";
  publishForm.scope = "global";
  publishForm.workspaceId = null;
  showPublishDialog.value = true;
};

const onPublishDialogOpened = () => {
  // 抽屉打开时加载选项数据
  loadUserNodes();
  loadWorkspaces();
};

const loadUserNodes = async () => {
  try {
    const data = await knowledgeAPI.getList({ current: 1, size: 999 });
    userNodes.value = data.records || [];
  } catch (error) {
    // ignore
  }
};

const loadWorkspaces = async () => {
  try {
    const data = await workspaceAPI.list();
    workspaces.value = data || [];
  } catch (error) {
    // ignore
  }
};

const handlePublish = async () => {
  if (!publishForm.nodeId) {
    ElMessage.warning("请选择知识节点");
    return;
  }
  if (publishForm.scope === "workspace" && !publishForm.workspaceId) {
    ElMessage.warning("请选择工作区");
    return;
  }
  publishLoading.value = true;
  try {
    await squareAPI.publish({
      nodeId: publishForm.nodeId,
      recommendText: publishForm.recommendText || undefined,
      scope: publishForm.scope,
      workspaceId: publishForm.scope === "workspace" ? publishForm.workspaceId : undefined,
    });
    ElMessage.success("发布成功");
    showPublishDialog.value = false;
    await loadList();
  } catch (error) {
    ElMessage.error("发布失败: " + (error.message || "未知错误"));
  } finally {
    publishLoading.value = false;
  }
};

// ========== 详情 ==========

const openDetail = async (post) => {
  try {
    const data = await squareAPI.getDetail(post.postId);
    currentPost.value = data;
    comments.value = data.comments || [];
    // 进入沉浸式详情视图（不再使用弹窗）
    viewMode.value = "detail";
  } catch (error) {
    ElMessage.error("加载详情失败: " + (error.message || "未知错误"));
  }
};

const closeDetail = () => {
  viewMode.value = "list";
  currentPost.value = null;
  comments.value = [];
  newComment.value = "";
};

// ========== 点赞 ==========

const handleToggleLike = async (post) => {
  try {
    const liked = await squareAPI.toggleLike(post.postId);
    post.isLiked = liked;
    post.likeCount = liked ? (post.likeCount || 0) + 1 : Math.max(0, (post.likeCount || 0) - 1);
    if (currentPost.value && currentPost.value.postId === post.postId) {
      currentPost.value.isLiked = liked;
      currentPost.value.likeCount = post.likeCount;
    }
  } catch (error) {
    ElMessage.error("操作失败: " + (error.message || "未知错误"));
  }
};

// ========== 收藏 ==========

const handleToggleBookmark = async (post) => {
  try {
    const bookmarked = await squareAPI.toggleBookmark(post.postId);
    post.isBookmarked = bookmarked;
    post.bookmarkCount = bookmarked
      ? (post.bookmarkCount || 0) + 1
      : Math.max(0, (post.bookmarkCount || 0) - 1);
    if (currentPost.value && currentPost.value.postId === post.postId) {
      currentPost.value.isBookmarked = bookmarked;
      currentPost.value.bookmarkCount = post.bookmarkCount;
    }
  } catch (error) {
    ElMessage.error("操作失败: " + (error.message || "未知错误"));
  }
};

// ========== 评论 ==========

const submitComment = async () => {
  if (!newComment.value.trim()) return;
  commentLoading.value = true;
  try {
    const comment = await squareAPI.addComment(currentPost.value.postId, {
      content: newComment.value.trim(),
    });
    comments.value.push(comment);
    if (currentPost.value) {
      currentPost.value.commentCount = (currentPost.value.commentCount || 0) + 1;
    }
    newComment.value = "";
    ElMessage.success("评论成功");
  } catch (error) {
    ElMessage.error("评论失败: " + (error.message || "未知错误"));
  } finally {
    commentLoading.value = false;
  }
};

const handleDeleteComment = async (commentId) => {
  try {
    await ElMessageBox.confirm("确定要删除这条评论吗？", "删除评论", {
      confirmButtonText: "确认删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }

  try {
    await squareAPI.deleteComment(currentPost.value.postId, commentId);
    comments.value = comments.value.filter((c) => c.id !== commentId);
    if (currentPost.value) {
      currentPost.value.commentCount = Math.max(
        0,
        (currentPost.value.commentCount || 0) - 1
      );
    }
    ElMessage.success("评论已删除");
  } catch (error) {
    ElMessage.error("删除失败: " + (error.message || "未知错误"));
  }
};

// ========== 举报 ==========

const openReportDialog = () => {
  reportForm.reason = "";
  showReportDialog.value = true;
};

const submitReport = async () => {
  if (!reportForm.reason.trim()) {
    ElMessage.warning("请输入举报原因");
    return;
  }
  reportLoading.value = true;
  try {
    await squareAPI.report(currentPost.value.postId, {
      reason: reportForm.reason.trim(),
    });
    ElMessage.success("举报已提交");
    showReportDialog.value = false;
  } catch (error) {
    ElMessage.error("举报失败: " + (error.message || "未知错误"));
  } finally {
    reportLoading.value = false;
  }
};

// ========== 工具 ==========

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN");
};

onMounted(() => {
  loadList();
});
</script>

<style scoped>
.square-page {
  min-height: 100%;
  background: var(--bg-page);
  padding: var(--spacing-xl) 0;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
}

.page-title {
  font-family: var(--font-family-display);
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  display: flex;
  gap: 8px;
  width: 320px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-list-inner {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  padding: 60px 0;
}

.post-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-lighter);
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
}

.post-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
  border-color: var(--color-primary-light);
}

.post-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.post-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.post-title {
  font-family: var(--font-family-display);
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 10px;
  line-height: 1.4;
}

.post-recommend {
  font-size: 14px;
  color: var(--text-secondary);
  font-style: italic;
  margin: 0 0 10px;
  padding: 8px 12px;
  background: var(--color-primary-alpha-10);
  border-left: 3px solid var(--color-accent);
  border-radius: 0 4px 4px 0;
}

.post-summary {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-actions {
  display: flex;
  gap: 20px;
  padding-top: 10px;
  border-top: 1px solid var(--border-lighter);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  font-family: var(--font-family-ui);
  font-size: 13px;
  color: var(--text-secondary);
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: color 0.15s, background 0.15s;
}

.action-btn:hover {
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.action-btn:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
}

.action-btn.active {
  color: var(--color-accent);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

/* ============================================================
   沉浸式详情视图（取代 el-dialog）
   遵循 Scholar's Desk 设计系统：sage/brass/parchment + serif
   ============================================================ */
.detail-view {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: var(--bg-page);
  outline: none;
  animation: detail-enter 0.28s ease-out;
}

@keyframes detail-enter {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.detail-topbar {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-md) var(--spacing-2xl);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-lighter);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}

.detail-exit {
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

.detail-exit:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.detail-exit:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
}

.detail-topbar-title {
  font-family: var(--font-family-display);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-2xl) var(--spacing-xl);
}

.detail-main {
  max-width: 760px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

.detail-article {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-2xl);
  border: 1px solid var(--border-lighter);
  box-shadow: var(--shadow-sm);
}

.detail-meta-row {
  margin-bottom: var(--spacing-lg);
}

.detail-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-author-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-author-name {
  font-family: var(--font-family-ui);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.detail-time {
  font-size: 12px;
  color: var(--text-muted);
}

.detail-title {
  font-family: var(--font-family-display);
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  line-height: 1.35;
  margin: 0 0 var(--spacing-lg);
}

.recommend-quote {
  position: relative;
  margin: 0 0 var(--spacing-lg);
  padding: 14px 18px 14px 36px;
  background: var(--color-primary-alpha-10);
  border-left: 3px solid var(--color-accent);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  font-family: var(--font-family-body);
  font-size: var(--font-size-md);
  color: var(--text-secondary);
  font-style: italic;
  line-height: 1.7;
}

.quote-mark {
  position: absolute;
  left: 10px;
  top: 4px;
  font-family: var(--font-family-display);
  font-size: 32px;
  line-height: 1;
  color: var(--color-accent);
  opacity: 0.6;
}

.node-summary {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--bg-list-item);
  border-radius: var(--radius-md);
}

.summary-label {
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

.node-summary p {
  font-family: var(--font-family-body);
  font-size: var(--font-size-base);
  color: var(--text-regular);
  line-height: 1.8;
  margin: 0;
}

.detail-actions {
  display: flex;
  gap: var(--spacing-md);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-lighter);
}

.detail-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  min-height: 40px;
  border: 1.5px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: var(--font-family-ui);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.detail-action-btn:hover {
  border-color: var(--color-primary-light);
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.detail-action-btn:focus-visible {
  outline: none;
  box-shadow: var(--shadow-focus-ring);
}

.detail-action-btn.active {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: var(--color-accent-alpha-10);
}

.detail-action-btn.warn:hover {
  border-color: var(--color-danger);
  color: var(--color-danger);
  background: var(--color-danger-bg);
}

.detail-action-count {
  padding-left: 4px;
  border-left: 1px solid var(--border-lighter);
  font-weight: var(--font-weight-bold);
  color: var(--text-muted);
}

/* —— 评论区 —— */
.comment-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl);
  border: 1px solid var(--border-lighter);
  box-shadow: var(--shadow-sm);
}

.comment-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-family-display);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--spacing-md);
}

.comment-count {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
}

.comment-input-area {
  margin-bottom: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--bg-list-item);
  border-radius: var(--radius-md);
}

.comment-textarea :deep(.el-textarea__inner) {
  font-family: var(--font-family-body);
  font-size: var(--font-size-base);
  line-height: 1.7;
  border-radius: var(--radius-md);
}

.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-sm);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-item {
  padding: var(--spacing-md);
  background: var(--bg-page);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-lighter);
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-username {
  font-family: var(--font-family-ui);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.comment-time {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: 8px;
}

.comment-content {
  font-family: var(--font-family-body);
  font-size: var(--font-size-base);
  color: var(--text-regular);
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

.comment-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: var(--spacing-xl);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
  text-align: center;
}

/* —— 抽屉底部 —— */
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.publish-form, .report-form {
  padding: 0 var(--spacing-xs);
}

/* —— 过渡：卡片列表 —— */
.card-list-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.card-list-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; position: absolute; width: 100%; }
.card-list-enter-from { opacity: 0; transform: translateY(12px); }
.card-list-leave-to   { opacity: 0; transform: translateX(-12px); }
.card-list-move { transition: transform 0.25s ease; }

/* —— 过渡：详情淡入 —— */
.d-fade-enter-active { transition: opacity 0.25s ease; }
.d-fade-leave-active { transition: opacity 0.18s ease; }
.d-fade-enter-from, .d-fade-leave-to { opacity: 0; }

/* —— 过渡：评论项 —— */
.comment-list-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.comment-list-leave-active { transition: opacity 0.18s ease; position: absolute; width: 100%; }
.comment-list-enter-from { opacity: 0; transform: translateY(-8px); }
.comment-list-leave-to { opacity: 0; }
.comment-list-move { transition: transform 0.25s ease; }

/* —— 响应式 —— */
@media (max-width: 768px) {
  .detail-body { padding: var(--spacing-lg) var(--spacing-md); }
  .detail-title { font-size: var(--font-size-2xl); }
  .detail-actions { flex-wrap: wrap; }
  .filter-bar { flex-direction: column; align-items: stretch; gap: 10px; }
  .search-box { width: 100%; }
}

/* 尊重用户的减弱动效偏好 */
@media (prefers-reduced-motion: reduce) {
  .detail-view, .card-list-enter-active, .card-list-leave-active,
  .comment-list-enter-active, .comment-list-leave-active,
  .d-fade-enter-active, .d-fade-leave-active,
  .card-list-move, .comment-list-move {
    animation: none !important;
    transition: none !important;
  }
}
</style>
