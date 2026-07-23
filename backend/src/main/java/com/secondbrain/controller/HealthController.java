package com.secondbrain.controller;

import com.secondbrain.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** 健康检查控制器. <p>系统健康检查接口</p> */
@RestController
@RequestMapping("/health")
@Tag(name = "健康检查", description = "系统健康检查接口")
public class HealthController {

    /**
     * 健康检查.
     *
     * @return 系统健康状态信息
     */
    @GetMapping
    @Operation(summary = "健康检查", description = "检查系统运行状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now());
        data.put("service", "AI-SecondBrain");
        return Result.success(data);
    }
}
