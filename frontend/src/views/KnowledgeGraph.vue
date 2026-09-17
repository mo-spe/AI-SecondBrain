<template>
  <div class="graph-workspace">
    <a class="skip-link" href="#knowledge-graph-canvas">跳到知识图谱</a>

    <header class="workspace-header">
      <div class="title-group">
        <span class="eyebrow">SECOND BRAIN · KNOWLEDGE NETWORK</span>
        <h1>知识网络图</h1>
        <p>在关联之间探索你的知识脉络。</p>
      </div>
      <div class="graph-metrics" aria-label="图谱统计">
        <div class="metric"><strong>{{ nodes.length }}</strong><span>知识点</span></div>
        <div class="metric"><strong>{{ edges.length }}</strong><span>关系</span></div>
      </div>
    </header>

    <section class="graph-toolbar" aria-label="图谱工具栏">
      <label class="graph-search">
        <el-icon aria-hidden="true"><Search /></el-icon>
        <input v-model.trim="searchTerm" type="search" placeholder="搜索知识点并聚焦" aria-label="搜索知识点" @input="handleSearch" />
        <button v-if="searchTerm" type="button" class="clear-search" aria-label="清除搜索" @click="clearSearch"><el-icon><Close /></el-icon></button>
      </label>
      <label class="mastery-filter">
        <span>掌握度</span>
        <select v-model="masteryFilter" aria-label="按掌握度筛选">
          <option value="all">全部</option>
          <option v-for="level in masteryOptions" :key="level.value" :value="String(level.value)">{{ level.label }}</option>
        </select>
      </label>
      <div class="toolbar-actions">
        <button type="button" class="tool-button" title="适配画布" aria-label="适配画布" @click="fitView"><el-icon><FullScreen /></el-icon><span>适配</span></button>
        <button type="button" class="tool-button" title="重置布局" aria-label="重置布局" @click="resetView"><el-icon><RefreshRight /></el-icon><span>重置</span></button>
        <button type="button" class="tool-button inspector-toggle" :aria-expanded="String(inspectorVisible)" aria-controls="graph-inspector" @click="inspectorVisible = !inspectorVisible"><el-icon><Tickets /></el-icon><span>{{ inspectorVisible ? '收起详情' : '打开详情' }}</span></button>
      </div>
    </section>

    <main class="graph-layout" :class="{ 'with-inspector': inspectorVisible }">
      <section id="knowledge-graph-canvas" class="graph-canvas-card" aria-label="可交互知识网络图">
        <div class="canvas-grid" aria-hidden="true"></div>
        <div v-if="loading" class="canvas-state loading-state" role="status">
          <span class="loading-orbit"></span><strong>正在编排你的知识网络</strong><span>读取当前工作区中的真实知识点与关系</span>
        </div>
        <div v-else-if="loadError" class="canvas-state error-state" role="alert">
          <el-icon><WarningFilled /></el-icon><strong>图谱暂时无法加载</strong><span>{{ loadError }}</span><button type="button" class="retry-button" @click="loadGraph">重新加载</button>
        </div>
        <div v-else-if="nodes.length === 0" class="canvas-state empty-state">
          <el-icon><Connection /></el-icon><strong>当前工作区还没有可展示的知识网络</strong><span>创建知识点并建立关系后，它们会在这里自然生长。</span><button type="button" class="retry-button" @click="router.push('/knowledge/new')">创建知识点</button>
        </div>
        <div v-else-if="visibleNodes.length === 0" class="canvas-state empty-state">
          <el-icon><Filter /></el-icon><strong>没有符合筛选条件的知识点</strong><button type="button" class="retry-button" @click="masteryFilter = 'all'">清除筛选</button>
        </div>
        <div v-else ref="chartContainer" class="graph-canvas" tabindex="0" role="img" :aria-label="canvasDescription" @keydown.esc.prevent="clearSelection"></div>
        <div v-if="!loading && !loadError && nodes.length" class="canvas-legend" aria-label="掌握度图例">
          <span v-for="item in masteryOptions" :key="item.value" class="legend-item"><i :style="{ backgroundColor: masteryColor(item.value) }"></i>{{ item.label }}</span>
        </div>
      </section>

      <aside v-if="inspectorVisible" id="graph-inspector" class="graph-inspector" aria-label="节点详情检查器">
        <button type="button" class="inspector-close" aria-label="收起详情检查器" @click="inspectorVisible = false"><el-icon><Close /></el-icon></button>
        <template v-if="selectedNode">
          <span class="inspector-kicker">NODE INSPECTOR</span><h2>{{ selectedNode.label }}</h2>
          <div class="node-state-row"><span class="mastery-chip" :style="{ '--chip-color': masteryColor(selectedNode.masteryLevel) }">{{ masteryText(selectedNode.masteryLevel) }}</span><span class="importance-chip">重要度 {{ selectedNode.importance || 0 }}/5</span></div>
          <p class="node-connection-count">与 {{ selectedNeighbors.length }} 个知识点直接相连</p>
          <div class="inspector-section">
            <div class="section-title"><span>关联节点</span><small>{{ selectedNeighbors.length }}</small></div>
            <div v-if="selectedNeighbors.length" class="neighbor-list">
              <button v-for="neighbor in selectedNeighbors" :key="neighbor.id" type="button" class="neighbor-item" @click="selectNode(neighbor.id)"><i :style="{ backgroundColor: masteryColor(neighbor.masteryLevel) }"></i><span>{{ neighbor.label }}</span><small>{{ neighbor.relationLabel || '相关' }}</small></button>
            </div>
            <p v-else class="muted-copy">这个知识点尚未与其他节点建立关系。</p>
          </div>
          <button type="button" class="open-detail-button" @click="openKnowledge(selectedNode.id)">打开知识详情 <el-icon><ArrowRight /></el-icon></button>
        </template>
        <template v-else>
          <span class="inspector-kicker">EXPLORATION MODE</span><h2>从一个知识点开始</h2>
          <p class="inspector-intro">点击节点会聚焦其一跳关系；滚轮缩放，拖动空白处平移，拖动节点可临时调整布局。</p>
          <div class="exploration-hints"><span><i></i>节点大小代表重要度</span><span><i></i>节点颜色代表掌握度</span><span><i></i>连线粗细代表关系强度</span></div>
        </template>
      </aside>
    </main>
    <p class="sr-only" aria-live="polite">{{ liveMessage }}</p>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import * as echarts from "echarts";
import { ArrowRight, Close, Connection, Filter, FullScreen, RefreshRight, Search, Tickets, WarningFilled } from "@element-plus/icons-vue";
import { knowledgeAPI } from "@/api/knowledge";

const router = useRouter();
const chartContainer = ref(null);
const loading = ref(true);
const loadError = ref("");
const nodes = ref([]);
const edges = ref([]);
const searchTerm = ref("");
const masteryFilter = ref("all");
const selectedNodeId = ref(null);
const inspectorVisible = ref(typeof window === "undefined" || window.innerWidth >= 960);
const liveMessage = ref("");
let chart = null;
let resizeObserver = null;

const masteryOptions = [
  { value: 5, label: "专家" }, { value: 4, label: "精通" }, { value: 3, label: "掌握" },
  { value: 2, label: "熟悉" }, { value: 1, label: "入门" }, { value: 0, label: "未掌握" },
];
const visibleNodes = computed(() => masteryFilter.value === "all" ? nodes.value : nodes.value.filter((node) => Number(node.masteryLevel ?? 0) === Number(masteryFilter.value)));
const visibleNodeIds = computed(() => new Set(visibleNodes.value.map((node) => node.id)));
const visibleEdges = computed(() => edges.value.filter((edge) => visibleNodeIds.value.has(edge.source) && visibleNodeIds.value.has(edge.target)));
const selectedNode = computed(() => nodes.value.find((node) => node.id === selectedNodeId.value) || null);
const selectedNeighbors = computed(() => {
  if (!selectedNode.value) return [];
  const related = new Map();
  edges.value.forEach((edge) => {
    if (edge.source === selectedNode.value.id) related.set(edge.target, edge.label);
    if (edge.target === selectedNode.value.id) related.set(edge.source, edge.label);
  });
  return [...related.entries()].map(([id, relationLabel]) => {
    const node = nodes.value.find((item) => item.id === id);
    return node ? { ...node, relationLabel } : null;
  }).filter(Boolean);
});
const selectedNeighborhood = computed(() => selectedNode.value ? new Set([selectedNode.value.id, ...selectedNeighbors.value.map((node) => node.id)]) : null);
const canvasDescription = computed(() => `知识网络图，共 ${visibleNodes.value.length} 个可见知识点、${visibleEdges.value.length} 条可见关系。${selectedNode.value ? `当前聚焦：${selectedNode.value.label}。` : "未选择节点。"}`);

const escapeHtml = (value) => String(value ?? "").replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[character]));
const masteryText = (level) => masteryOptions.find((item) => item.value === Number(level))?.label || "未掌握";
const masteryColor = (level) => ({ 5: "#78b892", 4: "#a2d1a8", 3: "#d8ae6b", 2: "#cf795f", 1: "#bd6977", 0: "#7d8986" }[Number(level)] || "#7d8986");

const normalizeGraph = (graph) => {
  const normalizedNodes = Array.isArray(graph?.nodes) ? graph.nodes.filter((node) => node?.id !== null && node?.id !== undefined).map((node) => ({
    ...node, id: String(node.id), label: String(node.label || "未命名知识点"), importance: Math.max(0, Math.min(5, Number(node.importance) || 0)), masteryLevel: Math.max(0, Math.min(5, Number(node.masteryLevel) || 0)),
  })) : [];
  const validIds = new Set(normalizedNodes.map((node) => node.id));
  const normalizedEdges = Array.isArray(graph?.edges) ? graph.edges.map((edge) => ({ ...edge, source: String(edge.source), target: String(edge.target), strength: Math.max(1, Math.min(5, Number(edge.strength) || 1)) })).filter((edge) => validIds.has(edge.source) && validIds.has(edge.target) && edge.source !== edge.target) : [];
  return { nodes: normalizedNodes, edges: normalizedEdges };
};

const loadGraph = async () => {
  loading.value = true; loadError.value = "";
  try {
    const graph = await knowledgeAPI.getRelationGraph();
    const normalized = normalizeGraph(graph);
    nodes.value = normalized.nodes; edges.value = normalized.edges; selectedNodeId.value = null;
    liveMessage.value = `已加载 ${nodes.value.length} 个知识点和 ${edges.value.length} 条关系。`;
  } catch (error) {
    nodes.value = []; edges.value = []; loadError.value = error?.message || "请检查网络连接后重试。"; liveMessage.value = "知识网络图加载失败。"; disposeChart();
  } finally {
    loading.value = false;
    // 图表容器受加载状态控制，必须在 DOM 切换完成后再初始化 ECharts。
    await nextTick();
    renderGraph();
  }
};

const graphOption = () => {
  const selectedIds = selectedNeighborhood.value;
  const searchLower = searchTerm.value.toLocaleLowerCase();
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const graphNodes = visibleNodes.value.map((node) => {
    const isSelected = node.id === selectedNodeId.value;
    const isNeighbor = selectedIds?.has(node.id);
    const isSearchMatch = searchLower && node.label.toLocaleLowerCase().includes(searchLower);
    const revealName = isSelected || isSearchMatch;
    const dimmed = selectedIds && !isNeighbor;
    return {
      ...node, name: node.label, knowledgeTitle: node.label, symbolSize: Math.max(28, Math.min(62, Number(node.size) || 22 + node.importance * 7)),
      itemStyle: { color: node.color || masteryColor(node.masteryLevel), borderColor: isSelected ? "#f9f4e8" : "rgba(238, 245, 241, 0.78)", borderWidth: isSelected ? 3 : 1.5, opacity: dimmed ? 0.18 : 1, shadowBlur: isSelected || isSearchMatch ? 22 : 9, shadowColor: node.color || masteryColor(node.masteryLevel) },
      label: { show: Boolean(revealName || node.importance >= 4), position: "bottom", distance: 7, color: dimmed ? "rgba(218, 228, 223, 0.28)" : "#eaf1ee", fontFamily: "var(--font-family-ui)", fontSize: 12, fontWeight: isSelected ? 700 : 500, width: revealName ? 200 : 118, overflow: revealName ? "break" : "truncate", lineHeight: 16 },
    };
  });
  const graphEdges = visibleEdges.value.map((edge) => {
    const active = selectedIds && selectedIds.has(edge.source) && selectedIds.has(edge.target);
    const dimmed = selectedIds && !active;
    return { ...edge, relationTitle: edge.label || "相关", lineStyle: { color: active ? "rgba(196, 222, 208, 0.92)" : "rgba(154, 178, 167, 0.32)", width: active ? Math.max(2, edge.strength) : Math.max(1, edge.strength * 0.56), opacity: dimmed ? 0.1 : 1, curveness: 0.08 }, label: { show: Boolean(active), formatter: edge.label || "相关", color: "#d7e4de", fontSize: 11, fontFamily: "var(--font-family-ui)" } };
  });
  return {
    animation: !reduceMotion, animationDurationUpdate: 260, aria: { enabled: true, description: canvasDescription.value },
    tooltip: { confine: true, padding: [12, 14], backgroundColor: "rgba(20, 27, 24, 0.98)", borderColor: "rgba(211, 230, 220, 0.3)", borderWidth: 1, textStyle: { color: "#edf4f0", fontFamily: "var(--font-family-ui)" }, formatter: (params) => {
      if (params.dataType !== "node") return `<span style="color:#a9bab2">关系</span><br/><strong>${escapeHtml(params.data?.relationTitle || "相关")}</strong>`;
      const title = params.data?.knowledgeTitle || params.data?.name || "未命名知识点";
      return `<div style="max-width:280px"><div style="margin-bottom:7px;color:#f4f8f5;font-size:14px;font-weight:700;line-height:1.45;word-break:break-word">${escapeHtml(title)}</div><div style="color:#b9c9c1;font-size:12px">掌握度：${escapeHtml(masteryText(params.data.masteryLevel))}　重要度：${params.data.importance || 0}/5</div><div style="margin-top:6px;color:#8fa39a;font-size:11px">点击可查看关联知识点与详情</div></div>`;
    } },
    series: [{ type: "graph", layout: "force", roam: true, draggable: true, selectedMode: "single", data: graphNodes, links: graphEdges, edgeSymbol: ["none", "arrow"], edgeSymbolSize: [0, 6], labelLayout: { hideOverlap: true }, emphasis: { focus: "adjacency", scale: 1.06, label: { show: true, width: 200, overflow: "break", lineHeight: 16, color: "#f4f8f5", fontWeight: 700 } }, force: { repulsion: 680, edgeLength: [90, 250], gravity: 0.045, friction: 0.72, layoutAnimation: !reduceMotion } }],
  };
};

const renderGraph = () => {
  if (!chartContainer.value || !visibleNodes.value.length) return;
  if (!chart) {
    chart = echarts.init(chartContainer.value, undefined, { renderer: "canvas" });
    chart.on("click", (params) => params.dataType === "node" ? selectNode(params.data.id) : clearSelection());
    chart.getZr().on("click", (event) => {
      if (!event.target) clearSelection();
    });
  }
  chart.setOption(graphOption(), true); chart.resize();
};
const disposeChart = () => { if (chart) { chart.dispose(); chart = null; } };
const selectNode = (id) => { selectedNodeId.value = String(id); inspectorVisible.value = true; liveMessage.value = `已聚焦 ${selectedNode.value?.label || "知识点"} 及其关联节点。`; renderGraph(); };
const clearSelection = () => { if (!selectedNodeId.value) return; selectedNodeId.value = null; liveMessage.value = "已清除节点聚焦。"; renderGraph(); };
const handleSearch = () => { const query = searchTerm.value.toLocaleLowerCase(); if (!query) return renderGraph(); const match = visibleNodes.value.find((node) => node.label.toLocaleLowerCase().includes(query)); if (match) selectNode(match.id); else renderGraph(); };
const clearSearch = () => { searchTerm.value = ""; renderGraph(); };
const fitView = () => { chart?.resize(); chart?.dispatchAction({ type: "restore" }); liveMessage.value = "已适配图谱视图。"; };
const resetView = () => { selectedNodeId.value = null; renderGraph(); liveMessage.value = "已重置本次会话中的图谱布局。"; };
const openKnowledge = (id) => router.push(`/knowledge/${id}`);

watch(masteryFilter, () => { if (selectedNodeId.value && !visibleNodeIds.value.has(selectedNodeId.value)) selectedNodeId.value = null; nextTick(renderGraph); });
onMounted(() => { loadGraph(); resizeObserver = new ResizeObserver(() => chart?.resize()); });
watch(chartContainer, (element) => { if (element) resizeObserver?.observe(element); });
onBeforeUnmount(() => { resizeObserver?.disconnect(); disposeChart(); });
</script>

<style scoped>
.graph-workspace { display: flex; flex-direction: column; height: calc(100vh - var(--navbar-height)); min-height: 640px; padding: 16px 18px 18px; overflow: hidden; color: var(--text-primary); }
.skip-link { position: fixed; left: 16px; top: -60px; z-index: 10; padding: 10px 14px; border-radius: 8px; background: var(--color-primary-dark); color: #fff; transition: top .2s ease; }.skip-link:focus { top: 16px; }
.workspace-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 12px; }.eyebrow,.inspector-kicker { display: block; color: var(--color-primary); font: 700 10px/1.2 var(--font-family-ui); letter-spacing: .13em; }.title-group h1 { margin: 4px 0 2px; font-size: 28px; }.title-group p { margin: 0; color: var(--text-secondary); font-size: 13px; }
.graph-metrics { display: flex; border-left: 1px solid var(--border-light); }.metric { min-width: 88px; padding-left: 20px; margin-left: 20px; }.metric + .metric { border-left: 1px solid var(--border-light); }.metric strong,.metric span { display: block; }.metric strong { font: 700 24px/1 var(--font-family-ui); color: var(--text-primary); }.metric span { margin-top: 6px; color: var(--text-secondary); font: 12px var(--font-family-ui); }
.graph-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; font-family: var(--font-family-ui); }.graph-search { display: flex; align-items: center; width: min(340px,36vw); height: 42px; gap: 9px; padding: 0 12px; border: 1px solid var(--border-light); border-radius: 9px; background: var(--bg-card); color: var(--text-secondary); transition: border-color .2s ease,box-shadow .2s ease; }.graph-search:focus-within { border-color: var(--color-primary); box-shadow: var(--shadow-focus-ring); }.graph-search input { width: 100%; border: 0; outline: 0; background: transparent; color: var(--text-primary); font: 14px var(--font-family-ui); }.clear-search,.inspector-close { display: inline-grid; place-items: center; border: 0; background: transparent; color: var(--text-secondary); cursor: pointer; }
.mastery-filter { display: flex; align-items: center; gap: 8px; height: 42px; padding: 0 12px; border: 1px solid var(--border-light); border-radius: 9px; background: var(--bg-card); color: var(--text-secondary); font-size: 13px; }.mastery-filter select { border: 0; outline: 0; background: transparent; color: var(--text-primary); font: inherit; cursor: pointer; }.toolbar-actions { display: flex; gap: 8px; margin-left: auto; }
.tool-button,.retry-button,.open-detail-button { display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 42px; padding: 0 12px; border: 1px solid var(--border-light); border-radius: 8px; background: var(--bg-card); color: var(--text-regular); font: 600 13px var(--font-family-ui); cursor: pointer; transition: background .2s ease,border-color .2s ease,color .2s ease; }.tool-button:hover,.retry-button:hover,.open-detail-button:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-alpha-10); }.tool-button:focus-visible,.retry-button:focus-visible,.open-detail-button:focus-visible,.neighbor-item:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.graph-layout { display: grid; flex: 1; grid-template-columns: minmax(0,1fr); gap: 16px; min-height: 0; }.graph-layout.with-inspector { grid-template-columns: minmax(0,1fr) 330px; }.graph-canvas-card { position: relative; min-height: 0; overflow: hidden; border: 1px solid #2b3934; border-radius: 14px; background: #151c19; box-shadow: 0 18px 42px rgba(13,24,20,.18); }.canvas-grid { position: absolute; inset: 0; pointer-events: none; background-image: linear-gradient(rgba(203,226,215,.045) 1px,transparent 1px),linear-gradient(90deg,rgba(203,226,215,.045) 1px,transparent 1px),radial-gradient(circle at 72% 18%,rgba(74,140,110,.18),transparent 30%); background-size: 28px 28px,28px 28px,auto; }.graph-canvas { position: absolute; inset: 0; outline: none; }.graph-canvas:focus-visible { box-shadow: inset 0 0 0 2px #a2d1a8; }
.canvas-state { position: absolute; inset: 0; z-index: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 11px; padding: 28px; text-align: center; color: #eaf1ee; font-family: var(--font-family-ui); }.canvas-state strong { font-size: 16px; }.canvas-state span { max-width: 380px; color: #afbbb6; font-size: 13px; line-height: 1.6; }.canvas-state .el-icon { font-size: 34px; color: #a2d1a8; }.error-state .el-icon { color: #e18a74; }.loading-orbit { width: 30px; height: 30px; border: 3px solid rgba(162,209,168,.22); border-top-color: #a2d1a8; border-radius: 50%; animation: spin .8s linear infinite; }.retry-button { margin-top: 7px; border-color: rgba(162,209,168,.45); background: rgba(162,209,168,.1); color: #eaf1ee; }
.canvas-legend { position: absolute; left: 16px; bottom: 14px; display: flex; flex-wrap: wrap; gap: 8px 13px; padding: 8px 11px; border: 1px solid rgba(217,235,225,.12); border-radius: 8px; background: rgba(18,25,22,.76); backdrop-filter: blur(8px); color: #cad7d1; font: 11px var(--font-family-ui); }.legend-item { display: inline-flex; align-items: center; gap: 5px; }.legend-item i,.neighbor-item i,.exploration-hints i { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: #a2d1a8; }
.graph-inspector { position: relative; min-height: 0; padding: 25px 22px; border: 1px solid #2a3732; border-radius: 14px; background: #1c2521; color: #e9f0ec; font-family: var(--font-family-ui); }.inspector-close { position: absolute; right: 14px; top: 14px; width: 30px; height: 30px; color: #a8b7b0; border-radius: 6px; }.inspector-close:hover { background: rgba(255,255,255,.07); color: #fff; }.graph-inspector h2 { max-width: 250px; margin: 8px 0 14px; color: #f1f6f3; font: 700 22px/1.25 var(--font-family-display); }.inspector-kicker { color: #a2d1a8; }.node-state-row { display: flex; flex-wrap: wrap; gap: 7px; }.mastery-chip,.importance-chip { padding: 5px 8px; border-radius: 999px; background: rgba(255,255,255,.07); color: #cad8d1; font-size: 11px; }.mastery-chip { border: 1px solid color-mix(in srgb,var(--chip-color) 55%,transparent); color: var(--chip-color); }.node-connection-count { margin: 22px 0 16px; color: #aebdb6; font-size: 13px; }.inspector-section { padding-top: 16px; border-top: 1px solid rgba(219,236,226,.1); }.section-title { display: flex; justify-content: space-between; align-items: center; color: #eff5f1; font-weight: 700; font-size: 12px; }.section-title small { color: #a2d1a8; }.neighbor-list { display: grid; gap: 5px; margin-top: 10px; }.neighbor-item { display: grid; grid-template-columns: 8px minmax(0,1fr) auto; align-items: center; gap: 8px; width: 100%; min-height: 38px; padding: 0 8px; border: 0; border-radius: 6px; background: transparent; color: #dbe6e0; text-align: left; cursor: pointer; }.neighbor-item:hover { background: rgba(255,255,255,.065); }.neighbor-item span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; }.neighbor-item small { color: #8ea29a; font-size: 10px; }.muted-copy,.inspector-intro { color: #aebdb6; font-size: 13px; line-height: 1.7; }.open-detail-button { width: 100%; margin-top: 22px; border-color: rgba(162,209,168,.4); background: rgba(162,209,168,.1); color: #e9f3ed; }.exploration-hints { display: grid; gap: 10px; margin-top: 25px; padding-top: 17px; border-top: 1px solid rgba(219,236,226,.1); }.exploration-hints span { display: flex; align-items: center; gap: 8px; color: #aebdb6; font-size: 12px; }.exploration-hints span:nth-child(2) i { background: #d8ae6b; }.exploration-hints span:nth-child(3) i { background: #7d8986; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; } @keyframes spin { to { transform: rotate(360deg); } }
@media (max-width:1180px) { .graph-layout.with-inspector { grid-template-columns:minmax(0,1fr) 280px; }.graph-inspector { padding:22px 18px; }.graph-inspector h2 { font-size:20px; } } @media (max-width:960px) { .graph-workspace { height:auto; min-height:calc(100vh - var(--navbar-height)); padding:24px; overflow:visible; }.graph-layout { flex:none; min-height:560px; }.graph-layout.with-inspector { grid-template-columns:1fr; }.graph-inspector { position:fixed; z-index:40; top:calc(var(--navbar-height) + 8px); right:8px; bottom:8px; width:min(360px,calc(100vw - 16px)); min-height:0; overflow:auto; border-radius:14px; box-shadow:-18px 0 44px rgba(7,14,10,.38); }.graph-canvas-card { min-height:560px; }.graph-toolbar { flex-wrap:wrap; }.toolbar-actions { margin-left:0; }.graph-search { width:min(100%,400px); } } @media (max-width:640px) { .graph-workspace { padding:18px 14px 24px; }.workspace-header { align-items:flex-start; flex-direction:column; }.title-group h1 { font-size:28px; }.graph-metrics { border-left:0; }.metric { padding-left:0; margin-left:0; margin-right:22px; }.metric + .metric { padding-left:22px; }.toolbar-actions { width:100%; }.tool-button { flex:1; padding:0 8px; }.tool-button span { display:none; }.graph-canvas-card { min-height:480px; }.canvas-legend { right:12px; left:12px; }.graph-inspector { top:0; right:0; bottom:0; width:100vw; border-radius:0; }.inspector-close { width:44px; height:44px; } } @media (prefers-reduced-motion:reduce) { .loading-orbit { animation-duration:1.6s; }.skip-link,.graph-search,.tool-button,.retry-button,.open-detail-button { transition:none; } }
</style>
