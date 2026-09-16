import { generateQuizWithAi, isAiConfigured } from './ai.service.js';

export const DIFFICULTY_CONFIG = {
  easy: {
    label: '基础',
    question: (title) => `关于“${title}”，以下哪一项最接近当前知识点的核心解释？`
  },
  medium: {
    label: '进阶',
    question: (title) => `如果要向同学解释“${title}”，下面哪种说法最符合知识库记录？`
  },
  hard: {
    label: '挑战',
    question: (title) => `在实际学习或复习“${title}”时，哪个判断最能体现它的关键边界？`
  }
};

const hashText = (value) => [...value].reduce((hash, character) => ((hash * 31) + character.charCodeAt(0)) >>> 0, 7);

const excerpt = (content) => {
  const normalized = content.replace(/\s+/g, ' ').trim();
  return normalized.length > 150 ? `${normalized.slice(0, 147)}…` : normalized;
};

const rotate = (items, offset) => items.map((_item, index) => items[(index + offset) % items.length]);

export const isSupportedDifficulty = (difficulty) => Object.hasOwn(DIFFICULTY_CONFIG, difficulty);

export const buildQuizQuestion = (knowledge, difficulty) => {
  const correctOption = `${knowledge.title}的核心理解是：${excerpt(knowledge.content)}`;
  const distractors = [
    `它只需要记住定义，不需要结合任何使用场景或上下文。`,
    `它与当前知识点记录的重点相反，不能用来解释这个概念。`,
    `它只适用于一个固定案例，换到其他情况就完全没有参考价值。`
  ];
  const correctIndex = hashText(`${knowledge.id}:${difficulty}`) % 4;
  const options = [...distractors.slice(0, correctIndex), correctOption, ...distractors.slice(correctIndex)];
  const config = DIFFICULTY_CONFIG[difficulty];

  return {
    question: config.question(knowledge.title),
    options,
    correctIndex,
    difficulty,
    difficultyLabel: config.label,
    explanation: `判定依据来自知识点“${knowledge.title}”当前保存的内容。课程版在未配置真实模型时使用 Mock Provider，确保课堂演示稳定。`,
    provider: 'mock'
  };
};

export const generateQuizQuestion = async (knowledge, difficulty) => {
  const difficultyLabel = DIFFICULTY_CONFIG[difficulty].label;
  return isAiConfigured()
    ? generateQuizWithAi(knowledge, difficulty, difficultyLabel)
    : buildQuizQuestion(knowledge, difficulty);
};
