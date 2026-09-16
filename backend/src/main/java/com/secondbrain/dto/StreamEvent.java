package com.secondbrain.dto;

/**
 * SSE流式事件 DTO.
 *
 * <p>用于流式AI回答时向SSE端点传递不同类型的事件。
 * 前端根据 type 字段进行事件分发（token → 追加文本, references → 更新引用列表等）</p>
 */
public class StreamEvent {

    public enum Type {
        /** AI生成的文本片段 */
        TOKEN,
        /** 检索到的知识引用（JSON数组） */
        REFERENCES,
        /** 性能指标（JSON对象，含 retrievalTime/generationTime） */
        METRICS,
        /** 流式生成完成 */
        DONE,
        /** 生成失败 */
        ERROR
    }

    private final Type type;
    private final String data;

    private StreamEvent(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public static StreamEvent token(String text) {
        return new StreamEvent(Type.TOKEN, text);
    }

    public static StreamEvent references(String json) {
        return new StreamEvent(Type.REFERENCES, json);
    }

    public static StreamEvent metrics(String json) {
        return new StreamEvent(Type.METRICS, json);
    }

    public static StreamEvent done() {
        return new StreamEvent(Type.DONE, "");
    }

    public static StreamEvent error(String message) {
        return new StreamEvent(Type.ERROR, message);
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }
}
