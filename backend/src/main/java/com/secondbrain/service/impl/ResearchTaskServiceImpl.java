package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.CreateResearchTaskRequest;
import com.secondbrain.dto.UpdateResearchTaskRequest;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.entity.ResearchTask;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.mapper.ResearchTaskMapper;
import com.secondbrain.service.ResearchTaskService;
import com.secondbrain.vo.ResearchTaskVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 研究任务服务实现.
 *
 * @author AI
 */
@Service
public class ResearchTaskServiceImpl implements ResearchTaskService {

    private static final Logger log = LoggerFactory.getLogger(ResearchTaskServiceImpl.class);

    /** 合法的任务状态 */
    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "RUNNING", "COMPLETED", "FAILED", "SKIPPED", "WAITING_USER");

    /** 允许转换到 RUNNING 的状态 */
    private static final Set<String> STARTABLE_STATUSES = Set.of("PENDING", "FAILED", "WAITING_USER");

    private final ResearchTaskMapper researchTaskMapper;
    private final ResearchProjectMapper researchProjectMapper;

    public ResearchTaskServiceImpl(ResearchTaskMapper researchTaskMapper,
                                   ResearchProjectMapper researchProjectMapper) {
        this.researchTaskMapper = researchTaskMapper;
        this.researchProjectMapper = researchProjectMapper;
    }

    @Override
    public List<ResearchTaskVO> listByProject(Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);

        List<ResearchTask> tasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTask>()
                        .eq(ResearchTask::getProjectId, projectId)
                        .orderByAsc(ResearchTask::getSortOrder));

        return tasks.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public ResearchTaskVO getById(Long id, Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);
        ResearchTask task = loadTask(id, projectId);
        return convertToVO(task);
    }

    @Override
    @Transactional
    public ResearchTaskVO create(Long projectId, CreateResearchTaskRequest request, Long userId) {
        checkProjectAccess(projectId, userId);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(400, "任务标题不能为空");
        }
        // 校验依赖的任务存在于同一项目中
        if (request.getDependsOn() != null) {
            validateDependency(projectId, null, request.getDependsOn());
        }

        ResearchTask task = new ResearchTask();
        task.setProjectId(projectId);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setQuestion(request.getQuestion());
        task.setStatus("PENDING");
        task.setDependsOn(request.getDependsOn());
        task.setRequiresExternalSearch(
                request.getRequiresExternalSearch() != null ? (request.getRequiresExternalSearch() ? 1 : 0) : 0);
        task.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);

        researchTaskMapper.insert(task);
        log.info("research_task_created id={} projectId={} title={}", task.getId(), projectId, task.getTitle());
        return convertToVO(task);
    }

    @Override
    @Transactional
    public ResearchTaskVO update(Long id, Long projectId, UpdateResearchTaskRequest request, Long userId) {
        checkProjectAccess(projectId, userId);
        ResearchTask task = loadTask(id, projectId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getQuestion() != null) {
            task.setQuestion(request.getQuestion());
        }
        if (request.getStatus() != null) {
            updateTaskStatus(task, request.getStatus());
        }
        if (request.getDependsOn() != null) {
            validateDependency(projectId, id, request.getDependsOn());
            task.setDependsOn(request.getDependsOn());
        }
        if (request.getRequiresExternalSearch() != null) {
            task.setRequiresExternalSearch(request.getRequiresExternalSearch() ? 1 : 0);
        }
        if (request.getResultSummary() != null) {
            task.setResultSummary(request.getResultSummary());
        }
        if (request.getSortOrder() != null) {
            task.setSortOrder(request.getSortOrder());
        }

        researchTaskMapper.updateById(task);
        log.info("research_task_updated id={} projectId={}", id, projectId);
        return convertToVO(task);
    }

    @Override
    @Transactional
    public void delete(Long id, Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);
        ResearchTask task = loadTask(id, projectId);

        // 清理其他任务对该任务的依赖
        List<ResearchTask> dependents = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTask>()
                        .eq(ResearchTask::getProjectId, projectId)
                        .eq(ResearchTask::getDependsOn, id));
        for (ResearchTask dependent : dependents) {
            dependent.setDependsOn(null);
            researchTaskMapper.updateById(dependent);
            log.info("research_task_dependency_cleared id={} wasDependingOn={}", dependent.getId(), id);
        }

        researchTaskMapper.deleteById(id);
        log.info("research_task_deleted id={} projectId={}", id, projectId);
    }

    @Override
    @Transactional
    public List<ResearchTaskVO> batchCreate(Long projectId, List<CreateResearchTaskRequest> requests, Long userId) {
        checkProjectAccess(projectId, userId);

        // 先创建所有任务（暂不设依赖）
        List<ResearchTask> created = new ArrayList<>();
        Map<Integer, Long> indexToId = new LinkedHashMap<>();

        for (int i = 0; i < requests.size(); i++) {
            CreateResearchTaskRequest req = requests.get(i);
            ResearchTask task = new ResearchTask();
            task.setProjectId(projectId);
            task.setTitle(req.getTitle().trim());
            task.setDescription(req.getDescription());
            task.setQuestion(req.getQuestion());
            task.setStatus("PENDING");
            task.setRequiresExternalSearch(
                    req.getRequiresExternalSearch() != null ? (req.getRequiresExternalSearch() ? 1 : 0) : 0);
            task.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : i);
            researchTaskMapper.insert(task);
            created.add(task);
            indexToId.put(i, task.getId());
        }

        // 回填依赖关系（dependsOn 表达的是 "依赖于第 N 个任务"，N 是数组索引）
        for (int i = 0; i < requests.size(); i++) {
            CreateResearchTaskRequest req = requests.get(i);
            if (req.getDependsOn() != null) {
                ResearchTask task = created.get(i);
                // dependsOn 存储的是索引，此处将其映射到实际 ID
                Long actualDependsOnId = indexToId.get(req.getDependsOn().intValue());
                if (actualDependsOnId != null) {
                    task.setDependsOn(actualDependsOnId);
                    researchTaskMapper.updateById(task);
                }
            }
        }

        log.info("research_tasks_batch_created projectId={} count={}", projectId, created.size());
        return created.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateStatusByProject(Long projectId, Long userId, String status) {
        checkProjectAccess(projectId, userId);

        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(400, "非法的任务状态：" + status);
        }

        List<ResearchTask> tasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTask>()
                        .eq(ResearchTask::getProjectId, projectId));

        for (ResearchTask task : tasks) {
            task.setStatus(status);
            if ("RUNNING".equals(status)) {
                task.setStartedAt(LocalDateTime.now());
            } else if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                task.setCompletedAt(LocalDateTime.now());
            }
            researchTaskMapper.updateById(task);
        }

        log.info("research_tasks_status_updated projectId={} status={} count={}",
                projectId, status, tasks.size());
    }

    @Override
    @Transactional
    public void markTaskCompleted(Long projectId, Long userId, Long taskId, String summary) {
        checkProjectAccess(projectId, userId);
        ResearchTask task = loadTask(taskId, projectId);

        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        if (summary != null && !summary.isBlank()) {
            task.setResultSummary(summary.length() > 2000 ? summary.substring(0, 2000) : summary);
        }
        researchTaskMapper.updateById(task);

        log.info("research_task_completed id={} projectId={}", taskId, projectId);
    }

    /**
     * 校验项目存在且用户有权限访问.
     */
    private void checkProjectAccess(Long projectId, Long userId) {
        ResearchProject project = researchProjectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "研究项目不存在");
        }
        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该研究项目");
        }
    }

    /**
     * 加载任务并校验所属项目.
     */
    private ResearchTask loadTask(Long id, Long projectId) {
        ResearchTask task = researchTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "研究任务不存在");
        }
        if (!task.getProjectId().equals(projectId)) {
            throw new BusinessException(400, "任务不属于该项目");
        }
        return task;
    }

    /**
     * 校验依赖的任务存在且属于同一项目，且不形成自引用.
     */
    private void validateDependency(Long projectId, Long currentTaskId, Long dependsOnId) {
        if (currentTaskId != null && dependsOnId.equals(currentTaskId)) {
            throw new BusinessException(400, "任务不能依赖自身");
        }
        ResearchTask dependency = researchTaskMapper.selectById(dependsOnId);
        if (dependency == null || !dependency.getProjectId().equals(projectId)) {
            throw new BusinessException(400, "依赖的任务不存在或不属于同一项目");
        }
    }

    /**
     * 更新任务状态，含合法性校验.
     */
    private void updateTaskStatus(ResearchTask task, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BusinessException(400, "非法的任务状态：" + newStatus);
        }

        String oldStatus = task.getStatus();

        // 启动任务：PENDING/FAILED/WAITING_USER → RUNNING
        if ("RUNNING".equals(newStatus) && !oldStatus.equals(newStatus)) {
            if (!STARTABLE_STATUSES.contains(oldStatus)) {
                throw new BusinessException(400,
                        "当前状态 [" + oldStatus + "] 不允许启动，仅 PENDING/FAILED/WAITING_USER 状态可启动");
            }
            // 检查依赖是否已完成
            if (task.getDependsOn() != null) {
                ResearchTask dependency = researchTaskMapper.selectById(task.getDependsOn());
                if (dependency != null && !"COMPLETED".equals(dependency.getStatus())) {
                    throw new BusinessException(400, "依赖的前置任务尚未完成");
                }
            }
            task.setStartedAt(LocalDateTime.now());
        }

        // 完成任务
        if ("COMPLETED".equals(newStatus) && !oldStatus.equals(newStatus)) {
            if (!"RUNNING".equals(oldStatus)) {
                throw new BusinessException(400, "仅运行中的任务可以标记完成");
            }
            task.setCompletedAt(LocalDateTime.now());
        }

        task.setStatus(newStatus);
    }

    /**
     * 判断任务是否就绪（依赖已满足）.
     */
    private boolean isTaskReady(ResearchTask task) {
        if (task.getDependsOn() == null) {
            return true;
        }
        ResearchTask dependency = researchTaskMapper.selectById(task.getDependsOn());
        return dependency != null && "COMPLETED".equals(dependency.getStatus());
    }

    /**
     * 查找依赖当前任务的其他任务.
     */
    private List<Long> findDependentTaskIds(Long taskId, Long projectId) {
        return researchTaskMapper.selectList(
                        new LambdaQueryWrapper<ResearchTask>()
                                .eq(ResearchTask::getProjectId, projectId)
                                .eq(ResearchTask::getDependsOn, taskId))
                .stream()
                .map(ResearchTask::getId)
                .toList();
    }

    /**
     * 将实体转换为视图对象.
     */
    private ResearchTaskVO convertToVO(ResearchTask task) {
        ResearchTaskVO vo = new ResearchTaskVO();
        vo.setId(task.getId());
        vo.setProjectId(task.getProjectId());
        vo.setPlanId(task.getPlanId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setQuestion(task.getQuestion());
        vo.setStatus(task.getStatus());
        vo.setStatusLabel(getStatusLabel(task.getStatus()));
        vo.setDependsOn(task.getDependsOn());
        vo.setAsyncTaskId(task.getAsyncTaskId());
        vo.setRequiresExternalSearch(task.getRequiresExternalSearch() != null
                && task.getRequiresExternalSearch() == 1);
        vo.setResultSummary(task.getResultSummary());
        vo.setSortOrder(task.getSortOrder());
        vo.setIsReady(isTaskReady(task));
        vo.setDependentTaskIds(findDependentTaskIds(task.getId(), task.getProjectId()));
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());

        // 填充依赖任务标题
        if (task.getDependsOn() != null) {
            ResearchTask dependency = researchTaskMapper.selectById(task.getDependsOn());
            vo.setDependsOnTitle(dependency != null ? dependency.getTitle() : null);
        }

        return vo;
    }

    /**
     * 获取状态的中文显示名称.
     */
    private String getStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "PENDING" -> "待执行";
            case "RUNNING" -> "执行中";
            case "COMPLETED" -> "已完成";
            case "FAILED" -> "失败";
            case "SKIPPED" -> "已跳过";
            case "WAITING_USER" -> "等待用户";
            default -> status;
        };
    }
}
