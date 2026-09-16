import { useState } from 'react';
import { ArrowRight, BrainCircuit, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function LoginPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      await signIn(form, mode);
      navigate('/');
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  const update = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }));

  return (
    <div className="auth-page">
      <section className="auth-story">
        <div className="story-topline"><BrainCircuit size={20} /> 费曼学习平台 / 课程版</div>
        <div className="story-copy">
          <p className="eyebrow">把“我会了”变成真的会</p>
          <h1>先讲给自己听，<br /><span>再把知识留下来。</span></h1>
          <p className="story-description">用知识点、复述、反馈和检索，把零散输入变成可以被调用的个人知识系统。</p>
        </div>
        <div className="story-points">
          {['记录重要知识点', '用自己的话复述理解', '让 AI 帮你发现盲区'].map((point) => <div key={point}><CheckCircle2 size={17} />{point}</div>)}
        </div>
        <div className="story-footer">课程项目独立版 · React / Express / MongoDB</div>
      </section>
      <section className="auth-panel">
        <div className="auth-card">
          <div className="auth-tabs">
            <button type="button" className={mode === 'login' ? 'selected' : ''} onClick={() => setMode('login')}>登录</button>
            <button type="button" className={mode === 'register' ? 'selected' : ''} onClick={() => setMode('register')}>创建账户</button>
          </div>
          <div className="auth-heading"><p className="eyebrow">你的学习空间</p><h2>{mode === 'login' ? '欢迎回来。' : '从今天开始积累。'}</h2><p>{mode === 'login' ? '继续上次停下的地方。' : '先建立一个属于你的知识库。'}</p></div>
          <form onSubmit={submit} className="auth-form">
            {mode === 'register' && <label>用户名<input value={form.username} onChange={update('username')} placeholder="例如：Lin" required minLength={2} /></label>}
            <label>邮箱<input type="email" value={form.email} onChange={update('email')} placeholder="you@example.com" required /></label>
            <label>密码<input type="password" value={form.password} onChange={update('password')} placeholder="至少 6 位字符" required minLength={6} /></label>
            {error && <div className="form-error">{error}</div>}
            <button className="primary-button" disabled={submitting} type="submit">{submitting ? '正在进入…' : mode === 'login' ? '进入学习空间' : '创建我的空间'}<ArrowRight size={17} /></button>
          </form>
        </div>
      </section>
    </div>
  );
}

