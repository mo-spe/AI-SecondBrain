package com.secondbrain.research.tool;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行结果.
 *
 * @author AI
 */
@Getter
@Setter
public class ToolResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 结果数据
     */
    private String data;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 是否可重试
     */
    private boolean retryable;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 附加元数据
     */
    private Map<String, Object> metadata = new HashMap<>();

    public static ToolResult success(String data) {
        ToolResult result = new ToolResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    public static ToolResult failure(String errorCode, String errorMessage, boolean retryable) {
        ToolResult result = new ToolResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setRetryable(retryable);
        return result;
    }
}
