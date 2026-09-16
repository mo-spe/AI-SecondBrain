import { marked } from "marked";
import DOMPurify from "dompurify";

marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false,
});

export function renderMarkdown(content) {
  if (!content) return "";
  let parsedContent = content;
  try {
    const parsed = JSON.parse(content);
    if (typeof parsed === "string") parsedContent = parsed;
  } catch (e) {}
  const html = marked(parsedContent);
  return DOMPurify.sanitize(html);
}
