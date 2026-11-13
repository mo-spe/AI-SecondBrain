import { generateQuizWithAi, generateShortAnswerWithAi, gradeShortAnswerWithAi, isAiConfigured } from './ai.service.js';
import { Knowledge } from '../models/Knowledge.js';
import { QuizAttempt } from '../models/QuizAttempt.js';

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

export const isSupportedQuestionType = (type) => ['multiple-choice', 'short-answer'].includes(type);

export const generateQuizQuestion = async (knowledge, difficulty, questionType = 'multiple-choice') => {
  const difficultyLabel = DIFFICULTY_CONFIG[difficulty].label;
  if (questionType === 'short-answer') {
    if (!isAiConfigured()) throw Object.assign(new Error('解答题需要配置真实 AI Provider 后才能生成和评分'), { statusCode: 503 });
    return generateShortAnswerWithAi(knowledge, difficulty, difficultyLabel);
  }
  const result = await (isAiConfigured()
    ? generateQuizWithAi(knowledge, difficulty, difficultyLabel)
    : buildQuizQuestion(knowledge, difficulty));
  return { ...result, questionType: 'multiple-choice' };
};

const normalizeAnswer = (quiz, userAnswer) => {
  if (quiz.questionType === 'short-answer') {
    if (typeof userAnswer !== 'string' || !userAnswer.trim() || userAnswer.trim().length > 5000) throw Object.assign(new Error('请输入1至5000字符的解答'), { statusCode: 400 });
    return userAnswer.trim();
  }
  if (!Number.isInteger(userAnswer) || userAnswer < 0 || userAnswer > 3) throw Object.assign(new Error('请选择一个有效答案'), { statusCode: 400 });
  return userAnswer;
};

export const gradeQuizAnswer = async (quiz, userAnswer) => {
  const answer = normalizeAnswer(quiz, userAnswer);
  if (quiz.questionType === 'short-answer') {
    if (!isAiConfigured()) throw Object.assign(new Error('解答题评分需要配置真实 AI Provider'), { statusCode: 503 });
    const grade = await gradeShortAnswerWithAi(quiz, answer);
    return { ...grade, questionType: 'short-answer', userAnswer: answer, correctAnswer: quiz.referenceAnswer, explanation: quiz.explanation, keyPoints: quiz.keyPoints };
  }
  const isCorrect = quiz.correctIndex === answer;
  return { questionType: 'multiple-choice', isCorrect, score: isCorrect ? 100 : 0, userAnswer: answer, correctIndex: quiz.correctIndex, correctAnswer: quiz.options[quiz.correctIndex], explanation: quiz.explanation, provider: quiz.provider };
};

export const gradeAndSaveQuiz = async ({ userId, quiz, userAnswer }) => {
  const answer = normalizeAnswer(quiz, userAnswer);
  const shortAnswer = quiz.questionType === 'short-answer';
  const attemptQuery = { _id: quiz.id, user: userId };
  let attempt = await QuizAttempt.findOne(attemptQuery);
  if (!attempt) {
    const grade = await gradeQuizAnswer(quiz, answer);
    try {
      // 两个集合可复用同一ObjectId；借助已有主键唯一约束，使同一题的网络重试只保存一份记录。
      attempt = await QuizAttempt.create({
        _id: quiz.id, user: userId, knowledge: quiz.knowledge, difficulty: quiz.difficulty,
        questionType: grade.questionType,
        questionSnapshot: {
          question: quiz.question, options: quiz.options, correctIndex: quiz.correctIndex,
          referenceAnswer: quiz.referenceAnswer, keyPoints: quiz.keyPoints
        },
        ...(shortAnswer ? { userTextAnswer: grade.userAnswer } : { userAnswer: grade.userAnswer }),
        score: grade.score, feedback: grade.feedback, isCorrect: grade.isCorrect, provider: grade.provider
      });
    } catch (error) {
      if (error?.code !== 11000) throw error;
      attempt = await QuizAttempt.findOne(attemptQuery);
      if (!attempt) throw error;
    }
  }
  const storedAnswer = shortAnswer ? attempt.userTextAnswer : attempt.userAnswer;
  if (String(attempt.knowledge) !== String(quiz.knowledge) || storedAnswer !== answer) {
    throw Object.assign(new Error('本题已提交其他答案，请重新生成题目'), { statusCode: 409 });
  }
  // 与费曼评价共用复习时间：重试可补写失败的状态，旧记录不会覆盖更新的测验或复述成绩。
  await Knowledge.updateOne({
    _id: quiz.knowledge, owner: userId,
    $or: [{ lastReviewedAt: null }, { lastReviewedAt: { $lte: attempt.createdAt } }]
  }, { $set: { reviewScore: attempt.score, lastReviewedAt: attempt.createdAt } });
  const result = {
    questionType: attempt.questionType, isCorrect: attempt.isCorrect, score: attempt.score,
    userAnswer: storedAnswer, explanation: quiz.explanation, provider: attempt.provider || quiz.provider,
    ...(shortAnswer
      ? { correctAnswer: attempt.questionSnapshot.referenceAnswer, feedback: attempt.feedback, keyPoints: attempt.questionSnapshot.keyPoints }
      : { correctIndex: attempt.questionSnapshot.correctIndex, correctAnswer: attempt.questionSnapshot.options[attempt.questionSnapshot.correctIndex] })
  };
  return { result, attempt };
};
