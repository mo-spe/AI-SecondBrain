<template>
  <main v-loading="loading" class="profile-page">
    <button class="back-link" type="button" @click="router.back()">
      <el-icon><ArrowLeft /></el-icon>返回
    </button>

    <div v-if="profile" class="profile-grid">
      <aside class="identity-panel">
        <div class="identity-topline"><span>COMMUNITY PROFILE</span><span>#{{ profile.userId }}</span></div>
        <el-avatar class="profile-avatar" :size="88" :src="profile.avatar">
          {{ profile.username?.charAt(0) }}
        </el-avatar>
        <h1>{{ profile.username }}</h1>
        <p v-if="profile.isFollowedBy && !profile.isSelf && !profile.isBlocked" class="relationship-note">也关注了你</p>
        <p class="introduction">{{ profile.introduction || "这个人还没有写社区简介。" }}</p>
        <div v-if="profile.expertiseTags?.length" class="expertise-tags" aria-label="擅长领域">
          <span v-for="tag in profile.expertiseTags" :key="tag">{{ tag }}</span>
        </div>

        <div class="profile-actions">
          <button v-if="profile.isSelf" class="primary-action" type="button" @click="openEditor">编辑社区资料</button>
          <template v-else-if="!profile.isBlocked">
            <button
              class="primary-action"
              :class="{ following: profile.isFollowing }"
              type="button"
              :disabled="actionLoading"
              :aria-label="profile.isFollowing ? `取消关注${profile.username}` : `关注${profile.username}`"
              @click="toggleFollow"
            >{{ profile.isFollowing ? "已关注" : "+ 关注" }}</button>
            <button class="secondary-action danger" type="button" :disabled="actionLoading" @click="confirmBlock">拉黑</button>
          </template>
          <button v-else-if="profile.isBlockedByMe" class="secondary-action" type="button" :disabled="actionLoading" @click="unblock">解除拉黑</button>
          <span v-else class="blocked-note">对方已限制与你互动</span>
        </div>

        <div v-if="!profile.isBlocked" class="relation-counts">
          <button type="button" @click="openRelations('followers')"><strong>{{ profile.followerCount || 0 }}</strong><span>粉丝</span></button>
          <button type="button" @click="openRelations('following')"><strong>{{ profile.followingCount || 0 }}</strong><span>关注</span></button>
        </div>
      </aside>

      <section class="contribution-panel">
        <div v-if="profile.isBlocked" class="restricted-panel">
          <span>INTERACTION LIMITED</span>
          <h2>互动内容暂不可见</h2>
          <p>拉黑关系存在时，仅展示最小公开身份信息。</p>
        </div>
        <template v-else>
          <section class="impact-strip" aria-label="社区贡献数据">
            <div><strong>{{ profile.answerCount || 0 }}</strong><span>回答</span></div>
            <div><strong>{{ profile.acceptedAnswerCount || 0 }}</strong><span>被采纳</span></div>
            <div><strong>{{ profile.questionCount || 0 }}</strong><span>问题</span></div>
            <div><strong>{{ profile.knowledgePostCount || 0 }}</strong><span>知识文章</span></div>
            <div><strong>{{ (profile.receivedLikeCount || 0) + (profile.receivedBookmarkCount || 0) }}</strong><span>赞与收藏</span></div>
          </section>

          <div class="content-heading">
            <div><span>PUBLIC CONTRIBUTIONS</span><h2>最近贡献</h2></div>
            <el-segmented v-model="activeTab" :options="tabOptions" />
          </div>

          <div class="contribution-list">
            <button
              v-for="item in activeContributions"
              :key="`${item.type}-${item.id}`"
              class="contribution-card"
              type="button"
              @click="openContribution(item)"
            >
              <span class="content-type">{{ contributionType(item.type) }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary }}</p>
              <time>{{ formatDate(item.createTime) }}</time>
            </button>
            <div v-if="activeContributions.length === 0" class="empty-contributions">
              <span>NO PUBLIC CONTRIBUTIONS</span>
              <h3>还没有公开内容</h3>
              <p>当用户发布问题、回答或知识文章后，会展示在这里。</p>
            </div>
          </div>
        </template>
      </section>
    </div>

    <el-drawer v-model="editorVisible" title="编辑社区资料" size="min(520px, 100%)">
      <el-form label-position="top">
        <el-form-item label="社区简介">
          <el-input v-model="editForm.introduction" type="textarea" :rows="7" maxlength="500" show-word-limit placeholder="介绍你的关注方向、经验和愿意分享的内容" />
        </el-form-item>
        <el-form-item label="擅长领域（最多 8 个）">
          <el-select v-model="editForm.expertiseTags" multiple filterable allow-create default-first-option :multiple-limit="8" placeholder="输入领域后按回车">
            <el-option v-for="tag in suggestedTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
        <p class="privacy-copy">这里只维护公开社区资料。邮箱、手机号和账户安全信息不会展示或在这里修改。</p>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="saveProfile">保存公开资料</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="relationsVisible" :title="relationMode === 'followers' ? '粉丝' : '关注的人'" size="min(440px, 100%)">
      <div v-loading="relationsLoading" class="relation-list">
        <button v-for="item in relations" :key="item.userId" type="button" @click="visitUser(item.userId)">
          <el-avatar :size="44" :src="item.avatar">{{ item.username?.charAt(0) }}</el-avatar>
          <span><strong>{{ item.username }}</strong><small>{{ item.introduction || "暂无社区简介" }}</small></span>
          <em v-if="item.isFollowing">已关注</em>
        </button>
        <el-empty v-if="!relationsLoading && relations.length === 0" description="这里还没有用户" />
      </div>
    </el-drawer>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft } from "@element-plus/icons-vue";
import { communityAPI } from "@/api/community";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const actionLoading = ref(false);
const profile = ref(null);
const activeTab = ref("answers");
const tabOptions = [
  { label: "回答", value: "answers" },
  { label: "问题", value: "questions" },
  { label: "知识文章", value: "knowledge" },
];
const suggestedTags = ["课程学习", "科研", "竞赛", "实习", "编程", "考研"];
const editorVisible = ref(false);
const editForm = reactive({ introduction: "", expertiseTags: [] });
const relationsVisible = ref(false);
const relationsLoading = ref(false);
const relationMode = ref("followers");
const relations = ref([]);

const activeContributions = computed(() => {
  if (!profile.value) return [];
  if (activeTab.value === "questions") return profile.value.recentQuestions || [];
  if (activeTab.value === "knowledge") return profile.value.recentKnowledgePosts || [];
  return profile.value.recentAnswers || [];
});

const loadProfile = async () => {
  loading.value = true;
  try {
    profile.value = await communityAPI.getUserProfile(route.params.id);
  } catch (error) {
    ElMessage.error("个人主页加载失败：" + (error.message || "请稍后重试"));
  } finally {
    loading.value = false;
  }
};

const toggleFollow = async () => {
  actionLoading.value = true;
  const wasFollowing = profile.value.isFollowing;
  profile.value.isFollowing = !wasFollowing;
  profile.value.followerCount += wasFollowing ? -1 : 1;
  try {
    if (wasFollowing) await communityAPI.unfollowUser(profile.value.userId);
    else await communityAPI.followUser(profile.value.userId);
  } catch (error) {
    profile.value.isFollowing = wasFollowing;
    profile.value.followerCount += wasFollowing ? 1 : -1;
    ElMessage.error("关注状态更新失败：" + (error.message || "请稍后重试"));
  } finally {
    actionLoading.value = false;
  }
};

const confirmBlock = async () => {
  try {
    await ElMessageBox.confirm("拉黑后双方关注关系会解除，也不能再关注对方。", `拉黑 ${profile.value.username}？`, {
      confirmButtonText: "确认拉黑", cancelButtonText: "取消", type: "warning",
    });
  } catch { return; }
  actionLoading.value = true;
  try {
    await communityAPI.blockUser(profile.value.userId);
    await loadProfile();
    ElMessage.success("已拉黑该用户");
  } finally {
    actionLoading.value = false;
  }
};

const unblock = async () => {
  actionLoading.value = true;
  try {
    await communityAPI.unblockUser(profile.value.userId);
    await loadProfile();
    ElMessage.success("已解除拉黑");
  } finally {
    actionLoading.value = false;
  }
};

const openEditor = () => {
  editForm.introduction = profile.value.introduction || "";
  editForm.expertiseTags = [...(profile.value.expertiseTags || [])];
  editorVisible.value = true;
};

const saveProfile = async () => {
  if (editForm.expertiseTags.some((tag) => tag.trim().length > 30)) {
    ElMessage.warning("单个领域标签不能超过 30 个字符");
    return;
  }
  actionLoading.value = true;
  try {
    profile.value = await communityAPI.updateMyProfile({
      introduction: editForm.introduction.trim(),
      expertiseTags: editForm.expertiseTags.map((tag) => tag.trim()).filter(Boolean),
    });
    editorVisible.value = false;
    ElMessage.success("社区资料已更新");
  } finally {
    actionLoading.value = false;
  }
};

const openRelations = async (mode) => {
  relationMode.value = mode;
  relationsVisible.value = true;
  relationsLoading.value = true;
  try {
    const loader = mode === "followers" ? communityAPI.getFollowers : communityAPI.getFollowing;
    const data = await loader(profile.value.userId, { current: 1, size: 50 });
    relations.value = data.records || [];
  } finally {
    relationsLoading.value = false;
  }
};

const visitUser = (userId) => {
  relationsVisible.value = false;
  router.push(`/community/users/${userId}`);
};

const openContribution = (item) => {
  if (item.type === "QUESTION" || item.type === "ANSWER") {
    router.push({ path: "/community", query: { question: item.id } });
    return;
  }
  router.push("/square");
};

const contributionType = (type) => ({ ANSWER: "回答", QUESTION: "问题", KNOWLEDGE: "知识文章" }[type] || "贡献");
const formatDate = (value) => value ? new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "short", day: "numeric" }).format(new Date(value)) : "";

watch(() => route.params.id, loadProfile);
onMounted(loadProfile);
</script>

<style scoped>
.profile-page { max-width: 1240px; min-height: 70vh; margin: 0 auto; padding: 28px 32px 72px; color: var(--text-primary); }
.back-link { display: inline-flex; align-items: center; gap: 7px; min-height: 44px; padding: 0; border: 0; background: transparent; color: var(--text-secondary); font: inherit; cursor: pointer; }
.back-link:focus-visible, button:focus-visible { outline: 3px solid var(--color-primary-alpha-20); outline-offset: 3px; }
.profile-grid { display: grid; grid-template-columns: 330px minmax(0, 1fr); gap: 64px; margin-top: 24px; }
.identity-panel { position: sticky; top: 88px; align-self: start; padding: 28px; border: 1px solid var(--border-light); border-radius: 12px; background: var(--bg-card); }
.identity-topline { display: flex; justify-content: space-between; margin-bottom: 28px; color: var(--text-muted); font-size: 10px; font-weight: 700; letter-spacing: .13em; }
.profile-avatar { border: 4px solid var(--bg-primary); box-shadow: 0 12px 30px rgba(15, 23, 42, .13); }
.identity-panel h1 { margin: 20px 0 5px; font-size: 30px; letter-spacing: -.035em; }
.relationship-note { margin: 0 0 14px; color: var(--color-primary); font-size: 13px; }
.introduction { min-height: 48px; margin: 18px 0; color: var(--text-secondary); line-height: 1.7; white-space: pre-wrap; }
.expertise-tags { display: flex; flex-wrap: wrap; gap: 7px; }
.expertise-tags span { padding: 6px 9px; border-radius: 4px; background: var(--color-primary-alpha-10); color: var(--color-primary); font-size: 12px; }
.profile-actions { display: flex; gap: 8px; margin-top: 25px; }
.profile-actions button { min-height: 44px; }
.primary-action, .secondary-action { flex: 1; border: 1px solid var(--color-primary); border-radius: 7px; font: inherit; font-weight: 650; cursor: pointer; }
.primary-action { background: var(--color-primary); color: #fff; }
.primary-action.following { background: transparent; color: var(--color-primary); }
.secondary-action { background: transparent; color: var(--text-secondary); border-color: var(--border-light); }
.secondary-action.danger:hover { border-color: var(--color-danger); color: var(--color-danger); }
.blocked-note { display: flex; align-items: center; min-height: 44px; color: var(--text-muted); font-size: 13px; }
.relation-counts { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; margin-top: 25px; border-top: 1px solid var(--border-light); }
.relation-counts button { display: flex; flex-direction: column; align-items: flex-start; min-height: 66px; padding: 15px 0 0; border: 0; background: transparent; color: inherit; cursor: pointer; }
.relation-counts button + button { padding-left: 20px; border-left: 1px solid var(--border-light); }
.relation-counts strong { font-size: 20px; }.relation-counts span { color: var(--text-muted); font-size: 12px; }
.contribution-panel { min-width: 0; }
.impact-strip { display: grid; grid-template-columns: repeat(5, 1fr); padding: 24px 0 30px; border-top: 1px solid var(--border-light); border-bottom: 1px solid var(--border-light); }
.impact-strip div { padding: 0 18px; border-right: 1px solid var(--border-light); }.impact-strip div:first-child { padding-left: 0; }.impact-strip div:last-child { border: 0; }
.impact-strip strong, .impact-strip span { display: block; }.impact-strip strong { font-size: clamp(24px, 3vw, 38px); letter-spacing: -.04em; }.impact-strip span { margin-top: 3px; color: var(--text-muted); font-size: 12px; }
.content-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 40px 0 18px; }
.content-heading > div > span, .restricted-panel > span, .empty-contributions > span { color: var(--color-primary); font-size: 10px; font-weight: 750; letter-spacing: .15em; }
.content-heading h2 { margin: 7px 0 0; font-size: 28px; }
.contribution-list { border-top: 1px solid var(--border-light); }
.contribution-card { display: block; width: 100%; min-height: 150px; padding: 23px 4px; border: 0; border-bottom: 1px solid var(--border-light); background: transparent; color: inherit; text-align: left; cursor: pointer; transition: padding .2s ease, background .2s ease; }
.contribution-card:hover { padding-inline: 14px; background: var(--bg-card); }
.content-type { color: var(--color-primary); font-size: 12px; font-weight: 700; }.contribution-card h3 { margin: 8px 0; font-size: 19px; }.contribution-card p { display: -webkit-box; overflow: hidden; margin: 0 0 12px; color: var(--text-secondary); line-height: 1.65; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }.contribution-card time { color: var(--text-muted); font-size: 12px; }
.empty-contributions, .restricted-panel { padding: 80px 24px; text-align: center; }.empty-contributions h3, .restricted-panel h2 { margin: 10px 0; }.empty-contributions p, .restricted-panel p { color: var(--text-muted); }
.privacy-copy { padding: 14px; border-left: 3px solid var(--color-primary); background: var(--color-primary-alpha-10); color: var(--text-secondary); font-size: 13px; line-height: 1.6; }
.relation-list button { display: grid; grid-template-columns: 44px minmax(0, 1fr) auto; align-items: center; gap: 13px; width: 100%; min-height: 68px; padding: 10px 4px; border: 0; border-bottom: 1px solid var(--border-light); background: transparent; color: inherit; text-align: left; cursor: pointer; }.relation-list span { min-width: 0; }.relation-list strong, .relation-list small { display: block; }.relation-list small { overflow: hidden; margin-top: 4px; color: var(--text-muted); text-overflow: ellipsis; white-space: nowrap; }.relation-list em { color: var(--color-primary); font-size: 12px; font-style: normal; }
@media (max-width: 900px) { .profile-grid { grid-template-columns: 1fr; gap: 32px; }.identity-panel { position: static; }.impact-strip { order: -1; } }
@media (max-width: 640px) { .profile-page { padding: 16px 16px 56px; }.profile-grid { margin-top: 12px; }.identity-panel { padding: 22px 20px; }.impact-strip { grid-template-columns: repeat(3, 1fr); row-gap: 22px; }.impact-strip div { padding: 0 10px; }.impact-strip div:nth-child(3) { border: 0; }.content-heading { align-items: stretch; flex-direction: column; }.content-heading :deep(.el-segmented) { width: 100%; }.profile-actions { flex-wrap: wrap; }.profile-actions button { min-width: 130px; } }
</style>
