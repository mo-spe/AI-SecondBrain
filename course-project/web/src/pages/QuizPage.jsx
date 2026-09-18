import { useEffect, useState } from 'react';
import { Check, ClipboardCheck, RotateCcw, Sparkles, X } from 'lucide-react';
import { apiRequest } from '../lib/api.js';
import PageLoading from '../components/PageLoading.jsx';
import { cachedRequest } from '../lib/dataCache.js';

const difficultyOptions = [
  { value: 'easy', label: '基础', hint: '抓住核心定义' },
  { value: 'medium', label: '进阶', hint: '换一种方式解释' },
  { value: 'hard', label: '挑战', hint: '辨认概念边界' }
];

const optionLetters = ['A', 'B', 'C', 'D'];

export default function QuizPage() {
  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [difficulty, setDifficulty] = useState('medium');
  const [quiz, setQuiz] = useState(null);
  const [answer, setAnswer] = useState(null);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [loadingItems, setLoadingItems] = useState(true);

  useEffect(() => {
    cachedRequest('knowledge', '/knowledge')
      .then(({ items: nextItems }) => {
        setItems(nextItems);
        if (nextItems[0]) setSelectedId(nextItems[0].id);
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoadingItems(false));
  }, []);

  const generateQuiz = async () => {
    if (!selectedId) return;
    setBusy(true);
    setError('');
    setResult(null);
    setAnswer(null);
    try {
      const { quiz: nextQuiz, provider } = await apiRequest('/quiz/generate', {
        method: 'POST',
        body: JSON.stringify({ knowledgeId: selectedId, difficulty })
      });
      setQuiz({ ...nextQuiz, provider });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  };

  const submitAnswer = async () => {
    if (!quiz || answer === null) return;
    setBusy(true);
    setError('');
    try {
      const { result: nextResult } = await apiRequest('/quiz/grade', {
        method: 'POST',
        body: JSON.stringify({ quizId: quiz.quizId, userAnswer: answer })
      });
      setResult(nextResult);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
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
      {error && <div className="inline-message">{error}</div>}
      {loadingItems ? <PageLoading label="正在准备智能测验…" /> : items.length === 0 ? <div className="list-empty"><ClipboardCheck size={22} /><h2>先准备一点知识</h2><p>创建知识点或在学习概览加载演示数据，然后开始测验。</p></div> : <div className="quiz-layout">
        <section className="quiz-setup">
          <div className="card-kicker"><span>01 / 设置本轮练习</span><span>{items.length} 个知识点</span></div>
          <label className="quiz-field">练习哪个知识点？<select value={selectedId} onChange={(event) => { setSelectedId(event.target.value); resetQuiz(); }}><option value="" disabled>选择知识点</option>{items.map((item) => <option key={item.id} value={item.id}>{item.title}</option>)}</select></label>
          <fieldset className="difficulty-field"><legend>题目难度</legend><div className="difficulty-options">{difficultyOptions.map((option) => <label className={`difficulty-option ${difficulty === option.value ? 'selected' : ''}`} key={option.value}><input type="radio" name="difficulty" value={option.value} checked={difficulty === option.value} onChange={(event) => { setDifficulty(event.target.value); resetQuiz(); }} /><span><strong>{option.label}</strong><small>{option.hint}</small></span></label>)}</div></fieldset>
          <div className="quiz-note"><ClipboardCheck size={18} /><p>{quiz?.provider && quiz.provider !== 'mock' ? '真实 AI Provider 已启用，题目依据知识点生成并记录到 Atlas。' : '未配置真实模型时使用稳定的 Mock 出题器，题目依据知识点生成并记录到 Atlas。'}</p></div>
          <button className="primary-button full-button" type="button" onClick={generateQuiz} disabled={busy || !selectedId}>{busy && !quiz ? '出题中…' : '生成一道题'}<Sparkles size={16} /></button>
        </section>
        <section className="quiz-question-card">
          {!quiz ? <div className="quiz-question-empty"><ClipboardCheck size={24} /><h2>准备好开始了吗？</h2><p>选择知识点和难度，生成一题来验证自己的理解。</p></div> : <>
            <div className="card-kicker"><span>02 / 选择你的答案</span><span>{quiz.difficultyLabel}</span></div>
            <p className="quiz-topic">{selectedKnowledge?.title}</p>
            <h2 className="quiz-question">{quiz.question}</h2>
            <div className="quiz-options" role="radiogroup" aria-label="题目选项">{quiz.options.map((option, index) => <label className={`quiz-option ${answer === index ? 'selected' : ''} ${result ? 'is-locked' : ''}`} key={option}><input type="radio" name="quiz-answer" value={index} checked={answer === index} onChange={() => setAnswer(index)} disabled={Boolean(result)} /><span className="option-letter">{optionLetters[index]}</span><span className="option-copy">{option}</span></label>)}</div>
            {!result ? <button className="primary-button full-button" type="button" onClick={submitAnswer} disabled={busy || answer === null}>{busy ? '判分中…' : '提交答案'}<Check size={16} /></button> : <div className={`quiz-result ${result.isCorrect ? 'correct' : 'incorrect'}`}><div className="result-heading"><span className="result-icon">{result.isCorrect ? <Check size={17} /> : <X size={17} />}</span><div><strong>{result.isCorrect ? '答对了，理解正在变得清晰。' : '这次没选中，再看一眼核心内容。'}</strong><small>本题得分 {result.score} / 100</small></div></div><p><b>正确答案：</b>{result.correctAnswer}</p><p><b>解析：</b>{result.explanation}</p><button className="outline-button full-button" type="button" onClick={resetQuiz}><RotateCcw size={16} /> 再来一题</button></div>}
          </>}
        </section>
      </div>}
    </div>
  );
}
