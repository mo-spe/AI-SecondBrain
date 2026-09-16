/** 按完整 SSE 事件分帧，保留回答中的空格与多行代码。 */
export function createSseParser(onEvent) {
  let buffer = "";
  let ended = false;
  return (chunk) => {
    if (ended) return;
    buffer += chunk;
    if (buffer.length > 1024 * 1024) throw new Error("回答数据格式异常，请重试");
    let boundary;
    while ((boundary = /\r\n\r\n|\n\n|\r\r/.exec(buffer))) {
      const frame = buffer.slice(0, boundary.index);
      buffer = buffer.slice(boundary.index + boundary[0].length);
      let name = "message";
      const data = [];
      for (const line of frame.split(/\r\n|\n|\r/)) {
        if (line.startsWith(":")) continue;
        const separator = line.indexOf(":");
        const field = separator < 0 ? line : line.slice(0, separator);
        const value = separator < 0 ? "" : line.slice(separator + 1).replace(/^ /, "");
        if (field === "event") name = value || "message";
        if (field === "data") data.push(value);
      }
      if (data.length && onEvent(name, data.join("\n")) === false) {
        ended = true;
        return;
      }
    }
  };
}

/** 同时约束等待响应头和读取正文的时间，结束或离开页面时释放连接。 */
export async function streamRagAnswer(payload, { token, signal, onEvent, idleTimeoutMs = 90_000, fetchImpl = fetch }) {
  const controller = new AbortController();
  let timer;
  let timedOut = false;
  let reader;
  let completed = false;
  const abort = () => controller.abort();
  const resetTimer = () => {
    clearTimeout(timer);
    timer = setTimeout(() => { timedOut = true; controller.abort(); }, idleTimeoutMs);
  };
  signal?.addEventListener("abort", abort, { once: true });
  if (signal?.aborted) abort();
  resetTimer();
  try {
    const response = await fetchImpl("/api/rag/answer/stream", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "text/event-stream", Authorization: `Bearer ${token}` },
      body: JSON.stringify(payload), signal: controller.signal,
    });
    if (!response.ok || !response.headers.get("content-type")?.includes("text/event-stream")) {
      let message = response.status === 401 ? "登录已过期，请重新登录" : "问答服务未返回有效回答，请稍后重试";
      try { const result = await response.json(); message = result.message || result.msg || message; } catch { /* 非 JSON 错误页不作为答案显示。 */ }
      throw new Error(message);
    }
    if (!response.body) throw new Error("浏览器无法读取流式回答，请重试");
    reader = response.body.getReader();
    const decoder = new TextDecoder();
    const parse = createSseParser((name, data) => {
      if (name === "error") throw new Error(data || "回答生成失败，请重试");
      onEvent(name, data);
      if (name === "done") { completed = true; return false; }
    });
    while (!completed) {
      const { done, value } = await reader.read();
      if (done) break;
      resetTimer();
      parse(decoder.decode(value, { stream: true }));
    }
    if (!completed) throw new Error("回答连接提前结束，已保留收到的内容，请重试");
  } catch (error) {
    if (timedOut) throw new Error("回答等待超时，已保留收到的内容，请稍后重试");
    throw error;
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", abort);
    controller.abort();
    if (reader) { await reader.cancel().catch(() => {}); reader.releaseLock(); }
  }
}
