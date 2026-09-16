package com.secondbrain.service;

import com.secondbrain.vo.ResearchReportVO;

/**
 * 研究报告服务接口.
 *
 * @author AI
 */
public interface ResearchReportService {

    /**
     * 创建或更新报告（版本递增）.
     */
    ResearchReportVO saveReport(Long projectId, String title, String summary,
                                 String contentMd, String keyFindings, String knowledgeGaps,
                                 int sourceCount, int conclusionCount,
                                 long durationMs, Long userId);

    /**
     * 获取项目最新报告.
     */
    ResearchReportVO getLatestByProject(Long projectId, Long userId);

    /**
     * 获取项目所有报告版本.
     */
    java.util.List<ResearchReportVO> listByProject(Long projectId, Long userId);
}
