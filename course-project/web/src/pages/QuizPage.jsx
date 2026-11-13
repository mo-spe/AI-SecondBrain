import { useEffect, useRef, useState } from 'react';
import { Check, ClipboardCheck, RotateCcw, Sparkles, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { apiRequest } from '../lib/api.js';
import PageLoading from '../components/PageLoading.jsx';
import { cachedRequest, invalidateApiCache } from '../lib/dataCache.js';
import MarkdownContent from '../components/MarkdownContent.jsx';
import LearningStatusBadge from '../components/LearningStatusBadge.jsx';

const difficultyOptions = [
  { value: 'easy', label: '基础', hint: '抓住核心定义' },
  { value: 'medium', label: '进阶', hint: '换一种方式解释' },
  { value: 'hard', label: '挑战', hint: '辨认概念边界' }
];

const optionLetters = ['A', 'B', 'C', 'D'];

export default function QuizPage() {
  const [searchParams] = useSearchParams();
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [difficulty, setDifficulty] = useState('medium');
  const [questionType, setQuestionType] = useState('multiple-choice');
  const [quiz, setQuiz] = useState(null);
  const [answer, setAnswer] = useState(null);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [loadingItems, setLoadingItems] = useState(true);
  const busyRef = useRef(false);
  const mountedRef = useRef(true);
  useEffect(() => { mountedRef.current = true; return () => { mountedRef.current = false; }; }, []);

  useEffect(() => {
    cachedRequest('knowledge', '/knowledge')
      .then(({ items: nextItems }) => {
        setItems(nextItems);
        const requestedId = searchParams.get('knowledgeId');
        if (nextItems[0]) setSelectedId(nextItems.some((item) => item.id === requestedId) ? requestedId : nextItems[0].id);
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoadingItems(false));
  }, [searchParams]);

  const generateQuiz = async () => {
    if (!selectedId || busyRef.current) return;
    busyRef.current = true;
    setBusy(true);
    setError('');
    setResult(null);
    setAnswer(null);
    setQuiz(null);
    try {
      const { quiz: nextQuiz, provider } = await apiRequest('/quiz/generate', {
        method: 'POST',
        body: JSON.stringify({ knowledgeId: selectedId, difficulty, questionType })
      });
      if (mountedRef.current) setQuiz({ ...nextQuiz, questionType: nextQuiz.questionType || questionType, provider });
    } catch (requestError) {
      if (mountedRef.current) setError(requestError.message);
    } finally {
      busyRef.current = false;
      if (mountedRef.current) setBusy(false);
    }
  };

  const submitAnswer = async () => {
    if (!quiz || answer === null || busyRef.current || result || (quiz.questionType === 'short-answer' && !String(answer).trim())) return;
    busyRef.current = true;
    setBusy(true);
    setError('');
    try {
      const { result: nextResult, item, saved } = await apiRequest('/quiz/grade', {
        method: 'POST',
        body: JSON.stringify({ quizId: quiz.quizId, userAnswer: answer })
      });
      invalidateApiCache('knowledge', 'knowledge-graph', `knowledge-detail:${quiz.knowledgeId}`, `quiz-attempts:${quiz.knowledgeId}`);
      if (mountedRef.current) {
        if (item) setItems((previous) => previous.map((entry) => entry.id === item.id ? item : entry));
        setResult({ ...nextResult, item, saved });
      }
    } catch (requestError) {
      if (mountedRef.current) setError(requestError.message);
    } finally {
      busyRef.current = false;
      if (mountedRef.current) setBusy(false);
    }
  };

  const resetQuiz = () => {
    setQuiz(null);
    setAnswer(null);
    setResult(null);
    setError('');
  };

  const selectedKnowledge = items.find((item) => item.id === selectedId);

  return (
    <div className="page-wrap">
      <header className="page-header">
        <div><p className="eyebrow">Practice loop / 03</p><h1>检验一下。</h1><p className="page-subtitle">答一道题，看看你是否真的抓住了概念。</p></div>
        <div className="quiz-provider"><Sparkles size={16} /><span>{quiz?.provider === 'mock' ? 'Mock fallback · 可替换' : quiz?.provider || 'Provider ready'}</span></div>
      </header>
      {error && <div className="inline-message" role="alert">{error}</div>}
      {loadingItems ? <PageLoading label="正在准备智能测验…" /> : items.length === 0 ? <div className="list-empty"><ClipboardCheck size={22} /><h2>先准备一点知识</h2><p>创建知识点或在学习概览加载演示数据，然后开始测验。</p></div> : <div className="quiz-layout">
        <section className="quiz-setup">
          <div className="card-kicker"><span>01 / 设置本轮练习</span><span>{items.length} 个知识点</span></div>
          <label className="quiz-field">练习哪个知识点？<select value={selectedId} disabled={busy} onChange={(event) => { setSelectedId(event.target.value); resetQuiz(); }}><option value="" disabled>选择知识点</option>{items.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}</select></label>
          <fieldset className="difficulty-field" disabled={busy}><legend>题目难度</legend><div className="difficulty-options">{difficultyOptions.map((option) => <label className={`difficulty-option ${difficulty === option.value ? 'selected' : ''}`} key={option.value}><input type="radio" name="difficulty" value={option.value} checked={difficulty === option.value} onChange={(event) => { setDifficulty(event.target.value); resetQuiz(); }} /><span><strong>{option.label}</strong><small>{option.hint}</small></span></label>)}</div></fieldset>
          <label className="quiz-field">题目类型<select value={questionType} disabled={busy} onChange={(event) => { setQuestionType(event.target.value); resetQuiz(); }}><option value="multiple-choice">选择题</option><option value="short-answer">解答题</option></select></label>
          <div className="quiz-note"><ClipboardCheck size={18} /><p>{questionType === 'short-answer' ? '解答题使用真实 AI 出题和评分，提交后展示参考答案与反馈。' : quiz?.provider && quiz.provider !== 'mock' ? '真实 AI Provider 已启用，题目依据知识点生成并记录到 Atlas。' : '未配置真实模型时使用稳定的 Mock 出题器，题目依据知识点生成并记录到 Atlas。'}</p></div>
          <button className="primary-button full-button" type="button" onClick={generateQuiz} disabled={busy || !selectedId}>{busy && !quiz ? '出题中…' : '生成一道题'}<Sparkles size={16} /></button>
        </section>
        <section className="quiz-question-card">
          {!quiz ? <div className="quiz-question-empty"><ClipboardCheck size={24} /><h2>准备好开始了吗？</h2><p>选择知识点、难度和题型，生成一题来验证自己的理解。</p></div> : <>
            <div className="card-kicker"><span>{quiz.questionType === 'short-answer' ? '02 / 写下你的解答' : '02 / 选择你的答案'}</span><span>{quiz.difficultyLabel}</span></div>
            <p className="quiz-topic">{selectedKnowledge?.title}</p>
            <div className="quiz-question"><MarkdownContent content={quiz.question} /></div>
            {quiz.questionType === 'short-answer' ? <label className="transcript-label quiz-short-answer">你的解答<textarea rows={8} maxLength={5000} value={answer ?? ''} onChange={(event) => setAnswer(event.target.value)} disabled={busy || Boolean(result)} placeholder="用自己的话解释概念，写出推理过程或举例说明。" /><small>{String(answer ?? '').length} / 5000 字符</small></label> : <div className="quiz-options" role="radiogroup" aria-label="题目选项">{quiz.options.map((option, index) => <label className={`quiz-option ${answer === index ? 'selected' : ''} ${result ? 'is-locked' : ''}`} key={option}><input type="radio" name="quiz-answer" value={index} checked={answer === index} onChange={() => setAnswer(index)} disabled={busy || Boolean(result)} /><span className="option-letter">{optionLetters[index]}</span><span className="option-copy">{option}</span></label>)}</div>}
            {!result ? <button className="primary-button full-button" type="button" onClick={submitAnswer} disabled={busy || answer === null || (quiz.questionType === 'short-answer' && !String(answer).trim())}>{busy ? '判分中…' : '提交答案'}<Check size={16} /></button> : <div className={`quiz-result ${result.isCorrect ? 'correct' : 'incorrect'}`} role="status">
              <div className="result-heading"><span className="result-icon">{result.isCorrect ? <Check size={17} /> : <X size={17} />}</span><div><strong>{quiz.questionType === 'short-answer' ? result.isCorrect ? '解答达标，继续巩固理解。' : '解答还有不足，看看反馈再练习。' : result.isCorrect ? '答对了，理解正在变得清晰。' : '这次没选中，再看一眼核心内容。'}</strong><small>本题得分 {result.score} / 100</small></div></div>
              {result.saved && result.item && <><LearningStatusBadge knowledge={result.item} /><p>测验记录和知识点学习状态已保存。</p></>}
              {result.feedback && <><h3>AI 评价</h3><MarkdownContent content={result.feedback} /></>}
              <h3>{quiz.questionType === 'short-answer' ? '参考答案' : '正确答案'}</h3><MarkdownContent content={result.correctAnswer} />
              {result.keyPoints?.length > 0 && <><h3>评分要点</h3><ul>{result.keyPoints.map((point, index) => <li key={index}>{point}</li>)}</ul></>}
              <h3>解析</h3><MarkdownContent content={result.explanation} />
              <button className="outline-button full-button" type="button" onClick={resetQuiz}><RotateCcw size={16} /> 再来一题</button>
            </div>}
          </>}
        </section>
      </div>}
    </div>
  );
}
