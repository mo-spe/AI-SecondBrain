package com.secondbrain.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.common.Result;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.dto.DeerFlowResearchRequest;
import com.secondbrain.dto.DeerFlowResearchResponse;
import com.secondbrain.dto.ResearchHistoryRequest;
import com.secondbrain.entity.ResearchHistory;
import com.secondbrain.service.DeerFlowResearchService;
import com.secondbrain.service.ResearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AI知识研究", description = "AI知识研究接口")
public class DeerFlowResearchController {

    private static final Logger log = LoggerFactory.getLogger(DeerFlowResearchController.class);

    @Autowired
    private DeerFlowResearchService deerFlowResearchService;

    @Autowired
    private ResearchHistoryService researchHistoryService;

    @Autowired
    private com.secondbrain.service.UserService userService;

    @PostMapping("/research/learning-report-async")
    @Operation(summary = "异步生成学习报告", description = "异步生成学习报告，返回任务ID")
    public Result<AsyncTaskResponse> generateLearningReportAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                userId = 1L;
                log.warn("无法获取用户ID，使用默认用户ID：{}", userId);
            }
            
            String topic = request.getTopic();
            if (topic == null || topic.isEmpty()) {
                topic = request.getLearningData();
            }
            if (topic == null || topic.isEmpty()) {
                topic = request.getGoal();
            }
            
            String depth = request.getDepth();
            if (depth == null || depth.isEmpty()) {
                depth = "medium";
            }
            
            log.info("收到异步生成学习报告请求，用户ID：{}，主题：{}，深度：{}", userId, topic, depth);
            
            String userApiKey = null;
            try {
                com.secondbrain.entity.User user = userService.getUserById(userId);
                if (user != null) {
                    userApiKey = user.getApiKey();
                }
            } catch (Exception e) {
                log.warn("获取用户API Key失败：{}", e.getMessage());
            }
            
            AsyncTaskResponse task = deerFlowResearchService.generateLearningReportAsync(
                userId,
                topic,
                depth,
                userApiKey
            );
            
            return Result.success("任务已创建，请使用任务ID查询进度", task);
        } catch (Exception e) {
            log.error("异步生成学习报告失败", e);
            return Result.error("异步生成学习报告失败：" + e.getMessage());
        }
    }

    @PostMapping("/research/learning-report")
    public ResponseEntity<Map<String, Object>> generateLearningReport(@RequestBody DeerFlowResearchRequest request) {
        try {
            log.info("收到生成学习报告请求，主题：{}", request.getTopic());

            String report = deerFlowResearchService.generateDeepLearningReport(
                request.getLearningData(),
                request.getTopic(),
                request.getDepth(),
                null
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
                request.getTargetLevel(),
                null
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

    @PostMapping("/research/learning-path-async")
    @Operation(summary = "异步生成学习路径", description = "异步生成学习路径，返回任务ID")
    public Result<AsyncTaskResponse> generateLearningPathAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                userId = 1L;
                log.warn("无法获取用户ID，使用默认用户ID：{}", userId);
            }
            
            String topic = request.getTopic();
            if (topic == null || topic.isEmpty()) {
                topic = request.getLearningData();
            }
            if (topic == null || topic.isEmpty()) {
                topic = request.getGoal();
            }
            
            String currentLevel = request.getCurrentLevel();
            if (currentLevel == null || currentLevel.isEmpty()) {
                currentLevel = "beginner";
            }
            
            String targetLevel = request.getTargetLevel();
            if (targetLevel == null || targetLevel.isEmpty()) {
                targetLevel = "advanced";
            }
            
            log.info("收到异步生成学习路径请求，用户ID：{}，主题：{}，当前水平：{}，目标水平：{}", userId, topic, currentLevel, targetLevel);
            
            String userApiKey = null;
            try {
                com.secondbrain.entity.User user = userService.getUserById(userId);
                if (user != null) {
                    userApiKey = user.getApiKey();
                }
            } catch (Exception e) {
                log.warn("获取用户API Key失败：{}", e.getMessage());
            }
            
            AsyncTaskResponse task = deerFlowResearchService.generateLearningPathAsync(
                userId,
                topic,
                currentLevel,
                targetLevel,
                userApiKey
            );
            
            return Result.success("任务已创建，请使用任务ID查询进度", task);
        } catch (Exception e) {
            log.error("异步生成学习路径失败", e);
            return Result.error("异步生成学习路径失败：" + e.getMessage());
        }
    }

    @PostMapping("/research/knowledge-gap")
    public ResponseEntity<Map<String, Object>> researchKnowledgeGap(@RequestBody DeerFlowResearchRequest request) {
        try {
            log.info("收到知识盲区分析请求，目标主题：{}", request.getTopic());

            String gapAnalysis = deerFlowResearchService.researchKnowledgeGap(
                request.getUserKnowledge(),
                request.getTopic(),
                null
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

    @PostMapping("/research/knowledge-gap-async")
    @Operation(summary = "异步分析知识盲区", description = "异步分析知识盲区，返回任务ID")
    public Result<AsyncTaskResponse> researchKnowledgeGapAsync(
            @RequestBody DeerFlowResearchRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                userId = 1L;
                log.warn("无法获取用户ID，使用默认用户ID：{}", userId);
            }
            
            log.info("收到异步知识盲区分析请求，用户ID：{}，目标主题：{}", userId, request.getTopic());
            
            String userApiKey = null;
            try {
                com.secondbrain.entity.User user = userService.getUserById(userId);
                if (user != null) {
                    userApiKey = user.getApiKey();
                }
            } catch (Exception e) {
                log.warn("获取用户API Key失败：{}", e.getMessage());
            }
            
            AsyncTaskResponse task = deerFlowResearchService.researchKnowledgeGapAsync(
                userId,
                request.getUserKnowledge(),
                request.getTopic(),
                userApiKey
            );
            
            return Result.success("任务已创建，请使用任务ID查询进度", task);
        } catch (Exception e) {
            log.error("异步知识盲区分析失败", e);
            return Result.error("异步知识盲区分析失败：" + e.getMessage());
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查DeerFlow研究服务健康状态")
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
            @RequestParam(required = false) String type,
            HttpServletRequest httpRequest) {
        try {
            log.info("收到获取研究历史列表请求，当前页：{}，每页大小：{}，类型：{}", current, size, type);

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

            IPage<ResearchHistory> page = researchHistoryService.getList(current, size, userId, type);

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

    @GetMapping("/async-task/status/{taskId}")
    @Operation(summary = "查询异步任务状态", description = "根据任务ID查询异步任务的执行状态和结果")
    public Result<AsyncTaskResponse> getTaskStatus(
            @PathVariable String taskId) {
        try {
            log.info("收到任务状态查询请求，taskNumber：{}", taskId);
            
            AsyncTaskResponse task = deerFlowResearchService.getTaskStatus(taskId);
            
            if (task == null) {
                log.warn("任务不存在，taskNumber：{}", taskId);
                return Result.error("任务不存在");
            }
            
            return Result.success("查询成功", task);
        } catch (Exception e) {
            log.error("查询任务状态失败，taskNumber：{}", taskId, e);
            return Result.error("查询任务状态失败：" + e.getMessage());
        }
    }
}
