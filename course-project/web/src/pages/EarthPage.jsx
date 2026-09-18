import { useEffect, useMemo, useRef, useState } from 'react';
import { BookOpen, Globe, RotateCcw, Sparkles } from 'lucide-react';
import * as Cesium from 'cesium';
import 'cesium/Build/Cesium/Widgets/widgets.css';
import PageLoading from '../components/PageLoading.jsx';
import { cachedRequest } from '../lib/dataCache.js';

function coordinatesFor(index, total) {
  if (total === 1) return { longitude: 12, latitude: 28 };
  return { longitude: -150 + ((index * 67) % 300), latitude: -42 + ((index * 31) % 84) };
}

export default function EarthPage() {
  const globeRef = useRef(null);
  const [graph, setGraph] = useState({ nodes: [], links: [], stats: {} });
  const [selectedId, setSelectedId] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const selected = useMemo(() => graph.nodes.find((node) => node.id === selectedId), [graph.nodes, selectedId]);

  useEffect(() => {
    cachedRequest('knowledge-graph', '/graph/knowledge-map').then(setGraph).catch((requestError) => setError(requestError.message)).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const container = globeRef.current;
    if (!container || graph.nodes.length === 0) return undefined;

    const viewer = new Cesium.Viewer(container, {
      animation: false,
      baseLayerPicker: false,
      baseLayer: false,
      geocoder: false,
      homeButton: false,
      infoBox: false,
      navigationHelpButton: false,
      sceneModePicker: false,
      selectionIndicator: false,
      timeline: false,
      terrainProvider: new Cesium.EllipsoidTerrainProvider()
    });
    viewer.scene.backgroundColor = Cesium.Color.fromCssColorString('#10201d');
    viewer.scene.globe.baseColor = Cesium.Color.fromCssColorString('#183b35');
    viewer.scene.globe.enableLighting = false;
    viewer.scene.skyBox.show = false;

    const positions = new Map();
    graph.nodes.forEach((node, index) => {
      const coordinate = coordinatesFor(index, graph.nodes.length);
      const position = Cesium.Cartesian3.fromDegrees(coordinate.longitude, coordinate.latitude, 180000);
      positions.set(node.id, position);
      const color = node.score === null ? Cesium.Color.fromCssColorString('#83c5aa') : node.score >= 80 ? Cesium.Color.fromCssColorString('#e7bf83') : Cesium.Color.fromCssColorString('#80b6c4');
      viewer.entities.add({
        id: `knowledge-${node.id}`,
        name: node.label,
        position,
        properties: { knowledgeId: node.id },
        point: { color, pixelSize: node.score === null ? 11 : 11 + node.score / 20, outlineColor: Cesium.Color.WHITE.withAlpha(0.8), outlineWidth: 1 },
        label: { text: node.label, font: '12px Manrope, sans-serif', fillColor: Cesium.Color.fromCssColorString('#edf5ef'), pixelOffset: new Cesium.Cartesian2(0, -24), style: Cesium.LabelStyle.FILL_AND_OUTLINE, outlineColor: Cesium.Color.fromCssColorString('#10201d'), outlineWidth: 3, showBackground: true, backgroundColor: Cesium.Color.fromCssColorString('#10201d').withAlpha(0.72), backgroundPadding: new Cesium.Cartesian2(5, 3) }
      });
    });
    graph.links.forEach((link) => {
      const source = positions.get(link.source);
      const target = positions.get(link.target);
      if (!source || !target) return;
      viewer.entities.add({ polyline: { positions: [source, target], width: 2, material: Cesium.Color.fromCssColorString('#76a994').withAlpha(0.7), arcType: Cesium.ArcType.GEODESIC } });
    });

    viewer.selectedEntityChanged.addEventListener((entity) => {
      const id = entity?.properties?.knowledgeId?.getValue?.();
      setSelectedId(id || '');
    });
    viewer.camera.flyTo({ destination: Cesium.Cartesian3.fromDegrees(0, 10, 24000000), duration: 0 });

    return () => viewer.destroy();
  }, [graph]);

  return (
    <div className="page-wrap earth-page">
      <header className="page-header"><div><p className="eyebrow">CesiumJS / 06</p><h1>把知识放上地球。</h1><p className="page-subtitle">这是课程版的地理化展示层，节点位置用于表达空间感，不改变知识关系数据。</p></div><div className="feynman-mark"><Globe size={18} /><span>CesiumJS · 无 Token 演示</span></div></header>
      {error && <div className="inline-message">{error}</div>}
      {loading ? <PageLoading label="正在准备数字地球…" /> : graph.nodes.length === 0 ? <div className="list-empty"><BookOpen size={22} /><h2>地球还没有知识节点</h2><p>先加载演示数据或创建自己的知识点。</p></div> : <div className="earth-layout"><section className="earth-card"><div className="earth-toolbar"><span><Sparkles size={14} /> {graph.stats.nodeCount} 个知识实体 · {graph.stats.linkCount} 条连接</span><small>拖拽旋转 · 滚轮缩放 · 点击实体</small></div><div className="earth-canvas" ref={globeRef} /></section><aside className="earth-detail">{selected ? <><span className="detail-label">当前实体</span><h2>{selected.label}</h2><p>{selected.excerpt}</p><div className="tag-row">{selected.tags.map((tag) => <span key={tag}>{tag}</span>)}</div>{selected.score !== null && <div className="detail-score"><span>最近掌握度</span><strong>{selected.score}</strong></div>}</> : <div className="graph-detail-empty"><Globe size={22} /><p>点击地球上的实体，查看对应知识内容。</p></div>}<button className="outline-button universe-reset" type="button" onClick={() => { setSelectedId(''); }}><RotateCcw size={15} /> 清除聚焦</button></aside></div>}
    </div>
  );
}
