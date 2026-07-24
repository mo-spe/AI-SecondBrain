package com.secondbrain.enums;

/**
 * AI使用场景枚举.
 * <p>定义系统支持的五大AI调用场景</p>
 */
public enum AiScenario {

    /**
     * 对话
     */
    CHAT("chat", "对话"),

    /**
     * 知识提取
     */
    EXTRACTION("extraction", "知识提取"),

    /**
     * 题目生成
     */
    QUESTION_GEN("question_gen", "题目生成"),

    /**
     * Embedding向量化
     */
    EMBEDDING("embedding", "Embedding向量化"),

    /**
     * 研究报告
     */
    RESEARCH("research", "研究报告");

    private final String code;
    private final String label;

    AiScenario(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据场景代码查找枚举.
     *
     * @param code 场景代码
     * @return 对应的枚举值，未匹配时返回 null
     */
    public static AiScenario fromCode(String code) {
        for (AiScenario scenario : values()) {
            if (scenario.code.equals(code)) {
                return scenario;
            }
        }
        return null;
    }
}
