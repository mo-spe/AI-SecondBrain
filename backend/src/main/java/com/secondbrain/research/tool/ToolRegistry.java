package com.secondbrain.research.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心.
 *
 * <p>自动收集所有 Tool 实现 Bean，提供按名称查找和 LLM function calling schema 生成。</p>
 *
 * @author AI
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolBeans) {
        for (Tool tool : toolBeans) {
            tools.put(tool.getName(), tool);
            log.info("tool_registered name={}", tool.getName());
        }
    }

    /**
     * 按名称获取工具.
     *
     * @param name 工具名称
     * @return 工具实例，不存在返回 null
     */
    public Tool get(String name) {
        return tools.get(name);
    }

    /**
     * 获取所有已注册工具.
     *
     * @return 工具集合
     */
    public Collection<Tool> getAll() {
        return tools.values();
    }

    /**
     * 生成 LLM function calling 所需的工具定义列表.
     *
     * @return function definitions
     */
    public List<Map<String, Object>> generateFunctionDefinitions() {
        return tools.values().stream()
                .map(tool -> Map.of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "parameters", tool.getInputSchema()))
                .toList();
    }
}
