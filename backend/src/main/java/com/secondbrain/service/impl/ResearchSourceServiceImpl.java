package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.ResearchProject;
import com.secondbrain.entity.ResearchSource;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.ResearchProjectMapper;
import com.secondbrain.mapper.ResearchSourceMapper;
import com.secondbrain.service.ResearchSourceService;
import com.secondbrain.vo.ResearchSourceVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 研究来源服务实现.
 *
 * @author AI
 */
@Service
public class ResearchSourceServiceImpl implements ResearchSourceService {

    private static final Logger log = LoggerFactory.getLogger(ResearchSourceServiceImpl.class);

    private final ResearchSourceMapper researchSourceMapper;
    private final ResearchProjectMapper researchProjectMapper;

    public ResearchSourceServiceImpl(ResearchSourceMapper researchSourceMapper,
                                     ResearchProjectMapper researchProjectMapper) {
        this.researchSourceMapper = researchSourceMapper;
        this.researchProjectMapper = researchProjectMapper;
    }

    @Override
    public List<ResearchSourceVO> listByProject(Long projectId, Long userId) {
        checkProjectAccess(projectId, userId);

        List<ResearchSource> sources = researchSourceMapper.selectList(
                new LambdaQueryWrapper<ResearchSource>()
                        .eq(ResearchSource::getProjectId, projectId)
                        .orderByDesc(ResearchSource::getCreateTime));

        return sources.stream()
                .map(s -> convertToVO(s, false))
                .collect(Collectors.toList());
    }

    @Override
    public ResearchSourceVO getById(Long id, Long userId) {
        ResearchSource source = researchSourceMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(404, "研究来源不存在");
        }
        checkProjectAccess(source.getProjectId(), userId);
        return convertToVO(source, true);
    }

    @Override
    @Transactional
    public ResearchSourceVO create(ResearchSource source, Long userId) {
        checkProjectAccess(source.getProjectId(), userId);
        researchSourceMapper.insert(source);
        log.info("research_source_created id={} projectId={} type={} reliability={}",
                source.getId(), source.getProjectId(), source.getSourceType(), source.getReliability());
        return convertToVO(source, true);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        ResearchSource source = researchSourceMapper.selectById(id);
        if (source == null) {
            throw new BusinessException(404, "研究来源不存在");
        }
        checkProjectAccess(source.getProjectId(), userId);
        researchSourceMapper.deleteById(id);
        log.info("research_source_deleted id={} projectId={}", id, source.getProjectId());
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
     * 转换为视图对象.
     *
     * @param includeFullContent 是否包含完整内容（列表查询时为 false）
     */
    private ResearchSourceVO convertToVO(ResearchSource source, boolean includeFullContent) {
        ResearchSourceVO vo = new ResearchSourceVO();
        vo.setId(source.getId());
        vo.setProjectId(source.getProjectId());
        vo.setTaskId(source.getTaskId());
        vo.setTitle(source.getTitle());
        vo.setUrl(source.getUrl());
        vo.setSourceType(source.getSourceType());
        vo.setSourceTypeLabel(getSourceTypeLabel(source.getSourceType()));
        vo.setSnippet(source.getSnippet());
        if (includeFullContent) {
            vo.setFullContent(source.getFullContent());
        }
        vo.setRelevanceScore(source.getRelevanceScore());
        vo.setReliability(source.getReliability());
        vo.setReliabilityLabel(getReliabilityLabel(source.getReliability()));
        vo.setFetchStatus(source.getFetchStatus());
        vo.setFetchStatusLabel(getFetchStatusLabel(source.getFetchStatus()));
        vo.setFetchedAt(source.getFetchedAt());
        vo.setCreateTime(source.getCreateTime());
        return vo;
    }

    private String getSourceTypeLabel(String sourceType) {
        if (sourceType == null) return "";
        return switch (sourceType) {
            case "web_search" -> "网页搜索";
            case "official_doc" -> "官方文档";
            case "paper" -> "学术论文";
            case "github" -> "GitHub";
            case "article" -> "文章";
            case "internal" -> "内部知识";
            default -> sourceType;
        };
    }

    private String getReliabilityLabel(String reliability) {
        if (reliability == null) return "";
        return switch (reliability) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            case "unverified" -> "未验证";
            default -> reliability;
        };
    }

    private String getFetchStatusLabel(String fetchStatus) {
        if (fetchStatus == null) return "";
        return switch (fetchStatus) {
            case "success" -> "成功";
            case "failed" -> "失败";
            case "timeout" -> "超时";
            case "skipped" -> "已跳过";
            default -> fetchStatus;
        };
    }
}
