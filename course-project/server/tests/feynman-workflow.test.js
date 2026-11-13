import test from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../src/app.js';
import { env } from '../src/config/env.js';
import { User } from '../src/models/User.js';
import { Knowledge } from '../src/models/Knowledge.js';
import { FeynmanAttempt } from '../src/models/FeynmanAttempt.js';
import { createAccessToken } from '../src/utils/jwt.js';

test('润色、评价、持久化、60分边界、重试和防伪造', async () => {
  const userId = '000000000000000000000001';
  const knowledgeId = '000000000000000000000002';
  const originalFetch = global.fetch;
  const originals = [User.findById, Knowledge.findOne, Knowledge.updateOne, FeynmanAttempt.findOne, FeynmanAttempt.create];
  const previousEnv = { ...env };
  const item = { id: knowledgeId, reviewScore: null, lastReviewedAt: null, title: '闭包', content: '函数保留对词法作用域的访问' };
  const records = [];
  let score = 59;
  let aiCalls = 0;
  let failUpdate = false;
  let server;
  try {
    Object.assign(env, { jwtSecret: 'isolated-test-secret', aiProvider: 'openai-compatible', aiApiKey: 'test-only', aiBaseUrl: 'https://ai.test.invalid/v1', aiChatModel: 'test' });
    User.findById = async (id) => id === userId ? { id: userId } : null;
    Knowledge.findOne = async (query) => query._id === knowledgeId && query.owner === userId ? item : null;
    Knowledge.updateOne = async (query, update) => {
      if (failUpdate) { failUpdate = false; throw Error('simulated persistence failure'); }
      if (!item.lastReviewedAt || item.lastReviewedAt <= query.$or[1].lastReviewedAt.$lte) Object.assign(item, update.$set);
    };
    FeynmanAttempt.findOne = async (query) => records.find((record) => record.clientAttemptId === query.clientAttemptId && (!query.knowledge || query.knowledge === record.knowledge)) || null;
    FeynmanAttempt.create = async (data) => {
      const record = { ...data, id: String(records.length + 1), createdAt: new Date(Date.now() + records.length * 1000) };
      records.push(record);
      return record;
    };
    global.fetch = async (url, options) => {
      if (!String(url).startsWith(env.aiBaseUrl)) return originalFetch(url, options);
      aiCalls++;
      const payload = JSON.parse(options.body);
      const input = JSON.parse(payload.messages[1].content);
      const polishing = payload.messages[0].content.includes('学术表达编辑');
      if (!polishing) assert.equal(input.transcript, '闭包允许函数保留词法作用域的访问。');
      return new Response(JSON.stringify({ choices: [{ message: { content: JSON.stringify(polishing ? { polishedText: '闭包允许函数保留词法作用域的访问。' } : { score, evaluation: '对概念的理解反馈', strengths: ['表述清晰'], weaknesses: ['补充例子'] }) } }] }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    };
    server = createApp().listen(0, '127.0.0.1');
    await new Promise((resolve) => server.once('listening', resolve));
    const base = `http://127.0.0.1:${server.address().port}/api/feynman`;
    const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${createAccessToken(userId)}` };
    const post = async (path, body, auth = headers) => {
      const response = await originalFetch(base + path, { method: 'POST', headers: auth, body: JSON.stringify(body) });
      return { status: response.status, body: await response.json() };
    };
    const input = { knowledgeId, transcript: '嗯，闭包就是函数还记着外面的变量', polishedText: '闭包允许函数保留词法作用域的访问。' };
    assert.equal((await post('/polish', input)).body.polishedText, input.polishedText);
    assert.equal(records.length, 0);
    for (const value of [59, 60, 61, 0, 100]) {
      score = value;
      const body = { ...input, clientAttemptId: `attempt-${value}`, score: 100 - value };
      const result = await post('/evaluate', body);
      assert.equal(result.status, 200);
      assert.equal(result.body.score, value);
      assert.equal(result.body.saved, true);
      assert.equal(item.reviewScore, value);
      const serialized = new Knowledge({ owner: userId, title: 'x', content: 'x', reviewScore: value }).toJSON();
      assert.equal(serialized.isLearned, true);
      assert.equal(serialized.isMastered, value >= 60);
    }
    const callsBeforeRetry = aiCalls;
    const count = records.length;
    await post('/evaluate', { ...input, clientAttemptId: 'attempt-59' });
    assert.equal(records.length, count);
    assert.equal(aiCalls, callsBeforeRetry);
    assert.equal(item.reviewScore, 100);
    assert.equal((await post('/evaluate', { ...input, clientAttemptId: 'attempt-59', transcript: '已改变' })).status, 409);
    assert.equal((await post('/attempts', { ...input, clientAttemptId: 'forged', score: 100 })).status, 400);
    assert.equal((await post('/evaluate', { ...input })).status, 400);
    assert.equal((await post('/polish', { ...input, knowledgeId: 'other' })).status, 404);
    assert.equal((await post('/polish', input, { 'Content-Type': 'application/json' })).status, 401);
    score = null;
    assert.equal((await post('/evaluate', { ...input, clientAttemptId: 'invalid-score' })).status, 502);
    assert.equal(records.length, count);
    score = 60;
    failUpdate = true;
    assert.equal((await post('/evaluate', { ...input, clientAttemptId: 'repair' })).status, 500);
    const retry = await post('/evaluate', { ...input, clientAttemptId: 'repair' });
    assert.equal(retry.status, 200);
    assert.equal(records.length, count + 1);
    assert.equal(item.reviewScore, 60);
    env.aiProvider = 'mock';
    assert.equal((await post('/polish', input)).status, 503);
    assert.equal(new Knowledge({ owner: userId, title: 'x', content: 'x' }).toJSON().isLearned, false);
  } finally {
    global.fetch = originalFetch;
    [User.findById, Knowledge.findOne, Knowledge.updateOne, FeynmanAttempt.findOne, FeynmanAttempt.create] = originals;
    Object.assign(env, previousEnv);
    if (server) await new Promise((resolve) => server.close(resolve));
  }
});
