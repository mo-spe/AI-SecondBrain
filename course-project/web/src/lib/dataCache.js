import { apiRequest } from './api.js';

const cache = new Map();
const pending = new Map();

const scopedKey = (key) => `${localStorage.getItem('feynman_token') || 'anonymous'}:${key}`;

export const cachedRequest = (key, path, options = {}) => {
  const keyWithScope = scopedKey(key);
  if (cache.has(keyWithScope)) return Promise.resolve(cache.get(keyWithScope));
  if (pending.has(keyWithScope)) return pending.get(keyWithScope);

  const request = apiRequest(path, options)
    .then((result) => {
      cache.set(keyWithScope, result);
      return result;
    })
    .finally(() => pending.delete(keyWithScope));

  pending.set(keyWithScope, request);
  return request;
};

export const invalidateApiCache = (...keys) => {
  keys.forEach((key) => {
    for (const cacheKey of cache.keys()) {
      if (cacheKey.endsWith(`:${key}`)) cache.delete(cacheKey);
    }
    for (const pendingKey of pending.keys()) {
      if (pendingKey.endsWith(`:${key}`)) pending.delete(pendingKey);
    }
  });
};

export const clearApiCache = () => {
  cache.clear();
  pending.clear();
};
