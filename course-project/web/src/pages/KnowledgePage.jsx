import { useEffect, useState } from 'react';
import { BookOpen, MoreHorizontal, Plus, Search, Trash2 } from 'lucide-react';
import { apiRequest } from '../lib/api.js';

const initialForm = { title: '', content: '', tags: '' };

export default function KnowledgePage() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [query, setQuery] = useState('');
  const [showComposer, setShowComposer] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const loadItems = () => apiRequest('/knowledge').then(({ items: nextItems }) => setItems(nextItems)).catch((requestError) => setError(requestError.message));
  useEffect(() => { loadItems(); }, []);

  const createItem = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    try {
      await apiRequest('/knowledge', { method: 'POST', body: JSON.stringify({ ...form, tags: form.tags.split(',') }) });
      setForm(initialForm);
      setShowComposer(false);
      await loadItems();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSaving(false);
    }
  };

  const removeItem = async (id) => {
    if (!window.confirm('确认删除这个知识点吗？')) return;
    await apiRequest(`/knowledge/${id}`, { method: 'DELETE' });
    setItems((current) => current.filter((item) => item.id !== id));
  };

  const visibleItems = items.filter((item) => `${item.title} ${item.content} ${item.tags.join(' ')}`.toLowerCase().includes(query.toLowerCase()));

  return (
    <div className="page-wrap"><header className="page-header"><div><p className="eyebrow">Knowledge base / 01</p><h1>我的知识。</h1><p className="page-subtitle">记录那些值得被再次讲清楚的东西。</p></div><button className="primary-button compact" onClick={() => setShowComposer(true)} type="button"><Plus size={17} /> 新建知识点</button></header>
      <div className="toolbar"><div className="search-box"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索知识点…" /></div><span className="result-count">{visibleItems.length} 个知识点</span></div>
      {error && <div className="inline-error">{error}</div>}
      {showComposer && <form className="composer" onSubmit={createItem}><div className="composer-heading"><div><p className="eyebrow">New note</p><h2>写下一个概念</h2></div><button type="button" className="ghost-button" onClick={() => setShowComposer(false)}>取消</button></div><label>标题<input required value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} placeholder="例如：什么是向量数据库？" /></label><label>用自己的话解释<textarea required value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} placeholder="先写下你现在的理解，不用追求完美。" rows={6} /></label><label>标签（用逗号分隔）<input value={form.tags} onChange={(event) => setForm({ ...form, tags: event.target.value })} placeholder="AI, RAG, 数据库" /></label><button className="primary-button" disabled={saving} type="submit">{saving ? '保存中…' : '保存知识点'}<Plus size={17} /></button></form>}
      {visibleItems.length === 0 && !showComposer ? <div className="list-empty"><BookOpen size={22} /><h2>{query ? '没有匹配的知识点' : '还没有知识点'}</h2><p>{query ? '换一个关键词试试。' : '把今天刚学会的一件事写下来。'}</p></div> : <div className="knowledge-list">{visibleItems.map((item) => <article className="knowledge-row" key={item.id}><div className="knowledge-symbol"><BookOpen size={18} /></div><div className="knowledge-copy"><h2>{item.title}</h2><p>{item.content}</p><div className="tag-row">{item.tags.map((tag) => <span key={tag}>{tag}</span>)}</div></div><div className="knowledge-meta"><time>{new Date(item.updatedAt).toLocaleDateString('zh-CN')}</time><button className="icon-button" type="button" onClick={() => removeItem(item.id)} aria-label={`删除 ${item.title}`}><Trash2 size={16} /></button><MoreHorizontal size={17} /></div></article>)}</div>}
    </div>
  );
}

