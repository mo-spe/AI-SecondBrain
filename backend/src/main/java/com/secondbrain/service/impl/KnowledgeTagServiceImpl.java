package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.KnowledgeNodeTagRelation;
import com.secondbrain.entity.KnowledgeTag;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.KnowledgeNodeTagRelationMapper;
import com.secondbrain.mapper.KnowledgeTagMapper;
import com.secondbrain.service.KnowledgeTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeTagServiceImpl implements KnowledgeTagService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeTagServiceImpl.class);

    private final KnowledgeTagMapper knowledgeTagMapper;
    private final KnowledgeNodeTagRelationMapper relationMapper;

    public KnowledgeTagServiceImpl(KnowledgeTagMapper knowledgeTagMapper,
                                   KnowledgeNodeTagRelationMapper relationMapper) {
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.relationMapper = relationMapper;
    }

    @Override
    public List<KnowledgeTag> listByUser(Long userId) {
        return knowledgeTagMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTag>()
                        .eq(KnowledgeTag::getUserId, userId)
                        .eq(KnowledgeTag::getDeleted, 0)
                        .orderByAsc(KnowledgeTag::getCreateTime));
    }

    @Override
    @Transactional
    public KnowledgeTag create(String tagName, String tagColor, Long userId) {
        if (tagName == null || tagName.isBlank()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        Long existing = knowledgeTagMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeTag>()
                        .eq(KnowledgeTag::getUserId, userId)
                        .eq(KnowledgeTag::getTagName, tagName)
                        .eq(KnowledgeTag::getDeleted, 0));
        if (existing > 0) {
            throw new BusinessException(400, "该标签已存在");
        }
        KnowledgeTag tag = new KnowledgeTag();
        tag.setUserId(userId);
        tag.setTagName(tagName.trim());
        tag.setTagColor(tagColor != null ? tagColor : "#6366f1");
        tag.setCreateTime(LocalDateTime.now());
        tag.setDeleted(0);
        knowledgeTagMapper.insert(tag);
        log.info("knowledge_tag_created id={} name={} userId={}", tag.getId(), tagName, userId);
        return tag;
    }

    @Override
    @Transactional
    public void delete(Long tagId, Long userId) {
        KnowledgeTag tag = knowledgeTagMapper.selectById(tagId);
        if (tag == null || tag.getDeleted() == 1) {
            throw new BusinessException(400, "标签不存在");
        }
        if (!tag.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己的标签");
        }
        tag.setDeleted(1);
        knowledgeTagMapper.updateById(tag);

        // 清除关联关系
        relationMapper.delete(
                new LambdaQueryWrapper<KnowledgeNodeTagRelation>()
                        .eq(KnowledgeNodeTagRelation::getTagId, tagId));

        log.info("knowledge_tag_deleted id={} userId={}", tagId, userId);
    }

    @Override
    @Transactional
    public void addTagToNode(Long nodeId, Long tagId) {
        Long existing = relationMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeNodeTagRelation>()
                        .eq(KnowledgeNodeTagRelation::getNodeId, nodeId)
                        .eq(KnowledgeNodeTagRelation::getTagId, tagId));
        if (existing > 0) {
            return;
        }
        KnowledgeNodeTagRelation relation = new KnowledgeNodeTagRelation();
        relation.setNodeId(nodeId);
        relation.setTagId(tagId);
        relation.setCreateTime(LocalDateTime.now());
        relationMapper.insert(relation);
    }

    @Override
    @Transactional
    public void removeTagFromNode(Long nodeId, Long tagId) {
        relationMapper.delete(
                new LambdaQueryWrapper<KnowledgeNodeTagRelation>()
                        .eq(KnowledgeNodeTagRelation::getNodeId, nodeId)
                        .eq(KnowledgeNodeTagRelation::getTagId, tagId));
    }

    @Override
    public List<KnowledgeTag> listByNode(Long nodeId) {
        List<Long> tagIds = relationMapper.selectList(
                        new LambdaQueryWrapper<KnowledgeNodeTagRelation>()
                                .eq(KnowledgeNodeTagRelation::getNodeId, nodeId))
                .stream()
                .map(KnowledgeNodeTagRelation::getTagId)
                .toList();

        if (tagIds.isEmpty()) {
            return List.of();
        }
        return knowledgeTagMapper.selectBatchIds(tagIds).stream()
                .filter(t -> t.getDeleted() == 0)
                .toList();
    }

    @Override
    public List<KnowledgeTag> listAll() {
        return knowledgeTagMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTag>()
                        .eq(KnowledgeTag::getDeleted, 0)
                        .orderByAsc(KnowledgeTag::getTagName));
    }
}
