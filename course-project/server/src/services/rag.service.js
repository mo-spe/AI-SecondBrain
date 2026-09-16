import { Knowledge } from '../models/Knowledge.js';
import { answerWithAi, isAiConfigured } from './ai.service.js';

const VECTOR_SIZE = 128;
const MIN_RELEVANCE = 0.16;

const hashToken = (token) => {
  let hash = 2166136261;
  for (const character of token) {
    hash ^= character.codePointAt(0);
    hash = Math.imul(hash, 16777619);
  }
  return Math.abs(hash) % VECTOR_SIZE;
};

const tokenize = (value) => {
  const text = String(value || '').toLowerCase();
  const tokens = text.match(/[a-z0-9_]+/g) || [];
  const han = [...text].filter((character) => /\p{Script=Han}/u.test(character));
  tokens.push(...han);
  for (let index = 0; index < han.length - 1; index += 1) tokens.push(`${han[index]}${han[index + 1]}`);
  return tokens;
};

const vectorize = (value) => {
  const vector = new Array(VECTOR_SIZE).fill(0);
  tokenize(value).forEach((token) => { vector[hashToken(token)] += 1; });
  const magnitude = Math.sqrt(vector.reduce((sum, valueAtIndex) => sum + valueAtIndex ** 2, 0));
  return magnitude ? vector.map((valueAtIndex) => valueAtIndex / magnitude) : vector;
};

const similarity = (left, right) => left.reduce((sum, value, index) => sum + value * right[index], 0);

const splitIntoChunks = (knowledge) => {
  const pieces = String(knowledge.content || '').split(/\n+|(?<=[。！？.!?])\s*/u).map((piece) => piece.trim()).filter(Boolean);
  return pieces.flatMap((piece) => {
    if (piece.length <= 700) return [piece];
    return piece.match(/.{1,700}/gu) || [piece];
  }).map((text, index) => ({
    knowledgeId: knowledge._id.toString(),
    title: knowledge.title,
    text,
    index,
    vector: vectorize(`${knowledge.title}\n${text}`)
  }));
};

export async function answerFromKnowledge(userId, query) {
  const knowledgeItems = await Knowledge.find({ owner: userId }).lean();
  const queryVector = vectorize(query);
  const matches = knowledgeItems
    .flatMap(splitIntoChunks)
    .map((chunk) => ({ ...chunk, score: similarity(queryVector, chunk.vector) }))
    .filter((chunk) => chunk.score >= MIN_RELEVANCE)
    .sort((left, right) => right.score - left.score)
    .slice(0, 3);

  if (matches.length === 0) {
    return {
      answer: '我在你的课程知识库中没有找到足够相关的资料。可以先创建一个知识点，或者换一种问法。',
      sources: [],
      provider: 'local-hash-vector'
    };
  }

  const sources = matches.map(({ knowledgeId, title, text, score }) => ({
    knowledgeId,
    title,
    excerpt: text,
    score: Number(score.toFixed(3))
  }));

  if (isAiConfigured()) {
    const generated = await answerWithAi(query, matches);
    return { ...generated, sources };
  }

  const answer = matches.length === 1
    ? `根据你的知识库，“${matches[0].title}”中记录：${matches[0].text}`
    : `根据你的知识库，最相关的内容来自“${matches[0].title}”：${matches[0].text}\n\n另一个相关片段来自“${matches[1].title}”：${matches[1].text}`;

  return {
    answer,
    sources,
    provider: 'local-hash-vector'
  };
}
