package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/** DeerFlow研究响应DTO. <p>用于深度学习研究返回结果</p> */
@Getter
@Setter
public class DeerFlowResearchResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 返回数据
     */
    private String data;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
