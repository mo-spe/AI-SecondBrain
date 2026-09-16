import jwt from 'jsonwebtoken';
import { env } from '../config/env.js';

const TOKEN_LIFETIME = '7d';

export const createAccessToken = (userId) => jwt.sign({ sub: userId }, env.jwtSecret, { expiresIn: TOKEN_LIFETIME });
export const verifyAccessToken = (token) => jwt.verify(token, env.jwtSecret);

