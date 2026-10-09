package com.secondbrain.dto;

import java.util.List;
import java.util.Map;

/** 词表图片提取与文章阅读接口对象。 */
public final class VocabularyArticleDtos {
    private VocabularyArticleDtos() {
    }

    /** 图片中的候选词，尚未经用户确认。 */
    public record CandidateWord(String word, int sourceImageIndex, boolean uncertain) {
    }

    /** 模型提取后的去重候选词。 */
    public record Extraction(List<CandidateWord> words) {
    }

    /** 仅返回可用状态与可操作的说明，不暴露个人密钥。 */
    public record VisionReadiness(boolean ready, String message) {
    }

    /** 用户确认词表后的生成参数。 */
    public record GenerateRequest(List<String> words, String topic, String difficulty) {
    }

    /** 带程序核验结果的已保存文章。 */
    public record ArticleView(Long id, List<String> words, String article,
                              Map<String, String> meanings, List<String> missingWords,
                              String topic, String difficulty, String createTime) {
    }

    /** 历史列表只传摘要，避免一次下载多篇完整文章。 */
    public record ArticleSummary(Long id, String topic, String difficulty,
                                 int wordCount, int coveredCount, String createTime) {
    }

    /**
     * 全文译文绑定正文指纹，防止补充阅读更新后展示旧翻译。
     *
     * @param articleId 所属文章标识
     * @param sourceHash 原文的 SHA-256 指纹
     * @param translation 保留段落顺序的完整中文译文
     */
    public record ArticleTranslation(Long articleId, String sourceHash, String translation) {
    }

    /**
     * 用正文位置定位实际点击的词，避免客户端提交任意待解释文本。
     *
     * @param sourceHash 点击时原文的 SHA-256 指纹
     * @param start 单词在原文中的 UTF-16 起始位置
     * @param end 单词的 UTF-16 结束位置，不包含该位置
     */
    public record WordMeaningRequest(String sourceHash, Integer start, Integer end) {
    }

    /**
     * 将语境释义绑定到原文中的一次具体出现。
     *
     * @param articleId 所属文章标识
     * @param sourceHash 原文的 SHA-256 指纹
     * @param start 原文中的起始位置
     * @param end 原文中的结束位置
     * @param word 点击的原始词形
     * @param meaning 简短中文语境释义
     * @param sentence 该次出现对应的原句或邻近片段
     */
    public record WordMeaning(Long articleId, String sourceHash, int start, int end,
                              String word, String meaning, String sentence) {
    }
}
