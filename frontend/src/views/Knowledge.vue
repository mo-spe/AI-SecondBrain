<template>
  <div class="knowledge-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">知识点管理</h1>
        <span class="page-subtitle">管理和组织您的知识体系，让知识更有序、更易于发现</span>
        <el-tag class="knowledge-count" type="info" size="small">{{ pagination.total || 0 }}个知识点</el-tag>
      </div>
      <div class="header-right">
        <el-button type="default" size="default" @click="handleImport">
          <el-icon><Download /></el-icon>
          <span>导入知识点</span>
        </el-button>
        <el-dropdown trigger="click">
          <el-button type="default" size="default">
            <span>批量操作</span>
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleBatchDelete">批量删除</el-dropdown-item>
              <el-dropdown-item @click="handleBatchExport">批量导出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" size="default" @click="handleAddKnowledge">
          <el-icon><Plus /></el-icon>
          <span>添加知识点</span>
        </el-button>
      </div>
    </div>

    <div class="tab-section">
      <div class="tabs-nav">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'all' }"
          @click="handleTabChange('all')"
        >
          <el-icon size="16"><Grid /></el-icon>
          <span>全部</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'pending' }"
          @click="handleTabChange('pending')"
        >
          <el-icon size="16"><Clock /></el-icon>
          <span>待确认</span>
          <span v-if="pendingCount > 0" class="tab-badge">{{ pendingCount }}</span>
        </button>
      </div>
    </div>

    <template v-if="activeTab === 'pending'">

    <div class="pending-section">
      <div class="pending-header">
        <div class="pending-header-left">
          <el-icon size="18"><EditPen /></el-icon>
          <span class="pending-header-title">待确认知识点 — 来自对话采集</span>
        </div>
        <div class="pending-header-right">
          <span class="pending-workspace-hint">请在确认前编辑完善知识点内容</span>
        </div>
      </div>

      <div v-if="pendingItems.length === 0" class="pending-empty">
        <el-empty description="暂无待确认的知识点" :image-size="120" />
        <p class="pending-empty-hint">采集对话时开启"提取知识点"，AI 提取的内容会出现在这里</p>
      </div>

      <div v-else class="pending-list">
        <div
          v-for="(item, index) in pendingItems"
          :key="index"
          class="pending-card"
        >
          <div class="pending-card-header">
            <span class="pending-card-index">知识点 {{ index + 1 }}</span>
            <el-button
              type="danger"
              link
              size="small"
              @click="removePendingItem(index)"
            >
              <el-icon size="14"><Delete /></el-icon>
              <span>删除</span>
            </el-button>
          </div>
          <div class="pending-card-body">
            <div class="pending-field">
              <label class="pending-label">标题</label>
              <el-input
                v-model="item.title"
                placeholder="请输入知识点标题"
                maxlength="200"
                show-word-limit
              />
            </div>
            <div class="pending-field">
              <label class="pending-label">摘要</label>
              <el-input
                v-model="item.summary"
                type="textarea"
                :rows="2"
                placeholder="请输入摘要"
                maxlength="500"
                show-word-limit
              />
            </div>
            <div class="pending-field">
              <label class="pending-label">详细内容</label>
              <el-input
                v-model="item.content"
                type="textarea"
                :rows="5"
                placeholder="请输入详细内容，支持 Markdown"
                maxlength="10000"
                show-word-limit
              />
            </div>
            <TagSuggest
              :title="item.title"
              :summary="item.summary"
            />
          </div>
        </div>

        <button class="pending-add-btn" @click="addEmptyPendingItem">
          <el-icon size="16"><Plus /></el-icon>
          <span>新增知识点</span>
        </button>
      </div>

      <div v-if="pendingItems.length > 0" class="pending-footer">
        <el-checkbox v-model="pendingGenerateCards" size="large">
          确认入库时同时生成复习卡片
        </el-checkbox>
        <div class="pending-footer-actions">
          <el-button @click="discardAllPending" size="large">
            <el-icon><Delete /></el-icon>
            <span>全部删除</span>
          </el-button>
          <el-button type="primary" @click="confirmPending" size="large">
            <el-icon><Check /></el-icon>
            <span>确认入库</span>
          </el-button>
        </div>
      </div>
    </div>

    </template>

    <template v-else>

    <div class="filter-section">
      <div class="search-box">
        <el-icon size="16" color="#94a3b8"><Search /></el-icon>
        <input
          type="text"
          v-model="searchKeyword"
          placeholder="搜索知识点名称、描述、标签..."
          @keyup.enter="handleSearch"
        />
        <button class="search-btn" @click="handleSearch">
          <el-icon size="16"><Search /></el-icon>
        </button>
      </div>
      <el-select
        v-model="filterTag"
        placeholder="全部标签"
        size="default"
        clearable
        @change="handleSearch"
      >
        <el-option label="全部标签" value="" />
        <el-option v-for="tag in tagTree" :key="tag.id" :label="tag.tagName" :value="tag.id" />
      </el-select>
      <el-select
        v-model="filterImportance"
        placeholder="重要程度"
        size="default"
        clearable
        @change="handleSearch"
      >
        <el-option label="全部" value="" />
        <el-option label="非常重要" :value="5" />
        <el-option label="重要" :value="4" />
        <el-option label="一般" :value="3" />
        <el-option label="较低" :value="2" />
        <el-option label="很低" :value="1" />
      </el-select>
      <el-select
        v-model="filterMastery"
        placeholder="掌握程度"
        size="default"
        clearable
        @change="handleSearch"
      >
        <el-option label="全部" value="" />
        <el-option label="已掌握" :value="5" />
        <el-option label="精通" :value="4" />
        <el-option label="掌握" :value="3" />
        <el-option label="熟悉" :value="2" />
        <el-option label="入门" :value="1" />
        <el-option label="未掌握" :value="0" />
      </el-select>
      <el-button type="default" size="default" @click="handleAdvancedFilter">
        <el-icon><Filter /></el-icon>
        <span>筛选</span>
      </el-button>
    </div>

    <div class="stats-section">
      <div class="stat-card purple">
        <div class="stat-icon">
          <el-icon size="24"><Grid /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">全部知识点</div>
          <div class="stat-value">{{ statistics.total || 0 }}</div>
          <div class="stat-change">较昨日 <span class="increase">↑{{ statistics.increase || 0 }}</span></div>
        </div>
      </div>
      <div class="stat-card orange">
        <div class="stat-icon">
          <el-icon size="24"><Star /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">高重要知识点</div>
          <div class="stat-value">{{ statistics.highImportance || 0 }}</div>
          <div class="stat-change">占比 {{ statistics.highImportanceRatio || 0 }}%</div>
        </div>
      </div>
      <div class="stat-card green">
        <div class="stat-icon">
          <el-icon size="24"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">已掌握知识点</div>
          <div class="stat-value">{{ statistics.mastered || 0 }}</div>
          <div class="stat-change">占比 {{ statistics.masteredRatio || 0 }}%</div>
        </div>
      </div>
      <div class="stat-card blue">
        <div class="stat-icon">
          <el-icon size="24"><Cherry /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">待复习知识点</div>
          <div class="stat-value">{{ statistics.toReview || 0 }}</div>
          <div class="stat-change">较昨日 <span class="increase">↑{{ statistics.toReviewIncrease || 0 }}</span></div>
        </div>
      </div>
      <div class="stat-card red">
        <div class="stat-icon">
          <el-icon size="24"><Frown /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">未掌握知识点</div>
          <div class="stat-value">{{ statistics.notMastered || 0 }}</div>
          <div class="stat-change">占比 {{ statistics.notMasteredRatio || 0 }}%</div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <aside class="system-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">知识标签</span>
          <button class="sidebar-add" @click="handleCreateTag">
            <el-icon size="14"><Plus /></el-icon>
          </button>
        </div>
        <div class="system-list">
          <div
            class="system-item"
            :class="{ active: selectedTagId === '' }"
            @click="selectTag('')"
          >
            <span class="system-name">全部知识</span>
            <span class="system-count">{{ pagination.total || 0 }}</span>
          </div>
          <div
            v-for="tag in flatTagList"
            :key="tag.id"
            class="system-item"
            :class="{ active: selectedTagId === tag.id }"
            :style="{ paddingLeft: (12 + tag._depth * 16) + 'px' }"
            @click="selectTag(tag.id)"
            @contextmenu.prevent="handleTagContextMenu($event, tag)"
          >
            <span
              v-if="tag._hasChildren"
              class="tag-expand-btn"
              @click.stop="toggleTagExpand(tag.id)"
            >
              <el-icon size="10"><component :is="tag._isExpanded ? ArrowDown : ArrowRight" /></el-icon>
            </span>
            <span v-else class="tag-expand-spacer"></span>
            <span
              class="tag-color-dot"
              :style="{ background: tag.tagColor || '#6366f1' }"
            ></span>
            <span class="system-name">{{ tag.tagName }}</span>
            <span class="system-count">{{ tag.nodeCount || 0 }}</span>
          </div>
          <div v-if="tagTree.length === 0" class="tag-empty">
            <p class="tag-empty-text">暂无标签</p>
            <p class="tag-empty-hint">点击下方按钮创建标签来分类你的知识</p>
          </div>
        </div>

        <!-- 右键菜单 -->
        <div
          v-if="editingTag"
          class="tag-context-menu"
          :style="{ position: 'fixed', left: contextMenuPosition.x, top: contextMenuPosition.y }"
          @click.stop
        >
          <div class="context-menu-item" @click="handleEditTag(editingTag)">
            <el-icon size="13"><Edit /></el-icon>
            编辑标签
          </div>
          <div class="context-menu-item danger" @click="handleDeleteTag(editingTag)">
            <el-icon size="13"><Delete /></el-icon>
            删除标签
          </div>
          <div class="context-menu-item" @click="editingTag = null">
            <el-icon size="13"><Close /></el-icon>
            取消
          </div>
        </div>

        <button class="add-system-btn" @click="handleCreateTag">
          <el-icon size="14"><Plus /></el-icon>
          <span>新建标签</span>
        </button>
      </aside>

      <main class="knowledge-content">
        <div class="content-header">
          <span class="content-title">知识点列表</span>
          <div class="content-actions">
            <el-button link size="small" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">
              <el-icon size="16"><Grid /></el-icon>
            </el-button>
            <el-button link size="small" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
              <el-icon size="16"><List /></el-icon>
            </el-button>
            <el-select
              v-model="sortBy"
              size="small"
              style="width: 120px"
              @change="handleSearch"
            >
              <el-option label="最新创建" value="newest" />
              <el-option label="最早创建" value="oldest" />
              <el-option label="重要程度" value="importance" />
              <el-option label="掌握程度" value="mastery" />
            </el-select>
          </div>
        </div>

        <div v-loading="loading" class="knowledge-list" :class="viewMode">
          <div
            v-for="knowledge in knowledgeList"
            :key="knowledge.id"
            class="knowledge-card"
            @click="viewDetail(knowledge)"
          >
            <div class="card-checkbox">
              <el-checkbox
                v-model="selectedKnowledgeIds"
                :value="knowledge.id"
                @click.stop
              />
            </div>
            <div class="card-header">
              <span @click.stop>
                <el-dropdown trigger="click" @command="(cmd) => handleCardCommand(cmd, knowledge)">
                  <button class="card-menu">
                    <el-icon size="14"><MoreFilled /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="generate">
                        <el-icon><MagicStick /></el-icon>
                        生成复习卡片
                      </el-dropdown-item>
                      <el-dropdown-item command="delete" divided>
                        <el-icon><Delete /></el-icon>
                        删除知识点
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </span>
            </div>
            <h3 class="card-title">{{ knowledge.title }}</h3>
            <p class="card-summary">{{ knowledge.summary }}</p>
            <div class="card-meta">
              <div class="meta-item">
                <el-icon size="12"><Star /></el-icon>
                <span class="label">重要</span>
                <el-rate
                  v-model="knowledge.importance"
                  disabled
                  show-score
                  text-color="#ff9900"
                  :max="5"
                  size="small"
                />
              </div>
              <div class="meta-item">
                <el-icon size="12"><Timer /></el-icon>
                <span :class="['difficulty', knowledge.difficulty]">{{ knowledge.difficulty === 'difficult' ? '困难' : '中等' }}</span>
              </div>
              <div class="meta-item" v-if="knowledge.status === 'review'">
                <el-icon size="12"><Clock /></el-icon>
                <span class="status review">待复习</span>
              </div>
              <div class="meta-item" v-else>
                <el-icon size="12"><CircleCheck /></el-icon>
                <span class="status mastered">已掌握</span>
              </div>
            </div>
            <div class="card-tags">
              <TagChips
                :tags="knowledge.tags || []"
                :editable="true"
                :node-id="knowledge.id"
                :available-tags="allFlatTags"
                @add="loadKnowledgeList"
                @remove="loadKnowledgeList"
              />
            </div>
            <div class="card-progress">
              <div class="progress-info">
                <span class="progress-label">掌握进度</span>
                <span class="progress-value">{{ getMasteryPercentage(knowledge.masteryLevel) }}%</span>
              </div>
              <el-progress
                :percentage="getMasteryPercentage(knowledge.masteryLevel)"
                :color="getMasteryColor(knowledge.masteryLevel)"
                :stroke-width="6"
                :text-inside="false"
              />
            </div>
            <div class="card-footer">
              <span class="creator">newuser 创建于 {{ formatDate(knowledge.createTime) }}</span>
              <el-button type="primary" size="small" text @click.stop="handleGenerateCard(knowledge)">
                纳入复习
              </el-button>
            </div>
          </div>

          <el-empty
            v-if="!loading && knowledgeList.length === 0"
            description="暂无知识点"
            :image-size="150"
          />
        </div>

        <div class="pagination-wrapper">
          <span class="total-count">共 {{ pagination.total }} 条</span>
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="prev, pager, next, jumper, ->, sizes"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </main>
    </div>

    <el-dialog
      v-model="showDetailDialog"
      title="知识点详情"
      width="900px"
      class="detail-dialog"
    >
      <div v-if="currentKnowledge" class="detail-content">
        <div class="detail-header">
          <div class="detail-title">{{ currentKnowledge.title }}</div>
          <div class="detail-time">
            <el-icon><Clock /></el-icon>
            <span>{{ formatDate(currentKnowledge.createTime) }}</span>
          </div>
        </div>
        <div class="detail-body">
          <div class="detail-section">
            <h4><el-icon><Star /></el-icon>重要程度</h4>
            <el-rate
              v-model="currentKnowledge.importance"
              disabled
              show-score
              text-color="#ff9900"
              :max="5"
              size="large"
            />
          </div>
          <div class="detail-section">
            <h4><el-icon><PriceTag /></el-icon>标签</h4>
            <TagChips
              :tags="currentKnowledge.tags || []"
              :editable="true"
              :node-id="currentKnowledge.id"
              :available-tags="allFlatTags"
              @add="onDetailTagChanged"
              @remove="onDetailTagChanged"
            />
          </div>
          <div class="detail-section">
            <h4><el-icon><Medal /></el-icon>掌握程度</h4>
            <el-progress
              :percentage="getMasteryPercentage(currentKnowledge.masteryLevel)"
              :color="getMasteryColor(currentKnowledge.masteryLevel)"
              :stroke-width="20"
            />
          </div>
          <div class="detail-section">
            <h4><el-icon><Document /></el-icon>复习信息</h4>
            <div class="review-info">
              <div class="info-item">
                <span class="info-label">复习次数：</span>
                <span class="info-value">{{ currentKnowledge.reviewCount }}次</span>
              </div>
              <div class="info-item">
                <span class="info-label">下次复习：</span>
                <span class="info-value">{{ formatDate(currentKnowledge.nextReviewTime) }}</span>
              </div>
            </div>
          </div>
          <div class="detail-section">
            <h4><el-icon><ChatDotRound /></el-icon>摘要</h4>
            <div class="detail-text">{{ currentKnowledge.summary }}</div>
          </div>
          <div class="detail-section" v-if="currentKnowledge.contentMd">
            <h4><el-icon><Document /></el-icon>内容</h4>
            <div class="detail-text">{{ currentKnowledge.contentMd }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDetailDialog = false" size="large">
            <el-icon><Close /></el-icon>关闭
          </el-button>
          <el-button @click="showVersionHistory = true" size="large">
            <el-icon><Clock /></el-icon>版本历史
          </el-button>
          <el-button @click="openShareDialog" size="large">
            <el-icon><Share /></el-icon>分享
          </el-button>
          <el-button type="primary" @click="editKnowledge(currentKnowledge)" size="large">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showEditDialog"
      :title="editForm.id ? '编辑知识点' : '添加知识点'"
      width="900px"
      class="edit-dialog"
      @closed="onEditDialogClosed"
    >
      <el-form
        :model="editForm"
        :rules="editRules"
        ref="editFormRef"
        label-width="100px"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="editForm.title"
            placeholder="请输入标题"
            maxlength="200"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="editForm.summary"
            type="textarea"
            :rows="4"
            placeholder="请输入摘要"
            maxlength="500"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="AI 推荐">
          <el-button type="primary" plain size="small" @click="fetchEditTagSuggest" :loading="editSuggestLoading">
            <el-icon><MagicStick /></el-icon>
            AI 推荐标签
          </el-button>
          <TagSuggest
            ref="editTagSuggestRef"
            :title="editForm.title"
            :summary="editForm.summary"
            style="margin-top: 8px;"
          />
        </el-form-item>
        <el-form-item label="内容" prop="contentMd">
          <el-input
            v-model="editForm.contentMd"
            type="textarea"
            :rows="10"
            placeholder="请输入内容，支持Markdown格式..."
            maxlength="10000"
            show-word-limit
            size="large"
          />
        </el-form-item>
        <el-form-item label="重要程度" prop="importance">
          <el-rate
            v-model="editForm.importance"
            show-score
            text-color="#ff9900"
            :max="5"
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDialog = false" size="large">
            <el-icon><Close /></el-icon>取消
          </el-button>
          <el-button type="primary" @click="handleSave" :loading="saveLoading" size="large">
            <el-icon><Check /></el-icon>保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showShareDialog"
      title="分享知识节点"
      width="500px"
      :close-on-click-modal="false"
      @opened="onShareDialogOpened"
    >
      <div v-if="!shareLink" class="share-create">
        <el-form label-width="100px">
          <el-form-item label="有效期">
            <el-radio-group v-model="shareForm.expireType">
              <el-radio value="permanent">永久有效</el-radio>
              <el-radio value="7d">7天</el-radio>
              <el-radio value="24h">24小时</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <div class="share-actions">
          <el-button type="primary" @click="handleCreateShare" :loading="shareLoading">
            生成分享链接
          </el-button>
        </div>
      </div>
      <div v-else class="share-result">
        <p class="share-result-label">分享链接已生成：</p>
        <div class="share-url-box">
          <input
            class="share-url-input"
            :value="shareLink"
            readonly
            ref="shareUrlInput"
            @focus="$event.target.select()"
          />
          <el-button type="primary" size="small" @click="copyShareLink">
            复制
          </el-button>
        </div>
        <el-button @click="shareLink = ''" style="margin-top: 12px">重新生成</el-button>
      </div>

      <div v-if="myShares.length > 0" class="share-history">
        <p class="share-history-title">历史分享记录</p>
        <div v-for="s in myShares" :key="s.id" class="share-record">
          <div class="share-record-info">
            <span class="share-record-type">{{ expireLabel(s.expireType) }}</span>
            <span class="share-record-count">{{ s.accessCount }} 次访问</span>
            <span class="share-record-time">{{ formatDate(s.createdAt) }}</span>
          </div>
          <el-button size="small" type="danger" @click="handleRevokeShare(s.id)">
            撤销
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="showShareDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <VersionHistory
      v-model="showVersionHistory"
      :node-id="currentKnowledge?.id"
      :can-rollback="true"
      @rollback-success="onRollbackSuccess"
    />

    <TagCreateDialog
      v-model="showTagDialog"
      :tag="editingTag"
      :available-tags="tagTree"
      @saved="handleTagSaved"
    />

    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { knowledgeAPI } from "@/api/knowledge";
import { reviewAPI } from "@/api/review";
import { collaborationAPI } from "@/api/collaboration";
import { tagsAPI } from "@/api/tags";
import VersionHistory from "@/components/VersionHistory.vue";
import TagCreateDialog from "@/components/TagCreateDialog.vue";
import TagChips from "@/components/TagChips.vue";
import TagSuggest from "@/components/TagSuggest.vue";
import {
  Plus,
  Search,
  Download,
  ArrowDown,
  Filter,
  Grid,
  Star,
  CircleCheck,
  Cherry,
  List,
  MoreFilled,
  Timer,
  Clock,
  Close,
  Check,
  Edit,
  Medal,
  ChatDotRound,
  Document,
  DocumentCopy,
  MapLocation,
  Monitor,
  DataBoard,
  Brush,
  Notebook,
  Folder,
  Share,
  EditPen,
  Delete,
  MagicStick,
  ArrowRight,
  PriceTag,
} from "@element-plus/icons-vue";

const router = useRouter();

const loading = ref(false);
const saveLoading = ref(false);
const selectedKnowledgeIds = ref([]);
const searchKeyword = ref("");
const filterImportance = ref("");
const filterMastery = ref("");
const filterTag = ref("");
const selectedTagId = ref("");
const viewMode = ref("grid");
const sortBy = ref("newest");
const activeTab = ref("all"); // 'all' | 'pending'
const showDetailDialog = ref(false);
const showEditDialog = ref(false);
const showVersionHistory = ref(false);

const knowledgeList = ref([]);
const currentKnowledge = ref(null);
const editFormRef = ref(null);
const editTagSuggestRef = ref(null);
const editSuggestLoading = ref(false);

// 编辑锁相关
const editingNodeId = ref(null);
const lockRenewTimer = ref(null);
const lockStatusMap = ref({});

// 分享相关
const showShareDialog = ref(false);

// 待确认知识点
const pendingItems = ref([]);
const pendingCount = ref(0);
const pendingGenerateCards = ref(false);
const shareForm = ref({ expireType: "permanent" });
const shareLink = ref("");
const shareLoading = ref(false);
const myShares = ref([]);

const tagTree = ref([]);
const expandedTags = ref(new Set());
const showTagDialog = ref(false);
const editingTag = ref(null);

const selectTag = (tagId) => {
  selectedTagId.value = tagId;
  pagination.value.current = 1;
  loadKnowledgeList();
};

const loadTagTree = async () => {
  try {
    const data = await tagsAPI.getTree();
    tagTree.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.warn("加载标签树失败", error);
    tagTree.value = [];
  }
};

const statistics = ref({
  total: 253,
  highImportance: 68,
  highImportanceRatio: 26.9,
  mastered: 102,
  masteredRatio: 40.3,
  toReview: 36,
  toReviewIncrease: 8,
  notMastered: 115,
  notMasteredRatio: 45.5,
  increase: 12,
});

const editForm = ref({
  id: null,
  title: "",
  summary: "",
  contentMd: "",
  importance: 3,
});

const editRules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  summary: [{ required: true, message: "请输入摘要", trigger: "blur" }],
};

const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
});

const loadKnowledgeList = async () => {
  try {
    loading.value = true;
    const params = {
      current: pagination.value.current,
      size: pagination.value.size,
    };
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value;
    }
    if (filterImportance.value !== "") {
      params.importance = filterImportance.value;
    }
    if (filterMastery.value !== "") {
      params.masteryLevel = filterMastery.value;
    }
    if (selectedTagId.value) {
      params.tagId = selectedTagId.value;
    }

    const data = await knowledgeAPI.getList(params);
    knowledgeList.value = (data.records || []).map((knowledge) => ({
      ...knowledge,
      difficulty: knowledge.difficulty || "medium",
      status: knowledge.masteryLevel >= 4 ? "mastered" : "review",
    }));
    pagination.value.total = data.total || 0;
  } catch (error) {
    ElMessage.error("加载知识点列表失败：" + error.message);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.value.current = 1;
  loadKnowledgeList();
};

const handleSizeChange = (size) => {
  pagination.value.size = size;
  pagination.value.current = 1;
  loadKnowledgeList();
};

const handleCurrentChange = (current) => {
  pagination.value.current = current;
  loadKnowledgeList();
};

const toggleTagExpand = (tagId) => {
  if (expandedTags.value.has(tagId)) {
    expandedTags.value.delete(tagId);
  } else {
    expandedTags.value.add(tagId);
  }
  expandedTags.value = new Set(expandedTags.value);
};

const handleTagContextMenu = (event, tag) => {
  event.preventDefault();
  editingTag.value = tag;
  contextMenuPosition.value = { x: event.clientX + 'px', y: event.clientY + 'px' };
};

const contextMenuPosition = ref({ x: '0px', y: '0px' });

const handleCreateTag = () => {
  editingTag.value = null;
  showTagDialog.value = true;
};

const handleEditTag = (tag) => {
  editingTag.value = tag;
  showTagDialog.value = true;
};

const handleTagSaved = async (formData) => {
  try {
    if (editingTag.value) {
      await tagsAPI.update(editingTag.value.id, formData);
      ElMessage.success("标签已更新");
    } else {
      await tagsAPI.create(formData);
      ElMessage.success("标签已创建");
    }
    showTagDialog.value = false;
    editingTag.value = null;
    await loadTagTree();
  } catch (e) {
    ElMessage.error((editingTag.value ? "更新" : "创建") + "失败：" + (e.message || "未知错误"));
  }
};

const handleDeleteTag = async (tag) => {
  const childCount = tag.children?.length || 0;
  const nodeCount = tag.nodeCount || 0;
  let message = `确定要删除标签"${tag.tagName}"吗？`;
  if (childCount > 0) {
    message += `\n该标签下有 ${childCount} 个子标签，删除后子标签将变为顶级标签。`;
  }
  if (nodeCount > 0) {
    message += `\n该标签已被 ${nodeCount} 个知识点使用，删除后这些知识点将失去此标签。`;
  }
  try {
    await ElMessageBox.confirm(message, "确认删除", {
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
      type: "warning",
    });
    await tagsAPI.delete(tag.id);
    ElMessage.success("标签已删除");
    if (selectedTagId.value === tag.id) {
      selectedTagId.value = "";
      loadKnowledgeList();
    }
    await loadTagTree();
  } catch (e) {
    if (e !== "cancel") {
      ElMessage.error("删除失败：" + (e.message || "未知错误"));
    }
  }
};

const renderTagNode = (tag, depth) => {
  const hasChildren = tag.children && tag.children.length > 0;
  const isExpanded = expandedTags.value.has(tag.id);
  return {
    ...tag,
    _depth: depth,
    _hasChildren: hasChildren,
    _isExpanded: isExpanded,
  };
};

const flatTagList = computed(() => {
  const result = [];
  const walk = (tags, depth) => {
    for (const tag of tags) {
      result.push(renderTagNode(tag, depth));
      if (tag.children && tag.children.length > 0 && expandedTags.value.has(tag.id)) {
        walk(tag.children, depth + 1);
      }
    }
  };
  walk(tagTree.value, 0);
  return result;
});

const allFlatTags = computed(() => {
  const result = [];
  const walk = (tags) => {
    for (const tag of tags) {
      result.push(tag);
      if (tag.children && tag.children.length > 0) {
        walk(tag.children);
      }
    }
  };
  walk(tagTree.value);
  return result;
});

const getMasteryPercentage = (level) => {
  return (level / 5) * 100;
};

const getMasteryColor = (level) => {
  const colorMap = {
    0: "#ff4d4f",
    1: "#ff6b6b",
    2: "#f56c6c",
    3: "#e6a23c",
    4: "#67c23a",
    5: "#409eff",
  };
  return colorMap[level] || "#909399";
};

const formatDate = (dateStr) => {
  if (!dateStr) return "-";
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const viewDetail = (knowledge) => {
  currentKnowledge.value = knowledge;
  showDetailDialog.value = true;
};

const editKnowledge = async (knowledge) => {
  try {
    const res = await collaborationAPI.acquireLock(knowledge.id);
    if (res.code === 409) {
      ElMessage.warning(res.message || "其他用户正在编辑此知识点");
      return;
    }
  } catch (error) {
    ElMessage.warning("获取编辑锁失败：" + (error.message || "未知错误"));
    return;
  }

  editingNodeId.value = knowledge.id;
  // 每5分钟自动续期
  lockRenewTimer.value = setInterval(() => {
    collaborationAPI.acquireLock(knowledge.id).catch(() => {});
  }, 5 * 60 * 1000);

  editForm.value = {
    id: knowledge.id,
    title: knowledge.title,
    summary: knowledge.summary,
    contentMd: knowledge.contentMd || "",
    importance: knowledge.importance,
  };
  showEditDialog.value = true;
};

const handleAddKnowledge = () => {
  editForm.value = {
    id: null,
    title: "",
    summary: "",
    contentMd: "",
    importance: 3,
  };
  showEditDialog.value = true;
};

const fetchEditTagSuggest = () => {
  editTagSuggestRef.value?.fetchSuggestions();
};

const onDetailTagChanged = async () => {
  if (currentKnowledge.value?.id) {
    try {
      const updatedTags = await tagsAPI.getByNode(currentKnowledge.value.id);
      currentKnowledge.value = { ...currentKnowledge.value, tags: Array.isArray(updatedTags) ? updatedTags : [] };
    } catch (e) {
      console.warn("刷新标签失败", e);
    }
  }
  loadKnowledgeList();
};

const handleSave = async () => {
  if (!editFormRef.value) return;
  try {
    await editFormRef.value.validate();
    saveLoading.value = true;
    if (editForm.value.id) {
      await knowledgeAPI.updateKnowledge(editForm.value.id, editForm.value);
      ElMessage.success("更新成功");
    } else {
      await knowledgeAPI.createKnowledge(editForm.value);
      ElMessage.success("创建成功");
    }
    releaseCurrentLock();
    showEditDialog.value = false;
    loadKnowledgeList();
  } catch (error) {
    if (error !== false) {
      ElMessage.error("保存失败：" + error.message);
    }
  } finally {
    saveLoading.value = false;
  }
};

/** 释放当前持有的编辑锁 */
const releaseCurrentLock = () => {
  if (editingNodeId.value) {
    collaborationAPI.releaseLock(editingNodeId.value).catch(() => {});
    editingNodeId.value = null;
  }
  if (lockRenewTimer.value) {
    clearInterval(lockRenewTimer.value);
    lockRenewTimer.value = null;
  }
};

/** 编辑对话框关闭时释放锁 */
const onEditDialogClosed = () => {
  releaseCurrentLock();
};

const handleImport = () => {
  ElMessage.info("导入功能开发中");
};

const handleAdvancedFilter = () => {
  ElMessage.info("高级筛选功能开发中");
};

const handleGenerateCard = async (knowledge) => {
  try {
    await reviewAPI.generateReviewCard({ nodeId: knowledge.id, cardType: "choice" });
    ElMessage.success(`已为"${knowledge.title}"生成复习卡片`);
  } catch (e) {
    ElMessage.error("生成失败：" + (e.message || "未知错误"));
  }
};

const handleCardCommand = async (command, knowledge) => {
  if (command === "generate") {
    try {
      await reviewAPI.generateReviewCard({ nodeId: knowledge.id, cardType: "choice" });
      ElMessage.success("复习卡片生成成功");
    } catch (e) {
      ElMessage.error("生成失败：" + (e.message || "未知错误"));
    }
  } else if (command === "delete") {
    try {
      await ElMessageBox.confirm(
        `确定要删除知识点"${knowledge.title}"吗？`,
        "确认删除",
        { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" },
      );
      await knowledgeAPI.deleteById(knowledge.id);
      ElMessage.success("删除成功");
      loadKnowledgeList();
    } catch (error) {
      if (error !== "cancel") {
        ElMessage.error("删除失败：" + error.message);
      }
    }
  }
};

const handleBatchDelete = async () => {
  if (selectedKnowledgeIds.value.length === 0) {
    ElMessage.warning("请选择要删除的知识点");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的${selectedKnowledgeIds.value.length}个知识点吗？`,
      "确认删除",
      { confirmButtonText: "确定", cancelButtonText: "取消", type: "warning" },
    );
    await knowledgeAPI.batchDelete(selectedKnowledgeIds.value);
    ElMessage.success("删除成功");
    selectedKnowledgeIds.value = [];
    loadKnowledgeList();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("删除失败：" + error.message);
    }
  }
};

const handleBatchExport = () => {
  if (selectedKnowledgeIds.value.length === 0) {
    ElMessage.warning("请选择要导出的知识点");
    return;
  }
  ElMessage.info("批量导出功能开发中");
};

/** 版本回滚成功后的回调 */
const onRollbackSuccess = () => {
  showDetailDialog.value = false;
  loadKnowledgeList();
};

// ========== 分享功能 ==========

const shareUrlInput = ref(null);

const openShareDialog = () => {
  shareForm.value = { expireType: "permanent" };
  shareLink.value = "";
  showShareDialog.value = true;
};

const onShareDialogOpened = () => {
  loadMyShares();
};

const handleCreateShare = async () => {
  if (!currentKnowledge.value) return;
  shareLoading.value = true;
  try {
    const data = await collaborationAPI.createShare({
      nodeId: currentKnowledge.value.id,
      expireType: shareForm.value.expireType,
    });
    shareLink.value = `${window.location.origin}/share/${data.token}`;
    ElMessage.success("分享链接已生成");
    loadMyShares();
  } catch (error) {
    ElMessage.error("创建分享失败：" + (error.message || "未知错误"));
  } finally {
    shareLoading.value = false;
  }
};

const copyShareLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLink.value);
    ElMessage.success("链接已复制到剪贴板");
  } catch {
    ElMessage.info("请手动复制链接");
  }
};

const loadMyShares = async () => {
  try {
    const data = await collaborationAPI.getShareList();
    myShares.value = Array.isArray(data) ? data : [];
  } catch {
    myShares.value = [];
  }
};

const handleRevokeShare = async (shareId) => {
  try {
    await ElMessageBox.confirm("确定要撤销此分享链接吗？", "确认撤销", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    });
    await collaborationAPI.revokeShare(shareId);
    ElMessage.success("分享已撤销");
    loadMyShares();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error("撤销失败：" + (error.message || "未知错误"));
    }
  }
};

const expireLabel = (type) => {
  const map = { permanent: "永久", "7d": "7天", "24h": "24小时" };
  return map[type] || type;
};

// ========== 待确认知识点 ==========

const loadPendingItems = async () => {
  try {
    const data = await knowledgeAPI.getPendingKnowledge();
    pendingItems.value = Array.isArray(data) ? data : [];
    pendingCount.value = pendingItems.value.length;
  } catch (error) {
    console.error("加载待确认知识点失败:", error);
  }
};

const addEmptyPendingItem = () => {
  pendingItems.value.push({
    id: null,
    title: "",
    summary: "",
    content: "",
    _new: true,
  });
};

const removePendingItem = async (index) => {
  const item = pendingItems.value[index];
  if (item.id && !item._new) {
    try {
      await knowledgeAPI.discardPendingKnowledge(item.id);
    } catch (error) {
      console.error("丢弃待确认知识点失败:", error);
    }
  }
  pendingItems.value.splice(index, 1);
  pendingCount.value = pendingItems.value.length;
};

const discardAllPending = async () => {
  try {
    await ElMessageBox.confirm("确定要丢弃所有待确认知识点吗？", "确认", {
      confirmButtonText: "全部丢弃",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  for (const item of pendingItems.value) {
    if (item.id && !item._new) {
      try {
        await knowledgeAPI.discardPendingKnowledge(item.id);
      } catch (error) {
        console.error("丢弃待确认知识点失败:", error);
      }
    }
  }
  pendingItems.value = [];
  pendingCount.value = 0;
  ElMessage.success("已全部丢弃");
};

const confirmPending = async () => {
  // 过滤掉空标题的项
  const validItems = pendingItems.value.filter((item) => item.title && item.title.trim());
  if (validItems.length === 0) {
    ElMessage.warning("至少需要一条有标题的知识点");
    return;
  }

  try {
    await knowledgeAPI.confirmPendingKnowledge({
      items: validItems.map((item) => ({
        pendingId: item.id,
        title: item.title,
        summary: item.summary,
        content: item.content,
      })),
      generateCards: pendingGenerateCards.value,
    });
    ElMessage.success(`已确认入库 ${validItems.length} 条知识点`);
    pendingItems.value = [];
    pendingCount.value = 0;
    pendingGenerateCards.value = false;
    loadKnowledgeList();
  } catch (error) {
    ElMessage.error("确认入库失败: " + (error.message || "未知错误"));
  }
};

const handleTabChange = (tab) => {
  activeTab.value = tab;
  if (tab === "pending") {
    loadPendingItems();
  } else {
    loadKnowledgeList();
  }
};

onMounted(() => {
  loadTagTree();
  loadKnowledgeList();
  loadPendingItems();
});

onBeforeUnmount(() => {
  releaseCurrentLock();
});
</script>

<style scoped>
.knowledge-page {
  min-height: 100%;
  background: var(--bg-page);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--spacing-xl) 0;
  margin-bottom: var(--spacing-lg);
  border-bottom: 1px solid var(--border-lighter);
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
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--text-muted);
  margin: 0;
}

.knowledge-count {
  align-self: flex-start;
}

.header-right {
  display: flex;
  gap: var(--spacing-md);
}

.filter-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.search-box {
  flex: 1;
  max-width: 350px;
  display: flex;
  align-items: center;
  background: var(--bg-input);
  border-radius: var(--radius-lg);
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--border-light);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}

.search-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-focus-ring);
}

.search-box input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  outline: none;
}

.search-box input::placeholder {
  color: var(--text-placeholder);
}

.search-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
}

.search-btn:hover {
  color: var(--color-primary);
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

.stat-card.purple .stat-icon {
  background: rgba(124, 58, 237, 0.1);
  color: #7c3aed;
}

.stat-card.orange .stat-icon {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.stat-card.green .stat-icon {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.stat-card.blue .stat-icon {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.stat-card.red .stat-icon {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-bottom: 4px;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
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

.main-content {
  display: flex;
  gap: var(--spacing-lg);
}

.system-sidebar {
  width: 260px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--border-lighter);
}

.sidebar-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.sidebar-add {
  width: 28px;
  height: 28px;
  border: none;
  background: var(--bg-input);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-secondary);
  transition: background var(--transition-base), color var(--transition-base);
}

.sidebar-add:hover {
  background: var(--color-primary);
  color: white;
}

.system-list {
  flex: 1;
  overflow-y: auto;
}

.system-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-base);
  margin-bottom: 2px;
}

.system-item:hover {
  background: var(--bg-sidebar-hover);
}

.system-item.active {
  background: var(--color-primary-alpha-10);
}

.system-item.active .system-name {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

.system-name {
  flex: 1;
  font-size: var(--font-size-base);
  color: var(--text-regular);
}

.system-count {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
  background: var(--bg-input);
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.add-system-btn {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  border: 1px dashed var(--border-light);
  border-radius: var(--radius-md);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  transition: border-color var(--transition-base), color var(--transition-base);
}

.add-system-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* ---- Tag Tree ---- */
.tag-expand-btn {
  width: 14px;
  height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-muted);
  flex-shrink: 0;
  transition: color var(--transition-fast);
}

.tag-expand-btn:hover {
  color: var(--color-primary);
}

.tag-expand-spacer {
  width: 14px;
  flex-shrink: 0;
}

.tag-color-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tag-empty {
  padding: 24px 12px;
  text-align: center;
}

.tag-empty-text {
  font-size: 13px;
  color: var(--text-muted);
  margin: 0 0 4px;
}

.tag-empty-hint {
  font-size: 12px;
  color: var(--text-placeholder);
  margin: 0;
}

/* ---- Tag Context Menu ---- */
.tag-context-menu {
  position: fixed;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  padding: 4px 0;
  z-index: 2000;
  min-width: 140px;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 13px;
  color: var(--text-regular);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.context-menu-item:hover {
  background: var(--bg-hover);
}

.context-menu-item.danger {
  color: #ef4444;
}

.context-menu-item.danger:hover {
  background: rgba(239, 68, 68, 0.08);
}

.knowledge-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.content-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.content-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.content-actions .el-button {
  padding: 4px 8px;
}

.content-actions .el-button.active {
  background: var(--color-primary-alpha-10);
  color: var(--color-primary);
}

.knowledge-list {
  flex: 1;
}

.knowledge-list.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-lg);
}

.knowledge-list.list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.knowledge-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  position: relative;
}

.knowledge-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.card-checkbox {
  position: absolute;
  top: var(--spacing-md);
  right: var(--spacing-md);
}

.card-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: var(--spacing-md);
}

.card-menu {
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

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--spacing-sm) 0;
  line-height: 1.4;
}

.card-summary {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  margin: 0 0 var(--spacing-md) 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
}

.card-tags {
  margin-bottom: var(--spacing-md);
  min-height: 26px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
}

.meta-item .label {
  color: var(--text-muted);
}

.difficulty {
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
}

.difficulty.medium {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.difficulty.difficult {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.status {
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
}

.status.review {
  background: rgba(249, 115, 22, 0.1);
  color: #f97316;
}

.status.mastered {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.card-progress {
  margin-bottom: var(--spacing-md);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.progress-label {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.progress-value {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-lighter);
}

.creator {
  font-size: var(--font-size-xs);
  color: var(--text-muted);
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-lighter);
  margin-top: var(--spacing-lg);
}

.total-count {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.detail-content {
  color: var(--text-primary);
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid var(--border-lighter);
}

.detail-title {
  flex: 1;
  font-size: var(--font-size-2xl);
  font-weight: bold;
  color: var(--text-primary);
  line-height: 1.4;
}

.detail-time {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  background: var(--bg-list-item);
  border-radius: var(--radius-md);
  padding: 16px;
}

.detail-section h4 {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-section h4 .el-icon {
  color: var(--color-primary);
}

.review-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-label {
  color: var(--text-muted);
  font-size: var(--font-size-base);
}

.info-value {
  color: var(--text-primary);
  font-size: var(--font-size-md);
  font-weight: bold;
}

.detail-text {
  color: var(--text-regular);
  font-size: var(--font-size-base);
  line-height: 1.6;
  white-space: pre-wrap;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-lighter);
}

:deep(.el-empty) {
  background: transparent;
}

:deep(.el-empty__description p) {
  color: var(--text-muted);
}

:deep(.el-select .el-input__wrapper) {
  box-shadow: none;
  border: 1px solid var(--border-light);
}

:deep(.el-button--primary) {
  background: var(--gradient-primary);
  border: none;
}

:deep(.el-rate__text) {
  font-size: var(--font-size-xs);
}

/* ===== 分享对话框 ===== */
.share-create {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.share-actions {
  display: flex;
  justify-content: flex-end;
}

.share-result {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.share-result-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--spacing-md);
}

.share-url-box {
  display: flex;
  gap: var(--spacing-sm);
  width: 100%;
}

.share-url-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  background: var(--bg-input);
  outline: none;
}

.share-url-input:focus {
  border-color: var(--color-primary);
}

.share-history {
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-lighter);
}

.share-history-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--spacing-md);
}

.share-record {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-sm) 0;
}

.share-record + .share-record {
  border-top: 1px solid var(--border-lighter);
}

.share-record-info {
  display: flex;
  gap: var(--spacing-md);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.share-record-type {
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

/* ===== Tab 导航 ===== */
.tab-section {
  margin-bottom: var(--spacing-lg);
}

.tabs-nav {
  display: flex;
  gap: var(--spacing-sm);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 4px;
  box-shadow: var(--shadow-sm);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  background: transparent;
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
}

.tab-btn:hover {
  color: var(--text-primary);
  background: var(--bg-input);
}

.tab-btn.active {
  background: var(--color-primary);
  color: #fff;
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.25);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

/* ===== 待确认知识点区域 ===== */
.pending-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.pending-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--border-lighter);
  background: var(--bg-list-item);
}

.pending-header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  color: var(--color-primary);
}

.pending-header-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.pending-header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.pending-workspace-hint {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.pending-empty {
  padding: var(--spacing-3xl) var(--spacing-xl);
  text-align: center;
}

.pending-empty-hint {
  font-size: var(--font-size-sm);
  color: var(--text-muted);
  margin-top: var(--spacing-md);
}

.pending-list {
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.pending-card {
  background: var(--bg-page);
  border: 1px solid var(--border-lighter);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: border-color var(--transition-base);
}

.pending-card:hover {
  border-color: var(--border-light);
}

.pending-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--border-lighter);
  background: var(--bg-list-item);
}

.pending-card-index {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-secondary);
}

.pending-card-body {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.pending-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pending-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.pending-add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-lg);
  border: 2px dashed var(--border-light);
  border-radius: var(--radius-lg);
  background: transparent;
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  cursor: pointer;
  transition: all var(--transition-base);
}

.pending-add-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-alpha-10);
}

.pending-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg) var(--spacing-xl);
  border-top: 1px solid var(--border-lighter);
  background: var(--bg-list-item);
}

.pending-footer-actions {
  display: flex;
  gap: var(--spacing-md);
}

/* ===== 响应式 ===== */

@media (max-width: 1200px) {
  .stats-section {
    grid-template-columns: repeat(3, 1fr);
  }
  .knowledge-list.grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: var(--spacing-lg);
  }
  .header-right {
    flex-wrap: wrap;
  }
  .filter-section {
    flex-wrap: wrap;
  }
  .search-box {
    width: 100%;
    max-width: none;
  }
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-content {
    flex-direction: column;
  }
  .system-sidebar {
    width: 100%;
  }
  .knowledge-list.grid {
    grid-template-columns: 1fr;
  }
  .pending-header {
    flex-direction: column;
    gap: var(--spacing-md);
    align-items: flex-start;
  }
  .pending-footer {
    flex-direction: column;
    gap: var(--spacing-lg);
    align-items: flex-start;
  }
  .pending-footer-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>