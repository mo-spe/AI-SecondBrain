import { Router } from 'express';
import { requireAuth } from '../middleware/auth.js';
import { Knowledge } from '../models/Knowledge.js';
import { QuizAttempt } from '../models/QuizAttempt.js';
import { QuizQuestion } from '../models/QuizQuestion.js';
import { generateQuizQuestion, gradeAndSaveQuiz, isSupportedDifficulty, isSupportedQuestionType } from '../services/quiz.service.js';

export const quizRouter = Router();
quizRouter.use(requireAuth);

const findOwnedKnowledge = (request, knowledgeId) => Knowledge.findOne({ _id: knowledgeId, owner: request.user.id });

quizRouter.post('/generate', async (request, response, next) => {
  try {
    const difficulty = String(request.body.difficulty || 'medium');
    if (!isSupportedDifficulty(difficulty)) return response.status(400).json({ message: '不支持的题目难度' });
    const questionType = request.body.questionType ?? 'multiple-choice';
    if (!isSupportedQuestionType(questionType)) return response.status(400).json({ message: '不支持的题目类型' });

    const knowledge = await findOwnedKnowledge(request, request.body.knowledgeId);
    if (!knowledge) return response.status(404).json({ message: '知识点不存在' });

    const quiz = await generateQuizQuestion(knowledge, difficulty, questionType);
    const question = await QuizQuestion.create({ user: request.user.id, knowledge: knowledge.id, ...quiz });
    return response.json({
      quiz: {
        quizId: question.id,
        questionType,
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
    const userAnswer = request.body.userAnswer;
    if (!quizId) return response.status(400).json({ message: '题目已失效，请重新生成' });

    const quiz = await QuizQuestion.findOne({ _id: quizId, user: request.user.id });
    if (!quiz) return response.status(404).json({ message: '题目已失效，请重新生成' });
    if (quiz.expiresAt <= new Date()) return response.status(410).json({ message: '题目已过期，请重新生成' });
    if (!await findOwnedKnowledge(request, quiz.knowledge)) return response.status(404).json({ message: '知识点不存在' });

    const { result, attempt } = await gradeAndSaveQuiz({ userId: request.user.id, quiz, userAnswer });
    const item = await findOwnedKnowledge(request, quiz.knowledge);
    if (!item) return response.status(404).json({ message: '知识点不存在' });

    return response.json({
      result,
      attempt,
      item,
      saved: true,
      provider: result.provider
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
