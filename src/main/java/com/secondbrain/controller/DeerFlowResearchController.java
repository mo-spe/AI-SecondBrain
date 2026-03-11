package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.dto.DeerFlowResearchRequest;
import com.secondbrain.dto.DeerFlowResearchResponse;
import com.secondbrain.dto.ResearchHistoryRequest;
import com.secondbrain.entity.ResearchHistory;
import com.secondbrain.service.DeerFlowResearchService;
import com.secondbrain.service.ResearchHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/deerflow")
public class DeerFlowResearchController {

    private static final Logger log = LoggerFactory.getLogger(DeerFlowResearchController.class);

    @Autowired
    private DeerFlowResearchService deerFlowResearchService;

    @Autowired
    private ResearchHistoryService researchHistoryService;

    @PostMapping("/research/learning-report")
    public ResponseEntity<Map<String, Object>> generateLearningReport(@RequestBody DeerFlowResearchRequest request) {
        try {
            log.info("收到生成学习报告请求，主题：{}", request.getTopic());

            String report = deerFlowResearchService.generateDeepLearningReport(
                request.getLearningData(),
                request.getTopic(),
                request.getDepth()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "学习报告生成成功");
            response.put("data", report);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成学习报告失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "生成学习报告失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/research/learning-path")
    public ResponseEntity<Map<String, Object>> generateLearningPath(@RequestBody DeerFlowResearchRequest request) {
        try {
            log.info("收到生成学习路径请求，主题：{}", request.getTopic());

            String learningPath = deerFlowResearchService.generateLearningPath(
                request.getTopic(),
                request.getCurrentLevel(),
                request.getTargetLevel()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "学习路径生成成功");
            response.put("data", learningPath);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成学习路径失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "生成学习路径失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/research/knowledge-gap")
    public ResponseEntity<Map<String, Object>> researchKnowledgeGap(@RequestBody DeerFlowResearchRequest request) {
        try {
            log.info("收到知识盲区分析请求，目标主题：{}", request.getTopic());

            String gapAnalysis = deerFlowResearchService.researchKnowledgeGap(
                request.getUserKnowledge(),
                request.getTopic()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "知识盲区分析成功");
            response.put("data", gapAnalysis);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("知识盲区分析失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "知识盲区分析失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            boolean isHealthy = deerFlowResearchService.checkHealth();

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("deerflow_healthy", isHealthy);
            response.put("message", isHealthy ? "DeerFlow研究服务正常" : "DeerFlow研究服务异常");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("健康检查失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("deerflow_healthy", false);
            response.put("message", "健康检查失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/research/history")
    public ResponseEntity<Map<String, Object>> saveResearchHistory(@RequestBody ResearchHistoryRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("收到保存研究历史请求，类型：{}，主题：{}", request.getType(), request.getTopic());

            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 200);
                response.put("message", "未登录，不保存历史记录");
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            ResearchHistory history = researchHistoryService.save(request, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "保存成功");
            response.put("data", history);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("保存研究历史失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "保存失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/research/history")
    public ResponseEntity<Map<String, Object>> getResearchHistoryList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            log.info("收到获取研究历史列表请求，当前页：{}，每页大小：{}", current, size);

            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 200);
                response.put("message", "获取成功");
                
                Map<String, Object> data = new HashMap<>();
                data.put("records", new java.util.ArrayList<>());
                data.put("total", 0);
                data.put("current", current);
                data.put("size", size);
                response.put("data", data);
                
                return ResponseEntity.ok(response);
            }

            IPage<ResearchHistory> page = researchHistoryService.getList(current, size, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", page.getRecords());
            data.put("total", page.getTotal());
            data.put("current", page.getCurrent());
            data.put("size", page.getSize());
            response.put("data", data);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取研究历史列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/research/history/{id}")
    public ResponseEntity<Map<String, Object>> deleteResearchHistory(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            log.info("收到删除研究历史请求，ID：{}", id);

            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 200);
                response.put("message", "未登录，不删除历史记录");
                return ResponseEntity.ok(response);
            }

            researchHistoryService.deleteById(id, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("删除研究历史失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
