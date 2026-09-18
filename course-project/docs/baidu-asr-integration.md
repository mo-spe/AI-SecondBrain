# 课程版真实语音转文字接入说明

更新时间：2026-09-18

## 目标

课程第七次课要求使用浏览器 `MediaRecorder` 录音，再通过服务端调用百度语音识别。本次实现保留现有 React + Express + MongoDB 架构，不把音频写入 MongoDB 或本地磁盘：浏览器录音只在本次页面会话中生成 Blob，服务端通过 `multer.memoryStorage()` 临时读取后直接发送给百度。

## 实现链路

1. React 使用 `MediaRecorder` 获取麦克风音频。
2. 录音最多持续 60 秒，录音中锁定知识点选择，避免音频归属发生变化。
3. 上传前使用 Web Audio API 解码浏览器录音，并重采样为单声道 16kHz PCM WAV。
4. Express 使用 Multer 内存存储接收 WAV 文件，不持久化原始音频。
5. `speech.service.js` 使用 `baidu-aip-sdk` 调用短语音识别，采用中文普通话模型 `dev_pid: 1537`。
6. 识别成功后返回 `transcript` 和 `provider: baidu-asr`，再进入现有 AI 评价流程。

百度官方文档说明短语音识别支持 PCM、WAV、AMR、M4A 等格式，推荐单声道 16kHz 或 8kHz，单段时长上限为 60 秒，因此前端不能直接把浏览器常见的 WebM/Opus 文件原样交给百度。

## 本地配置

复制 `server/.env.example` 为 `server/.env`，填写以下三项：

```ini
BAIDU_APP_ID=你的百度应用 App ID
BAIDU_API_KEY=你的百度应用 API Key
BAIDU_SECRET_KEY=你的百度应用 Secret Key
BAIDU_ASR_TIMEOUT_MS=30000
```

密钥只放在本地 `.env` 或云平台的 Secret Environment Variables 中，不要提交到 Git，也不要写入 React 前端代码。缺少任意一项时，`POST /api/feynman/transcribe` 返回 503，避免退回到伪造的 mock 文本。

## 启动与联调

在 `course-project/server` 中启动服务端：

```powershell
npm run dev
```

在 `course-project/web` 中启动前端：

```powershell
npm run dev
```

登录后进入“费曼复述”，选择知识点，录制不超过 60 秒的讲解，点击“上传并转成文字”。成功时页面 Provider 标记为 `baidu-asr`；如果看到百度凭证、网络或超时提示，先检查 `.env` 和百度智能云应用状态。

## 安全与边界

- 后端只接收当前登录用户拥有的知识点。
- 原始音频不落库、不写磁盘，服务端只在请求生命周期内保留内存 Buffer。
- 录音停止、组件卸载、路由切换和异常时都会停止 MediaStream tracks。
- 10MB 是服务端文件大小保护；前端 60 秒限制是课程短语音识别的业务限制。
- 当前实现是百度短语音识别，不是长音频异步转写；超过 60 秒应拆分或重新录制。

## 官方参考

- [百度语音识别 Node.js SDK](https://cloud.baidu.com/doc/SPEECH/s/dlbxg1cvw)
- [百度短语音识别 REST API](https://cloud.baidu.com/doc/SPEECH/s/Jlbxdezuf)
- [百度音频文件转码要求](https://cloud.baidu.com/doc/SPEECH/s/7k38lxpwf)
