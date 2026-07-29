package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.ResearchPlan;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchPlanMapper;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.service.ResearchPlanService;
import com.secondbrain.vo.ResearchPlanVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 研究计划服务实现.
 *
 * @author AI
 */
@Service
public class ResearchPlanServiceImpl implements ResearchPlanService {

    private static final Logger log = LoggerFactory.getLogger(ResearchPlanServiceImpl.class);

    private final ResearchPlanMapper researchPlanMapper;
    private final ResearchProjectMapper researchProjectMapper;

    public ResearchPlanServiceImpl(ResearchPlanMapper researchPlanMapper,
                                   ResearchProjectMapper researchProjectMapper) {
        this.researchPlanMapper = researchPlanMapper;
        this.researchProjectMapper = researchProjectMapper;
    }

    @Override
    public ResearchPlanVO getLatestByProject(Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);

        List<ResearchPlan> plans = researchPlanMapper.selectList(
                new LambdaQueryWrapper<ResearchPlan>()
                        .eq(ResearchPlan::getProjectId, projectId)
                        .orderByDesc(ResearchPlan::getVersion)
                        .last("LIMIT 1"));

        if (plans.isEmpty()) {
            return null;
        }
        return convertToVO(plans.get(0));
    }

    @Override
    public List<ResearchPlanVO> listByProject(Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);

        List<ResearchPlan> plans = researchPlanMapper.selectList(
                new LambdaQueryWrapper<ResearchPlan>()
                        .eq(ResearchPlan::getProjectId, projectId)
                        .orderByDesc(ResearchPlan::getVersion));

        return plans.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public ResearchPlanVO getById(Long id, Long userId) {
        ResearchPlan plan = researchPlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(404, "研究计划不存在");
        }
        checkProjectAccess(plan.getProjectId(), userId);
        return convertToVO(plan);
    }

    @Override
    @Transactional
    public ResearchPlanVO createPlan(Long projectId, String complexity, String agentChain,
                                     String tasksJson, String rationale, Integer estimatedTokens,
                                     String createdBy, Long userId) {
        checkProjectAccess(projectId, userId);

        // 确定新版本号
        List<ResearchPlan> existing = researchPlanMapper.selectList(
                new LambdaQueryWrapper<ResearchPlan>()
                        .eq(ResearchPlan::getProjectId, projectId)
                        .orderByDesc(ResearchPlan::getVersion)
                        .last("LIMIT 1"));
        int newVersion = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;

        ResearchPlan plan = new ResearchPlan();
        plan.setProjectId(projectId);
        plan.setVersion(newVersion);
        plan.setComplexity(complexity);
        plan.setAgentChain(agentChain);
        plan.setTasksJson(tasksJson);
        plan.setRationale(rationale);
        plan.setEstimatedTokens(estimatedTokens);
        plan.setCreatedBy(createdBy);

        researchPlanMapper.insert(plan);

        // 同步更新项目的 complexity 字段
        ResearchProject project = researchProjectMapper.selectById(projectId);
        if (project != null) {
            project.setComplexity(complexity);
            researchProjectMapper.updateById(project);
        }

        log.info("research_plan_created id={} projectId={} version={} complexity={}",
                plan.getId(), projectId, newVersion, complexity);
        return convertToVO(plan);
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

    private ResearchPlanVO convertToVO(ResearchPlan plan) {
        ResearchPlanVO vo = new ResearchPlanVO();
        vo.setId(plan.getId());
        vo.setProjectId(plan.getProjectId());
        vo.setVersion(plan.getVersion());
        vo.setComplexity(plan.getComplexity());
        vo.setComplexityLabel(getComplexityLabel(plan.getComplexity()));
        vo.setAgentChain(plan.getAgentChain());
        vo.setTasksJson(plan.getTasksJson());
        vo.setRationale(plan.getRationale());
        vo.setEstimatedTokens(plan.getEstimatedTokens());
        vo.setCreatedBy(plan.getCreatedBy());
        vo.setCreateTime(plan.getCreateTime());
        return vo;
    }

    private String getComplexityLabel(String complexity) {
        if (complexity == null) return "";
        return switch (complexity) {
            case "SIMPLE" -> "简单";
            case "STANDARD" -> "标准";
            case "DEEP" -> "深度";
            default -> complexity;
        };
    }
}
