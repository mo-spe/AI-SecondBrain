<template>
  <div class="square-page">
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

        <div
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
        </div>
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

    <!-- 发布对话框 -->
    <el-dialog
      v-model="showPublishDialog"
      title="发布到知识广场"
      width="560px"
      @opened="onPublishDialogOpened"
    >
      <el-form :model="publishForm" label-position="top">
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
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="写一段推荐语，告诉大家为什么值得阅读..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishLoading" @click="handlePublish">
          发布
        </el-button>
      </template>
    </el-dialog>

    <!-- 帖子详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      width="680px"
      @closed="currentPost = null"
    >
      <template #header>
        <div class="detail-header" v-if="currentPost">
          <h2 class="detail-title">{{ currentPost.nodeTitle }}</h2>
          <div class="detail-meta">
            <div class="detail-author">
              <el-avatar :size="28" :src="currentPost.authorAvatar">
                {{ currentPost.authorName?.charAt(0) }}
              </el-avatar>
              <span>{{ currentPost.authorName }}</span>
            </div>
            <span class="detail-time">{{ formatDate(currentPost.createdAt) }}</span>
          </div>
        </div>
      </template>

      <div v-if="currentPost" class="detail-body">
        <blockquote v-if="currentPost.recommendText" class="recommend-quote">
          {{ currentPost.recommendText }}
        </blockquote>

        <div v-if="currentPost.nodeSummary" class="node-summary">
          <h4>摘要</h4>
          <p>{{ currentPost.nodeSummary }}</p>
        </div>

        <div class="detail-actions">
          <el-button
            :type="currentPost.isLiked ? 'primary' : 'default'"
            :icon="currentPost.isLiked ? StarFilled : Star"
            @click="handleToggleLike(currentPost)"
          >
            {{ currentPost.isLiked ? '已点赞' : '点赞' }} ({{ currentPost.likeCount || 0 }})
          </el-button>
          <el-button
            :type="currentPost.isBookmarked ? 'primary' : 'default'"
            :icon="Collection"
            @click="handleToggleBookmark(currentPost)"
          >
            {{ currentPost.isBookmarked ? '已收藏' : '收藏' }} ({{ currentPost.bookmarkCount || 0 }})
          </el-button>
          <el-button type="warning" plain :icon="Warning" @click="openReportDialog">
            举报
          </el-button>
        </div>

        <div class="comment-section">
          <h4>评论 ({{ currentPost.commentCount || 0 }})</h4>

          <div class="comment-list" v-if="comments.length > 0">
            <div
              v-for="comment in comments"
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-header">
                <div class="comment-user">
                  <el-avatar :size="24" :src="comment.avatar">
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
          </div>
          <div v-else class="comment-empty">
            <span>暂无评论</span>
          </div>

          <div class="comment-input">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="写下你的评论..."
            />
            <el-button
              type="primary"
              size="small"
              :loading="commentLoading"
              :disabled="!newComment.trim()"
              @click="submitComment"
            >
              发表评论
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 举报对话框 -->
    <el-dialog
      v-model="showReportDialog"
      title="举报内容"
      width="480px"
    >
      <el-form :model="reportForm" label-position="top">
        <el-form-item label="举报原因" required>
          <el-input
            v-model="reportForm.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请详细描述举报原因..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReportDialog = false">取消</el-button>
        <el-button type="primary" :loading="reportLoading" @click="submitReport">
          提交举报
        </el-button>
      </template>
    </el-dialog>
  </div>
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

// 详情对话框
const showDetailDialog = ref(false);
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
    showDetailDialog.value = true;
  } catch (error) {
    ElMessage.error("加载详情失败: " + (error.message || "未知错误"));
  }
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
  transition: box-shadow 0.2s, transform 0.2s;
}

.post-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
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
  background: rgba(99, 102, 241, 0.04);
  border-left: 3px solid var(--color-primary);
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
  font-size: 13px;
  color: var(--text-secondary);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: color 0.15s, background 0.15s;
}

.action-btn:hover {
  color: var(--color-primary);
  background: rgba(99, 102, 241, 0.06);
}

.action-btn.active {
  color: var(--color-primary);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

/* 详情对话框 */
.detail-header {
  padding-right: 32px;
}

.detail-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-author {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-primary);
}

.detail-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.detail-body {
  padding-top: 8px;
}

.recommend-quote {
  margin: 0 0 16px;
  padding: 12px 16px;
  background: rgba(99, 102, 241, 0.04);
  border-left: 3px solid var(--color-primary);
  border-radius: 0 4px 4px 0;
  font-size: 14px;
  color: var(--text-secondary);
  font-style: italic;
  line-height: 1.6;
}

.node-summary {
  margin-bottom: 16px;
}

.node-summary h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin: 0 0 8px;
}

.node-summary p {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.7;
  margin: 0;
}

.detail-actions {
  display: flex;
  gap: 10px;
  padding: 16px 0;
  border-top: 1px solid var(--border-lighter);
  border-bottom: 1px solid var(--border-lighter);
  margin-bottom: 16px;
}

/* 评论区 */
.comment-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.comment-item {
  padding: 12px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
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
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.comment-time {
  font-size: 11px;
  color: var(--text-secondary);
  margin-left: 8px;
}

.comment-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

.comment-empty {
  text-align: center;
  padding: 24px;
  color: var(--text-secondary);
  font-size: 13px;
}

.comment-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}
</style>
