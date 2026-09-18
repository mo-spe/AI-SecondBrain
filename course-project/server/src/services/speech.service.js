import baiduAipSdk from 'baidu-aip-sdk';
import { env } from '../config/env.js';

const AipSpeechClient = baiduAipSdk.speech;

export class SpeechProviderError extends Error {
  constructor(message, cause) {
    super(message, cause ? { cause } : undefined);
    this.name = 'SpeechProviderError';
    this.statusCode = 502;
  }
}

export const isBaiduAsrConfigured = () => Boolean(
  env.baiduAppId && env.baiduApiKey && env.baiduSecretKey
);

const withTimeout = (promise, timeoutMs) => new Promise((resolve, reject) => {
  const timer = setTimeout(() => {
    reject(new SpeechProviderError('百度语音识别请求超时，请稍后重试。'));
  }, timeoutMs);

  promise.then(resolve, reject).finally(() => clearTimeout(timer));
});

const createClient = () => {
  if (!isBaiduAsrConfigured()) {
    throw new SpeechProviderError('百度语音识别尚未配置，请填写 BAIDU_APP_ID、BAIDU_API_KEY 和 BAIDU_SECRET_KEY。');
  }
  return new AipSpeechClient(env.baiduAppId, env.baiduApiKey, env.baiduSecretKey);
};

export const transcribeWithBaidu = async (audioBuffer, { format = 'wav', rate = 16000, cuid } = {}) => {
  const client = createClient();

  try {
    const result = await withTimeout(
      client.recognize(audioBuffer, format, rate, {
        dev_pid: 1537,
        cuid: cuid || `feynman-${Date.now()}`
      }),
      env.baiduAsrTimeoutMs
    );

    if (!result || result.err_no !== 0) {
      throw new SpeechProviderError(`百度语音识别失败：${result?.err_msg || '未知错误'}`);
    }

    const transcript = Array.isArray(result.result) ? result.result.join('').trim() : '';
    if (!transcript) throw new SpeechProviderError('百度语音识别未返回文字，请重新录制并尽量靠近麦克风。');

    return { transcript, provider: 'baidu-asr' };
  } catch (error) {
    if (error instanceof SpeechProviderError) throw error;
    throw new SpeechProviderError('百度语音识别网络请求失败，请检查服务端网络和百度云凭证。', error);
  }
};
