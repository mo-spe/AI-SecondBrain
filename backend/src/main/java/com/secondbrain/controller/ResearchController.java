package com.secondbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.common.Result;
import com.secondbrain.dto.CreateResearchProjectRequest;
import com.secondbrain.dto.CreateResearchTaskRequest;
import com.secondbrain.dto.UpdateResearchProjectRequest;
import com.secondbrain.dto.UpdateResearchTaskRequest;
import com.secondbrain.service.ResearchMemoryService;
import com.secondbrain.service.ResearchPlanService;
import com.secondbrain.service.ResearchProjectService;
import com.secondbrain.service.ResearchReportService;
import com.secondbrain.service.ResearchSourceService;
import com.secondbrain.service.ResearchStepService;
import com.secondbrain.service.ResearchTaskService;
import com.secondbrain.vo.ResearchPlanVO;
import com.secondbrain.vo.ResearchProjectVO;
import com.secondbrain.vo.ResearchReportVO;
import com.secondbrain.vo.ResearchSourceVO;
import com.secondbrain.vo.ResearchStepVO;
import com.secondbrain.vo.ResearchTaskVO;
import com.secondbrain.entity.ResearchMemory;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 研究项目管理控制器.
 *
 * <p>提供研究项目的 CRUD 及生命周期管理 API。</p>
 *
 * @author AI
 */
@RestController
@RequestMapping("/research/projects")
@Tag(name = "AI 研究", description = "AI Research Agent 研究项目管理")
public class ResearchController {

    private final ResearchProjectService researchProjectService;
    private final ResearchTaskService researchTaskService;
    private final ResearchPlanService researchPlanService;
    private final ResearchSourceService researchSourceService;
    private final ResearchStepService researchStepService;
    private final ResearchReportService researchReportService;
    private final ResearchMemoryService researchMemoryService;

    public ResearchController(ResearchProjectService researchProjectService,
                              ResearchTaskService researchTaskService,
                              ResearchPlanService researchPlanService,
                              ResearchSourceService researchSourceService,
                              ResearchStepService researchStepService,
                              ResearchReportService researchReportService,
                              ResearchMemoryService researchMemoryService) {
        this.researchProjectService = researchProjectService;
        this.researchTaskService = researchTaskService;
        this.researchPlanService = researchPlanService;
        this.researchSourceService = researchSourceService;
        this.researchStepService = researchStepService;
        this.researchReportService = researchReportService;
        this.researchMemoryService = researchMemoryService;
    }

    /**
     * 获取用户ID，未登录时抛出异常.
     */
    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new com.secondbrain.exception.BusinessException(401, "请先登录");
        }
        return userId;
    }

    /**
     * 获取工作空间ID（可为null）.
     */
    private Long getWorkspaceId(HttpServletRequest request) {
        return (Long) request.getAttribute("workspaceId");
    }

    @PostMapping
    @Operation(summary = "创建研究项目")
    public Result<ResearchProjectVO> create(@Valid @RequestBody CreateResearchProjectRequest request,
                                             HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchProjectVO vo = researchProjectService.create(request, userId);
        return Result.success("创建成功", vo);
    }

    @GetMapping
    @Operation(summary = "查询研究项目列表")
    public Result<Page<ResearchProjectVO>> list(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "标题关键词") @RequestParam(required = false) String keyword,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        Page<ResearchProjectVO> page = researchProjectService.list(current, size, status, keyword,
                userId, workspaceId);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询研究项目详情")
    public Result<ResearchProjectVO> getById(
            @Parameter(description = "项目ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        ResearchProjectVO vo = researchProjectService.getById(id, userId, workspaceId);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新研究项目")
    public Result<ResearchProjectVO> update(
            @Parameter(description = "项目ID") @PathVariable Long id,
            @Valid @RequestBody UpdateResearchProjectRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        ResearchProjectVO vo = researchProjectService.update(id, request, userId, workspaceId);
        return Result.success("更新成功", vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除研究项目")
    public Result<Void> delete(
            @Parameter(description = "项目ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        researchProjectService.delete(id, userId, workspaceId);
        return Result.success("已删除");
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "启动研究")
    public Result<ResearchProjectVO> execute(
            @Parameter(description = "项目ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        ResearchProjectVO vo = researchProjectService.execute(id, userId, workspaceId);
        return Result.success("研究已启动", vo);
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停研究")
    public Result<Void> pause(
            @Parameter(description = "项目ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        researchProjectService.pause(id, userId, workspaceId);
        return Result.success("已暂停");
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复研究")
    public Result<ResearchProjectVO> resume(
            @Parameter(description = "项目ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        ResearchProjectVO vo = researchProjectService.resume(id, userId, workspaceId);
        return Result.success("已恢复", vo);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档研究")
    public Result<Void> archive(
            @Parameter(description = "项目ID") @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Long workspaceId = getWorkspaceId(httpRequest);
        researchProjectService.archive(id, userId, workspaceId);
        return Result.success("已归档");
    }

    // ======================== 研究任务 ========================

    @GetMapping("/{projectId}/tasks")
    @Operation(summary = "查询项目下的任务列表")
    public Result<List<ResearchTaskVO>> listTasks(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchTaskVO> tasks = researchTaskService.listByProject(projectId, userId);
        return Result.success(tasks);
    }

    @GetMapping("/{projectId}/tasks/{taskId}")
    @Operation(summary = "查询任务详情")
    public Result<ResearchTaskVO> getTask(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchTaskVO vo = researchTaskService.getById(taskId, projectId, userId);
        return Result.success(vo);
    }

    @PostMapping("/{projectId}/tasks")
    @Operation(summary = "创建研究任务")
    public Result<ResearchTaskVO> createTask(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Valid @RequestBody CreateResearchTaskRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchTaskVO vo = researchTaskService.create(projectId, request, userId);
        return Result.success("创建成功", vo);
    }

    @PutMapping("/{projectId}/tasks/{taskId}")
    @Operation(summary = "更新研究任务")
    public Result<ResearchTaskVO> updateTask(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Valid @RequestBody UpdateResearchTaskRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchTaskVO vo = researchTaskService.update(taskId, projectId, request, userId);
        return Result.success("更新成功", vo);
    }

    @DeleteMapping("/{projectId}/tasks/{taskId}")
    @Operation(summary = "删除研究任务")
    public Result<Void> deleteTask(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        researchTaskService.delete(taskId, projectId, userId);
        return Result.success("已删除");
    }

    @PostMapping("/{projectId}/tasks/batch")
    @Operation(summary = "批量创建研究任务")
    public Result<List<ResearchTaskVO>> batchCreateTasks(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Valid @RequestBody List<CreateResearchTaskRequest> requests,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchTaskVO> tasks = researchTaskService.batchCreate(projectId, requests, userId);
        return Result.success("批量创建成功", tasks);
    }

    // ======================== 研究计划 ========================

    @GetMapping("/{projectId}/plans/latest")
    @Operation(summary = "获取项目最新研究计划")
    public Result<ResearchPlanVO> getLatestPlan(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchPlanVO vo = researchPlanService.getLatestByProject(projectId, userId);
        return Result.success(vo);
    }

    @GetMapping("/{projectId}/plans")
    @Operation(summary = "查询项目所有研究计划")
    public Result<List<ResearchPlanVO>> listPlans(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchPlanVO> plans = researchPlanService.listByProject(projectId, userId);
        return Result.success(plans);
    }

    @GetMapping("/{projectId}/plans/{planId}")
    @Operation(summary = "查询研究计划详情")
    public Result<ResearchPlanVO> getPlan(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "计划ID") @PathVariable Long planId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchPlanVO vo = researchPlanService.getById(planId, userId);
        return Result.success(vo);
    }

    @PostMapping("/{projectId}/plans")
    @Operation(summary = "创建研究计划（由 Planner Agent 调用）")
    public Result<ResearchPlanVO> createPlan(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        String complexity = (String) body.get("complexity");
        String agentChain = (String) body.get("agentChain");
        String tasksJson = (String) body.get("tasksJson");
        String rationale = (String) body.get("rationale");
        Integer estimatedTokens = body.get("estimatedTokens") != null
                ? ((Number) body.get("estimatedTokens")).intValue() : null;
        String createdBy = (String) body.get("createdBy");
        ResearchPlanVO vo = researchPlanService.createPlan(projectId, complexity, agentChain,
                tasksJson, rationale, estimatedTokens, createdBy, userId);
        return Result.success("创建成功", vo);
    }

    // ======================== 研究来源 ========================

    @GetMapping("/{projectId}/sources")
    @Operation(summary = "查询项目下的研究来源")
    public Result<List<ResearchSourceVO>> listSources(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchSourceVO> sources = researchSourceService.listByProject(projectId, userId);
        return Result.success(sources);
    }

    @GetMapping("/{projectId}/sources/{sourceId}")
    @Operation(summary = "查询研究来源详情")
    public Result<ResearchSourceVO> getSource(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "来源ID") @PathVariable Long sourceId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchSourceVO vo = researchSourceService.getById(sourceId, userId);
        return Result.success(vo);
    }

    @DeleteMapping("/{projectId}/sources/{sourceId}")
    @Operation(summary = "删除研究来源")
    public Result<Void> deleteSource(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "来源ID") @PathVariable Long sourceId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        researchSourceService.delete(sourceId, userId);
        return Result.success("已删除");
    }

    // ======================== 研究步骤 ========================

    @GetMapping("/{projectId}/tasks/{taskId}/steps")
    @Operation(summary = "查询任务执行步骤")
    public Result<List<ResearchStepVO>> listSteps(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchStepVO> steps = researchStepService.listByTask(taskId, userId);
        return Result.success(steps);
    }

    @GetMapping("/{projectId}/steps")
    @Operation(summary = "查询项目下所有执行步骤")
    public Result<List<ResearchStepVO>> listProjectSteps(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchStepVO> steps = researchStepService.listByProject(projectId, userId);
        return Result.success(steps);
    }

    // ======================== 研究报告 ========================

    @GetMapping("/{projectId}/report")
    @Operation(summary = "获取项目最新研究报告")
    public Result<ResearchReportVO> getLatestReport(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        ResearchReportVO vo = researchReportService.getLatestByProject(projectId, userId);
        return Result.success(vo);
    }

    @GetMapping("/{projectId}/reports")
    @Operation(summary = "获取项目所有研究报告版本")
    public Result<List<ResearchReportVO>> listReports(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchReportVO> reports = researchReportService.listByProject(projectId, userId);
        return Result.success(reports);
    }

    // ======================== 研究记忆 ========================

    @GetMapping("/{projectId}/memory")
    @Operation(summary = "查询项目下所有研究记忆")
    public Result<List<ResearchMemory>> listMemory(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchMemory> memories = researchMemoryService.listByProject(projectId, userId);
        return Result.success(memories);
    }

    @GetMapping("/{projectId}/memory/{type}")
    @Operation(summary = "按类型查询研究记忆")
    public Result<List<ResearchMemory>> listMemoryByType(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "记忆类型") @PathVariable String type,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        List<ResearchMemory> memories = researchMemoryService.listByType(projectId, userId, type);
        return Result.success(memories);
    }

    /**
     * 将研究知识候选保存到当前项目所属工作区的知识库。
     *
     * @param projectId 项目ID
     * @param memoryId 候选记忆ID
     * @param httpRequest HTTP请求
     * @return 保存结果
     */
    @PostMapping("/{projectId}/memory/{memoryId}/accept")
    @Operation(summary = "保存研究知识候选")
    public Result<Void> acceptCandidate(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "候选记忆ID") @PathVariable Long memoryId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        researchMemoryService.acceptCandidate(projectId, memoryId, userId);
        return Result.success("知识已保存到知识库", null);
    }

    /**
     * 持久化忽略研究知识候选，使其刷新后不再出现。
     *
     * @param projectId 项目ID
     * @param memoryId 候选记忆ID
     * @param httpRequest HTTP请求
     * @return 忽略结果
     */
    @DeleteMapping("/{projectId}/memory/{memoryId}/candidate")
    @Operation(summary = "忽略研究知识候选")
    public Result<Void> dismissCandidate(
            @Parameter(description = "项目ID") @PathVariable Long projectId,
            @Parameter(description = "候选记忆ID") @PathVariable Long memoryId,
            HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        researchMemoryService.dismissCandidate(projectId, memoryId, userId);
        return Result.success("知识候选已忽略", null);
    }
}
