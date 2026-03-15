package com.secondbrain.controller;

import com.secondbrain.common.Result;
import com.secondbrain.dto.AsyncTaskResponse;
import com.secondbrain.service.AsyncTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/async-task")
@Tag(name = "异步任务", description = "异步任务管理")
public class AsyncTaskController {

    @Autowired
    private AsyncTaskService asyncTaskService;

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