import { FeynmanAttempt } from '../models/FeynmanAttempt.js';
import { Knowledge } from '../models/Knowledge.js';
import { evaluateFeynmanWithAi } from './ai.service.js';

const conflict = () => Object.assign(new Error('本次评价编号已用于其他内容，请重新评价。'), { statusCode: 409 });

export const evaluateAndSaveFeynman = async ({ userId, knowledge, transcript, polishedText, clientAttemptId }) => {
  let attempt = await FeynmanAttempt.findOne({ user: userId, clientAttemptId });
  if (!attempt) {
    const result = await evaluateFeynmanWithAi(knowledge, polishedText);
    try {
      attempt = await FeynmanAttempt.create({
        user: userId, knowledge: knowledge.id, clientAttemptId, transcript,
        polishedText, score: result.score, evaluation: result.evaluation,
        strengths: result.strengths, weaknesses: result.weaknesses
      });
    } catch (error) {
      if (error?.code !== 11000) throw error;
      attempt = await FeynmanAttempt.findOne({ user: userId, clientAttemptId });
      if (!attempt) throw error;
    }
  }
  if (String(attempt.knowledge) !== knowledge.id || attempt.transcript !== transcript || attempt.polishedText !== polishedText) throw conflict();
  // 重试可修复记录已保存但状态更新失败的情况；较旧请求不能回退较新的掌握状态。
  await Knowledge.updateOne({
    _id: knowledge.id, owner: userId,
    $or: [{ lastReviewedAt: null }, { lastReviewedAt: { $lte: attempt.createdAt } }]
  }, { $set: { reviewScore: attempt.score, lastReviewedAt: attempt.createdAt } });
  return attempt;
};
