import { useEffect, useRef, useState } from 'react';
import { Save } from 'lucide-react';
import { apiRequest } from '../lib/api.js';
import { invalidateApiCache } from '../lib/dataCache.js';
import KnowledgeContentInput from './KnowledgeContentInput.jsx';

export default function KnowledgeEditor({ knowledge, onSaved, onCancel }) {
  const dialogRef = useRef(null);
  const savingRef = useRef(false);
  const initialForm = { title: knowledge.title, content: knowledge.content, tags: knowledge.tags.join(', ') };
  const [form, setForm] = useState(initialForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const dirty = Object.keys(form).some((key) => form[key] !== initialForm[key]);

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousOverflow = document.body.style.overflow;
    dialog.showModal();
    document.body.style.overflow = 'hidden';
    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  useEffect(() => {
    if (!dirty && !saving) return;
    const warnBeforeUnload = (event) => { event.preventDefault(); event.returnValue = ''; };
    window.addEventListener('beforeunload', warnBeforeUnload);
    return () => window.removeEventListener('beforeunload', warnBeforeUnload);
  }, [dirty, saving]);

  const cancel = () => {
    if (savingRef.current) return;
    if (!dirty || window.confirm('有尚未保存的修改，确认放弃吗？')) onCancel();
  };

  const save = async (event) => {
    event.preventDefault();
    if (savingRef.current) return;
    if (!form.title.trim() || !form.content.trim()) {
      setError('标题和内容不能为空或只有空格。');
      return;
    }
    const tags = [...new Set(form.tags.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean))];
    if (tags.length > 8) {
      setError('最多添加 8 个标签，请减少标签后再保存。');
      return;
    }
    savingRef.current = true;
    setSaving(true);
    setError('');
    try {
      const { item } = await apiRequest(`/knowledge/${knowledge.id}`, {
        method: 'PUT',
        body: JSON.stringify({ title: form.title.trim(), content: form.content.trim(), tags })
      });
      // 清理所有消费知识内容的视图，避免保存后切换模块仍读到旧标题或正文。
      invalidateApiCache('knowledge', 'knowledge-graph', `knowledge-detail:${knowledge.id}`);
      onSaved(item);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      savingRef.current = false;
      setSaving(false);
    }
  };

  return (
    <dialog ref={dialogRef} className="knowledge-editor-dialog" aria-labelledby="knowledge-editor-title" onCancel={(event) => { event.preventDefault(); cancel(); }}>
      <form className="composer knowledge-editor" onSubmit={save} aria-busy={saving}>
        <div className="composer-heading"><h2 id="knowledge-editor-title">编辑知识点</h2><button type="button" className="ghost-button" onClick={cancel} disabled={saving}>取消</button></div>
        <p className="editor-hint">补充新的理解，复述和测验历史会保留。</p>
        <label>标题<input autoFocus required disabled={saving} value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} /></label>
        <KnowledgeContentInput disabled={saving} rows={9} value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} />
        <label>标签<input disabled={saving} value={form.tags} onChange={(event) => setForm({ ...form, tags: event.target.value })} aria-describedby="editor-tags-hint" /></label>
        <small id="editor-tags-hint" className="editor-hint">用中文或英文逗号分隔，最多 8 个标签。</small>
        {error && <div className="form-error" role="alert">{error}</div>}
        <div className="editor-footer"><button className="outline-button" type="button" onClick={cancel} disabled={saving}>取消</button><button className="primary-button" type="submit" disabled={saving || !dirty}><Save size={17} />{saving ? '保存中…' : '保存修改'}</button></div>
      </form>
    </dialog>
  );
}
