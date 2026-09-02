package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.secondbrain.dto.CreateResearchProjectRequest;
import com.secondbrain.dto.UpdateResearchProjectRequest;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.research.orchestrator.ResearchOrchestrator;
import com.secondbrain.service.ResearchProjectService;
import com.secondbrain.vo.ResearchProjectVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 研究项目服务实现.
 *
 * <p>实现研究项目的 CRUD、生命周期管理和权限校验。</p>
 *
 * @author AI
 */
@Service
public class ResearchProjectServiceImpl implements ResearchProjectService {

    private static final Logger log = LoggerFactory.getLogger(ResearchProjectServiceImpl.class);

    /** 允许执行启动操作的状态 */
    private static final Set<String> EXECUTABLE_STATUSES = Set.of("DRAFT", "PAUSED", "FAILED", "PARTIAL");

    /** 允许暂停操作的状态 */
    private static final Set<String> PAUSABLE_STATUSES = Set.of("RESEARCHING", "REVIEWING");

    /** 允许归档操作的状态 */
    private static final Set<String> ARCHIVABLE_STATUSES = Set.of("COMPLETED", "FAILED", "PARTIAL");

    private final ResearchProjectMapper researchProjectMapper;
    private final ResearchOrchestrator researchOrchestrator;

    public ResearchProjectServiceImpl(ResearchProjectMapper researchProjectMapper,
                                       ResearchOrchestrator researchOrchestrator) {
        this.researchProjectMapper = researchProjectMapper;
        this.researchOrchestrator = researchOrchestrator;
    }

    @Override
    public Page<ResearchProjectVO> list(int current, int size, String status, String keyword,
                                        Long userId, Long workspaceId) {
        LambdaQueryWrapper<ResearchProject> wrapper = buildBaseWrapper(userId, workspaceId);

        if (status != null && !status.isBlank()) {
            wrapper.eq(ResearchProject::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(ResearchProject::getTitle, keyword);
        }
        wrapper.orderByDesc(ResearchProject::getCreateTime);

        Page<ResearchProject> page = new Page<>(current, size);
        Page<ResearchProject> resultPage = researchProjectMapper.selectPage(page, wrapper);

        // 转换为 VO 分页
        Page<ResearchProjectVO> voPage = new Page<>();
        voPage.setCurrent(resultPage.getCurrent());
        voPage.setSize(resultPage.getSize());
        voPage.setTotal(resultPage.getTotal());
        List<ResearchProjectVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ResearchProjectVO getById(Long id, Long userId, Long workspaceId) {
        ResearchProject project = loadAndCheckAccess(id, userId, workspaceId);
        return convertToVO(project);
    }

    @Override
    @Transactional
    public ResearchProjectVO create(CreateResearchProjectRequest request, Long userId) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException(400, "研究标题不能为空");
        }
        if (request.getGoal() == null || request.getGoal().isBlank()) {
            throw new BusinessException(400, "研究目标不能为空");
        }

        ResearchProject project = new ResearchProject();
        project.setUserId(userId);
        project.setWorkspaceId(request.getWorkspaceId());
        project.setTitle(request.getTitle().trim());
        project.setGoal(request.getGoal().trim());
        project.setStatus("DRAFT");
        project.setMaxIterations(3);
        project.setCurrentIteration(0);
        project.setVersion(1);

        researchProjectMapper.insert(project);
        log.info("research_project_created id={} title={} userId={} workspaceId={}",
                project.getId(), project.getTitle(), userId, request.getWorkspaceId());
        return convertToVO(project);
    }

    @Override
    @Transactional
    public ResearchProjectVO update(Long id, UpdateResearchProjectRequest request,
                                    Long userId, Long workspaceId) {
        ResearchProject project = loadAndCheckAccess(id, userId, workspaceId);

        if (!"DRAFT".equals(project.getStatus())) {
            throw new BusinessException(400, "仅草稿状态的项目可以编辑");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            project.setTitle(request.getTitle().trim());
        }
        if (request.getGoal() != null && !request.getGoal().isBlank()) {
            project.setGoal(request.getGoal().trim());
        }

        researchProjectMapper.updateById(project);
        log.info("research_project_updated id={} title={}", id, project.getTitle());
        return convertToVO(project);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, Long workspaceId) {
        ResearchProject project = loadAndCheckAccess(id, userId, workspaceId);

        researchProjectMapper.deleteById(id);
        log.info("research_project_deleted id={} title={} userId={}", id, project.getTitle(), userId);
    }

    @Override
    @Transactional
    public ResearchProjectVO execute(Long id, Long userId, Long workspaceId) {
        ResearchProject project = loadAndCheckAccess(id, userId, workspaceId);

        if (!EXECUTABLE_STATUSES.contains(project.getStatus())) {
            throw new BusinessException(400,
                    "当前状态 [" + project.getStatus()
                            + "] 不允许启动研究，仅 DRAFT/PAUSED/FAILED/PARTIAL 状态可启动");
        }

        // 首次执行时记录开始时间
        if (project.getStartedAt() == null) {
            project.setStartedAt(LocalDateTime.now());
        }
        project.setStatus("RESEARCHING");
        project.setPausedAt(null);

        researchProjectMapper.updateById(project);
        log.info("research_project_executed id={}", id);

        // 异步启动研究编排器 — ResearchOrchestrator.start() 标记了 @Async，在新线程中执行 Agent 链
        researchOrchestrator.start(id, userId);

        return convertToVO(project);
    }

    @Override
    @Transactional
    public void pause(Long id, Long userId, Long workspaceId) {
        ResearchProject project = loadAndCheckAccess(id, userId, workspaceId);

        if (!PAUSABLE_STATUSES.contains(project.getStatus())) {
            throw new BusinessException(400,
                    "当前状态 [" + project.getStatus() + "] 不允许暂停，仅 RESEARCHING/REVIEWING 状态可暂停");
        }

        project.setStatus("PAUSED");
        project.setPausedAt(LocalDateTime.now());

        researchProjectMapper.updateById(project);
        log.info("research_project_paused id={}", id);
    }

    @Override
    @Transactional
    public ResearchProjectVO resume(Long id, Long userId, Long workspaceId) {
        ResearchProject project = loadAndCheckAccess(id, userId, workspaceId);

        if (!"PAUSED".equals(project.getStatus())) {
            throw new BusinessException(400,
                    "当前状态 [" + project.getStatus() + "] 不允许恢复，仅 PAUSED 状态可恢复");
        }

        project.setStatus("RESEARCHING");
        project.setPausedAt(null);

        researchProjectMapper.updateById(project);
        log.info("research_project_resumed id={}", id);

        // 异步恢复执行 — Orchestrator.start() 会重新加载项目并续接 agent 链
        researchOrchestrator.start(id, userId);

        return convertToVO(project);
    }

    @Override
    @Transactional
    public void archive(Long id, Long userId, Long workspaceId) {
        ResearchProject project = loadAndCheckAccess(id, userId, workspaceId);

        if (!ARCHIVABLE_STATUSES.contains(project.getStatus())) {
            throw new BusinessException(400,
                    "当前状态 [" + project.getStatus()
                            + "] 不允许归档，仅 COMPLETED/FAILED/PARTIAL 状态可归档");
        }

        project.setStatus("ARCHIVED");
        researchProjectMapper.updateById(project);
        log.info("research_project_archived id={}", id);
    }

    /**
     * 构建基础查询条件，实现用户/工作空间数据隔离.
     *
     * <p>工作空间模式下按 workspaceId 过滤，个人模式下按 userId 过滤。</p>
     */
    private LambdaQueryWrapper<ResearchProject> buildBaseWrapper(Long userId, Long workspaceId) {
        LambdaQueryWrapper<ResearchProject> wrapper = new LambdaQueryWrapper<>();
        if (workspaceId != null) {
            wrapper.eq(ResearchProject::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(ResearchProject::getUserId, userId);
        }
        return wrapper;
    }

    /**
     * 加载项目并校验访问权限.
     *
     * <p>校验逻辑：工作空间项目校验 workspaceId 匹配，个人项目校验 userId 匹配。</p>
     */
    private ResearchProject loadAndCheckAccess(Long id, Long userId, Long workspaceId) {
        ResearchProject project = researchProjectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(404, "研究项目不存在");
        }
        if (!hasAccess(project, userId, workspaceId)) {
            throw new BusinessException(403, "无权访问该研究项目");
        }
        return project;
    }

    /**
     * 校验当前用户是否有权访问该项目.
     */
    private boolean hasAccess(ResearchProject project, Long userId, Long workspaceId) {
        if (workspaceId != null) {
            return workspaceId.equals(project.getWorkspaceId());
        }
        return project.getUserId().equals(userId);
    }

    /**
     * 将实体转换为视图对象.
     */
    private ResearchProjectVO convertToVO(ResearchProject project) {
        ResearchProjectVO vo = new ResearchProjectVO();
        vo.setId(project.getId());
        vo.setTitle(project.getTitle());
        vo.setGoal(project.getGoal());
        vo.setStatus(project.getStatus());
        vo.setStatusLabel(getStatusLabel(project.getStatus()));
        vo.setComplexity(project.getComplexity());
        vo.setAgentWorkflow(project.getAgentWorkflow());
        vo.setMaxIterations(project.getMaxIterations());
        vo.setCurrentIteration(project.getCurrentIteration());
        vo.setResultSummary(project.getResultSummary());
        vo.setWorkspaceId(project.getWorkspaceId());
        vo.setVersion(project.getVersion());
        vo.setStartedAt(project.getStartedAt());
        vo.setPausedAt(project.getPausedAt());
        vo.setCompletedAt(project.getCompletedAt());
        vo.setCreateTime(project.getCreateTime());
        vo.setUpdateTime(project.getUpdateTime());
        return vo;
    }

    /**
     * 获取状态的中文显示名称.
     */
    private String getStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PLANNING" -> "规划中";
            case "RESEARCHING" -> "研究中";
            case "REVIEWING" -> "验证中";
            case "SYNTHESIZING" -> "综合中";
            case "COMPLETED" -> "已完成";
            case "PARTIAL" -> "部分完成";
            case "ARCHIVED" -> "已归档";
            case "FAILED" -> "失败";
            case "PAUSED" -> "已暂停";
            default -> status;
        };
    }
}
