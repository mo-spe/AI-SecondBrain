package com.secondbrain.controller;

import com.secondbrain.service.DeerFlowResearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** 测试控制器. <p>提供系统连接测试等接口</p> */
@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final DeerFlowResearchService deerFlowResearchService;

    public TestController(DeerFlowResearchService deerFlowResearchService) {
        this.deerFlowResearchService = deerFlowResearchService;
    }

    /**
     * 测试DeerFlow连接
     *
     * @return 连接测试结果
     */
    @GetMapping("/deerflow-connection")
    public Map<String, Object> testDeerFlowConnection() {
        Map<String, Object> result = new HashMap<>();
        
        boolean isHealthy = deerFlowResearchService.checkHealth();
        result.put("success", true);
        result.put("healthy", isHealthy);
        result.put("message", isHealthy ? "DeerFlow API连接正常" : "DeerFlow API连接失败");
        
        return result;
    }
}
