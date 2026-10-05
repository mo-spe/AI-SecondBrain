package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.VocabularyArticleDtos;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.service.VocabularyArticleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 用户私有的截图词表提取与语境文章接口。 */
@RestController
@RequestMapping("/vocabulary/articles")
public class VocabularyArticleController {
    private final VocabularyArticleService service;

    /**
     * 构造词表文章接口。
     *
     * @param service 词表提取与文章服务
     */
    public VocabularyArticleController(VocabularyArticleService service) {
        this.service = service;
    }

    /**
     * 让客户端在上传前检查个人视觉配置，不暴露密钥。
     *
     * @param request 登录请求上下文
     * @return 视觉识别状态与配置问题说明
     */
    @GetMapping("/vision-ready")
    public Result<VocabularyArticleDtos.VisionReadiness> visionReady(HttpServletRequest request) {
        return Result.success(service.visionReady(userId(request)));
    }

    /**
     * 用户主动上传截图，得到尚待确认的英文词候选。
     *
     * @param files 单张或多张截图
     * @param request 登录请求上下文
     * @return 去重候选词
     */
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<VocabularyArticleDtos.Extraction> extract(@RequestParam("files") List<MultipartFile> files,
                                                             HttpServletRequest request) {
        return Result.success(service.extract(files, userId(request)));
    }

    /**
     * 根据用户确认的词表生成并保存文章。
     *
     * @param body 词表与偏好
     * @param request 登录请求上下文
     * @return 文章及程序校验结果
     */
    @PostMapping
    public Result<VocabularyArticleDtos.ArticleView> generate(
            @RequestBody VocabularyArticleDtos.GenerateRequest body, HttpServletRequest request) {
        return Result.success(service.generate(body, userId(request)));
    }

    /**
     * 查询当前用户的历史文章。
     *
     * @param request 登录请求上下文
     * @return 最近文章
     */
    @GetMapping
    public Result<List<VocabularyArticleDtos.ArticleSummary>> list(HttpServletRequest request) {
        return Result.success(service.list(userId(request)));
    }

    /**
     * 查询当前用户的指定文章。
     *
     * @param id 文章标识
     * @param request 登录请求上下文
     * @return 私有文章
     */
    @GetMapping("/{id}")
    public Result<VocabularyArticleDtos.ArticleView> get(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(service.get(id, userId(request)));
    }

    private Long userId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "请先登录");
        return userId;
    }
}
