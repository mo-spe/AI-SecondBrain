package com.secondbrain.controller;

import com.secondbrain.dto.KnowledgeDTO;
import com.secondbrain.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    @Autowired
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/test-extract")
    public Map<String, Object> testExtractKnowledge(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String content = request.get("content");
            System.out.println("开始测试AI知识点提取，内容长度：" + content.length());
            
            List<KnowledgeDTO> knowledgeList = aiService.extractKnowledge(content);
            
            System.out.println("AI知识点提取完成，提取到" + knowledgeList.size() + "个知识点");
            
            result.put("success", true);
            result.put("data", knowledgeList);
            result.put("count", knowledgeList.size());
        } catch (Exception e) {
            System.out.println("AI知识点提取失败：" + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
