import { User } from '../models/User.js';
import { verifyAccessToken } from '../utils/jwt.js';

export const requireAuth = async (request, response, next) => {
  try {
    const authorization = request.headers.authorization || '';
    const token = authorization.startsWith('Bearer ') ? authorization.slice(7) : null;

    if (!token) {
      return response.status(401).json({ message: '请先登录' });
    }

    const payload = verifyAccessToken(token);
    const user = await User.findById(payload.sub);

    if (!user) {
      return response.status(401).json({ message: '登录状态已失效' });
    }

    request.user = user;
    return next();
  } catch (_error) {
    return response.status(401).json({ message: '登录状态已失效' });
  }
};

