<template>
  <main class="community-page">
    <template v-if="!currentQuestion">
      <header class="community-hero">
        <div class="hero-copy">
          <span class="eyebrow">KNOWLEDGE COMMONS</span>
          <h1>把一个人的困惑，<br /><em>变成一群人的知识。</em></h1>
          <p>提出真实问题，分享经过实践的回答，也让你的个人知识在需要它的人那里产生价值。</p>
        </div>
        <button class="ask-primary" type="button" @click="showQuestionDrawer = true">
          <el-icon><EditPen /></el-icon>
          提出问题
        </button>
      </header>

      <div class="community-layout">
        <section class="question-column" aria-labelledby="question-feed-title">
          <div class="feed-toolbar">
            <div>
              <h2 id="question-feed-title">正在发生的思考</h2>
              <p>从问题出发，遇见真正懂它的人</p>
            </div>
            <div class="feed-filters">
              <el-segmented v-model="sort" :options="sortOptions" @change="loadQuestions" />
              <el-input
                v-model="keyword"
                clearable
                aria-label="搜索问题"
                placeholder="搜索问题或领域"
                @keyup.enter="loadQuestions"
                @clear="loadQuestions"
              >
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
          </div>

          <div v-loading="loading" class="question-feed">
            <article
              v-for="question in questions"
              :key="question.id"
              class="question-card"
              tabindex="0"
              @click="openQuestion(question.id)"
              @keyup.enter="openQuestion(question.id)"
            >
              <div class="question-signal" :class="{ answered: question.answerCount > 0 }">
                <strong>{{ question.answerCount }}</strong>
                <span>{{ question.answerCount > 0 ? "个回答" : "待回答" }}</span>
              </div>
              <div class="question-main">
                <div class="author-line">
                  <el-avatar :size="28" :src="question.authorAvatar">
                    {{ question.authorName?.charAt(0) }}
                  </el-avatar>
                  <router-link
                    :to="`/community/users/${question.authorId}`"
                    class="author-link"
                    @click.stop="rememberCommunityState"
                  >{{ question.authorName }}</router-link>
                  <time>{{ formatTime(question.createTime) }}</time>
                </div>
                <h3>{{ question.title }}</h3>
                <p>{{ question.content }}</p>
                <div class="question-footer">
                  <div class="tag-list">
                    <span v-for="tag in question.tags" :key="tag" class="topic-tag">{{ tag }}</span>
                  </div>
                  <span class="view-count"><el-icon><View /></el-icon>{{ question.viewCount || 0 }}</span>
                </div>
              </div>
              <el-icon class="question-arrow"><ArrowRight /></el-icon>
            </article>

            <div v-if="!loading && questions.length === 0" class="empty-panel">
              <el-icon><ChatDotRound /></el-icon>
              <h3>{{ keyword ? "没有找到相关问题" : "这里还没有问题" }}</h3>
              <p>一个具体、真诚的问题，往往是知识连接的开始。</p>
              <el-button type="primary" @click="showQuestionDrawer = true">提出第一个问题</el-button>
            </div>
          </div>

          <el-pagination
            v-if="pagination.total > pagination.size"
            v-model:current-page="pagination.current"
            :page-size="pagination.size"
            :total="pagination.total"
            layout="prev, pager, next"
            @current-change="loadQuestions"
          />
        </section>

        <aside class="community-aside">
          <section class="aside-note">
            <span class="aside-index">01</span>
            <h2>知识不是表演</h2>
            <p>清楚说明你的依据、边界和亲身经验，比给出一个听起来完美的答案更重要。</p>
          </section>
          <section class="aside-rules">
            <h3>社区回答原则</h3>
            <ol>
              <li><span>01</span>回应真实问题，而不是堆砌术语</li>
              <li><span>02</span>引用知识前确认公开内容</li>
              <li><span>03</span>尊重不同经验和认知边界</li>
            </ol>
          </section>
        </aside>
      </div>
    </template>

    <template v-else>
      <button class="back-button" type="button" @click="closeQuestion">
        <el-icon><ArrowLeft /></el-icon>返回问题列表
      </button>
      <div class="detail-layout">
        <article class="question-detail">
          <div class="detail-author">
            <el-avatar :size="38" :src="currentQuestion.authorAvatar">
              {{ currentQuestion.authorName?.charAt(0) }}
            </el-avatar>
            <div>
              <router-link
                :to="`/community/users/${currentQuestion.authorId}`"
                class="author-link strong"
                @click="rememberCommunityState"
              >{{ currentQuestion.authorName }}</router-link>
              <time>{{ formatTime(currentQuestion.createTime) }}</time>
            </div>
          </div>
          <h1>{{ currentQuestion.title }}</h1>
          <div class="tag-list">
            <span v-for="tag in currentQuestion.tags" :key="tag" class="topic-tag">{{ tag }}</span>
          </div>
          <p class="question-content">{{ currentQuestion.content }}</p>
          <div class="detail-stats">
            <span>{{ currentQuestion.answerCount }} 个回答</span>
            <span>{{ currentQuestion.viewCount }} 次浏览</span>
          </div>
        </article>

        <section class="answers-section">
          <header><h2>来自社区的回答</h2><span>{{ currentQuestion.answers?.length || 0 }}</span></header>
          <article
            v-for="answer in currentQuestion.answers"
            :key="answer.id"
            class="answer-card"
            :class="{ accepted: answer.accepted }"
          >
            <div v-if="answer.accepted" class="accepted-label"><el-icon><CircleCheck /></el-icon>已采纳</div>
            <div class="answer-author">
              <el-avatar :size="32" :src="answer.authorAvatar">{{ answer.authorName?.charAt(0) }}</el-avatar>
              <router-link
                :to="`/community/users/${answer.authorId}`"
                class="author-link strong"
                @click="rememberCommunityState"
              >{{ answer.authorName }}</router-link>
              <time>{{ formatTime(answer.createTime) }}</time>
            </div>
            <div class="markdown-body" v-html="renderMarkdown(answer.content)"></div>
            <div v-if="answer.knowledgeSnapshots?.length" class="knowledge-snapshots">
              <div class="snapshot-heading"><el-icon><Collection /></el-icon>回答者分享的知识点</div>
              <details v-for="snapshot in answer.knowledgeSnapshots" :key="snapshot.sourceId" class="snapshot-card">
                <summary>{{ snapshot.title }}</summary>
                <p v-if="snapshot.summary">{{ snapshot.summary }}</p>
                <div class="markdown-body compact" v-html="renderMarkdown(snapshot.content)"></div>
              </details>
            </div>
            <el-button
              v-if="canAccept(answer)"
              class="accept-button"
              plain
              type="success"
              @click="acceptAnswer(answer.id)"
            >采纳这个回答</el-button>
          </article>
          <div v-if="!currentQuestion.answers?.length" class="answer-empty">还没有回答。你的经验也许正是提问者需要的。</div>
        </section>

        <section class="answer-composer">
          <div class="composer-heading"><span>YOUR PERSPECTIVE</span><h2>分享你的理解</h2></div>
          <label for="answer-content">回答内容</label>
          <el-input id="answer-content" v-model="answerForm.content" type="textarea" :rows="8" maxlength="10000" show-word-limit />
          <label for="knowledge-select">附带个人知识点 <small>可选，最多 5 个</small></label>
          <el-select
            id="knowledge-select"
            v-model="answerForm.knowledgeNodeIds"
            multiple
            collapse-tags
            :max-collapse-tags="2"
            placeholder="选择要公开分享的知识点"
            @visible-change="loadKnowledgeNodes"
          >
            <el-option v-for="node in knowledgeNodes" :key="node.id" :label="node.title" :value="node.id" />
          </el-select>
          <p v-if="answerForm.knowledgeNodeIds.length" class="privacy-notice">
            <el-icon><Warning /></el-icon>
            发布后，所选知识点的当前标题、摘要和正文将作为公开快照附在回答中。
          </p>
          <div class="composer-actions">
            <span>支持 Markdown</span>
            <el-button type="primary" :loading="answerSubmitting" @click="submitAnswer">发布回答</el-button>
          </div>
        </section>
      </div>
    </template>

    <el-drawer v-model="showQuestionDrawer" title="提出一个值得讨论的问题" size="min(520px, 100%)">
      <el-form label-position="top">
        <el-form-item label="问题标题" required>
          <el-input v-model="questionForm.title" maxlength="120" show-word-limit placeholder="具体说明你真正想解决的问题" />
        </el-form-item>
        <el-form-item label="背景与细节" required>
          <el-input v-model="questionForm.content" type="textarea" :rows="10" maxlength="5000" show-word-limit placeholder="你已经尝试了什么？问题发生在什么场景？" />
        </el-form-item>
        <el-form-item label="知识领域标签">
          <el-select v-model="questionForm.tags" multiple filterable allow-create default-first-option :multiple-limit="5" placeholder="输入并创建标签">
            <el-option v-for="tag in suggestedTags" :key="tag" :label="tag" :value="tag" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="showQuestionDrawer = false">取消</el-button>
          <el-button type="primary" :loading="questionSubmitting" @click="submitQuestion">发布问题</el-button>
        </div>
      </template>
    </el-drawer>
  </main>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowLeft, ArrowRight, ChatDotRound, CircleCheck, Collection, EditPen, Search, View, Warning } from "@element-plus/icons-vue";
import { communityAPI } from "@/api/community";
import { knowledgeAPI } from "@/api/knowledge";
import { renderMarkdown } from "@/utils/markdown";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();
const route = useRoute();
const COMMUNITY_STATE_KEY = "community-question-state";
const loading = ref(false);
const questions = ref([]);
const currentQuestion = ref(null);
const sort = ref("newest");
const keyword = ref("");
const pagination = reactive({ current: 1, size: 10, total: 0 });
const sortOptions = [{ label: "最新", value: "newest" }, { label: "待回答", value: "unanswered" }];
const suggestedTags = ["课程学习", "科研", "竞赛", "实习", "编程", "考研"];

const showQuestionDrawer = ref(false);
const questionSubmitting = ref(false);
const questionForm = reactive({ title: "", content: "", tags: [] });
const answerSubmitting = ref(false);
const answerForm = reactive({ content: "", knowledgeNodeIds: [] });
const knowledgeNodes = ref([]);

const loadQuestions = async () => {
  loading.value = true;
  try {
    const data = await communityAPI.listQuestions({ sort: sort.value, keyword: keyword.value || undefined, current: pagination.current, size: pagination.size });
    questions.value = data.records || [];
    pagination.total = data.total || 0;
  } catch (error) {
    ElMessage.error("问题加载失败：" + (error.message || "请稍后重试"));
  } finally {
    loading.value = false;
  }
};

const openQuestion = async (id) => {
  try {
    currentQuestion.value = await communityAPI.getQuestion(id);
    window.scrollTo({ top: 0, behavior: "smooth" });
  } catch (error) {
    ElMessage.error("问题详情加载失败：" + (error.message || "请稍后重试"));
  }
};

const closeQuestion = () => {
  currentQuestion.value = null;
  answerForm.content = "";
  answerForm.knowledgeNodeIds = [];
  loadQuestions();
};

const submitQuestion = async () => {
  if (questionForm.title.trim().length < 5 || questionForm.content.trim().length < 10) {
    ElMessage.warning("请填写至少 5 字的标题和至少 10 字的问题描述");
    return;
  }
  questionSubmitting.value = true;
  try {
    const created = await communityAPI.createQuestion({ title: questionForm.title.trim(), content: questionForm.content.trim(), tags: questionForm.tags });
    showQuestionDrawer.value = false;
    Object.assign(questionForm, { title: "", content: "", tags: [] });
    await openQuestion(created.id);
    ElMessage.success("问题已发布");
  } catch (error) {
    ElMessage.error("发布失败：" + (error.message || "请检查填写内容"));
  } finally {
    questionSubmitting.value = false;
  }
};

const loadKnowledgeNodes = async (visible) => {
  if (!visible || knowledgeNodes.value.length) return;
  try {
    const data = await knowledgeAPI.getList({ current: 1, size: 100 });
    knowledgeNodes.value = data.records || [];
  } catch (error) {
    ElMessage.error("个人知识点加载失败：" + (error.message || "请稍后重试"));
  }
};

const submitAnswer = async () => {
  if (answerForm.content.trim().length < 10) {
    ElMessage.warning("请至少用 10 个字符说明你的观点");
    return;
  }
  answerSubmitting.value = true;
  try {
    await communityAPI.createAnswer(currentQuestion.value.id, { content: answerForm.content.trim(), knowledgeNodeIds: answerForm.knowledgeNodeIds });
    answerForm.content = "";
    answerForm.knowledgeNodeIds = [];
    await openQuestion(currentQuestion.value.id);
    ElMessage.success("回答已发布");
  } catch (error) {
    ElMessage.error("回答发布失败：" + (error.message || "请稍后重试"));
  } finally {
    answerSubmitting.value = false;
  }
};

const canAccept = (answer) => currentQuestion.value?.authorId === userStore.userInfo.id
  && !currentQuestion.value.acceptedAnswerId
  && answer.authorId !== userStore.userInfo.id;

const acceptAnswer = async (answerId) => {
  try {
    await communityAPI.acceptAnswer(currentQuestion.value.id, answerId);
    await openQuestion(currentQuestion.value.id);
    ElMessage.success("已采纳回答");
  } catch (error) {
    ElMessage.error("采纳失败：" + (error.message || "请稍后重试"));
  }
};

const formatTime = (value) => {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }).format(new Date(value));
};

const rememberCommunityState = () => {
  sessionStorage.setItem(COMMUNITY_STATE_KEY, JSON.stringify({
    sort: sort.value,
    keyword: keyword.value,
    current: pagination.current,
    questionId: currentQuestion.value?.id || null,
    scrollY: window.scrollY,
  }));
};

const restoreCommunityState = async () => {
  const directQuestionId = route.query.question;
  if (directQuestionId) {
    await loadQuestions();
    await openQuestion(directQuestionId);
    return;
  }
  let saved = null;
  try {
    saved = JSON.parse(sessionStorage.getItem(COMMUNITY_STATE_KEY) || "null");
  } catch {
    sessionStorage.removeItem(COMMUNITY_STATE_KEY);
  }
  if (!saved) {
    await loadQuestions();
    return;
  }
  sort.value = saved.sort || "newest";
  keyword.value = saved.keyword || "";
  pagination.current = saved.current || 1;
  await loadQuestions();
  if (saved.questionId) await openQuestion(saved.questionId);
  await nextTick();
  window.scrollTo({ top: saved.scrollY || 0, behavior: "auto" });
  sessionStorage.removeItem(COMMUNITY_STATE_KEY);
};

onMounted(restoreCommunityState);
</script>

<style scoped>
.community-page { max-width: 1240px; margin: 0 auto; padding: 36px 32px 72px; color: var(--text-primary); }
.community-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 40px; padding: 36px 0 44px; border-bottom: 1px solid var(--border-light); }
.hero-copy { max-width: 760px; }
.eyebrow, .composer-heading span { color: var(--color-primary); font-size: 12px; font-weight: 700; letter-spacing: .16em; }
.hero-copy h1 { margin: 14px 0 18px; font-size: clamp(38px, 5vw, 68px); line-height: 1.06; letter-spacing: -.045em; }
.hero-copy h1 em { color: var(--color-primary); font-style: normal; }
.hero-copy p { max-width: 660px; margin: 0; color: var(--text-secondary); font-size: 17px; line-height: 1.75; }
.ask-primary { display: inline-flex; align-items: center; gap: 9px; min-height: 48px; padding: 0 22px; border: 0; border-radius: 8px; background: var(--color-primary); color: white; font: inherit; font-weight: 650; cursor: pointer; box-shadow: 0 10px 24px var(--color-primary-alpha-20); transition: transform .2s ease, box-shadow .2s ease; }
.ask-primary:hover { transform: translateY(-2px); box-shadow: 0 14px 30px var(--color-primary-alpha-20); }
.community-layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 56px; padding-top: 38px; }
.feed-toolbar { display: flex; justify-content: space-between; align-items: flex-end; gap: 24px; margin-bottom: 24px; }
.feed-toolbar h2, .answers-section h2 { margin: 0; font-size: 25px; letter-spacing: -.02em; }
.feed-toolbar p { margin: 7px 0 0; color: var(--text-muted); }
.feed-filters { display: flex; gap: 10px; width: min(420px, 50%); }
.question-feed { min-height: 280px; border-top: 1px solid var(--border-light); }
.author-link { min-height: 44px; display: inline-flex; align-items: center; color: inherit; text-decoration: none; font-weight: 600; }
.author-link:hover { color: var(--color-primary); text-decoration: underline; text-underline-offset: 3px; }
.author-link:focus-visible { border-radius: 4px; outline: 3px solid var(--color-primary-alpha-20); outline-offset: 2px; }
.author-link.strong { font-weight: 700; }
.question-card { position: relative; display: grid; grid-template-columns: 78px minmax(0, 1fr) 24px; gap: 20px; padding: 26px 4px; border-bottom: 1px solid var(--border-light); cursor: pointer; transition: background .2s ease, padding .2s ease; }
.question-card:hover, .question-card:focus-visible { padding-inline: 14px; background: var(--bg-card); outline: none; }
.question-card:focus-visible { box-shadow: inset 3px 0 var(--color-primary); }
.question-signal { display: flex; flex-direction: column; align-items: center; justify-content: center; align-self: start; min-height: 62px; border: 1px solid var(--border-light); border-radius: 7px; color: var(--text-muted); }
.question-signal.answered { border-color: #86c9b2; color: #167756; background: #eefaf5; }
.question-signal strong { font-size: 20px; line-height: 1; }
.question-signal span { margin-top: 7px; font-size: 11px; }
.author-line, .answer-author, .detail-author { display: flex; align-items: center; gap: 9px; color: var(--text-secondary); font-size: 13px; }
.author-line time, .answer-author time { margin-left: 4px; color: var(--text-muted); }
.question-main h3 { margin: 13px 0 8px; font-size: 19px; line-height: 1.35; }
.question-main > p { display: -webkit-box; overflow: hidden; margin: 0; color: var(--text-secondary); line-height: 1.65; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.question-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 7px; }
.topic-tag { padding: 4px 9px; border-radius: 4px; background: var(--color-primary-alpha-10); color: var(--color-primary); font-size: 12px; }
.view-count { display: inline-flex; align-items: center; gap: 5px; color: var(--text-muted); font-size: 12px; }
.question-arrow { align-self: center; color: var(--text-muted); transition: transform .2s ease; }
.question-card:hover .question-arrow { transform: translateX(4px); color: var(--color-primary); }
.community-aside { position: sticky; top: 92px; align-self: start; }
.aside-note { padding: 28px; background: #172033; color: #fff; border-radius: 8px; }
.aside-index { color: #f0ad4e; font: 700 13px/1 monospace; }
.aside-note h2 { margin: 32px 0 12px; font-size: 25px; }
.aside-note p { margin: 0; color: #cbd3df; line-height: 1.7; }
.aside-rules { margin-top: 28px; }
.aside-rules h3 { font-size: 14px; }
.aside-rules ol { padding: 0; margin: 16px 0; list-style: none; }
.aside-rules li { display: grid; grid-template-columns: 28px 1fr; gap: 10px; padding: 12px 0; border-top: 1px solid var(--border-light); color: var(--text-secondary); font-size: 13px; line-height: 1.5; }
.aside-rules li span { color: var(--color-primary); font-family: monospace; }
.empty-panel { padding: 72px 20px; text-align: center; }
.empty-panel > .el-icon { font-size: 38px; color: var(--color-primary); }
.empty-panel h3 { margin: 16px 0 8px; }
.empty-panel p { color: var(--text-muted); }
.back-button { display: inline-flex; align-items: center; gap: 7px; min-height: 44px; padding: 0; border: 0; background: none; color: var(--text-secondary); cursor: pointer; }
.back-button:hover { color: var(--color-primary); }
.detail-layout { max-width: 860px; margin: 20px auto 0; }
.question-detail { padding: 38px 0 34px; border-bottom: 1px solid var(--border-light); }
.detail-author div { display: flex; flex-direction: column; gap: 3px; }
.detail-author time { color: var(--text-muted); font-size: 12px; }
.question-detail h1 { margin: 24px 0 16px; font-size: clamp(30px, 4vw, 48px); line-height: 1.18; letter-spacing: -.035em; }
.question-content { margin: 30px 0; font-size: 17px; line-height: 1.85; white-space: pre-wrap; }
.detail-stats { display: flex; gap: 18px; color: var(--text-muted); font-size: 13px; }
.answers-section { margin-top: 42px; }
.answers-section > header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.answers-section > header span { color: var(--text-muted); }
.answer-card { position: relative; padding: 32px 0; border-bottom: 1px solid var(--border-light); }
.answer-card.accepted { padding-inline: 24px; border: 1px solid #9bd8c2; border-radius: 8px; background: #f5fcf9; }
.accepted-label { position: absolute; top: 18px; right: 20px; display: inline-flex; gap: 5px; color: #167756; font-size: 13px; font-weight: 650; }
.markdown-body { margin-top: 20px; color: var(--text-regular); font-size: 16px; line-height: 1.8; }
.markdown-body :deep(p:first-child) { margin-top: 0; }
.knowledge-snapshots { margin-top: 24px; padding: 20px; border-left: 3px solid var(--color-primary); background: var(--bg-page); }
.snapshot-heading { display: flex; align-items: center; gap: 7px; margin-bottom: 12px; color: var(--color-primary); font-size: 13px; font-weight: 650; }
.snapshot-card { padding: 12px 0; border-top: 1px solid var(--border-light); }
.snapshot-card summary { font-weight: 650; cursor: pointer; }
.snapshot-card > p { color: var(--text-secondary); }
.markdown-body.compact { font-size: 14px; }
.accept-button { margin-top: 20px; }
.answer-empty { padding: 46px 0; color: var(--text-muted); text-align: center; border-bottom: 1px solid var(--border-light); }
.answer-composer { margin-top: 48px; padding: 30px; border: 1px solid var(--border-light); border-radius: 8px; background: var(--bg-card); }
.composer-heading h2 { margin: 7px 0 24px; font-size: 24px; }
.answer-composer label { display: block; margin: 20px 0 8px; font-size: 14px; font-weight: 650; }
.answer-composer label small { color: var(--text-muted); font-weight: 400; }
.answer-composer .el-select { width: 100%; }
.privacy-notice { display: flex; align-items: flex-start; gap: 8px; padding: 12px; color: #8a5a00; background: #fff8e8; border-radius: 6px; font-size: 13px; line-height: 1.5; }
.composer-actions, .drawer-actions { display: flex; justify-content: flex-end; align-items: center; gap: 12px; margin-top: 20px; }
.composer-actions > span { margin-right: auto; color: var(--text-muted); font-size: 12px; }

@media (max-width: 900px) { .community-layout { grid-template-columns: 1fr; } .community-aside { display: none; } .community-hero { align-items: flex-start; flex-direction: column; } }
@media (max-width: 640px) { .community-page { padding: 20px 16px 56px; } .hero-copy h1 { font-size: 38px; } .feed-toolbar { align-items: stretch; flex-direction: column; } .feed-filters { width: 100%; flex-direction: column; } .question-card { grid-template-columns: 58px minmax(0, 1fr); gap: 13px; } .question-arrow { display: none; } .question-main h3 { font-size: 17px; } .answer-composer { padding: 20px 16px; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { scroll-behavior: auto !important; transition-duration: .01ms !important; } }
</style>
