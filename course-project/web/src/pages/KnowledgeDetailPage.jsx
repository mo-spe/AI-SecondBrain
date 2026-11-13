import { useEffect, useState } from 'react';
import { ArrowLeft, BrainCircuit, CheckCircle2, ClipboardCheck, Clock3, Mic2, Pencil, Tag, XCircle } from 'lucide-react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import LearningStatusBadge from '../components/LearningStatusBadge.jsx';
import KnowledgeEditor from '../components/KnowledgeEditor.jsx';
import MarkdownContent from '../components/MarkdownContent.jsx';
import PageLoading from '../components/PageLoading.jsx';
import { cachedRequest } from '../lib/dataCache.js';

const formatDate = (value) => (value ? new Date(value).toLocaleString('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }) : '还没有记录');

function RecordEmpty({ children }) {
  return <div className="detail-record-empty">{children}</div>;
}

export default function KnowledgeDetailPage() {
  const { id } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const [savedMessage, setSavedMessage] = useState('');
  const editing = searchParams.get('edit') === 'true';
  const setEditing = (value) => {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      if (value) next.set('edit', 'true');
      else next.delete('edit');
      return next;
    }, { replace: true });
  };
  const [knowledge, setKnowledge] = useState(null);
  const [feynmanAttempts, setFeynmanAttempts] = useState([]);
  const [quizAttempts, setQuizAttempts] = useState([]);
  const [feynmanHistory, setFeynmanHistory] = useState({ loading: true, error: '' });
  const [quizHistory, setQuizHistory] = useState({ loading: true, error: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    setSavedMessage('');
    setKnowledge(null);
    setFeynmanAttempts([]);
    setQuizAttempts([]);
    setFeynmanHistory({ loading: true, error: '' });
    setQuizHistory({ loading: true, error: '' });
    // 正文可读性不依赖历史查询，避免最慢的一条云端请求阻塞整个详情页。
    cachedRequest(`knowledge-detail:${id}`, `/knowledge/${id}`)
      .then(({ item }) => { if (active) setKnowledge(item); })
      .catch((requestError) => { if (active) setError(requestError.message || '知识点加载失败，请稍后重试。'); })
      .finally(() => { if (active) setLoading(false); });
    const loadHistory = (key, path, setAttempts, setHistory) => {
      cachedRequest(key, path).then(({ attempts }) => {
        if (!active) return;
        setAttempts(attempts);
        setHistory({ loading: false, error: '' });
      }).catch(() => {
        if (active) setHistory({ loading: false, error: '历史记录加载失败，请刷新页面重试。' });
      });
    };
    loadHistory(`feynman-attempts:${id}`, `/feynman/attempts/${id}`, setFeynmanAttempts, setFeynmanHistory);
    loadHistory(`quiz-attempts:${id}`, `/quiz/attempts/${id}`, setQuizAttempts, setQuizHistory);
    return () => { active = false; };
  }, [id]);

  if (loading) return <div className="page-wrap"><PageLoading label="正在打开知识详情…" /></div>;

  if (error || !knowledge) {
    return <div className="page-wrap"><Link className="back-link" to="/knowledge"><ArrowLeft size={16} /> 返回知识库</Link><div className="detail-error"><XCircle size={22} /><h1>这个知识点暂时打不开</h1><p>{error || '知识点不存在，或者已经被删除。'}</p></div></div>;
  }

  return (
    <div className="page-wrap knowledge-detail-page">
      <Link className="back-link" to="/knowledge"><ArrowLeft size={16} /> 返回知识库</Link>
      <header className="detail-header">
        <div className="detail-heading-copy">
          <p className="eyebrow">Knowledge note / detail</p>
          <h1>{knowledge.title}</h1><LearningStatusBadge knowledge={knowledge} />
          <div className="detail-tags">{knowledge.tags.length ? knowledge.tags.map((tag) => <span key={tag}><Tag size={12} /> {tag}</span>) : <span>未添加标签</span>}</div>
        </div>
        <div className="detail-actions"><button className="outline-button" type="button" onClick={() => { setSavedMessage(''); setEditing(true); }}><Pencil size={16} /> 编辑</button><Link className="outline-button" to={`/feynman?knowledgeId=${knowledge.id}`}><Mic2 size={16} /> 开始复述</Link><Link className="primary-button" to={`/quiz?knowledgeId=${knowledge.id}`}><ClipboardCheck size={16} /> 生成测验</Link></div>
      </header>

      {savedMessage && <div className="inline-message success" role="status">{savedMessage}</div>}
      {editing && <KnowledgeEditor key={knowledge.id} knowledge={knowledge} onCancel={() => setEditing(false)} onSaved={(item) => { setKnowledge(item); setEditing(false); setSavedMessage('知识点已更新。'); }} />}

      <div className="knowledge-detail-grid">
        <main className="detail-main-column">
          <article className="detail-content-panel">
            <div className="detail-panel-heading"><span>我的理解</span><BrainCircuit size={19} /></div>
            <MarkdownContent content={knowledge.content} />
            <div className="detail-updated"><Clock3 size={14} /> 最近编辑于 {formatDate(knowledge.updatedAt)}</div>
          </article>

          <section className="detail-record-panel">
            <div className="detail-panel-heading"><div><span>费曼复述</span><small>{feynmanHistory.loading ? '加载中…' : feynmanHistory.error ? '读取失败' : `${feynmanAttempts.length} 次记录`}</small></div><Mic2 size={19} /></div>
            {feynmanHistory.loading ? <PageLoading label="正在读取复述历史…" /> : feynmanHistory.error ? <RecordEmpty><p role="alert">{feynmanHistory.error}</p></RecordEmpty> : feynmanAttempts.length === 0 ? <RecordEmpty><p>还没有复述记录。</p><Link to={`/feynman?knowledgeId=${knowledge.id}`}>现在讲给自己听</Link></RecordEmpty> : <div className="detail-record-list">{feynmanAttempts.map((attempt) => <article className="detail-record-item" key={attempt.id}><div className="record-item-top"><div><strong>掌握度 {attempt.score}<small>/ 100</small></strong><time>{formatDate(attempt.createdAt)}</time></div><CheckCircle2 size={18} /></div><p>{attempt.evaluation}</p><details className="attempt-texts"><summary>查看复述原文与润色结果</summary><h3>复述原文</h3><p>{attempt.transcript}</p><h3>润色后的复述</h3><p>{attempt.polishedText}</p></details><div className="record-split"><div><span>做得好的地方</span>{attempt.strengths.slice(0, 2).map((item) => <p key={item}>+ {item}</p>)}</div><div><span>下一次试试</span>{attempt.weaknesses.slice(0, 2).map((item) => <p key={item}>→ {item}</p>)}</div></div></article>)}</div>}
          </section>
        </main>

        <aside className="detail-side-column">
          <section className="detail-score-panel"><span className="detail-label">最近掌握度</span><div className="detail-score-large">{typeof knowledge.reviewScore === 'number' ? knowledge.reviewScore : '未评估'}<small>{typeof knowledge.reviewScore === 'number' ? '/ 100' : ''}</small></div><p>{knowledge.lastReviewedAt ? `最近复习于 ${formatDate(knowledge.lastReviewedAt)}` : '完成一次费曼复述或智能测验后，这里会出现掌握度。'}</p></section>
          <section className="detail-record-panel compact-record-panel"><div className="detail-panel-heading"><div><span>测验历史</span><small>{quizHistory.loading ? '加载中…' : quizHistory.error ? '读取失败' : `${quizAttempts.length} 次记录`}</small></div><ClipboardCheck size={19} /></div>{quizHistory.loading ? <PageLoading label="正在读取测验历史…" /> : quizHistory.error ? <RecordEmpty><p role="alert">{quizHistory.error}</p></RecordEmpty> : quizAttempts.length === 0 ? <RecordEmpty><p>还没有测验记录。</p><Link to={`/quiz?knowledgeId=${knowledge.id}`}>开始第一题</Link></RecordEmpty> : <div className="quiz-history-list">{quizAttempts.slice(0, 6).map((attempt) => <article className={`quiz-history-item ${attempt.isCorrect ? 'correct' : 'incorrect'}`} key={attempt.id}><div><strong>{attempt.isCorrect ? '答对' : '待复习'}</strong><time>{formatDate(attempt.createdAt)}</time></div>{attempt.isCorrect ? <CheckCircle2 size={17} /> : <XCircle size={17} />}</article>)}</div>}</section>
        </aside>
      </div>
    </div>
  );
}
