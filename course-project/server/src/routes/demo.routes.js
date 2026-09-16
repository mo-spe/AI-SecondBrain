import { Router } from 'express';
import { requireAuth } from '../middleware/auth.js';
import { Knowledge } from '../models/Knowledge.js';

export const demoRouter = Router();
demoRouter.use(requireAuth);

const demoItems = [
  { title: '向量数据库', content: '向量数据库用于保存高维向量，并通过相似度搜索找到语义接近的内容。RAG 会先检索相关知识，再把结果交给模型回答。', tags: ['RAG', 'AI', '数据库'] },
  { title: '文本分块', content: '文本分块是把较长文档切成适合检索的小片段。合理的分块需要保留上下文，也要控制每个片段的长度，避免召回结果过于宽泛。', tags: ['RAG', 'AI'] },
  { title: 'MongoDB Atlas', content: 'MongoDB Atlas 是托管在云上的 MongoDB 服务。应用通过连接串访问集群，数据库用户负责身份认证，Network Access 负责限制可连接的来源地址。', tags: ['数据库', '云服务'] },
  { title: 'React 状态管理', content: 'React 组件通过 state 保存会变化的数据，通过 props 传递信息。Context 适合共享登录态等跨层级状态，但局部交互仍应尽量保持在局部组件内。', tags: ['React', '前端'] },
  { title: 'JWT 鉴权', content: 'JWT 登录流程通常是服务端签发短期令牌，前端在后续请求中通过 Bearer Token 证明身份。服务端仍然需要校验令牌并确认用户资源归属。', tags: ['后端', '安全'] },
  { title: '费曼学习法', content: '费曼学习法要求学习者不用原文术语，用自己的话解释概念，再通过例子和反例暴露理解中的空白。复述、反馈和再次修正构成一个学习循环。', tags: ['学习', 'AI'] }
];

demoRouter.post('/seed', async (request, response, next) => {
  try {
    const existing = await Knowledge.find({ owner: request.user.id, title: { $in: demoItems.map((item) => item.title) } }).select('title').lean();
    const existingTitles = new Set(existing.map((item) => item.title));
    const pending = demoItems.filter((item) => !existingTitles.has(item.title)).map((item) => ({ ...item, owner: request.user.id }));
    if (pending.length > 0) await Knowledge.insertMany(pending);
    return response.status(201).json({ createdCount: pending.length, skippedCount: existing.length });
  } catch (error) {
    return next(error);
  }
});
