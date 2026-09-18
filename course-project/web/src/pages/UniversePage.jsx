import { useEffect, useMemo, useRef, useState } from 'react';
import { BookOpen, Globe2, RotateCcw, Sparkles } from 'lucide-react';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import PageLoading from '../components/PageLoading.jsx';
import { cachedRequest } from '../lib/dataCache.js';

const sceneSize = { width: 760, height: 540 };

function getPosition(index, total) {
  if (total === 1) return new THREE.Vector3(0, 0, 0);
  const phi = Math.acos(1 - (2 * (index + 0.5)) / total);
  const theta = Math.PI * (1 + Math.sqrt(5)) * index;
  const radius = Math.min(7, 3.5 + total * 0.45);
  return new THREE.Vector3(
    radius * Math.sin(phi) * Math.cos(theta),
    radius * Math.cos(phi),
    radius * Math.sin(phi) * Math.sin(theta)
  );
}

export default function UniversePage() {
  const canvasHostRef = useRef(null);
  const [graph, setGraph] = useState({ nodes: [], links: [], stats: {} });
  const [selectedId, setSelectedId] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const selected = useMemo(() => graph.nodes.find((node) => node.id === selectedId), [graph.nodes, selectedId]);

  useEffect(() => {
    cachedRequest('knowledge-graph', '/graph/knowledge-map').then(setGraph).catch((requestError) => setError(requestError.message)).finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const host = canvasHostRef.current;
    if (!host || graph.nodes.length === 0) return undefined;

    const scene = new THREE.Scene();
    scene.background = new THREE.Color('#10201d');
    const camera = new THREE.PerspectiveCamera(45, sceneSize.width / sceneSize.height, 0.1, 100);
    camera.position.set(0, 2.5, 18);
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(sceneSize.width, sceneSize.height);
    renderer.domElement.setAttribute('aria-label', 'Three.js 三维知识图谱');
    host.appendChild(renderer.domElement);

    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.enablePan = false;
    controls.minDistance = 8;
    controls.maxDistance = 28;
    controls.autoRotate = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    controls.autoRotateSpeed = 0.45;

    const universe = new THREE.Group();
    scene.add(universe);
    scene.add(new THREE.AmbientLight('#d8eee2', 1.6));
    const starPositions = new Float32Array(420);
    for (let index = 0; index < starPositions.length; index += 3) {
      starPositions[index] = (Math.random() - 0.5) * 42;
      starPositions[index + 1] = (Math.random() - 0.5) * 30;
      starPositions[index + 2] = (Math.random() - 0.5) * 42;
    }
    const starGeometry = new THREE.BufferGeometry();
    starGeometry.setAttribute('position', new THREE.BufferAttribute(starPositions, 3));
    const stars = new THREE.Points(starGeometry, new THREE.PointsMaterial({ color: '#91b4a5', size: 0.045 }));
    scene.add(stars);

    const positions = new Map();
    const meshes = [];
    graph.nodes.forEach((node, index) => {
      const position = getPosition(index, graph.nodes.length);
      positions.set(node.id, position);
      const color = node.score === null ? '#83c5aa' : node.score >= 80 ? '#e7bf83' : '#80b6c4';
      const material = new THREE.MeshBasicMaterial({ color });
      const mesh = new THREE.Mesh(new THREE.SphereGeometry(node.score === null ? 0.58 : 0.58 + (node.score / 100) * 0.28, 24, 16), material);
      mesh.position.copy(position);
      mesh.userData = { ...node, baseColor: color, baseScale: 1 };
      universe.add(mesh);
      meshes.push(mesh);
    });

    graph.links.forEach((link) => {
      const source = positions.get(link.source);
      const target = positions.get(link.target);
      if (!source || !target) return;
      const line = new THREE.Line(new THREE.BufferGeometry().setFromPoints([source, target]), new THREE.LineBasicMaterial({ color: '#76a994', transparent: true, opacity: 0.72 }));
      universe.add(line);
    });

    const raycaster = new THREE.Raycaster();
    const pointer = new THREE.Vector2();
    const pickNode = (event) => {
      const bounds = renderer.domElement.getBoundingClientRect();
      pointer.x = ((event.clientX - bounds.left) / bounds.width) * 2 - 1;
      pointer.y = -((event.clientY - bounds.top) / bounds.height) * 2 + 1;
      raycaster.setFromCamera(pointer, camera);
      return raycaster.intersectObjects(meshes)[0]?.object;
    };
    const handleMove = (event) => {
      const hovered = pickNode(event);
      meshes.forEach((mesh) => { mesh.scale.setScalar(mesh === hovered ? 1.22 : 1); });
      renderer.domElement.style.cursor = hovered ? 'pointer' : 'grab';
    };
    const handleClick = (event) => {
      const clicked = pickNode(event);
      if (clicked) setSelectedId(clicked.userData.id);
    };
    renderer.domElement.addEventListener('pointermove', handleMove);
    renderer.domElement.addEventListener('pointerdown', handleClick);

    let frameId;
    const animate = () => {
      controls.update();
      renderer.render(scene, camera);
      frameId = window.requestAnimationFrame(animate);
    };
    animate();

    const resize = () => {
      const width = Math.max(320, host.clientWidth);
      const height = Math.min(540, Math.max(360, width * 0.67));
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height);
    };
    resize();
    window.addEventListener('resize', resize);

    return () => {
      window.cancelAnimationFrame(frameId);
      window.removeEventListener('resize', resize);
      renderer.domElement.removeEventListener('pointermove', handleMove);
      renderer.domElement.removeEventListener('pointerdown', handleClick);
      controls.dispose();
      universe.traverse((object) => {
        if (object.geometry) object.geometry.dispose();
        if (object.material) object.material.dispose();
      });
      starGeometry.dispose();
      stars.material.dispose();
      renderer.dispose();
      host.removeChild(renderer.domElement);
    };
  }, [graph]);

  return (
    <div className="page-wrap universe-page">
      <header className="page-header"><div><p className="eyebrow">Three.js / 05</p><h1>进入知识宇宙。</h1><p className="page-subtitle">拖拽旋转你的知识空间，节点大小代表最近一次掌握度。</p></div><div className="feynman-mark"><Globe2 size={18} /><span>三维视图 · 课程展示</span></div></header>
      {error && <div className="inline-message">{error}</div>}
      {loading ? <PageLoading label="正在准备知识宇宙…" /> : graph.nodes.length === 0 ? <div className="list-empty"><BookOpen size={22} /><h2>宇宙还没有星体</h2><p>先创建知识点，Three.js 会把它们放进空间。</p></div> : <div className="universe-layout"><section className="universe-card"><div className="universe-toolbar"><span><Sparkles size={14} /> {graph.stats.nodeCount} 个知识节点 · {graph.stats.linkCount} 条连接</span><small>拖拽旋转 · 滚轮缩放 · 点击节点</small></div><div className="universe-canvas" ref={canvasHostRef} /></section><aside className="universe-detail">{selected ? <><span className="detail-label">当前聚焦</span><h2>{selected.label}</h2><p>{selected.excerpt}</p><div className="tag-row">{selected.tags.map((tag) => <span key={tag}>{tag}</span>)}</div>{selected.score !== null && <div className="detail-score"><span>最近掌握度</span><strong>{selected.score}</strong></div>}</> : <div className="graph-detail-empty"><Globe2 size={22} /><p>点击一个知识节点，聚焦查看它的内容。</p></div>}<button className="outline-button universe-reset" type="button" onClick={() => setSelectedId('')}><RotateCcw size={15} /> 清除聚焦</button></aside></div>}
    </div>
  );
}
