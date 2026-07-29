package com.secondbrain.research.tool;

import java.util.Map;

/**
 * Agent 工具接口.
 *
 * <p>所有 Agent 可调用的工具必须实现此接口。
 * 工具通过 ToolRegistry 注册，Agent 按名称获取并调用。</p>
 *
 * @author AI
 */
public interface Tool {

    /**
     * 工具唯一名称.
     *
     * @return 工具名称，如 "WebSearchTool"
     */
    String getName();

    /**
     * 工具功能描述（用于 LLM function calling）.
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 工具参数 JSON Schema（用于 LLM function calling）.
     *
     * @return 参数 schema 的 Map 表示
     */
    Map<String, Object> getInputSchema();

    /**
     * 执行工具.
     *
     * @param params 工具参数
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> params);
}
