package com.secondbrain.service;

import com.secondbrain.dto.VocabularyArticleDtos;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 管理用户私有的截图词表提取和语境文章。 */
public interface VocabularyArticleService {
    /**
     * 检查个人视觉模型与密钥是否已配置，不读取或返回密钥。
     *
     * @param userId 当前用户
     * @return 可用状态及配置问题说明
     */
    VocabularyArticleDtos.VisionReadiness visionReady(Long userId);

    /**
     * 从用户明确提交的图片中提取待确认词表。
     *
     * @param files 词表截图
     * @param userId 当前用户
     * @return 去重候选词
     */
    VocabularyArticleDtos.Extraction extract(List<MultipartFile> files, Long userId);

    /**
     * 使用用户确认的词表写作并核验覆盖。
     *
     * @param request 词表与写作偏好
     * @param userId 当前用户
     * @return 保存后的文章
     */
    VocabularyArticleDtos.ArticleView generate(VocabularyArticleDtos.GenerateRequest request, Long userId);

    /**
     * 查询当前用户的文章列表。
     *
     * @param userId 当前用户
     * @return 文章列表
     */
    List<VocabularyArticleDtos.ArticleSummary> list(Long userId);

    /**
     * 查询当前用户的一篇文章。
     *
     * @param id 文章标识
     * @param userId 当前用户
     * @return 文章内容
     */
    VocabularyArticleDtos.ArticleView get(Long id, Long userId);

    /**
     * 为当前用户已保存的文章补写尚未覆盖的目标词，并重新核验覆盖。
     *
     * @param id 文章标识
     * @param userId 当前用户
     * @return 更新后的文章与覆盖结果
     */
    VocabularyArticleDtos.ArticleView completeMissing(Long id, Long userId);

    /**
     * 按需翻译当前用户的完整文章，并复用相同正文的缓存译文。
     *
     * @param id 文章标识
     * @param userId 当前用户
     * @return 与当前正文绑定的中文译文
     */
    VocabularyArticleDtos.ArticleTranslation translate(Long id, Long userId);

    /**
     * 按需解释原文中用户点击的英文词，复用已有释义与该次出现的缓存。
     *
     * @param id 文章标识
     * @param request 原文版本及单词位置
     * @param userId 当前用户
     * @return 当前原句中的中文释义
     */
    VocabularyArticleDtos.WordMeaning wordMeaning(Long id, VocabularyArticleDtos.WordMeaningRequest request,
                                                 Long userId);
}
