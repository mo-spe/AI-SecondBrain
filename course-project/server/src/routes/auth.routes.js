import { Router } from 'express';
import { User } from '../models/User.js';
import { requireAuth } from '../middleware/auth.js';
import { hashPassword, comparePassword } from '../utils/password.js';
import { createAccessToken } from '../utils/jwt.js';

export const authRouter = Router();

const normalizeEmail = (email) => String(email || '').trim().toLowerCase();

authRouter.post('/register', async (request, response, next) => {
  try {
    const username = String(request.body.username || '').trim();
    const email = normalizeEmail(request.body.email);
    const password = String(request.body.password || '');

    if (username.length < 2 || !email.includes('@') || password.length < 6) {
      return response.status(400).json({ message: '用户名、邮箱或密码格式不符合要求' });
    }

    const existingUser = await User.findOne({ $or: [{ email }, { username }] });
    if (existingUser) {
      return response.status(409).json({ message: '用户名或邮箱已存在' });
    }

    const user = await User.create({ username, email, passwordHash: await hashPassword(password) });
    return response.status(201).json({ token: createAccessToken(user.id), user });
  } catch (error) {
    return next(error);
  }
});

authRouter.post('/login', async (request, response, next) => {
  try {
    const email = normalizeEmail(request.body.email);
    const password = String(request.body.password || '');
    const user = await User.findOne({ email });

    if (!user || !(await comparePassword(password, user.passwordHash))) {
      return response.status(401).json({ message: '邮箱或密码错误' });
    }

    return response.json({ token: createAccessToken(user.id), user });
  } catch (error) {
    return next(error);
  }
});

authRouter.get('/me', requireAuth, (request, response) => {
  response.json({ user: request.user });
});

