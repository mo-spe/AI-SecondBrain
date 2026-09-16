import { useEffect, useMemo, useState } from 'react';
import { BookOpen, Minus, Network, Plus, RotateCcw } from 'lucide-react';
import { apiRequest } from '../lib/api.js';

const GRAPH_WIDTH = 680;
const GRAPH_HEIGHT = 470;

function layoutNodes(nodes) {
  if (nodes.length === 1) return [{ ...nodes[0], x: GRAPH_WIDTH / 2, y: GRAPH_HEIGHT / 2 }];
  const radius = Math.min(165, 55 + nodes.length * 15);
  return nodes.map((node, index) => {
    const angle = (Math.PI * 2 * index) / nodes.length - Math.PI / 2;
    return { ...node, x: GRAPH_WIDTH / 2 + Math.cos(angle) * radius, y: GRAPH_HEIGHT / 2 + Math.sin(angle) * radius };
  });
}

export default function GraphPage() {
  const [graph, setGraph] = useState({ nodes: [], links: [], stats: {} });
  const [selectedId, setSelectedId] = useState('');
  const [zoom, setZoom] = useState(1);
  const [error, setError] = useState('');

  useEffect(() => {
    apiRequest('/graph/knowledge-map').then(setGraph).catch((requestError) => setError(requestError.message));
  }, []);

  const nodes = useMemo(() => layoutNodes(graph.nodes), [graph.nodes]);
  const nodeMap = useMemo(() => new Map(nodes.map((node) => [node.id, node])), [nodes]);
  const selected = nodeMap.get(selectedId);

  return (
    <div className="page-wrap graph-page">
      <header className="page-header"><div><p className="eyebrow">Knowledge map / 04</p><h1>看见知识之间。</h1><p className="page-subtitle">共享标签会成为一条线，点击节点查看它在知识库里的位置。</p></div><div className="feynman-mark"><Network size={18} /><span>关系由你的标签生成</span></div></header>
      {error && <div className="inline-message">{error}</div>}
      {graph.nodes.length === 0 ? <div className="list-empty"><BookOpen size={22} /><h2>图谱还没有节点</h2><p>先创建两个带有相同标签的知识点。</p></div> : <div className="graph-layout">
        <section className="graph-canvas-card"><div className="graph-toolbar"><div><span className="graph-count">{graph.stats.nodeCount} 节点 · {graph.stats.linkCount} 条关系</span><small>{graph.stats.tagCount} 个标签参与连接</small></div><div className="zoom-controls"><button type="button" onClick={() => setZoom((value) => Math.max(.7, value - .1))} aria-label="缩小"><Minus size={15} /></button><span>{Math.round(zoom * 100)}%</span><button type="button" onClick={() => setZoom((value) => Math.min(1.5, value + .1))} aria-label="放大"><Plus size={15} /></button><button type="button" onClick={() => setZoom(1)} aria-label="重置缩放"><RotateCcw size={15} /></button></div></div><div className="graph-viewport"><svg viewBox={`0 0 ${GRAPH_WIDTH} ${GRAPH_HEIGHT}`} role="img" aria-label="知识点关系图谱"><g transform={`translate(${GRAPH_WIDTH * (1 - zoom) / 2} ${GRAPH_HEIGHT * (1 - zoom) / 2}) scale(${zoom})`}>{graph.links.map((link) => { const source = nodeMap.get(link.source); const target = nodeMap.get(link.target); return source && target ? <line key={link.id} className="graph-link" x1={source.x} y1={source.y} x2={target.x} y2={target.y} strokeWidth={Math.min(4, 1 + link.weight)} /> : null; })}{nodes.map((node) => <g className={`graph-node ${selectedId === node.id ? 'selected' : ''}`} key={node.id} transform={`translate(${node.x} ${node.y})`} onClick={() => setSelectedId(node.id)} tabIndex="0" role="button" aria-label={`查看 ${node.label}`} onKeyDown={(event) => event.key === 'Enter' && setSelectedId(node.id)}><circle r={selectedId === node.id ? 27 : 22} /><text y="42" textAnchor="middle">{node.label.length > 13 ? `${node.label.slice(0, 13)}…` : node.label}</text><title>{node.label}</title></g>)}</g></svg></div></section>
        <aside className="graph-detail-card">{selected ? <><span className="detail-label">选中的知识点</span><h2>{selected.label}</h2><p>{selected.excerpt}</p><div className="tag-row">{selected.tags.map((tag) => <span key={tag}>{tag}</span>)}</div>{selected.score !== null && <div className="detail-score"><span>最近掌握度</span><strong>{selected.score}</strong></div>}</> : <div className="graph-detail-empty"><Network size={22} /><p>点击一个节点，查看它的内容和标签。</p></div>}<div className="graph-legend"><span><i className="legend-dot" />知识点</span><span><i className="legend-line" />共享标签</span></div></aside>
      </div>}
    </div>
  );
}
