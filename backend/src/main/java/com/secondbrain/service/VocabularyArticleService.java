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
}
