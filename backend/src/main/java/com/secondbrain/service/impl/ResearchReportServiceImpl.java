package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.entity.ResearchReport;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.mapper.ResearchReportMapper;
import com.secondbrain.service.ResearchReportService;
import com.secondbrain.vo.ResearchReportVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 研究报告服务实现.
 *
 * @author AI
 */
@Service
public class ResearchReportServiceImpl implements ResearchReportService {

    private final ResearchReportMapper researchReportMapper;
    private final ResearchProjectMapper researchProjectMapper;

    public ResearchReportServiceImpl(ResearchReportMapper researchReportMapper,
                                     ResearchProjectMapper researchProjectMapper) {
        this.researchReportMapper = researchReportMapper;
        this.researchProjectMapper = researchProjectMapper;
    }

    @Override
    @Transactional
    public ResearchReportVO saveReport(Long projectId, String title, String summary,
                                        String contentMd, String keyFindings,
                                        String knowledgeGaps, int sourceCount,
                                        int conclusionCount, long durationMs, Long userId) {
        checkProjectAccess(projectId, userId);

        // 确定新版本号
        List<ResearchReport> existing = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReport>()
                        .eq(ResearchReport::getProjectId, projectId)
                        .orderByDesc(ResearchReport::getVersion)
                        .last("LIMIT 1"));
        int newVersion = existing.isEmpty() ? 1 : existing.get(0).getVersion() + 1;

        ResearchReport report = new ResearchReport();
        report.setProjectId(projectId);
        report.setVersion(newVersion);
        report.setTitle(title);
        report.setSummary(summary);
        report.setContentMd(contentMd);
        report.setKeyFindings(keyFindings);
        report.setKnowledgeGaps(knowledgeGaps);
        report.setSourceCount(sourceCount);
        report.setConclusionCount(conclusionCount);
        report.setDurationTotalMs(durationMs);
        report.setGeneratedBy("SYNTHESIZER_AGENT");

        researchReportMapper.insert(report);
        return convertToVO(report);
    }

    @Override
    public ResearchReportVO getLatestByProject(Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);

        List<ResearchReport> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReport>()
                        .eq(ResearchReport::getProjectId, projectId)
                        .orderByDesc(ResearchReport::getVersion)
                        .last("LIMIT 1"));

        return reports.isEmpty() ? null : convertToVO(reports.get(0));
    }

    @Override
    public List<ResearchReportVO> listByProject(Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);

        List<ResearchReport> reports = researchReportMapper.selectList(
                new LambdaQueryWrapper<ResearchReport>()
                        .eq(ResearchReport::getProjectId, projectId)
                        .orderByDesc(ResearchReport::getVersion));

        return reports.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private void checkProjectAccess(Long projectId, Long userId) {
        ResearchProject project = researchProjectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "研究项目不存在");
        }
        if (!project.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该研究项目");
        }
    }

    private ResearchReportVO convertToVO(ResearchReport report) {
        ResearchReportVO vo = new ResearchReportVO();
        vo.setId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setVersion(report.getVersion());
        vo.setTitle(report.getTitle());
        vo.setSummary(report.getSummary());
        vo.setContentMd(report.getContentMd());
        vo.setKeyFindings(report.getKeyFindings());
        vo.setKnowledgeGaps(report.getKnowledgeGaps());
        vo.setSourceCount(report.getSourceCount());
        vo.setConclusionCount(report.getConclusionCount());
        vo.setDurationTotalMs(report.getDurationTotalMs());
        vo.setGeneratedBy(report.getGeneratedBy());
        vo.setCreateTime(report.getCreateTime());
        return vo;
    }
}
