package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.service.AsyncTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/** 异步任务控制器. <p>提供异步任务状态查询等管理接口</p> */
@RestController
@RequestMapping("/async-task")
@Tag(name = "异步任务", description = "异步任务管理")
public class AsyncTaskController {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskController.class);

    private final AsyncTaskService asyncTaskService;

    public AsyncTaskController(AsyncTaskService asyncTaskService) {
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * 查询任务状态
     *
     * @param taskNumber 任务编号
     * @param httpRequest HTTP请求
     * @return 任务状态信息
     */
    @GetMapping("/status/{taskNumber}")
    @Operation(summary = "查询任务状态", description = "根据任务编号查询任务状态")
    public Result<AsyncTaskResponse> getTaskStatus(
            @PathVariable String taskNumber,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        AsyncTaskResponse response = asyncTaskService.getTaskStatus(taskNumber);
        
        if (response == null) {
            return Result.error("任务不存在");
        }
        
        return Result.success(response);
    }
}
