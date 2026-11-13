import { Children, isValidElement, lazy, Suspense } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import 'katex/dist/katex.min.css';
import './markdown-content.css';

const MermaidDiagram = lazy(() => import('./MermaidDiagram.jsx'));
const remarkPlugins = [remarkGfm, remarkMath];
const rehypePlugins = [[rehypeKatex, { trust: false, strict: 'warn', throwOnError: false }]];

function MarkdownPre({ children }) {
  const code = Children.toArray(children)[0];
  if (isValidElement(code) && /(?:^|\s)language-mermaid(?:\s|$)/i.test(code.props.className || '')) {
    return <Suspense fallback={<p role="status">正在加载流程图…</p>}><MermaidDiagram source={String(code.props.children).replace(/\n$/, '')} /></Suspense>;
  }
  return <pre>{children}</pre>;
}

const components = {
  pre: MarkdownPre,
  table: ({ children }) => <div className="markdown-table-scroll" tabIndex={0} role="region" aria-label="表格，可横向滚动"><table>{children}</table></div>,
  a: ({ href, children }) => <a href={href} target="_blank" rel="noopener noreferrer">{children}</a>,
  img: ({ src, alt }) => <img src={src} alt={alt || ''} loading="lazy" />
};

export default function MarkdownContent({ content }) {
  return (
    <div className="markdown-content">
      {/* 不启用原始 HTML，避免知识正文中的脚本或事件属性进入页面。 */}
      <ReactMarkdown skipHtml remarkPlugins={remarkPlugins} rehypePlugins={rehypePlugins} components={components}>{content || ''}</ReactMarkdown>
    </div>
  );
}
