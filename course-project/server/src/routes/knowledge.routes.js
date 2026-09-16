import { Router } from 'express';
import { Knowledge } from '../models/Knowledge.js';
import { requireAuth } from '../middleware/auth.js';

export const knowledgeRouter = Router();
knowledgeRouter.use(requireAuth);

const cleanTags = (tags) => (Array.isArray(tags) ? tags.map((tag) => String(tag).trim()).filter(Boolean).slice(0, 8) : []);

knowledgeRouter.get('/', async (request, response, next) => {
  try {
    const items = await Knowledge.find({ owner: request.user.id }).sort({ updatedAt: -1 });
    return response.json({ items });
  } catch (error) {
    return next(error);
  }
});

knowledgeRouter.post('/', async (request, response, next) => {
  try {
    const title = String(request.body.title || '').trim();
    const content = String(request.body.content || '').trim();

    if (!title || !content) {
      return response.status(400).json({ message: '标题和内容不能为空' });
    }

    const item = await Knowledge.create({ owner: request.user.id, title, content, tags: cleanTags(request.body.tags) });
    return response.status(201).json({ item });
  } catch (error) {
    return next(error);
  }
});

knowledgeRouter.get('/:id', async (request, response, next) => {
  try {
    const item = await Knowledge.findOne({ _id: request.params.id, owner: request.user.id });
    return item ? response.json({ item }) : response.status(404).json({ message: '知识点不存在' });
  } catch (error) {
    return next(error);
  }
});

knowledgeRouter.put('/:id', async (request, response, next) => {
  try {
    const updates = {
      title: String(request.body.title || '').trim(),
      content: String(request.body.content || '').trim(),
      tags: cleanTags(request.body.tags)
    };

    if (!updates.title || !updates.content) {
      return response.status(400).json({ message: '标题和内容不能为空' });
    }

    const item = await Knowledge.findOneAndUpdate(
      { _id: request.params.id, owner: request.user.id },
      updates,
      { new: true, runValidators: true }
    );
    return item ? response.json({ item }) : response.status(404).json({ message: '知识点不存在' });
  } catch (error) {
    return next(error);
  }
});

knowledgeRouter.delete('/:id', async (request, response, next) => {
  try {
    const deleted = await Knowledge.findOneAndDelete({ _id: request.params.id, owner: request.user.id });
    return deleted ? response.status(204).send() : response.status(404).json({ message: '知识点不存在' });
  } catch (error) {
    return next(error);
  }
});

