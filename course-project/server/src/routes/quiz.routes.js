import { Router } from 'express';
import { requireAuth } from '../middleware/auth.js';
import { Knowledge } from '../models/Knowledge.js';
import { QuizAttempt } from '../models/QuizAttempt.js';
import { QuizQuestion } from '../models/QuizQuestion.js';
import { generateQuizQuestion, isSupportedDifficulty } from '../services/quiz.service.js';

export const quizRouter = Router();
quizRouter.use(requireAuth);

const findOwnedKnowledge = (request, knowledgeId) => Knowledge.findOne({ _id: knowledgeId, owner: request.user.id });

quizRouter.post('/generate', async (request, response, next) => {
  try {
    const difficulty = String(request.body.difficulty || 'medium');
    if (!isSupportedDifficulty(difficulty)) return response.status(400).json({ message: '不支持的题目难度' });

    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });

    const quiz = await generateQuizQuestion(knowledge, difficulty);
    const question = await QuizQuestion.create({ user: request.user.id, knowledge: knowledge.id, ...quiz });
    return response.json({
      quiz: {
        quizId: question.id,
        question: quiz.question,
        options: quiz.options,
        difficulty: quiz.difficulty,
        difficultyLabel: quiz.difficultyLabel,
        knowledgeId: knowledge.id,
        knowledgeTitle: knowledge.title,
        provider: quiz.provider
      },
      provider: quiz.provider
    });
  } catch (error) {
    return next(error);
  }
});

quizRouter.post('/grade', async (request, response, next) => {
  try {
    const quizId = String(request.body.quizId || '');
    const userAnswer = Number(request.body.userAnswer);
    if (!Number.isInteger(userAnswer) || userAnswer < 0 || userAnswer > 3) {
      return response.status(400).json({ message: '请选择一个答案' });
    }
    if (!quizId) return response.status(400).json({ message: '题目已失效，请重新生成' });

    const quiz = await QuizQuestion.findOne({ _id: quizId, user: request.user.id });
    if (!quiz) return response.status(404).json({ message: '题目已失效，请重新生成' });
    if (quiz.expiresAt <= new Date()) return response.status(410).json({ message: '题目已过期，请重新生成' });

    const isCorrect = quiz.correctIndex === userAnswer;
    const attempt = await QuizAttempt.create({
      user: request.user.id,
      knowledge: quiz.knowledge,
      difficulty: quiz.difficulty,
      questionSnapshot: {
        question: quiz.question,
        options: quiz.options,
        correctIndex: quiz.correctIndex
      },
      userAnswer,
      isCorrect
    });

    return response.json({
      result: {
        isCorrect,
        score: isCorrect ? 100 : 0,
        userAnswer,
        correctIndex: quiz.correctIndex,
        correctAnswer: quiz.options[quiz.correctIndex],
        explanation: quiz.explanation
      },
      attempt,
      provider: quiz.provider
    });
  } catch (error) {
    return next(error);
  }
});

quizRouter.get('/attempts/:knowledgeId', async (request, response, next) => {
  try {
    const knowledge = await findOwnedKnowledge(request, request.params.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });
    const attempts = await QuizAttempt.find({ user: request.user.id, knowledge: knowledge.id }).sort({ createdAt: -1 }).limit(20);
    return response.json({ attempts });
  } catch (error) {
    return next(error);
  }
});
