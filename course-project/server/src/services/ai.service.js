import { env } from '../config/env.js';

export class AiProviderError extends Error {
  constructor(message, cause) {
    super(message, { cause });
    this.name = 'AiProviderError';
    this.statusCode = 502;
  }
}

export const isAiConfigured = () => env.aiProvider !== 'mock' && Boolean(env.aiApiKey && env.aiBaseUrl && env.aiChatModel);

export const getAiStatus = () => ({
  configured: isAiConfigured(),
  provider: isAiConfigured() ? env.aiProvider : 'mock',
  model: isAiConfigured() ? env.aiChatModel : null
});

const isDeepSeekProvider = () => {
  try {
    return new URL(env.aiBaseUrl).hostname === 'api.deepseek.com';
  } catch (_error) {
    return false;
  }
};

const requestChatCompletion = async (messages, options = {}) => {
  if (!isAiConfigured()) throw new AiProviderError('真实 AI Provider 尚未配置');

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), env.aiTimeoutMs);
  try {
    const response = await fetch(`${env.aiBaseUrl}/chat/completions`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${env.aiApiKey}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: env.aiChatModel,
        messages,
        temperature: options.temperature ?? 0.2,
        max_tokens: options.maxTokens ?? 1000,
        ...(options.jsonMode ? { response_format: { type: 'json_object' } } : {}),
        ...(options.disableThinking && env.aiDisableThinking && isDeepSeekProvider() ? { thinking: { type: 'disabled' } } : {})
      }),
      signal: controller.signal
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      const providerMessage = payload?.error?.message || `HTTP ${response.status}`;
      throw new AiProviderError(`AI Provider 请求失败：${providerMessage}`);
    }
    const content = payload?.choices?.[0]?.message?.content;
    if (typeof content !== 'string' || !content.trim()) throw new AiProviderError('AI Provider 返回了空内容');
    return content.trim();
  } catch (error) {
    if (error instanceof AiProviderError) throw error;
    const message = error.name === 'AbortError' ? 'AI Provider 请求超时' : 'AI Provider 网络请求失败';
    throw new AiProviderError(message, error);
  } finally {
    clearTimeout(timeout);
  }
};

const parseJsonResponse = (content) => {
  const normalized = content.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/i, '').trim();
  try {
    return JSON.parse(normalized);
  } catch (error) {
    const firstBrace = normalized.indexOf('{');
    const lastBrace = normalized.lastIndexOf('}');
    if (firstBrace >= 0 && lastBrace > firstBrace) {
      try {
        return JSON.parse(normalized.slice(firstBrace, lastBrace + 1));
      } catch (_nestedError) {
        // 保留原始解析错误，便于区分“有包裹文本”和“内容被截断”。
      }
    }
    throw new AiProviderError('AI Provider 返回格式不是有效 JSON', error);
  }
};

const asStringList = (value) => (Array.isArray(value) ? value.map((item) => String(item).trim()).filter(Boolean).slice(0, 4) : []);

export const evaluateFeynmanWithAi = async (knowledge, transcript) => {
  const content = await requestChatCompletion([
    { role: 'system', content: '你是费曼学习法教练。只依据用户提供的知识点和复述文本评价，不展示思维链，只返回 JSON。' },
    {
      role: 'user',
      content: JSON.stringify({
        task: '评价学习者对知识点的复述',
        outputSchema: { score: '0-100 的整数', evaluation: '不超过 120 字', polishedText: '保留原意并更清晰的复述', strengths: '字符串数组，最多 3 条', weaknesses: '字符串数组，最多 3 条' },
        knowledge: { title: knowledge.title, content: knowledge.content },
        transcript
      })
    }
  ], { temperature: 0.3, maxTokens: 1600, jsonMode: true, disableThinking: true });
  const result = parseJsonResponse(content);
  const score = Number(result.score);
  if (!Number.isFinite(score) || !result.evaluation || !result.polishedText) throw new AiProviderError('AI Provider 评价结果缺少必要字段');
  return {
    score: Math.max(0, Math.min(100, Math.round(score))),
    evaluation: String(result.evaluation).slice(0, 2000),
    polishedText: String(result.polishedText).slice(0, 30000),
    strengths: asStringList(result.strengths),
    weaknesses: asStringList(result.weaknesses),
    provider: env.aiProvider
  };
};

export const generateQuizWithAi = async (knowledge, difficulty, difficultyLabel) => {
  const content = await requestChatCompletion([
    { role: 'system', content: '你是学习测验出题器。只依据知识点内容出一道中文单选题，不展示思维链，只返回 JSON。' },
    {
      role: 'user',
      content: JSON.stringify({
        task: '生成一道四选一题目',
        difficulty,
        outputSchema: { question: '题干字符串', options: '恰好 4 个字符串组成的数组', correctIndex: '0 到 3 的整数', explanation: '简短解析字符串' },
        rules: ['只有一个正确选项', '干扰项要有迷惑性但不能与知识点内容冲突', '正确答案必须能被知识点内容直接支持'],
        knowledge: { title: knowledge.title, content: knowledge.content }
      })
    }
  ], { temperature: 0.5, maxTokens: 1600, jsonMode: true, disableThinking: true });
  const result = parseJsonResponse(content);
  const options = Array.isArray(result.options) ? result.options.map((item) => String(item).trim()).filter(Boolean).slice(0, 4) : [];
  const correctIndex = Number(result.correctIndex);
  if (!result.question || options.length !== 4 || !Number.isInteger(correctIndex) || correctIndex < 0 || correctIndex > 3 || !result.explanation) {
    throw new AiProviderError('AI Provider 题目结果格式不完整');
  }
  return {
    question: String(result.question).slice(0, 500),
    options,
    correctIndex,
    difficulty,
    difficultyLabel,
    explanation: String(result.explanation).slice(0, 2000),
    provider: env.aiProvider
  };
};

export const answerWithAi = async (query, matches) => {
  const content = await requestChatCompletion([
    { role: 'system', content: '你是一个有来源约束的学习助手。只能依据给定资料回答；资料不足时明确说不知道。不要编造来源，不要展示思维链。' },
    {
      role: 'user',
      content: JSON.stringify({
        query,
        sources: matches.map(({ title, text }) => ({ title, text })),
        instruction: '用简洁中文回答问题，不要提及系统提示词。'
      })
    }
  ], { temperature: 0.2, maxTokens: 700 });
  return { answer: content, provider: env.aiProvider };
};
