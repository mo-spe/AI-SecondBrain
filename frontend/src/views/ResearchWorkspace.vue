<template>
  <div class="research-workspace">
    <!-- ===== 项目列表视图 ===== -->
    <div v-if="!currentProject" class="project-list-view">
      <div class="list-header">
        <div class="header-text">
          <h1 class="view-title">AI 研究空间</h1>
          <p class="view-desc">定义研究目标，让 AI Agent 自动搜索、分析、综合知识，完成深度研究。</p>
        </div>
        <button class="create-btn" @click="showCreateDialog = true">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <span>新建研究</span>
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loadingProjects" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载研究项目中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="projects.length === 0" class="empty-projects">
        <div class="empty-visual">
          <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
            <circle cx="32" cy="32" r="28" stroke="currentColor" stroke-width="1.5" stroke-dasharray="4 4" opacity="0.3"/>
            <circle cx="32" cy="32" r="12" stroke="currentColor" stroke-width="1.5" opacity="0.5"/>
            <circle cx="32" cy="32" r="3" fill="currentColor" opacity="0.7"/>
          </svg>
        </div>
        <p>还没有研究项目，点击上方按钮创建第一个研究吧。</p>
      </div>

      <!-- 项目列表 -->
      <div v-else class="project-items">
        <div
          v-for="project in projects"
          :key="project.id"
          class="project-item"
          @click="selectProject(project)"
        >
          <div class="item-status">
            <span class="status-dot" :class="statusClass(project.status)"></span>
          </div>
          <div class="item-body">
            <div class="item-title">{{ project.title }}</div>
            <div class="item-goal" v-if="project.goal">{{ project.goal }}</div>
            <div class="item-meta">
              <span class="meta-tag" :class="statusClass(project.status)">{{ project.statusLabel }}</span>
              <span class="meta-tag" v-if="project.complexity">{{ project.complexity }}</span>
              <span class="meta-sep">&middot;</span>
              <span>{{ formatDate(project.createTime) }}</span>
              <template v-if="project.taskCount">
                <span class="meta-sep">&middot;</span>
                <span>{{ project.completedTaskCount || 0 }}/{{ project.taskCount }} 个任务</span>
              </template>
            </div>
          </div>
          <div class="item-actions">
            <button
              v-if="project.status === 'RESEARCHING'"
              class="action-btn warn"
              title="暂停"
              @click.stop="pauseProject(project.id)"
            >
              <svg width="14" height="14" viewBox="0 0 14 14"><rect x="2" y="2" width="3.5" height="10" rx="0.5" fill="currentColor"/><rect x="8.5" y="2" width="3.5" height="10" rx="0.5" fill="currentColor"/></svg>
            </button>
            <button
              v-if="project.status === 'PAUSED'"
              class="action-btn primary"
              title="继续"
              @click.stop="resumeProject(project.id)"
            >
              <svg width="14" height="14" viewBox="0 0 14 14"><polygon points="3,1.5 12,7 3,12.5" fill="currentColor"/></svg>
            </button>
            <button
              v-if="project.status === 'DRAFT'"
              class="action-btn primary"
              title="启动"
              @click.stop="startProject(project.id)"
            >
              <svg width="14" height="14" viewBox="0 0 14 14"><polygon points="3,1.5 12,7 3,12.5" fill="currentColor"/></svg>
            </button>
            <button
              v-if="project.status === 'COMPLETED' || project.status === 'PARTIAL' || project.status === 'FAILED'"
              class="action-btn"
              title="重新执行"
              @click.stop="retryProject(project.id)"
            >
              <svg width="14" height="14" viewBox="0 0 14 14"><path d="M3 5.5A5 5 0 0111 5.5M11 5.5V2.5M11 5.5H8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" fill="none"/><path d="M11 8.5A5 5 0 013 8.5M3 8.5v3M3 8.5h3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>
            </button>
            <button class="action-btn" title="删除" @click.stop="deleteProject(project.id)">
              <svg width="14" height="14" viewBox="0 0 14 14"><path d="M4 4.5l.7 7h4.6l.7-7M2.5 4.5h9M5.5 4.5V3a.5.5 0 01.5-.5h2a.5.5 0 01.5.5v1.5" stroke="currentColor" stroke-width="1.1" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>
            </button>
          </div>
        </div>
      </div>

      <div class="list-pagination" v-if="totalProjects > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalProjects"
          layout="prev, next"
          @current-change="fetchProjects"
        />
      </div>
    </div>

    <!-- ===== 研究空间视图 ===== -->
    <div v-else class="workspace-view">
      <!-- 左侧面板：研究导航 -->
      <aside class="left-panel" :class="{ collapsed: leftCollapsed }">
        <div class="panel-header">
          <button v-show="!leftCollapsed" class="back-btn" @click="goBackToList" title="返回列表">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M10 3L5 8l5 5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <div class="panel-title" v-show="!leftCollapsed">{{ currentProject.title }}</div>
          <button class="collapse-btn" @click="leftCollapsed = !leftCollapsed" :title="leftCollapsed ? '展开' : '收起'">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M5 3l4 4-4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"
                :style="{ transform: leftCollapsed ? 'scaleX(-1)' : '' }"/>
            </svg>
          </button>
        </div>

        <nav v-show="!leftCollapsed" class="research-nav">
          <button
            v-for="item in navSections"
            :key="item.key"
            class="nav-item"
            :class="{ active: activeNav === item.key }"
            @click="activeNav = item.key"
          >
            <span class="nav-icon" v-html="item.icon"></span>
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.badge !== null && item.badge > 0" class="nav-badge">{{ item.badge }}</span>
          </button>
        </nav>

        <div v-show="!leftCollapsed" class="panel-status">
          <div class="status-row">
            <span class="status-dot" :class="statusClass(currentProject.status)"></span>
            <span class="status-text">{{ currentProject.statusLabel || '未知' }}</span>
          </div>
          <div class="progress-bar" v-if="currentProject.taskCount">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <div class="progress-text" v-if="currentProject.taskCount">
            {{ currentProject.completedTaskCount || 0 }}/{{ currentProject.taskCount }} 个任务
          </div>
        </div>
      </aside>

      <!-- 中间面板：研究内容 -->
      <main class="center-panel">
        <!-- 研究目标 -->
        <div v-if="activeNav === 'goal'" class="content-section">
          <div class="section-label">研究目标</div>
          <h2 class="section-heading">{{ currentProject.goal || '未设定研究目标' }}</h2>
          <div class="section-meta" v-if="currentProject.complexity || currentProject.maxIterations">
            <span v-if="currentProject.complexity">复杂度评估：{{ currentProject.complexity }}</span>
            <span v-if="currentProject.maxIterations">最大迭代：{{ currentProject.maxIterations }} 轮</span>
            <span v-if="currentProject.currentIteration">当前迭代：第 {{ currentProject.currentIteration }} 轮</span>
          </div>
        </div>

        <!-- 知识背景 -->
        <div v-if="activeNav === 'background'" class="content-section">
          <div class="section-label">知识背景</div>
          <div v-if="backgroundMemories.length === 0" class="empty-section">
            <p>启动研究后，AI 将从你的知识库中检索相关背景知识。</p>
          </div>
          <div v-else class="memory-list">
            <div v-for="m in backgroundMemories" :key="m.id" class="memory-card">
              <div class="memory-key">{{ displayMemoryKey(m.memoryKey) }}</div>
              <div class="memory-content" v-html="renderMd(m.content)"></div>
            </div>
          </div>
        </div>

        <!-- 知识缺口 -->
        <div v-if="activeNav === 'gaps'" class="content-section">
          <div class="section-label">知识缺口</div>
          <div v-if="gapMemories.length === 0" class="empty-section">
            <p>Knowledge Agent 运行后将自动识别知识缺口。</p>
          </div>
          <div v-else class="memory-list">
            <div v-for="m in gapMemories" :key="m.id" class="memory-card gap">
              <div class="memory-key">{{ displayMemoryKey(m.memoryKey) }}</div>
              <div class="memory-content" v-html="renderMd(m.content)"></div>
            </div>
          </div>
        </div>

        <!-- 研究计划 -->
        <div v-if="activeNav === 'plan'" class="content-section">
          <div class="section-label">研究计划</div>
          <div v-if="!latestPlan" class="empty-section">
            <p>启动研究后，Planner Agent 将自动生成研究计划。</p>
          </div>
          <div v-else class="plan-detail">
            <div class="plan-header">
              <span class="plan-version">版本 {{ latestPlan.version }}</span>
              <span class="plan-complexity">{{ latestPlan.complexityLabel || latestPlan.complexity }}</span>
              <span class="plan-chain">Agent 链路：{{ latestPlan.agentChain }}</span>
            </div>
            <div class="plan-rationale" v-if="latestPlan.rationale" v-html="renderMd(latestPlan.rationale)"></div>
          </div>
        </div>

        <!-- 研究任务 -->
        <div v-if="activeNav === 'tasks'" class="content-section">
          <div class="section-label">研究任务</div>
          <div v-if="tasks.length === 0" class="empty-section">
            <p>任务由 Planner Agent 自动生成，启动研究后即可查看。</p>
          </div>
          <div v-else class="task-list">
            <div
              v-for="task in tasks"
              :key="task.id"
              class="task-item"
              :class="{ active: expandedTaskId === task.id }"
            >
              <div class="task-header" @click="expandedTaskId = expandedTaskId === task.id ? null : task.id">
                <span class="task-status-dot" :class="statusClass(task.status)"></span>
                <span class="task-title">{{ task.title }}</span>
                <span class="task-status-label">{{ task.statusLabel }}</span>
                <svg class="task-chevron" :class="{ open: expandedTaskId === task.id }" width="12" height="12" viewBox="0 0 12 12">
                  <path d="M4 2.5L7.5 6 4 9.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                </svg>
              </div>
              <div class="task-body" v-if="expandedTaskId === task.id">
                <p class="task-desc" v-if="task.description">{{ task.description }}</p>
                <p class="task-question" v-if="task.question"><strong>研究问题：</strong>{{ task.question }}</p>
                <div v-if="task.resultSummary" class="task-result" v-html="renderMd(task.resultSummary)"></div>
                <!-- 执行步骤 -->
                <div class="task-steps" v-if="taskSteps[task.id] && taskSteps[task.id].length > 0">
                  <div class="steps-label">执行步骤</div>
                  <div v-for="step in taskSteps[task.id]" :key="step.id" class="step-row">
                    <span class="step-agent">{{ step.agentName }}</span>
                    <span class="step-type">{{ step.stepTypeLabel }}</span>
                    <span class="step-title">{{ step.title }}</span>
                    <span class="step-status" :class="statusClass(step.status)">{{ step.statusLabel }}</span>
                    <span class="step-duration" v-if="step.durationMs">{{ (step.durationMs / 1000).toFixed(1) }}s</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 研究来源 -->
        <div v-if="activeNav === 'sources'" class="content-section">
          <div class="section-label">研究来源</div>
          <div v-if="sources.length === 0" class="empty-section">
            <p>Research Agent 发现并抓取的外部资料将展示在此。</p>
          </div>
          <div v-else class="source-list">
            <div
              v-for="source in sources"
              :key="source.id"
              class="source-item"
              @click="viewSource(source)"
            >
              <div class="source-header">
                <span class="source-reliability" :class="'rel-' + (source.reliability || 'UNKNOWN').toLowerCase()">
                  {{ source.reliabilityLabel }}
                </span>
                <span class="source-type">{{ source.sourceTypeLabel }}</span>
              </div>
              <a class="source-title" :href="source.url" target="_blank" @click.stop>{{ source.title }}</a>
              <div class="source-snippet" v-if="source.snippet">{{ source.snippet }}</div>
              <div class="source-footer" v-if="source.relevanceScore">
                相关度：{{ (source.relevanceScore * 100).toFixed(0) }}%
              </div>
            </div>
          </div>
        </div>

        <!-- 研究发现 -->
        <div v-if="activeNav === 'findings'" class="content-section">
          <div class="section-label">研究发现</div>
          <div v-if="findingMemories.length === 0" class="empty-section">
            <p>研究 Agent 完成分析后，发现将汇总于此。</p>
          </div>
          <div v-else class="memory-list">
            <div v-for="m in findingMemories" :key="m.id" class="memory-card finding">
              <div class="memory-key">{{ displayMemoryKey(m.memoryKey) }}</div>
              <div class="memory-content" v-html="renderMd(m.content)"></div>
            </div>
          </div>
        </div>

        <!-- 研究结论 -->
        <div v-if="activeNav === 'conclusion'" class="content-section">
          <div class="section-label">研究结论</div>
          <div v-if="conclusionMemories.length === 0" class="empty-section">
            <p>所有研究任务完成后，Critic Agent 验证后的结论将展示在此。</p>
          </div>
          <div v-else class="memory-list">
            <div v-for="m in conclusionMemories" :key="m.id" class="memory-card conclusion">
              <div class="memory-key">{{ displayMemoryKey(m.memoryKey) }}</div>
              <div class="memory-content" v-html="renderMd(m.content)"></div>
            </div>
          </div>
        </div>

        <!-- 研究报告 -->
        <div v-if="activeNav === 'report'" class="content-section">
          <div class="section-label">研究报告</div>
          <div v-if="!latestReport" class="empty-section">
            <p>Synthesizer Agent 完成综合后将生成最终研究报告。</p>
          </div>
          <div v-else class="report-detail">
            <div class="report-header">
              <span class="report-version">版本 {{ latestReport.version }}</span>
              <span>{{ latestReport.title }}</span>
            </div>
            <div class="report-summary" v-if="latestReport.summary" v-html="renderMd(latestReport.summary)"></div>
            <div class="report-body" v-if="latestReport.contentMd" v-html="renderMd(latestReport.contentMd)"></div>
            <div class="report-stats">
              <span>{{ latestReport.sourceCount }} 个来源</span>
              <span>{{ latestReport.conclusionCount }} 条结论</span>
              <span v-if="latestReport.durationTotalMs">总耗时 {{ (latestReport.durationTotalMs / 1000).toFixed(0) }}s</span>
            </div>
          </div>
        </div>

        <!-- 知识候选 -->
        <div v-if="activeNav === 'candidates'" class="content-section">
          <div class="section-label">知识候选项</div>
          <div v-if="candidateMemories.length === 0" class="empty-section">
            <p>研究过程中发现的有价值知识将出现在此，由你来决定是否沉淀到知识库。</p>
          </div>
          <div v-else class="candidate-list">
            <div v-for="m in candidateMemories" :key="m.id" class="candidate-item">
              <div class="candidate-content" v-html="renderMd(m.content)"></div>
              <div class="candidate-actions">
                <button class="confirm-btn" :disabled="candidateProcessingIds.has(m.id)" @click="confirmKnowledge(m)">
                  <svg width="14" height="14" viewBox="0 0 14 14"><path d="M3 7l3 3 5-6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>
                  {{ candidateProcessingIds.has(m.id) ? "保存中…" : "保存到知识库" }}
                </button>
                <button class="dismiss-btn" :disabled="candidateProcessingIds.has(m.id)" @click="dismissKnowledge(m)">忽略</button>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 右侧面板：上下文 -->
      <aside class="right-panel" :class="{ collapsed: rightCollapsed }">
        <div class="panel-header">
          <span class="panel-label" v-show="!rightCollapsed">上下文</span>
          <button class="collapse-btn" @click="rightCollapsed = !rightCollapsed" :title="rightCollapsed ? '展开' : '收起'">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M9 3l-4 4 4 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"
                :style="{ transform: rightCollapsed ? 'scaleX(-1)' : '' }"/>
            </svg>
          </button>
        </div>

        <div v-show="!rightCollapsed" class="context-content">
          <!-- Agent 状态 -->
          <div class="context-block">
            <div class="context-label">Agent 状态</div>
            <div class="agent-pulse" :class="{ active: currentProject.status === 'RESEARCHING' || currentProject.status === 'REVIEWING' }">
              <span class="pulse-dot"></span>
              <span>{{ agentStatusText }}</span>
            </div>
            <div class="agent-phase" v-if="agentPhase">
              <span class="phase-label">当前阶段</span>
              <span>{{ agentPhase }}</span>
            </div>
            <div class="agent-task" v-if="agentCurrentTask">
              <span class="phase-label">当前任务</span>
              <span>{{ agentCurrentTask }}</span>
            </div>
            <div class="tool-status" v-if="toolCalls.length > 0">
              <div class="tool-status-label">工具调用</div>
              <div v-for="(tc, i) in toolCalls" :key="i" class="tool-row">
                <code class="tool-name">{{ tc.name }}</code>
                <span class="tool-state" :class="tc.state">{{ toolStateLabel(tc.state) }}</span>
              </div>
            </div>
          </div>

          <!-- 我的知识 -->
          <div class="context-block">
            <div class="context-label">相关知识</div>
            <div v-if="relatedKnowledge.length === 0" class="context-empty">暂无相关知识匹配。</div>
            <div v-else class="knowledge-tags">
              <span v-for="k in relatedKnowledge" :key="k.id" class="k-tag">{{ k.memoryKey || k.title }}</span>
            </div>
          </div>

          <!-- 外部来源概览 -->
          <div class="context-block">
            <div class="context-label">外部来源</div>
            <div v-if="sources.length === 0" class="context-empty">暂无外部来源。</div>
            <div v-else class="ext-source-list">
              <a
                v-for="s in sources.slice(0, 8)"
                :key="s.id"
                class="ext-link"
                :href="s.url"
                target="_blank"
                :title="s.title"
              >{{ s.title }}</a>
            </div>
          </div>
        </div>
      </aside>

      <!-- 底部栏：AI Agent 交互 -->
      <div class="bottom-bar">
        <div class="agent-controls">
          <button
            v-if="currentProject.status === 'DRAFT'"
            class="ctrl-btn start"
            @click="startProject(currentProject.id)"
          >
            <svg width="14" height="14" viewBox="0 0 14 14"><polygon points="3,1.5 12,7 3,12.5" fill="currentColor"/></svg>
            <span>启动研究</span>
          </button>
          <button
            v-if="currentProject.status === 'RESEARCHING'"
            class="ctrl-btn pause"
            @click="pauseProject(currentProject.id)"
          >
            <svg width="14" height="14" viewBox="0 0 14 14"><rect x="2" y="2" width="3.5" height="10" rx="0.5" fill="currentColor"/><rect x="8.5" y="2" width="3.5" height="10" rx="0.5" fill="currentColor"/></svg>
            <span>暂停</span>
          </button>
          <button
            v-if="currentProject.status === 'PAUSED'"
            class="ctrl-btn resume"
            @click="resumeProject(currentProject.id)"
          >
            <svg width="14" height="14" viewBox="0 0 14 14"><polygon points="3,1.5 12,7 3,12.5" fill="currentColor"/></svg>
            <span>继续研究</span>
          </button>
          <button
            v-if="currentProject.status === 'COMPLETED' || currentProject.status === 'PARTIAL' || currentProject.status === 'FAILED'"
            class="ctrl-btn retry"
            @click="retryProject(currentProject.id)"
          >
            <svg width="14" height="14" viewBox="0 0 14 14"><path d="M3 5.5A5 5 0 0111 5.5M11 5.5V2.5M11 5.5H8" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" fill="none"/><path d="M11 8.5A5 5 0 013 8.5M3 8.5v3M3 8.5h3" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>
            <span>重新执行</span>
          </button>
        </div>

        <div class="input-area">
          <input
            v-model="agentMessage"
            class="agent-input"
            placeholder="向 AI Agent 发送指令或追问..."
            @keyup.enter="sendAgentMessage"
            :disabled="currentProject.status !== 'RESEARCHING' && currentProject.status !== 'DRAFT'"
          />
          <button
            class="send-btn"
            @click="sendAgentMessage"
            :disabled="!agentMessage.trim() || (currentProject.status !== 'RESEARCHING' && currentProject.status !== 'DRAFT')"
            title="发送"
          >
            <svg width="15" height="15" viewBox="0 0 15 15" fill="none">
              <path d="M2 2l11 5.5L2 13V8.5L9 7.5 2 6.5V2z" fill="currentColor"/>
            </svg>
          </button>
        </div>

        <div class="agent-summary" v-if="agentSummary" :title="agentSummary">{{ agentSummary }}</div>
      </div>
    </div>

    <!-- 创建项目对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建研究项目" width="520px" class="research-dialog" :append-to-body="false">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="研究标题" required>
          <el-input v-model="createForm.title" placeholder="例如：量子计算对现有密码体系的影响评估" maxlength="300" />
        </el-form-item>
        <el-form-item label="研究目标" required>
          <el-input
            v-model="createForm.goal"
            type="textarea"
            :rows="4"
            placeholder="描述你想让 AI 研究什么，以及期望得到什么结论..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createProject" :loading="creating">创建并打开</el-button>
      </template>
    </el-dialog>

    <!-- 来源详情对话框 -->
    <el-dialog v-model="showSourceDialog" :title="viewingSource?.title" width="700px" class="research-dialog" :append-to-body="false">
      <div v-if="viewingSource" class="source-detail">
        <div class="source-meta-row">
          <span>{{ viewingSource.sourceTypeLabel }}</span>
          <span>{{ viewingSource.reliabilityLabel }}</span>
          <span v-if="viewingSource.relevanceScore">相关度 {{ (viewingSource.relevanceScore * 100).toFixed(0) }}%</span>
          <a :href="viewingSource.url" target="_blank" class="source-ext-link">打开原始链接</a>
        </div>
        <div class="source-content" v-if="viewingSource.fullContent" v-html="renderMd(viewingSource.fullContent)"></div>
        <div v-else class="source-content">{{ viewingSource.snippet || '无内容摘要' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { researchAPI } from "@/api/research";
import { marked } from "marked";
import DOMPurify from "dompurify";

marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false });

// ---- 项目列表状态 ----
const projects = ref([]);
const totalProjects = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const loadingProjects = ref(false);
const showCreateDialog = ref(false);
const creating = ref(false);
const createForm = ref({ title: "", goal: "" });

// ---- 工作空间状态 ----
const currentProject = ref(null);
const leftCollapsed = ref(false);
const rightCollapsed = ref(false);
const activeNav = ref("goal");

// ---- 研究数据 ----
const tasks = ref([]);
const latestPlan = ref(null);
const sources = ref([]);
const latestReport = ref(null);
const memories = ref([]);
const candidateProcessingIds = ref(new Set());
const taskSteps = ref({});
const expandedTaskId = ref(null);

// ---- Agent 状态 ----
const agentMessage = ref("");
const agentPhase = ref("");
const agentCurrentTask = ref("");
const agentSummary = ref("");
const toolCalls = ref([]);
let pollTimer = null;
let pollInFlight = false;

// ---- 来源对话框 ----
const showSourceDialog = ref(false);
const viewingSource = ref(null);

// ---- 导航配置 ----
const navSections = computed(() => {
  const taskCount = tasks.value.length || 0;
  const sourceCount = sources.value.length || 0;
  return [
    { key: "goal", label: "研究目标", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.2"/><circle cx="7" cy="7" r="2" fill="currentColor" opacity="0.5"/></svg>', badge: 0 },
    { key: "background", label: "知识背景", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="2" y="1.5" width="10" height="11" rx="1" stroke="currentColor" stroke-width="1.1"/><line x1="4.5" y1="4.5" x2="9.5" y2="4.5" stroke="currentColor" stroke-width="0.8"/><line x1="4.5" y1="7" x2="9.5" y2="7" stroke="currentColor" stroke-width="0.8"/><line x1="4.5" y1="9.5" x2="7" y2="9.5" stroke="currentColor" stroke-width="0.8"/></svg>', badge: 0 },
    { key: "gaps", label: "知识缺口", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.2"/><path d="M5 5.5a2 2 0 114 0c0 1.5-2 2.5-2 3.5M7 11h0" stroke="currentColor" stroke-width="1" stroke-linecap="round"/></svg>', badge: 0 },
    { key: "plan", label: "研究计划", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M2 4.5h10M2 7.5h10M2 10.5h6" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>', badge: 0 },
    { key: "tasks", label: "研究任务", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="1.5" y="1.5" width="11" height="11" rx="1.5" stroke="currentColor" stroke-width="1.1"/><path d="M4 7l2 2 4-4" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" fill="none"/></svg>', badge: taskCount },
    { key: "sources", label: "研究来源", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M2 5l5-3 5 3v5l-5 3-5-3V5z" stroke="currentColor" stroke-width="1.1"/><circle cx="7" cy="7" r="1.5" fill="currentColor" opacity="0.4"/></svg>', badge: sourceCount },
    { key: "findings", label: "研究发现", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 1.5v3M7 9.5v3M1.5 7h3M9.5 7h3M3.5 3.5l2 2M8.5 5.5l2-2M3.5 10.5l2-2M8.5 8.5l2 2" stroke="currentColor" stroke-width="1" stroke-linecap="round"/></svg>', badge: 0 },
    { key: "conclusion", label: "研究结论", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><polygon points="7,1.5 13,5 7,8.5 1,5" stroke="currentColor" stroke-width="1" fill="none"/><polygon points="7,8.5 13,5 13,10 7,13.5 1,10 1,5" stroke="currentColor" stroke-width="1" fill="none" opacity="0.5"/></svg>', badge: 0 },
    { key: "report", label: "研究报告", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><rect x="2.5" y="1.5" width="9" height="11" rx="1" stroke="currentColor" stroke-width="1.1"/><rect x="5" y="4" width="4" height="1" fill="currentColor" opacity="0.5"/><rect x="5" y="6" width="4" height="1" fill="currentColor" opacity="0.5"/><rect x="5" y="8" width="3" height="1" fill="currentColor" opacity="0.5"/></svg>', badge: 0 },
    { key: "candidates", label: "知识候选", icon: '<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><circle cx="7" cy="7" r="5.5" stroke="currentColor" stroke-width="1.2"/><path d="M7 4v4M7 10.5h0" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>', badge: 0 },
  ];
});

// ---- 计算属性 ----
const progressPercent = computed(() => {
  if (!currentProject.value?.taskCount) return 0;
  return Math.round(((currentProject.value.completedTaskCount || 0) / currentProject.value.taskCount) * 100);
});

const agentStatusText = computed(() => {
  const status = currentProject.value?.status;
  const map = {
    DRAFT: "就绪",
    PLANNING: "规划中...",
    RESEARCHING: "研究中",
    REVIEWING: "验证中",
    SYNTHESIZING: "综合中",
    PAUSED: "已暂停",
    COMPLETED: "已完成",
    PARTIAL: "部分完成",
    FAILED: "失败",
    ARCHIVED: "已归档",
  };
  return map[status] || status || "未知";
});

// ---- 记忆分类 ----
const backgroundMemories = computed(() => memories.value.filter((m) => m.memoryType === "BACKGROUND"));
const gapMemories = computed(() => memories.value.filter((m) => m.memoryType === "KNOWLEDGE_GAP"));
const findingMemories = computed(() => memories.value.filter((m) => m.memoryType === "FINDING"));
const conclusionMemories = computed(() => memories.value.filter((m) => m.memoryType === "CONCLUSION"));
const candidateMemories = computed(() => memories.value.filter((m) => m.memoryType === "CANDIDATE"));
const relatedKnowledge = computed(() => memories.value.filter((m) => m.memoryType === "RELATED_KNOWLEDGE"));

// ---- 生命周期 ----
onMounted(() => {
  fetchProjects();
});

onUnmounted(() => {
  stopPolling();
});

// ---- 监听器 ----
let lastProjectId = null;

watch(currentProject, (project) => {
  if (project) {
    const isNewProject = project.id !== lastProjectId;
    lastProjectId = project.id;
    if (isNewProject) {
      activeNav.value = "goal";
      expandedTaskId.value = null;
      loadProjectData(project.id);
    }
    if (isResearchActive(project.status)) {
      if (!pollTimer) startPolling(project.id);
    } else {
      stopPolling();
    }
  } else {
    lastProjectId = null;
    stopPolling();
  }
});

// ---- 项目列表方法 ----
async function fetchProjects() {
  loadingProjects.value = true;
  try {
    const data = await researchAPI.listProjects({ current: currentPage.value, size: pageSize.value });
    projects.value = data.records || [];
    totalProjects.value = data.total || 0;
  } catch (e) {
    ElMessage.error("加载研究项目失败");
  } finally {
    loadingProjects.value = false;
  }
}

function selectProject(project) {
  currentProject.value = project;
}

function goBackToList() {
  currentProject.value = null;
  stopPolling();
}

async function createProject() {
  if (!createForm.value.title || !createForm.value.goal) {
    ElMessage.warning("请填写研究标题和研究目标");
    return;
  }
  creating.value = true;
  try {
    const project = await researchAPI.createProject(createForm.value);
    showCreateDialog.value = false;
    createForm.value = { title: "", goal: "" };
    await fetchProjects();
    currentProject.value = project;
    ElMessage.success("研究项目创建成功");
  } catch (e) {
    ElMessage.error("创建项目失败");
  } finally {
    creating.value = false;
  }
}

async function startProject(id) {
  try {
    const project = await researchAPI.executeProject(id);
    updateProjectInList(project);
    if (currentProject.value?.id === id) currentProject.value = project;
    ElMessage.success("研究已启动");
  } catch (e) {
    ElMessage.error(e.message || "启动失败");
  }
}

async function pauseProject(id) {
  try {
    await researchAPI.pauseProject(id);
    await refreshCurrentProject();
    ElMessage.success("研究已暂停");
  } catch (e) {
    ElMessage.error(e.message || "暂停失败");
  }
}

async function resumeProject(id) {
  try {
    const project = await researchAPI.resumeProject(id);
    updateProjectInList(project);
    if (currentProject.value?.id === id) currentProject.value = project;
    ElMessage.success("研究已恢复");
  } catch (e) {
    ElMessage.error(e.message || "恢复失败");
  }
}

async function retryProject(id) {
  try {
    const project = await researchAPI.executeProject(id);
    updateProjectInList(project);
    if (currentProject.value?.id === id) currentProject.value = project;
    ElMessage.success("研究已重新启动");
  } catch (e) {
    ElMessage.error(e.message || "重新执行失败");
  }
}

async function deleteProject(id) {
  try {
    await ElMessageBox.confirm("确定要删除这个研究项目吗？此操作不可撤销。", "确认删除", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });
    await researchAPI.deleteProject(id);
    projects.value = projects.value.filter((p) => p.id !== id);
    if (currentProject.value?.id === id) currentProject.value = null;
    ElMessage.success("项目已删除");
  } catch (e) {
    if (e !== "cancel") ElMessage.error("删除失败");
  }
}

function updateProjectInList(project) {
  const idx = projects.value.findIndex((p) => p.id === project.id);
  if (idx >= 0) projects.value[idx] = project;
}

async function refreshCurrentProject() {
  if (!currentProject.value) return;
  try {
    const project = await researchAPI.getProject(currentProject.value.id);
    currentProject.value = project;
    updateProjectInList(project);
  } catch (e) { /* ignore */ }
}

// ---- 工作空间数据加载 ----
async function loadProjectData(projectId) {
  try {
    const [taskList, plan, sourceList, report, memoryList, stepList] = await Promise.all([
      researchAPI.listTasks(projectId).catch(() => []),
      researchAPI.getLatestPlan(projectId).catch(() => null),
      researchAPI.listSources(projectId).catch(() => []),
      researchAPI.getLatestReport(projectId).catch(() => null),
      researchAPI.listMemory(projectId).catch(() => []),
      researchAPI.listProjectSteps(projectId).catch(() => []),
    ]);
    tasks.value = taskList || [];
    latestPlan.value = plan;
    sources.value = sourceList || [];
    latestReport.value = report;
    memories.value = memoryList || [];

    const stepsByTask = {};
    (stepList || []).forEach((s) => {
      if (!stepsByTask[s.taskId]) stepsByTask[s.taskId] = [];
      stepsByTask[s.taskId].push(s);
    });
    taskSteps.value = stepsByTask;
  } catch (e) { /* ignore */ }
}

// ---- 轮询 ----
function startPolling(projectId) {
  if (!isResearchActive(currentProject.value?.status)) return;
  stopPolling();
  pollTimer = setInterval(async () => {
    if (pollInFlight) return;
    pollInFlight = true;
    try {
      const project = await researchAPI.getProject(projectId);
      if (currentProject.value?.id === projectId) {
        currentProject.value = project;
        updateProjectInList(project);
      }

      const isActive = isResearchActive(project.status);

      if (isActive) {
        const [stepList, memoryList, taskList] = await Promise.all([
          researchAPI.listProjectSteps(projectId).catch(() => []),
          researchAPI.listMemory(projectId).catch(() => []),
          researchAPI.listTasks(projectId).catch(() => []),
        ]);
        tasks.value = taskList || [];
        memories.value = memoryList || [];
        const stepsByTask = {};
        (stepList || []).forEach((s) => {
          if (!stepsByTask[s.taskId]) stepsByTask[s.taskId] = [];
          stepsByTask[s.taskId].push(s);
        });
        taskSteps.value = stepsByTask;

        const recentSteps = (stepList || []).slice(-5);
        toolCalls.value = recentSteps
          .filter((s) => s.toolName)
          .map((s) => ({ name: s.toolName, state: s.status?.toLowerCase() || "running" }));

        if (recentSteps.length > 0) {
          const lastStep = recentSteps[recentSteps.length - 1];
          agentPhase.value = lastStep.agentName;
          agentCurrentTask.value = lastStep.title;
        }
      }

      // 研究已结束，停止轮询并加载最终数据
      if (project.status === "COMPLETED" || project.status === "PARTIAL" || project.status === "FAILED" || project.status === "ARCHIVED") {
        stopPolling();
        await loadProjectData(projectId);
      }
    } catch (e) { /* ignore */ }
    finally {
      pollInFlight = false;
    }
  }, 3000);
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
  pollInFlight = false;
  toolCalls.value = [];
  agentPhase.value = "";
  agentCurrentTask.value = "";
}

// ---- Agent 交互 ----
function sendAgentMessage() {
  if (!agentMessage.value.trim()) return;
  agentSummary.value = agentMessage.value;
  agentMessage.value = "";
}

// ---- 知识确认 ----
async function confirmKnowledge(memory) {
  if (!currentProject.value || candidateProcessingIds.value.has(memory.id)) return;
  setCandidateProcessing(memory.id, true);
  try {
    await researchAPI.acceptCandidate(currentProject.value.id, memory.id);
    memories.value = memories.value.filter((m) => m.id !== memory.id);
    ElMessage.success("知识已保存到知识库");
  } finally {
    setCandidateProcessing(memory.id, false);
  }
}

async function dismissKnowledge(memory) {
  if (!currentProject.value || candidateProcessingIds.value.has(memory.id)) return;
  setCandidateProcessing(memory.id, true);
  try {
    await researchAPI.dismissCandidate(currentProject.value.id, memory.id);
    memories.value = memories.value.filter((m) => m.id !== memory.id);
    ElMessage.success("知识候选已忽略");
  } finally {
    setCandidateProcessing(memory.id, false);
  }
}

function setCandidateProcessing(memoryId, processing) {
  const nextIds = new Set(candidateProcessingIds.value);
  if (processing) nextIds.add(memoryId);
  else nextIds.delete(memoryId);
  candidateProcessingIds.value = nextIds;
}

// ---- 来源查看 ----
function viewSource(source) {
  viewingSource.value = source;
  showSourceDialog.value = true;
}

// ---- 工具函数 ----
function statusClass(status) {
  const map = {
    DRAFT: "st-draft",
    PLANNING: "st-active",
    RESEARCHING: "st-active",
    REVIEWING: "st-active",
    SYNTHESIZING: "st-active",
    RUNNING: "st-active",
    PAUSED: "st-paused",
    PENDING: "st-pending",
    WAITING_USER: "st-paused",
    COMPLETED: "st-done",
    PARTIAL: "st-partial",
    FAILED: "st-failed",
    SKIPPED: "st-skipped",
    ARCHIVED: "st-archived",
  };
  return map[status] || "";
}

function isResearchActive(status) {
  return status === "RESEARCHING"
    || status === "REVIEWING"
    || status === "SYNTHESIZING";
}

function displayMemoryKey(memoryKey) {
  if (!memoryKey) return "";
  const separatorIndex = memoryKey.indexOf(":");
  return separatorIndex >= 0 ? memoryKey.slice(separatorIndex + 1) : memoryKey;
}

function toolStateLabel(state) {
  const map = { running: "执行中", completed: "已完成", failed: "失败" };
  return map[state] || state;
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function renderMd(content) {
  if (!content) return "";
  return DOMPurify.sanitize(marked(content));
}
</script>

<style scoped>
/* ===== 设计令牌 (Study Room — warm academic, matches Scholar's Desk) ===== */
.research-workspace {
  --rw-bg: #F5F2EA;
  --rw-surface: #FBFAF5;
  --rw-surface-hover: #EFECE4;
  --rw-surface-active: #E8E4DA;
  --rw-border: #D5CFC2;
  --rw-border-light: #E5DFD2;
  --rw-primary: #2B5F4B;
  --rw-primary-light: #4A8C6E;
  --rw-primary-muted: rgba(43, 95, 75, 0.08);
  --rw-primary-alpha-hover: rgba(43, 95, 75, 0.14);
  --rw-accent: #B8723A;
  --rw-accent-muted: rgba(184, 114, 58, 0.1);
  --rw-accent-hover: rgba(184, 114, 58, 0.18);
  --rw-text: #1E1B18;
  --rw-text-secondary: #6B6458;
  --rw-text-muted: #9B9488;
  --rw-success: #3B7D5A;
  --rw-success-muted: rgba(59, 125, 90, 0.1);
  --rw-warning: #B8723A;
  --rw-warning-muted: rgba(184, 114, 58, 0.1);
  --rw-error: #B8443A;
  --rw-error-muted: rgba(184, 68, 58, 0.08);
  --rw-info: #4A7896;
  --rw-radius-sm: 2px;
  --rw-radius: 6px;
  --rw-radius-lg: 8px;
  --rw-font-display: 'Cormorant Garamond', 'Georgia', 'Noto Serif SC', 'Source Han Serif SC', serif;
  --rw-font-body: 'Crimson Pro', 'Georgia', 'Noto Serif SC', serif;
  --rw-font-ui: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
  --rw-font-mono: 'Fira Code', 'Cascadia Code', 'JetBrains Mono', 'Consolas', monospace;
  --rw-shadow-sm: 0 1px 2px rgba(30, 27, 24, 0.04);
  --rw-shadow-md: 0 2px 8px rgba(30, 27, 24, 0.05);

  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: var(--rw-bg);
  color: var(--rw-text);
  font-family: var(--rw-font-body);
  font-size: 15px;
  line-height: 1.6;
  overflow: hidden;
}

/* ===== 项目列表视图 ===== */
.project-list-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 40px 48px;
  overflow-y: auto;
}

.list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 36px;
}

.header-text {
  max-width: 560px;
}

.view-title {
  font-family: var(--rw-font-display);
  font-size: 28px;
  font-weight: 600;
  color: var(--rw-text);
  margin: 0 0 8px 0;
  letter-spacing: -0.01em;
  line-height: 1.25;
}

.view-desc {
  font-size: 15px;
  color: var(--rw-text-secondary);
  margin: 0;
  line-height: 1.6;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--rw-primary);
  color: #fff;
  border: none;
  border-radius: var(--rw-radius);
  font-size: 14px;
  font-family: var(--rw-font-ui);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s;
  white-space: nowrap;
  box-shadow: var(--rw-shadow-sm);
}

.create-btn:hover {
  background: var(--rw-primary-light);
  box-shadow: var(--rw-shadow-md);
}

/* 加载状态 */
.loading-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--rw-text-muted);
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--rw-border-light);
  border-top-color: var(--rw-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 空状态 */
.empty-projects {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--rw-text-muted);
}

.empty-visual {
  color: var(--rw-text-muted);
  margin-bottom: 8px;
}

/* 项目列表 */
.project-items {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.project-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 15px 18px;
  border-radius: var(--rw-radius);
  cursor: pointer;
  transition: background 0.15s;
  border: 1px solid transparent;
}

.project-item:hover {
  background: var(--rw-surface);
  border-color: var(--rw-border);
}

.item-body {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-family: var(--rw-font-ui);
  font-size: 14px;
  font-weight: 500;
  color: var(--rw-text);
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-goal {
  font-size: 13px;
  color: var(--rw-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 6px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--rw-text-muted);
  font-family: var(--rw-font-ui);
}

.meta-tag {
  padding: 1px 7px;
  background: var(--rw-surface);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius-sm);
  font-size: 11px;
  font-family: var(--rw-font-ui);
}

.meta-tag.st-active {
  color: var(--rw-primary);
  border-color: var(--rw-primary);
  background: var(--rw-primary-muted);
}

.meta-tag.st-done {
  color: var(--rw-success);
  background: var(--rw-success-muted);
}

.meta-tag.st-partial {
  color: var(--rw-warning);
  background: color-mix(in srgb, var(--rw-warning) 12%, transparent);
}

.meta-tag.st-failed {
  color: var(--rw-error);
  background: var(--rw-error-muted);
}

.meta-tag.st-paused {
  color: var(--rw-warning);
  background: var(--rw-warning-muted);
}

.meta-sep {
  color: var(--rw-border);
}

.item-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}

.project-item:hover .item-actions {
  opacity: 1;
}

.action-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--rw-border);
  border-radius: var(--rw-radius-sm);
  background: transparent;
  color: var(--rw-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn:hover {
  background: var(--rw-surface-hover);
  color: var(--rw-text);
}

.action-btn.primary {
  border-color: var(--rw-primary);
  color: var(--rw-primary);
}

.action-btn.primary:hover {
  background: var(--rw-primary-muted);
}

.action-btn.warn {
  border-color: var(--rw-warning);
  color: var(--rw-warning);
}

.action-btn.warn:hover {
  background: var(--rw-warning-muted);
}

.list-pagination {
  display: flex;
  justify-content: center;
  padding: 24px 0 0;
}

/* ===== 状态点 ===== */
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--rw-text-muted);
  display: inline-block;
}

.status-dot.st-draft { background: var(--rw-text-muted); }
.status-dot.st-active { background: var(--rw-primary); animation: pulse-dot 2s ease-in-out infinite; }
.status-dot.st-paused { background: var(--rw-warning); }
.status-dot.st-pending { background: var(--rw-border); }
.status-dot.st-done { background: var(--rw-success); }
.status-dot.st-partial { background: var(--rw-warning); }
.status-dot.st-failed { background: var(--rw-error); }
.status-dot.st-skipped { background: transparent; border: 1.5px dashed var(--rw-text-muted); }
.status-dot.st-archived { background: var(--rw-text-muted); }

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}

/* ===== 工作空间视图 ===== */
.workspace-view {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* ===== 左侧面板 ===== */
.left-panel {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--rw-surface);
  border-right: 1px solid var(--rw-border);
  transition: width 0.2s ease;
}

.left-panel.collapsed {
  width: 48px;
}

.left-panel.collapsed .panel-header {
  padding: 12px 10px;
  justify-content: center;
}

.right-panel.collapsed {
  width: 48px;
}

.right-panel.collapsed .panel-header {
  padding: 12px 10px;
  justify-content: center;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--rw-border-light);
  min-height: 49px;
}

.panel-title {
  flex: 1;
  font-family: var(--rw-font-ui);
  font-size: 13px;
  font-weight: 500;
  color: var(--rw-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.back-btn, .collapse-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--rw-radius-sm);
  background: transparent;
  color: var(--rw-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.back-btn:hover, .collapse-btn:hover {
  background: var(--rw-surface-hover);
  color: var(--rw-text);
}

/* 研究导航 */
.research-nav {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
}

.nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  margin-bottom: 1px;
  border: none;
  border-radius: var(--rw-radius-sm);
  background: transparent;
  color: var(--rw-text-secondary);
  font-size: 13px;
  font-family: var(--rw-font-ui);
  cursor: pointer;
  transition: all 0.12s;
  text-align: left;
}

.nav-item:hover {
  background: var(--rw-surface-hover);
  color: var(--rw-text);
}

.nav-item.active {
  background: var(--rw-primary-muted);
  color: var(--rw-primary);
}

.nav-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-badge {
  font-size: 10px;
  font-family: var(--rw-font-ui);
  padding: 1px 6px;
  background: var(--rw-border-light);
  color: var(--rw-text-muted);
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

/* 面板底部状态 */
.panel-status {
  padding: 12px;
  border-top: 1px solid var(--rw-border-light);
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.status-text {
  font-family: var(--rw-font-ui);
  font-size: 12px;
  color: var(--rw-text-secondary);
}

.progress-bar {
  height: 3px;
  background: var(--rw-border-light);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.progress-fill {
  height: 100%;
  background: var(--rw-primary);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.progress-text {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
}

/* ===== 中间面板 ===== */
.center-panel {
  flex: 1;
  overflow-y: auto;
  padding: 32px 36px;
  min-width: 0;
}

.content-section {
  max-width: 780px;
}

.section-label {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--rw-text-muted);
  margin-bottom: 12px;
}

.section-heading {
  font-family: var(--rw-font-display);
  font-size: 24px;
  font-weight: 600;
  color: var(--rw-text);
  margin: 0 0 14px 0;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.section-meta {
  display: flex;
  gap: 20px;
  font-family: var(--rw-font-ui);
  font-size: 13px;
  color: var(--rw-text-muted);
}

.empty-section {
  padding: 48px 0;
  color: var(--rw-text-muted);
  font-size: 14px;
  line-height: 1.6;
}

/* 记忆卡片 */
.memory-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.memory-card {
  padding: 18px 20px;
  background: var(--rw-surface);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius);
  border-left: 3px solid var(--rw-primary);
}

.memory-card.gap {
  border-left-color: var(--rw-warning);
}

.memory-card.finding {
  border-left-color: var(--rw-accent);
}

.memory-card.conclusion {
  border-left-color: var(--rw-success);
}

.memory-key {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  font-weight: 500;
  color: var(--rw-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

/* 研究计划 */
.plan-detail {
  background: var(--rw-surface);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius);
  padding: 22px;
}

.plan-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  font-family: var(--rw-font-ui);
  font-size: 13px;
  color: var(--rw-text-secondary);
}

.plan-version {
  padding: 3px 10px;
  background: var(--rw-primary-muted);
  color: var(--rw-primary);
  border-radius: var(--rw-radius-sm);
  font-size: 12px;
  font-weight: 500;
}

.plan-complexity {
  padding: 3px 10px;
  background: var(--rw-surface-hover);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius-sm);
  font-size: 12px;
}

.plan-chain {
  font-size: 12px;
  color: var(--rw-text-muted);
}

/* 任务列表 */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-item {
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius);
  overflow: hidden;
  background: var(--rw-surface);
  transition: border-color 0.15s;
}

.task-item:hover {
  border-color: var(--rw-border);
}

.task-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 16px;
  cursor: pointer;
  user-select: none;
}

.task-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--rw-text-muted);
}

.task-status-dot.st-active { background: var(--rw-primary); animation: pulse-dot 2s ease-in-out infinite; }
.task-status-dot.st-done { background: var(--rw-success); }
.task-status-dot.st-failed { background: var(--rw-error); }
.task-status-dot.st-paused { background: var(--rw-warning); }
.task-status-dot.st-pending { background: var(--rw-border); }
.task-status-dot.st-skipped { background: transparent; border: 1.5px dashed var(--rw-text-muted); }

.task-title {
  flex: 1;
  font-family: var(--rw-font-ui);
  font-size: 13px;
  font-weight: 500;
  color: var(--rw-text);
}

.task-status-label {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
}

.task-chevron {
  flex-shrink: 0;
  color: var(--rw-text-muted);
  transition: transform 0.2s ease;
}

.task-chevron.open {
  transform: rotate(90deg);
}

.task-body {
  padding: 0 16px 16px;
  border-top: 1px solid var(--rw-border-light);
  padding-top: 14px;
  margin: 0 16px;
}

.task-desc {
  font-size: 14px;
  color: var(--rw-text-secondary);
  margin: 0 0 10px 0;
  line-height: 1.6;
}

.task-question {
  font-size: 14px;
  color: var(--rw-info);
  margin: 0 0 10px 0;
  line-height: 1.5;
}

/* 执行步骤 */
.steps-label {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 12px 0 6px;
}

.step-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 0;
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
  border-bottom: 1px solid var(--rw-border-light);
}

.step-agent {
  color: var(--rw-primary);
  font-weight: 500;
  width: 110px;
  flex-shrink: 0;
}

.step-type {
  width: 60px;
  flex-shrink: 0;
}

.step-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-status.st-active { color: var(--rw-primary); }
.step-status.st-done { color: var(--rw-success); }
.step-status.st-failed { color: var(--rw-error); }

.step-duration {
  width: 40px;
  text-align: right;
  flex-shrink: 0;
}

/* 来源列表 */
.source-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-item {
  padding: 15px 18px;
  background: var(--rw-surface);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.source-item:hover {
  border-color: var(--rw-border);
  background: var(--rw-surface-hover);
}

.source-header {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.source-reliability {
  font-family: var(--rw-font-ui);
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 3px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.rel-high { background: var(--rw-success-muted); color: var(--rw-success); }
.rel-medium { background: var(--rw-warning-muted); color: var(--rw-warning); }
.rel-low { background: var(--rw-error-muted); color: var(--rw-error); }
.rel-unknown { background: var(--rw-surface-hover); color: var(--rw-text-muted); }

.source-type {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
}

.source-title {
  font-family: var(--rw-font-ui);
  font-size: 13px;
  font-weight: 500;
  color: var(--rw-primary);
  text-decoration: none;
  display: block;
  margin-bottom: 6px;
}

.source-title:hover {
  text-decoration: underline;
}

.source-snippet {
  font-size: 13px;
  color: var(--rw-text-secondary);
  line-height: 1.6;
}

.source-footer {
  margin-top: 8px;
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
}

/* 研究报告 */
.report-detail {
  background: var(--rw-surface);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius);
  padding: 26px;
}

.report-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  font-family: var(--rw-font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--rw-text);
}

.report-version {
  padding: 3px 10px;
  background: var(--rw-primary-muted);
  color: var(--rw-primary);
  border-radius: var(--rw-radius-sm);
  font-family: var(--rw-font-ui);
  font-size: 11px;
  font-weight: 500;
}

.report-stats {
  display: flex;
  gap: 20px;
  margin-top: 22px;
  padding-top: 16px;
  border-top: 1px solid var(--rw-border-light);
  font-family: var(--rw-font-ui);
  font-size: 12px;
  color: var(--rw-text-muted);
}

/* 知识候选 */
.candidate-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.candidate-item {
  padding: 20px;
  background: var(--rw-surface);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius);
  border-left: 3px solid var(--rw-accent);
}

.candidate-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--rw-border-light);
}

.confirm-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: var(--rw-primary);
  color: #fff;
  border: none;
  border-radius: var(--rw-radius-sm);
  font-family: var(--rw-font-ui);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}

.confirm-btn:hover { background: var(--rw-primary-light); }

.dismiss-btn {
  padding: 8px 18px;
  background: transparent;
  color: var(--rw-text-muted);
  border: 1px solid var(--rw-border);
  border-radius: var(--rw-radius-sm);
  font-family: var(--rw-font-ui);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.dismiss-btn:hover {
  border-color: var(--rw-text-muted);
  color: var(--rw-text);
}

/* ===== 右侧面板 ===== */
.right-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--rw-surface);
  border-left: 1px solid var(--rw-border);
  transition: width 0.2s ease;
}

.panel-label {
  font-family: var(--rw-font-ui);
  font-size: 13px;
  font-weight: 500;
  color: var(--rw-text);
}

.context-content {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
}

.context-block {
  margin-bottom: 22px;
}

.context-label {
  font-family: var(--rw-font-ui);
  font-size: 10px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--rw-text-muted);
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--rw-border-light);
}

.context-empty {
  font-family: var(--rw-font-ui);
  font-size: 12px;
  color: var(--rw-text-muted);
  font-style: italic;
}

/* Agent 状态 */
.agent-pulse {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--rw-font-ui);
  font-size: 13px;
  color: var(--rw-text-secondary);
  margin-bottom: 8px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--rw-text-muted);
  flex-shrink: 0;
}

.agent-pulse.active .pulse-dot {
  background: var(--rw-primary);
  animation: pulse-dot 1.5s ease-in-out infinite;
}

.phase-label {
  font-size: 10px;
  color: var(--rw-text-muted);
  margin-right: 6px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.agent-phase {
  font-family: var(--rw-font-ui);
  font-size: 12px;
  color: var(--rw-primary);
  margin-bottom: 4px;
}

.agent-task {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
  margin-bottom: 10px;
}

/* 工具调用 */
.tool-status-label {
  font-family: var(--rw-font-ui);
  font-size: 10px;
  color: var(--rw-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 6px;
}

.tool-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  font-family: var(--rw-font-ui);
  font-size: 11px;
}

.tool-name {
  font-family: var(--rw-font-mono);
  font-size: 10px;
  color: var(--rw-text-secondary);
  background: var(--rw-surface-hover);
  padding: 1px 5px;
  border-radius: 2px;
}

.tool-state {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
}

.tool-state.running { background: var(--rw-primary-muted); color: var(--rw-primary); }
.tool-state.completed { background: var(--rw-success-muted); color: var(--rw-success); }
.tool-state.failed { background: var(--rw-error-muted); color: var(--rw-error); }

/* 知识标签 */
.knowledge-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.k-tag {
  padding: 4px 10px;
  background: var(--rw-surface-hover);
  border: 1px solid var(--rw-border-light);
  border-radius: var(--rw-radius-sm);
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-secondary);
}

/* 外部来源 */
.ext-source-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ext-link {
  font-family: var(--rw-font-ui);
  font-size: 12px;
  color: var(--rw-primary);
  text-decoration: none;
  padding: 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ext-link:hover {
  text-decoration: underline;
  color: var(--rw-accent);
}

/* ===== 底部栏 ===== */
.bottom-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 16px;
  background: var(--rw-surface);
  border-top: 1px solid var(--rw-border);
  min-height: 52px;
}

.agent-controls {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.ctrl-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--rw-border);
  border-radius: var(--rw-radius-sm);
  background: transparent;
  color: var(--rw-text-secondary);
  font-family: var(--rw-font-ui);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.ctrl-btn:hover {
  background: var(--rw-surface-hover);
  color: var(--rw-text);
}

.ctrl-btn.start, .ctrl-btn.resume {
  border-color: var(--rw-primary);
  color: var(--rw-primary);
}

.ctrl-btn.start:hover, .ctrl-btn.resume:hover {
  background: var(--rw-primary-muted);
}

.ctrl-btn.pause {
  border-color: var(--rw-warning);
  color: var(--rw-warning);
}

.ctrl-btn.pause:hover {
  background: var(--rw-warning-muted);
}

.ctrl-btn.retry {
  border-color: var(--rw-accent);
  color: var(--rw-accent);
}

.ctrl-btn.retry:hover {
  background: var(--rw-accent-muted);
}

.input-area {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.agent-input {
  flex: 1;
  padding: 8px 14px;
  background: var(--rw-bg);
  border: 1px solid var(--rw-border);
  border-radius: var(--rw-radius);
  color: var(--rw-text);
  font-family: var(--rw-font-ui);
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
}

.agent-input:focus {
  border-color: var(--rw-primary);
}

.agent-input::placeholder {
  color: var(--rw-text-muted);
}

.agent-input:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.send-btn {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--rw-border);
  border-radius: var(--rw-radius-sm);
  background: var(--rw-surface);
  color: var(--rw-text-muted);
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: var(--rw-primary-muted);
  color: var(--rw-primary);
  border-color: var(--rw-primary);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.agent-summary {
  font-family: var(--rw-font-ui);
  font-size: 11px;
  color: var(--rw-text-muted);
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 0;
}

/* ===== 对话框样式 ===== */
:deep(.research-dialog) {
  --el-dialog-bg: var(--rw-surface);
  border-radius: var(--rw-radius-lg);
}

:deep(.research-dialog .el-dialog__header) {
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--rw-border-light);
}

:deep(.research-dialog .el-dialog__title) {
  font-family: var(--rw-font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--rw-text);
}

:deep(.research-dialog .el-dialog__body) {
  padding: 20px 24px;
  color: var(--rw-text);
}

:deep(.research-dialog .el-dialog__footer) {
  padding: 12px 24px 20px;
  border-top: 1px solid var(--rw-border-light);
}

:deep(.research-dialog .el-form-item__label) {
  font-family: var(--rw-font-ui);
  color: var(--rw-text-secondary);
  font-size: 13px;
}

:deep(.research-dialog .el-input__wrapper) {
  background: var(--rw-bg);
  border-color: var(--rw-border);
  box-shadow: none;
}

:deep(.research-dialog .el-input__inner) {
  color: var(--rw-text);
  font-family: var(--rw-font-ui);
}

:deep(.research-dialog .el-textarea__inner) {
  background: var(--rw-bg);
  border-color: var(--rw-border);
  color: var(--rw-text);
  font-family: var(--rw-font-ui);
  font-size: 14px;
  line-height: 1.7;
}

:deep(.research-dialog .el-button--primary) {
  background: var(--rw-primary);
  border-color: var(--rw-primary);
}

:deep(.research-dialog .el-button--primary:hover) {
  background: var(--rw-primary-light);
  border-color: var(--rw-primary-light);
}

/* 来源详情 */
.source-detail {
  color: var(--rw-text);
}

.source-meta-row {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--rw-border-light);
  font-family: var(--rw-font-ui);
  font-size: 12px;
  color: var(--rw-text-muted);
}

.source-ext-link {
  color: var(--rw-primary);
  text-decoration: none;
}

.source-ext-link:hover {
  text-decoration: underline;
}

.source-content {
  font-size: 14px;
  line-height: 1.75;
  color: var(--rw-text-secondary);
  max-height: 420px;
  overflow-y: auto;
}

/* ===== Markdown 内容样式 ===== */
.memory-content :deep(p),
.plan-rationale :deep(p),
.report-summary :deep(p),
.report-body :deep(p),
.task-result :deep(p) {
  margin: 8px 0;
  font-size: 14px;
  color: var(--rw-text-secondary);
  line-height: 1.7;
}

.memory-content :deep(h1),
.memory-content :deep(h2),
.memory-content :deep(h3),
.report-body :deep(h1),
.report-body :deep(h2),
.report-body :deep(h3) {
  font-family: var(--rw-font-display);
  color: var(--rw-text);
  margin: 16px 0 8px 0;
}

.memory-content :deep(h1),
.report-body :deep(h1) { font-size: 18px; }

.memory-content :deep(h2),
.report-body :deep(h2) { font-size: 16px; }

.memory-content :deep(h3),
.report-body :deep(h3) { font-size: 14px; }

.memory-content :deep(ul),
.memory-content :deep(ol),
.report-body :deep(ul),
.report-body :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.memory-content :deep(li),
.report-body :deep(li) {
  font-size: 14px;
  color: var(--rw-text-secondary);
  margin: 4px 0;
}

.memory-content :deep(code),
.report-body :deep(code) {
  background: var(--rw-surface-hover);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: var(--rw-font-mono);
  font-size: 12px;
  color: var(--rw-accent);
}

.memory-content :deep(pre),
.report-body :deep(pre) {
  background: var(--rw-bg);
  padding: 14px;
  border-radius: var(--rw-radius-sm);
  overflow-x: auto;
  margin: 12px 0;
  border: 1px solid var(--rw-border-light);
}

.memory-content :deep(pre code),
.report-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--rw-text);
}

.memory-content :deep(a),
.report-body :deep(a) {
  color: var(--rw-primary);
}

.memory-content :deep(blockquote),
.report-body :deep(blockquote) {
  border-left: 2px solid var(--rw-primary);
  margin: 10px 0;
  padding: 6px 14px;
  color: var(--rw-text-muted);
  background: var(--rw-surface-hover);
  border-radius: 0 var(--rw-radius-sm) var(--rw-radius-sm) 0;
}

.memory-content :deep(table),
.report-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
}

.memory-content :deep(th),
.memory-content :deep(td),
.report-body :deep(th),
.report-body :deep(td) {
  border: 1px solid var(--rw-border-light);
  padding: 8px 12px;
  text-align: left;
}

.memory-content :deep(th),
.report-body :deep(th) {
  background: var(--rw-surface-hover);
  font-weight: 500;
  font-family: var(--rw-font-ui);
}

/* ===== 分页样式 ===== */
:deep(.el-pagination .el-pager li) {
  background: transparent;
  color: var(--rw-text-secondary);
  font-family: var(--rw-font-ui);
}

:deep(.el-pagination button) {
  background: transparent;
  color: var(--rw-text-secondary);
}

:deep(.el-pagination button:disabled) {
  opacity: 0.3;
}

:deep(.el-pagination .el-pager li.is-active) {
  background: var(--rw-primary);
  color: #fff;
}

/* ===== EL Message Box 深色覆盖 ===== */
:deep(.el-message-box) {
  background: var(--rw-surface);
  border: 1px solid var(--rw-border);
}

:deep(.el-message-box__title) {
  color: var(--rw-text);
}

:deep(.el-message-box__message) {
  color: var(--rw-text-secondary);
}
</style>

<style>
/* 去除 .page 内边距，让研究空间填满整个内容区 */
.page:has(.research-workspace) {
  padding: 0;
  position: relative;
  height: calc(100vh - var(--navbar-height));
}
</style>
