import { useEffect, useState } from 'react';
import { ArrowUpRight, BookOpen, Clock3, Database, Mic2, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import PageLoading from '../components/PageLoading.jsx';
import { apiRequest } from '../lib/api.js';
import { cachedRequest, invalidateApiCache } from '../lib/dataCache.js';

export default function DashboardPage() {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [loadingItems, setLoadingItems] = useState(true);
  const [seedMessage, setSeedMessage] = useState('');
  const [seeding, setSeeding] = useState(false);

  const loadItems = async () => {
    setLoadingItems(true);
    try {
      const { items: nextItems } = await cachedRequest('knowledge', '/knowledge');
      setItems(nextItems);
    } catch (_error) {
      setItems([]);
    } finally {
      setLoadingItems(false);
    }
  };
  useEffect(() => { loadItems(); }, []);

  const seedDemo = async () => {
    setSeeding(true);
    setSeedMessage('');
    try {
      const result = await apiRequest('/demo/seed', { method: 'POST' });
      setSeedMessage(`已加入 ${result.createdCount} 条演示知识${result.skippedCount ? `，跳过 ${result.skippedCount} 条已有内容` : ''}。`);
      invalidateApiCache('knowledge', 'knowledge-graph');
      await loadItems();
    } catch (requestError) {
      setSeedMessage(requestError.message);
    } finally {
      setSeeding(false);
    }
  };

  const reviewedItems = items.filter((item) => typeof item.reviewScore === 'number');
  const averageScore = reviewedItems.length ? Math.round(reviewedItems.reduce((sum, item) => sum + item.reviewScore, 0) / reviewedItems.length) : '—';
  const cards = [
    { label: '知识点总数', value: items.length || '—', hint: items.length ? '继续让知识互相连接' : '从第一条开始积累', icon: BookOpen, tone: 'teal' },
    { label: '本周复述', value: reviewedItems.length, hint: reviewedItems.length ? '已留下复述反馈' : '完成一次复述吧', icon: Mic2, tone: 'amber' },
    { label: '平均掌握度', value: averageScore, hint: reviewedItems.length ? '来自最近保存的复述' : '完成 AI 评价后显示', icon: Sparkles, tone: 'ink' }
  ];
  return (
    <div className="page-wrap">
      <header className="page-header dashboard-header"><div><p className="eyebrow">{new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' })}</p><h1>早上好，{user?.username}。</h1><p className="page-subtitle">今天也用自己的话，确认一次真正的理解。</p></div><div className="dashboard-actions"><button className="outline-button" type="button" onClick={seedDemo} disabled={seeding}><Database size={16} />{seeding ? '加载中…' : '加载演示数据'}</button><Link className="outline-button" to="/knowledge">写下新知识 <ArrowUpRight size={16} /></Link></div></header>
      {seedMessage && <div className="inline-message success">{seedMessage}</div>}
      {loadingItems ? <PageLoading label="正在读取知识概览…" /> : <><section className="stats-grid">{cards.map(({ label, value, hint, icon: Icon, tone }) => <div className={`stat-card ${tone}`} key={label}><div className="stat-icon"><Icon size={18} /></div><p>{label}</p><strong>{value}</strong><span>{hint}</span></div>)}</section>
        <section className="dashboard-lower"><div className="empty-panel"><div className="empty-icon"><BookOpen size={22} /></div><p className="eyebrow">{items.length ? '知识库正在生长' : '知识库还很安静'}</p><h2>{items.length ? '让一个概念连接到另一个概念。' : '从一个你刚学会的概念开始。'}</h2><p>{items.length ? '现在可以去费曼复述、知识问答或知识图谱，看看这些内容如何被调用。' : '不要追求一次写完。先留下核心定义，之后可以通过复述和 AI 评价不断补齐。'}</p>{items.length ? <Link className="primary-button compact" to="/universe">进入知识宇宙 <ArrowUpRight size={16} /></Link> : <Link className="primary-button compact" to="/knowledge">创建第一个知识点 <ArrowUpRight size={16} /></Link>}</div><div className="next-panel"><div className="panel-heading"><div><p className="eyebrow">学习循环</p><h2>接下来会发生什么</h2></div><Clock3 size={19} /></div><div className="learning-step"><span>01</span><div><strong>记录</strong><p>把知识写进自己的语言里。</p></div></div><div className="learning-step"><span>02</span><div><strong>复述</strong><p>不用看笔记，讲给自己听。</p></div></div><div className="learning-step"><span>03</span><div><strong>校准</strong><p>用 AI 找到理解中的空白。</p></div></div></div></section></>}
    </div>
  );
}
