import { useState } from 'react';
import { ArrowUpRight, BookOpen, MessageCircle, Search, Sparkles } from 'lucide-react';
import { apiRequest } from '../lib/api.js';

const suggestions = ['什么是我最近记录的核心概念？', '把知识库里的内容联系起来', '哪些知识点还需要补充例子？'];

export default function RagPage() {
  const [query, setQuery] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const ask = async (event) => {
    event?.preventDefault();
    if (query.trim().length < 2) return;
    setLoading(true);
    setError('');
    try {
      setResult(await apiRequest('/rag/ask', { method: 'POST', body: JSON.stringify({ query }) }));
    } catch (requestError) {
      setError(requestError.message);
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-wrap rag-page">
      <header className="page-header">
        <div><p className="eyebrow">Retrieval / 03</p><h1>问问你的知识库。</h1><p className="page-subtitle">回答只来自你已经留下的内容，并把依据一起展示出来。</p></div>
        <div className="feynman-mark"><MessageCircle size={18} /><span>课程版独立检索</span></div>
      </header>
      <section className="rag-hero">
        <div className="rag-hero-copy"><span className="rag-pulse"><Sparkles size={15} /> grounded answer</span><h2>从记忆里找线索，<br /><em>不是凭空猜答案。</em></h2><p>课程版先从自己的知识库检索上下文，再交给可替换的 AI Provider 组织回答；每次都会返回相似知识片段，方便核对来源。</p></div>
        <form className="rag-search" onSubmit={ask}><div><Search size={18} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="例如：什么是向量数据库？" aria-label="向知识库提问" /></div><button className="primary-button" disabled={loading || query.trim().length < 2} type="submit">{loading ? '检索中…' : '开始检索'}<ArrowUpRight size={16} /></button></form>
        <div className="suggestion-row">{suggestions.map((suggestion) => <button key={suggestion} type="button" onClick={() => setQuery(suggestion)}>{suggestion}</button>)}</div>
      </section>
      {error && <div className="inline-message">{error}</div>}
      {!result ? <div className="rag-empty"><BookOpen size={22} /><h2>你的回答会出现在这里</h2><p>先提出一个和知识库有关的问题。</p></div> : <section className="rag-result"><div className="answer-panel"><div className="result-kicker"><span>回答 / {result.provider}</span><MessageCircle size={17} /></div><h2>{result.answer}</h2><div className="answer-query">你问：{result.query}</div></div><div className="source-panel"><div className="result-kicker"><span>检索依据 / {result.sources.length}</span><BookOpen size={17} /></div>{result.sources.length === 0 ? <p className="no-source">没有达到相关性阈值的知识片段。</p> : result.sources.map((source) => <article className="source-row" key={`${source.knowledgeId}-${source.excerpt}`}><div><strong>{source.title}</strong><p>{source.excerpt}</p></div><span>{Math.round(source.score * 100)}%</span></article>)}</div></section>}
    </div>
  );
}
