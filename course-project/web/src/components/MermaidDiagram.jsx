import { useEffect, useId, useState } from 'react';
import mermaid from 'mermaid';
import DOMPurify from 'dompurify';

mermaid.initialize({
  startOnLoad: false,
  securityLevel: 'strict',
  theme: 'neutral',
  htmlLabels: false,
  flowchart: { htmlLabels: false },
  suppressErrorRendering: true,
  maxTextSize: 50000,
  maxEdges: 500
});

let renderSequence = 0;

export default function MermaidDiagram({ source }) {
  const instanceId = useId().replace(/:/g, '');
  const [result, setResult] = useState(null);

  useEffect(() => {
    let active = true;
    // 输入预览时稍作合并，避免每次按键都启动流程图布局计算。
    const timer = window.setTimeout(async () => {
      const host = document.createElement('div');
      host.className = 'mermaid-render-host';
      host.setAttribute('aria-hidden', 'true');
      document.body.append(host);
      try {
        // 每次渲染使用独立容器和 ID，防止预览、详情及过期异步任务互相覆盖。
        const { svg } = await mermaid.render(`diagram-${instanceId}-${++renderSequence}`, source, host);
        const safeSvg = DOMPurify.sanitize(svg, { USE_PROFILES: { svg: true, svgFilters: true } });
        if (active) setResult({ source, svg: safeSvg });
      } catch {
        if (active) setResult({ source, error: true });
      } finally {
        host.remove();
      }
    }, 300);
    return () => { active = false; window.clearTimeout(timer); };
  }, [source, instanceId]);

  if (result?.source !== source) return <p className="markdown-render-status" role="status">正在绘制流程图…</p>;
  if (result.error) return <div className="mermaid-error"><p role="alert">流程图暂时无法渲染，请检查 Mermaid 语法。</p><pre><code>{source}</code></pre></div>;

  return <figure className="mermaid-diagram"><div className="mermaid-svg" role="img" aria-label="Mermaid 图表" dangerouslySetInnerHTML={{ __html: result.svg }} /><details><summary>查看图表源码</summary><pre><code>{source}</code></pre></details></figure>;
}
