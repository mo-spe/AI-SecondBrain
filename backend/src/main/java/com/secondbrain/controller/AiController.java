package com.secondbrain.controller;

import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI服务控制器
 *
 * 提供AI相关功能的测试和调用接口，包括知识点提取等。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 测试AI知识点提取接口
     *
     * @param request 包含content字段的请求体
     * @return 提取的知识点列表及状态
     */
    @PostMapping("/test-extract")
    public Map<String, Object> testExtractKnowledge(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        log.info("开始测试AI知识点提取，内容长度：{}", content.length());

        List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(content);
        log.info("AI知识点提取完成，提取到{}个知识点", knowledgeList.size());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", knowledgeList);
        result.put("count", knowledgeList.size());
        return result;
    }
}
