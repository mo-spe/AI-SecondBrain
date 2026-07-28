package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.TagSuggestion;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeNodeTagRelation;
import com.secondbrain.entity.KnowledgeTag;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.KnowledgeNodeTagRelationMapper;
import com.secondbrain.mapper.KnowledgeTagMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.KnowledgeTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class KnowledgeTagServiceImpl implements KnowledgeTagService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeTagServiceImpl.class);

    private final KnowledgeTagMapper knowledgeTagMapper;
    private final KnowledgeNodeTagRelationMapper relationMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final AiService aiService;

    public KnowledgeTagServiceImpl(KnowledgeTagMapper knowledgeTagMapper,
                                   KnowledgeNodeTagRelationMapper relationMapper,
                                   KnowledgeNodeMapper knowledgeNodeMapper,
                                   AiService aiService) {
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.relationMapper = relationMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.aiService = aiService;
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
    public KnowledgeTag create(String tagName, String tagColor, Long parentId, Long userId) {
        if (tagName == null || tagName.isBlank()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        // 校验同级标签不重名
        Long existing = knowledgeTagMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeTag>()
                        .eq(KnowledgeTag::getUserId, userId)
                        .eq(KnowledgeTag::getTagName, tagName)
                        .eq(parentId != null, KnowledgeTag::getParentId, parentId)
                        .isNull(parentId == null, KnowledgeTag::getParentId)
                        .eq(KnowledgeTag::getDeleted, 0));
        if (existing > 0) {
            throw new BusinessException(400, "同级下已存在同名标签");
        }
        KnowledgeTag tag = new KnowledgeTag();
        tag.setUserId(userId);
        tag.setTagName(tagName.trim());
        tag.setTagColor(tagColor != null ? tagColor : "#6366f1");
        tag.setParentId(parentId);
        tag.setCreateTime(LocalDateTime.now());
        tag.setDeleted(0);
        knowledgeTagMapper.insert(tag);
        log.info("knowledge_tag_created id={} name={} userId={} parentId={}", tag.getId(), tagName, userId, parentId);
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
        // 将子标签的 parentId 置为 null，避免级联删除
        List<KnowledgeTag> children = knowledgeTagMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTag>()
                        .eq(KnowledgeTag::getParentId, tagId)
                        .eq(KnowledgeTag::getDeleted, 0));
        for (KnowledgeTag child : children) {
            child.setParentId(null);
            knowledgeTagMapper.updateById(child);
            log.info("knowledge_tag_reparented id={} oldParentId={}", child.getId(), tagId);
        }

        tag.setDeleted(1);
        knowledgeTagMapper.updateById(tag);

        // 清除关联关系
        relationMapper.delete(
                new LambdaQueryWrapper<KnowledgeNodeTagRelation>()
                        .eq(KnowledgeNodeTagRelation::getTagId, tagId));

        log.info("knowledge_tag_deleted id={} userId={} reparentedChildren={}", tagId, userId, children.size());
    }

    @Override
    @Transactional
    public KnowledgeTag update(Long tagId, String tagName, String tagColor, Long parentId, Long userId) {
        KnowledgeTag tag = knowledgeTagMapper.selectById(tagId);
        if (tag == null || tag.getDeleted() == 1) {
            throw new BusinessException(400, "标签不存在");
        }
        if (!tag.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能修改自己的标签");
        }
        if (tagName != null && !tagName.isBlank()) {
            tag.setTagName(tagName.trim());
        }
        if (tagColor != null) {
            tag.setTagColor(tagColor);
        }
        if (parentId != null) {
            // 校验不能将自己的子孙设为自己的父标签（防止循环引用）
            if (parentId > 0 && isDescendant(tagId, parentId)) {
                throw new BusinessException(400, "不能将标签移动到自己的子标签下");
            }
            tag.setParentId(parentId > 0 ? parentId : null);
        }
        knowledgeTagMapper.updateById(tag);
        log.info("knowledge_tag_updated id={} name={} parentId={}", tagId, tag.getTagName(), tag.getParentId());
        return tag;
    }

    @Override
    public List<KnowledgeTag> listTreeByUser(Long userId) {
        List<KnowledgeTag> allTags = knowledgeTagMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTag>()
                        .eq(KnowledgeTag::getUserId, userId)
                        .eq(KnowledgeTag::getDeleted, 0)
                        .orderByAsc(KnowledgeTag::getCreateTime));

        // 构建 tagId → tag 映射
        Map<Long, KnowledgeTag> tagMap = new HashMap<>();
        for (KnowledgeTag tag : allTags) {
            tag.setChildren(new ArrayList<>());
            tag.setNodeCount(0);
            tagMap.put(tag.getId(), tag);
        }

        // 获取所有 relation 记录，统计每个标签的直接知识点数
        List<KnowledgeNodeTagRelation> allRelations = relationMapper.selectList(null);
        Map<Long, Long> directNodeCount = new HashMap<>();
        for (KnowledgeNodeTagRelation rel : allRelations) {
            directNodeCount.merge(rel.getTagId(), 1L, Long::sum);
        }

        // 先设置直接知识点数
        for (KnowledgeTag tag : allTags) {
            tag.setNodeCount(directNodeCount.getOrDefault(tag.getId(), 0L).intValue());
        }

        // 组装树：parentId==null 的为根节点
        List<KnowledgeTag> roots = new ArrayList<>();
        for (KnowledgeTag tag : allTags) {
            if (tag.getParentId() == null) {
                roots.add(tag);
            } else {
                KnowledgeTag parent = tagMap.get(tag.getParentId());
                if (parent != null) {
                    parent.getChildren().add(tag);
                } else {
                    // 父标签已删除，作为根节点
                    roots.add(tag);
                }
            }
        }

        // 递归汇总 nodeCount（子标签的知识点数计入父标签）
        for (KnowledgeTag root : roots) {
            accumulateNodeCount(root);
        }

        return roots;
    }

    /**
     * 递归汇总节点数：当前标签的 nodeCount += 所有子标签的 nodeCount.
     */
    private void accumulateNodeCount(KnowledgeTag tag) {
        if (tag.getChildren() == null || tag.getChildren().isEmpty()) {
            return;
        }
        for (KnowledgeTag child : tag.getChildren()) {
            accumulateNodeCount(child);
            tag.setNodeCount(tag.getNodeCount() + child.getNodeCount());
        }
    }

    /**
     * 判断 candidateParentId 是否是 tagId 的后代（用于防循环引用检查）.
     */
    private boolean isDescendant(Long tagId, Long candidateParentId) {
        KnowledgeTag parent = knowledgeTagMapper.selectById(candidateParentId);
        while (parent != null && parent.getParentId() != null) {
            if (parent.getParentId().equals(tagId)) {
                return true;
            }
            parent = knowledgeTagMapper.selectById(parent.getParentId());
        }
        return false;
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

    @Override
    public List<TagSuggestion> suggestTags(String title, String summary, Long userId) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", """
                        你是一个知识分类专家。给定以下知识点的标题和摘要，请推荐1-3个最合适的分类标签。

                        要求：
                        1. 标签应简洁（2-6个字）、准确、具有概括性
                        2. 优先使用常见的技术领域分类
                        3. 只返回JSON数组，每个元素为{"tagName":"标签名"}，不要有其他内容
                        4. 如果没有合适的标签，返回空数组[]
                        """),
                Map.of("role", "user", "content",
                        "知识点标题：" + title + "\n知识点摘要：" + summary)
        );

        try {
            String aiResponse = aiService.chat(userId, AiScenario.CHAT.getCode(), messages);
            return parseTagSuggestions(aiResponse, userId);
        } catch (Exception e) {
            log.warn("AI标签建议失败 title={} userId={}", title, userId, e);
            return List.of();
        }
    }

    /**
     * 解析AI返回的标签建议，匹配已有标签.
     */
    private List<TagSuggestion> parseTagSuggestions(String aiResponse, Long userId) {
        List<TagSuggestion> suggestions = new ArrayList<>();
        // 提取JSON数组
        String json = aiResponse.trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start == -1 || end == -1) {
            return suggestions;
        }
        json = json.substring(start, end + 1);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> rawList = mapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            List<KnowledgeTag> existingTags = knowledgeTagMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeTag>()
                            .eq(KnowledgeTag::getUserId, userId)
                            .eq(KnowledgeTag::getDeleted, 0));

            for (int i = 0; i < rawList.size(); i++) {
                Map<String, Object> item = rawList.get(i);
                String tagName = (String) item.get("tagName");
                if (tagName == null || tagName.isBlank()) continue;

                // 匹配已有标签（名称完全匹配）
                KnowledgeTag matched = existingTags.stream()
                        .filter(t -> t.getTagName().equalsIgnoreCase(tagName.trim()))
                        .findFirst()
                        .orElse(null);

                int confidence = 80 - i * 15; // 第一个80%，第二个65%，第三个50%
                if (matched != null) {
                    suggestions.add(TagSuggestion.fromExisting(matched.getTagName(), matched.getId(), confidence));
                } else {
                    suggestions.add(TagSuggestion.newTag(tagName.trim(), confidence));
                }
            }
        } catch (Exception e) {
            log.warn("解析AI标签建议失败 response={}", aiResponse, e);
        }
        return suggestions;
    }
}
