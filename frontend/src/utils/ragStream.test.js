import test from "node:test";
import assert from "node:assert/strict";
import { createSseParser, streamRagAnswer } from "./ragStream.js";

const response = (chunks) => new Response(new ReadableStream({ start(controller) {
  for (const chunk of chunks) controller.enqueue(new TextEncoder().encode(chunk));
  controller.close();
} }), { headers: { "Content-Type": "text/event-stream" } });

test("SSE preserves whitespace, multiline data and chunk boundaries", () => {
  const events = [];
  const parse = createSseParser((name, data) => events.push([name, data]));
  parse(":ping\r\nevent: token\r\ndata:  a \r");
  parse("\ndata: b\r\n\r");
  parse("\ndata: next\n\n");
  assert.deepEqual(events, [["token", " a \nb"], ["message", "next"]]);
});

test("done finishes without waiting for server to close connection", async () => {
  let cancelled = false;
  const events = [];
  const body = new ReadableStream({ start(controller) {
    controller.enqueue(new TextEncoder().encode("event: token\ndata: answer \n\nevent: done\ndata: completed\n\n"));
  }, cancel() { cancelled = true; } });
  await streamRagAnswer({ question: "Redis" }, { token: "fixture", fetchImpl: async () => new Response(body, { headers: { "Content-Type": "text/event-stream" } }), onEvent: (...args) => events.push(args) });
  assert.equal(cancelled, true);
  assert.deepEqual(events[0], ["token", "answer "]);
});

test("SSE errors reject visibly and partial answers remain available", async () => {
  const events = [];
  await assert.rejects(streamRagAnswer({}, { token: "fixture", fetchImpl: async () => response(["event: token\ndata: partial\n\nevent: error\ndata: 服务暂时不可用\n\n"]), onEvent: (...args) => events.push(args) }), /服务暂时不可用/);
  assert.deepEqual(events, [["token", "partial"]]);
});

test("EOF without done is a failure", async () => {
  await assert.rejects(streamRagAnswer({}, { fetchImpl: async () => response(["event: token\ndata: partial\n\n"]), onEvent() {} }), /提前结束/);
});

test("JSON authentication failures are never treated as empty answers", async () => {
  await assert.rejects(streamRagAnswer({}, { fetchImpl: async () => new Response(JSON.stringify({ code: 401, message: "请先登录" }), { headers: { "Content-Type": "application/json" } }), onEvent() {} }), /请先登录/);
});

test("waiting for response headers is bounded", async () => {
  await assert.rejects(streamRagAnswer({}, { idleTimeoutMs: 10, fetchImpl: (_, { signal }) => new Promise((resolve, reject) => signal.addEventListener("abort", () => reject(new DOMException("aborted", "AbortError")))), onEvent() {} }), /等待超时/);
});

test("caller cancellation aborts the request", async () => {
  const controller = new AbortController();
  let entered;
  const ready = new Promise(resolve => { entered = resolve; });
  const request = streamRagAnswer({}, { signal: controller.signal, fetchImpl: (_, { signal }) => new Promise((resolve, reject) => {
    signal.addEventListener("abort", () => reject(new DOMException("aborted", "AbortError")));
    entered();
  }), onEvent() {} });
  await ready;
  controller.abort();
  await assert.rejects(request, { name: "AbortError" });
});
