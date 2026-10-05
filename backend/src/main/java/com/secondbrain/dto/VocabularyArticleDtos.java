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
}
