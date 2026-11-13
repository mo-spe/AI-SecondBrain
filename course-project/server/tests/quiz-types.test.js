import test from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../src/app.js';
import { env } from '../src/config/env.js';
import { createAccessToken } from '../src/utils/jwt.js';
import { User } from '../src/models/User.js';
import { Knowledge } from '../src/models/Knowledge.js';
import { QuizQuestion } from '../src/models/QuizQuestion.js';
import { QuizAttempt } from '../src/models/QuizAttempt.js';

test('题型、学习状态、评分边界、幂等重试和旧模型兼容', async () => {
  const uid = '000000000000000000000001';
  const kid = '000000000000000000000002';
  const oldEnv = { ...env };
  const originalFetch = global.fetch;
  const originals = [User.findById, Knowledge.findOne, Knowledge.updateOne, QuizQuestion.create, QuizQuestion.findOne, QuizAttempt.create, QuizAttempt.findOne];
  const knowledge = new Knowledge({ _id: kid, owner: uid, title: '闭包', content: '闭包保留词法环境的访问。' });
  const questions = [];
  const attempts = [];
  let server;
  let score = 75;
  let invalidQuestion = false;
  let nullAiResult = false;
  let failGrade = false;
  let failUpdate = false;
  let failCreate = false;
  let gradeCalls = 0;
  try {
    Object.assign(env, { aiProvider: 'openai-compatible', aiApiKey: 'isolated', aiBaseUrl: 'https://quiz.test.invalid/v1', aiChatModel: 'test' });
    User.findById = async () => ({ id: uid });
    Knowledge.findOne = async (q) => String(q._id) === kid && q.owner === uid ? knowledge : null;
    Knowledge.updateOne = async (query, update) => {
      assert.equal(String(query._id), kid);
      assert.equal(query.owner, uid);
      if (failUpdate) { failUpdate = false; throw new Error('simulated status failure'); }
      if (!knowledge.lastReviewedAt || knowledge.lastReviewedAt <= query.$or[1].lastReviewedAt.$lte) {
        knowledge.set(update.$set);
        return { matchedCount: 1 };
      }
      return { matchedCount: 0 };
    };
    QuizQuestion.create = async (data) => {
      const question = new QuizQuestion(data);
      await question.validate();
      questions.push(question);
      return question;
    };
    QuizQuestion.findOne = async (q) => questions.find((item) => item.id === q._id && String(item.user) === q.user) || null;
    QuizAttempt.create = async (data) => {
      if (failCreate) { failCreate = false; throw new Error('simulated attempt failure'); }
      const attempt = new QuizAttempt(data);
      attempt.createdAt = new Date(Date.now() + attempts.length * 1000);
      await attempt.validate();
      if (attempts.some((item) => item.id === String(data._id))) throw Object.assign(new Error('duplicate'), { code: 11000 });
      attempts.push(attempt);
      return attempt;
    };
    QuizAttempt.findOne = async (query) => attempts.find((item) => item.id === query._id && String(item.user) === query.user) || null;
    global.fetch = async (url, options) => {
      if (!String(url).startsWith(env.aiBaseUrl)) return originalFetch(url, options);
      const messages = JSON.parse(options.body).messages;
      const input = JSON.parse(messages[1].content);
      let result;
      if ('userAnswer' in input) {
        gradeCalls++;
        assert.equal(input.referenceAnswer, '闭包保留词法环境。');
        if (failGrade) return new Response(JSON.stringify({ error: { message: 'temporary failure' } }), { status: 503 });
        result = { score, feedback: '解释了词法环境，建议补充实际例子。' };
      } else if (input.outputSchema.referenceAnswer) {
        result = invalidQuestion ? { question: '缺少参考答案' } : { question: '解释闭包并举例。', referenceAnswer: '闭包保留词法环境。', keyPoints: ['词法环境', '举例说明'], explanation: '结合定义与案例。' };
      } else result = { question: '什么是闭包？', options: ['保留词法环境', '数据库', '网络', '图片'], correctIndex: 0, explanation: '闭包保留词法环境。' };
      return new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify(nullAiResult ? null : result) } }] }), { status: 200 });
    };
    server = createApp().listen(0, '127.0.0.1');
    await new Promise((resolve) => server.once('listening', resolve));
    const base = `http://127.0.0.1:${server.address().port}/api/quiz`;
    const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${createAccessToken(uid)}` };
    const post = async (path, data) => {
      const response = await originalFetch(base + path, { method: 'POST', headers, body: JSON.stringify(data) });
      return { status: response.status, body: await response.json() };
    };
    for (const questionType of ['multiple-choice', 'short-answer']) for (const difficulty of ['easy', 'medium', 'hard']) {
      const generated = await post('/generate', { knowledgeId: kid, difficulty, questionType });
      assert.equal(generated.status, 200);
      assert.equal(generated.body.quiz.questionType, questionType);
      for (const hidden of ['referenceAnswer', 'keyPoints', 'correctIndex', 'explanation']) assert.equal(hidden in generated.body.quiz, false);
      const graded = await post('/grade', { quizId: generated.body.quiz.quizId, userAnswer: questionType === 'short-answer' ? '闭包使函数保留词法环境。' : 0 });
      assert.equal(graded.status, 200);
      assert.equal(graded.body.result.score, questionType === 'short-answer' ? 75 : 100);
      assert.equal(graded.body.attempt.questionType, questionType);
      assert.equal(graded.body.saved, true);
      assert.equal(graded.body.item.reviewScore, graded.body.result.score);
      assert.equal(graded.body.item.isLearned, true);
      assert.equal(graded.body.item.isMastered, true);
    }
    const qid = questions.at(-1).id;
    for (const answer of ['', '  ', 'x'.repeat(5001), 0, null, {}]) assert.equal((await post('/grade', { quizId: qid, userAnswer: answer })).status, 400);
    const before = attempts.length;
    const freshShort = async () => {
      const generated = await post('/generate', { knowledgeId: kid, questionType: 'short-answer' });
      assert.equal(generated.status, 200);
      return generated.body.quiz.quizId;
    };
    const ungraded = await freshShort();
    const previousScore = knowledge.reviewScore;
    nullAiResult = true;
    assert.equal((await post('/grade', { quizId: ungraded, userAnswer: '测试' })).status, 502);
    assert.equal((await post('/generate', { knowledgeId: kid, questionType: 'short-answer' })).status, 502);
    nullAiResult = false;
    score = 101;
    assert.equal((await post('/grade', { quizId: ungraded, userAnswer: '测试' })).status, 502);
    assert.equal(attempts.length, before);
    assert.equal(knowledge.reviewScore, previousScore);
    let olderQuizId;
    for (const value of [59, 60, 61]) {
      score = value;
      const quizId = await freshShort();
      if (value === 59) olderQuizId = quizId;
      const graded = await post('/grade', { quizId, userAnswer: '测试', score: 100 });
      assert.equal(graded.status, 200);
      assert.equal(graded.body.result.isCorrect, value >= 60);
      assert.equal(graded.body.item.reviewScore, value);
      assert.equal(graded.body.item.isLearned, true);
      assert.equal(graded.body.item.learningStatus, value >= 60 ? 'mastered' : 'learning');
      assert.equal(graded.body.item.lastReviewedAt, graded.body.attempt.createdAt);
    }
    const callsBeforeRetry = gradeCalls;
    const countBeforeRetry = attempts.length;
    const olderRetry = await post('/grade', { quizId: olderQuizId, userAnswer: '测试' });
    assert.equal(olderRetry.body.result.score, 59);
    assert.equal(olderRetry.body.item.reviewScore, 61);
    assert.equal(gradeCalls, callsBeforeRetry);
    assert.equal(attempts.length, countBeforeRetry);
    assert.equal((await post('/grade', { quizId: olderQuizId, userAnswer: '修改答案' })).status, 409);
    const retryQuiz = await freshShort();
    failGrade = true;
    assert.equal((await post('/grade', { quizId: retryQuiz, userAnswer: '测试' })).status, 502);
    failGrade = false;
    assert.equal((await post('/grade', { quizId: retryQuiz, userAnswer: '测试' })).status, 200);
    const repairQuiz = await freshShort();
    score = 59;
    failCreate = true;
    assert.equal((await post('/grade', { quizId: repairQuiz, userAnswer: '测试' })).status, 500);
    assert.equal(knowledge.reviewScore, 61);
    failUpdate = true;
    assert.equal((await post('/grade', { quizId: repairQuiz, userAnswer: '测试' })).status, 500);
    assert.equal(knowledge.reviewScore, 61);
    const repairCount = attempts.length;
    const repairCalls = gradeCalls;
    assert.equal((await post('/grade', { quizId: repairQuiz, userAnswer: '测试' })).body.item.reviewScore, 59);
    assert.equal(attempts.length, repairCount);
    assert.equal(gradeCalls, repairCalls);
    knowledge.reviewScore = 100;
    knowledge.lastReviewedAt = new Date(Date.now() + 3600000);
    const lateRetry = await post('/grade', { quizId: repairQuiz, userAnswer: '测试' });
    assert.equal(lateRetry.body.item.reviewScore, 100);
    questions.at(-1).expiresAt = new Date(0);
    assert.equal((await post('/grade', { quizId: repairQuiz, userAnswer: '测试' })).status, 410);
    assert.equal((await post('/grade', { quizId: 'other', userAnswer: 0 })).status, 404);
    assert.equal((await post('/generate', { knowledgeId: 'other' })).status, 404);
    assert.equal((await post('/generate', { knowledgeId: kid, questionType: 'bad' })).status, 400);
    assert.equal((await post('/generate', { knowledgeId: kid, difficulty: 'bad' })).status, 400);
    invalidQuestion = true;
    assert.equal((await post('/generate', { knowledgeId: kid, questionType: 'short-answer' })).status, 502);
    env.aiProvider = 'mock';
    assert.equal((await post('/generate', { knowledgeId: kid, questionType: 'short-answer' })).status, 503);
    const old = await post('/generate', { knowledgeId: kid });
    assert.equal(old.status, 200);
    assert.equal(old.body.quiz.questionType, 'multiple-choice');
    assert.equal((await post('/grade', { quizId: old.body.quiz.quizId, userAnswer: null })).status, 400);
    knowledge.lastReviewedAt = null;
    const mockQuiz = questions.at(-1);
    const wrong = await post('/grade', { quizId: mockQuiz.id, userAnswer: (mockQuiz.correctIndex + 1) % 4 });
    assert.equal(wrong.body.item.reviewScore, 0);
    assert.equal(wrong.body.item.learningStatus, 'learning');
    const nextMock = await post('/generate', { knowledgeId: kid });
    const correct = await post('/grade', { quizId: nextMock.body.quiz.quizId, userAnswer: questions.at(-1).correctIndex });
    assert.equal(correct.body.item.reviewScore, 100);
    assert.equal(correct.body.item.learningStatus, 'mastered');
    const concurrentQuiz = await post('/generate', { knowledgeId: kid });
    const concurrentAnswer = questions.at(-1).correctIndex;
    const concurrentCount = attempts.length;
    const concurrent = await Promise.all([0, 1].map(() => post('/grade', { quizId: concurrentQuiz.body.quiz.quizId, userAnswer: concurrentAnswer })));
    assert.deepEqual(concurrent.map((response) => response.status), [200, 200]);
    assert.equal(attempts.length, concurrentCount + 1);
    const legacy = new QuizAttempt({ user: uid, knowledge: kid, difficulty: 'easy', questionSnapshot: { question: '旧题', options: ['a', 'b', 'c', 'd'], correctIndex: 0 }, userAnswer: 0, isCorrect: true });
    await legacy.validate();
    assert.equal(legacy.questionType, 'multiple-choice');
  } finally {
    [User.findById, Knowledge.findOne, Knowledge.updateOne, QuizQuestion.create, QuizQuestion.findOne, QuizAttempt.create, QuizAttempt.findOne] = originals;
    global.fetch = originalFetch;
    Object.assign(env, oldEnv);
    if (server) await new Promise((resolve) => server.close(resolve));
  }
});
