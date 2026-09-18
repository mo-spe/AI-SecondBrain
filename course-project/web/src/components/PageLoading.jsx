export default function PageLoading({ label = '正在整理你的学习空间…' }) {
  return (
    <div className="page-loading" role="status" aria-live="polite">
      <div className="loading-mark" aria-hidden="true"><span /><span /><span /></div>
      <div><strong>{label}</strong><p>数据回来后会自动继续。</p></div>
    </div>
  );
}
