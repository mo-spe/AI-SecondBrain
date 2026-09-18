import 'dotenv/config';

const configuredMongoUri = process.env.MONGODB_URI || '';

export const env = {
  port: Number(process.env.PORT || 4000),
  // 网页复制连接串时可能把 ASCII 连字符替换成非标准连字符，启动前统一规范化避免 SRV 解析失败。
  mongodbUri: configuredMongoUri.replace(/\u2011/g, '-'),
  jwtSecret: process.env.JWT_SECRET || 'development-only-secret',
  clientOrigin: process.env.CLIENT_ORIGIN || 'http://localhost:5173',
  serveWeb: process.env.SERVE_WEB === 'true',
  aiProvider: process.env.AI_PROVIDER || 'mock',
  aiBaseUrl: (process.env.AI_BASE_URL || 'https://api.openai.com/v1').replace(/\/$/, ''),
  aiApiKey: process.env.AI_API_KEY || '',
  aiChatModel: process.env.AI_CHAT_MODEL || 'gpt-4o-mini',
  aiTimeoutMs: Number(process.env.AI_TIMEOUT_MS || 30000),
  aiDisableThinking: process.env.AI_DISABLE_THINKING !== 'false',
  baiduAppId: process.env.BAIDU_APP_ID || '',
  baiduApiKey: process.env.BAIDU_API_KEY || '',
  baiduSecretKey: process.env.BAIDU_SECRET_KEY || '',
  baiduAsrTimeoutMs: Number(process.env.BAIDU_ASR_TIMEOUT_MS || 30000)
};
