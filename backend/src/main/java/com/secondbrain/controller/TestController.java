package com.secondbrain.controller;

import com.secondbrain.service.DeerFlowResearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private DeerFlowResearchService deerFlowResearchService;

    @GetMapping("/deerflow-connection")
    public Map<String, Object> testDeerFlowConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isHealthy = deerFlowResearchService.checkHealth();
            result.put("success", true);
            result.put("healthy", isHealthy);
            result.put("message", isHealthy ? "DeerFlow API连接正常" : "DeerFlow API连接失败");
        } catch (Exception e) {
            log.error("测试DeerFlow连接失败", e);
            result.put("success", false);
            result.put("healthy", false);
            result.put("message", "DeerFlow API连接失败：" + e.getMessage());
            result.put("error", e.getClass().getName());
        }
        
        return result;
    }
}
