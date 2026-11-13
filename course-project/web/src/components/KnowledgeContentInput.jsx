import { lazy, Suspense, useDeferredValue, useId, useState } from 'react';

const MarkdownContent = lazy(() => import('./MarkdownContent.jsx'));

export default function KnowledgeContentInput({ value, onChange, disabled = false, rows = 8 }) {
  const previewId = useId();
  const [preview, setPreview] = useState(false);
  const deferredContent = useDeferredValue(value);
  return (
    <div className="knowledge-content-input">
      <label>用自己的话解释<textarea required disabled={disabled} rows={rows} value={value} onChange={onChange} placeholder="支持 Markdown、$行内公式$、$$独立公式$$ 和 mermaid 代码块。" /></label>
      <div className="content-preview-toolbar"><span>支持 Markdown · LaTeX · Mermaid</span><button className="outline-button" type="button" aria-expanded={preview} aria-controls={previewId} onClick={() => setPreview((current) => !current)}>{preview ? '收起预览' : '预览效果'}</button></div>
      {preview && <section className="content-preview-panel" id={previewId} aria-label="正文预览"><Suspense fallback={<p role="status">正在加载预览…</p>}>{deferredContent.trim() ? <MarkdownContent content={deferredContent} /> : <p className="editor-hint">输入内容后，这里会显示排版效果。</p>}</Suspense></section>}
    </div>
  );
}
