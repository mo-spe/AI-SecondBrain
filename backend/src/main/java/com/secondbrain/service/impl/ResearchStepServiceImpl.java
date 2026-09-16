package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.ResearchStep;
import com.secondbrain.entity.ResearchTask;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchStepMapper;
import com.secondbrain.mapper.ResearchTaskMapper;
import com.secondbrain.service.ResearchStepService;
import com.secondbrain.vo.ResearchStepVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 研究步骤服务实现.
 *
 * @author AI
 */
@Service
public class ResearchStepServiceImpl implements ResearchStepService {

    private final ResearchStepMapper researchStepMapper;
    private final ResearchTaskMapper researchTaskMapper;

    public ResearchStepServiceImpl(ResearchStepMapper researchStepMapper,
                                   ResearchTaskMapper researchTaskMapper) {
        this.researchStepMapper = researchStepMapper;
        this.researchTaskMapper = researchTaskMapper;
    }

    @Override
    public List<ResearchStepVO> listByTask(Long taskId, Long userId) {
        ResearchTask task = researchTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "研究任务不存在");
        }

        List<ResearchStep> steps = researchStepMapper.selectList(
                new LambdaQueryWrapper<ResearchStep>()
                        .eq(ResearchStep::getTaskId, taskId)
                        .orderByAsc(ResearchStep::getSortOrder));

        return steps.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<ResearchStepVO> listByProject(Long projectId, Long userId) {
        // 通过 task 关联查询：查询该项目下所有任务的步骤
        List<ResearchTask> tasks = researchTaskMapper.selectList(
                new LambdaQueryWrapper<ResearchTask>()
                        .eq(ResearchTask::getProjectId, projectId));

        List<Long> taskIds = tasks.stream().map(ResearchTask::getId).toList();
        if (taskIds.isEmpty()) {
            return List.of();
        }

        List<ResearchStep> steps = researchStepMapper.selectList(
                new LambdaQueryWrapper<ResearchStep>()
                        .in(ResearchStep::getTaskId, taskIds)
                        .orderByAsc(ResearchStep::getSortOrder));

        return steps.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private ResearchStepVO convertToVO(ResearchStep step) {
        ResearchStepVO vo = new ResearchStepVO();
        vo.setId(step.getId());
        vo.setTaskId(step.getTaskId());
        vo.setAgentName(step.getAgentName());
        vo.setStepType(step.getStepType());
        vo.setStepTypeLabel(getStepTypeLabel(step.getStepType()));
        vo.setTitle(step.getTitle());
        vo.setContent(step.getContent());
        vo.setToolName(step.getToolName());
        vo.setToolInput(step.getToolInput());
        vo.setToolOutput(step.getToolOutput());
        vo.setStatus(step.getStatus());
        vo.setStatusLabel(getStatusLabel(step.getStatus()));
        vo.setErrorMessage(step.getErrorMessage());
        vo.setSortOrder(step.getSortOrder());
        vo.setDurationMs(step.getDurationMs());
        vo.setCreateTime(step.getCreateTime());
        return vo;
    }

    private String getStepTypeLabel(String stepType) {
        if (stepType == null) return "";
        return switch (stepType) {
            case "THINKING" -> "思考";
            case "TOOL_CALL" -> "工具调用";
            case "TOOL_RESULT" -> "工具结果";
            case "LLM_CALL" -> "LLM 调用";
            case "PROCESSING" -> "处理中";
            default -> stepType;
        };
    }

    private String getStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "RUNNING" -> "运行中";
            case "COMPLETED" -> "已完成";
            case "FAILED" -> "失败";
            case "SKIPPED" -> "已跳过";
            default -> status;
        };
    }
}
