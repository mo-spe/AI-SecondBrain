package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.TagSuggestion;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeNodeTagRelation;
import com.secondbrain.entity.KnowledgeTag;
import com.secondbrain.entity.WorkspaceMember;
import com.secondbrain.enums.AiScenario;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.KnowledgeNodeTagRelationMapper;
import com.secondbrain.mapper.KnowledgeTagMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.KnowledgeTagService;
import com.secondbrain.service.WorkspaceService;
import com.secondbrain.util.WorkspaceRole;
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
    private final WorkspaceService workspaceService;

    public KnowledgeTagServiceImpl(KnowledgeTagMapper knowledgeTagMapper,
                                   KnowledgeNodeTagRelationMapper relationMapper,
                                   KnowledgeNodeMapper knowledgeNodeMapper,
                                   AiService aiService,
                                   WorkspaceService workspaceService) {
        this.knowledgeTagMapper = knowledgeTagMapper;
        this.relationMapper = relationMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.aiService = aiService;
        this.workspaceService = workspaceService;
    }

    /**
     * 构建标签查询条件：工作区内按 workspaceId 共享，个人空间按 userId 隔离.
     */
    private LambdaQueryWrapper<KnowledgeTag> scopeWrapper(Long userId, Long workspaceId) {
        LambdaQueryWrapper<KnowledgeTag> wrapper = new LambdaQueryWrapper<>();
        if (workspaceId != null) {
            // 工作区标签对所有成员可见，与创建者无关
            wrapper.eq(KnowledgeTag::getWorkspaceId, workspaceId);
        } else {
            wrapper.eq(KnowledgeTag::getUserId, userId)
                    .isNull(KnowledgeTag::getWorkspaceId);
        }
        return wrapper.eq(KnowledgeTag::getDeleted, 0);
    }

    @Override
    public List<KnowledgeTag> listByUser(Long userId, Long workspaceId) {
        return knowledgeTagMapper.selectList(
                scopeWrapper(userId, workspaceId)
                        .orderByAsc(KnowledgeTag::getCreateTime));
    }

    @Override
    @Transactional
    public KnowledgeTag create(String tagName, String tagColor, Long parentId, Long userId, Long workspaceId) {
        if (tagName == null || tagName.isBlank()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        // 校验同级标签不重名（重名校验需与可见范围一致）
        Long existing = knowledgeTagMapper.selectCount(
                scopeWrapper(userId, workspaceId)
                        .eq(KnowledgeTag::getTagName, tagName)
                        .eq(parentId != null, KnowledgeTag::getParentId, parentId)
                        .isNull(parentId == null, KnowledgeTag::getParentId));
        if (existing > 0) {
            throw new BusinessException(400, "同级下已存在同名标签");
        }
        KnowledgeTag tag = new KnowledgeTag();
        tag.setUserId(userId);
        tag.setWorkspaceId(workspaceId);
        tag.setTagName(tagName.trim());
        tag.setTagColor(tagColor != null ? tagColor : "#6366f1");
        tag.setParentId(parentId);
        tag.setCreateTime(LocalDateTime.now());
        tag.setDeleted(0);
        knowledgeTagMapper.insert(tag);
        log.info("knowledge_tag_created id={} name={} userId={} workspaceId={} parentId={}",
                tag.getId(), tagName, userId, workspaceId, parentId);
        return tag;
    }

    /**
     * 校验当前用户对标签的修改/删除权限.
     * <p>个人空间标签：仅创建者可操作。
     * 工作区共享标签：当前处于该工作区、且角色具备编辑权限（owner/admin/editor）的成员可操作。</p>
     */
    private void checkManageable(KnowledgeTag tag, Long userId, Long workspaceId, String action) {
        if (tag.getWorkspaceId() != null) {
            // 工作区标签：必须处于同一工作区，且成员角色可编辑
            if (!tag.getWorkspaceId().equals(workspaceId)) {
                throw new BusinessException(403, "只能" + action + "当前工作区的标签");
            }
            WorkspaceMember member = workspaceService.getMemberByWorkspaceAndUser(tag.getWorkspaceId(), userId);
            if (member == null) {
                throw new BusinessException(403, "您不是该工作区的成员");
            }
            if (!WorkspaceRole.canEdit(member.getRole())) {
                throw new BusinessException(403, "权限不足：当前角色为 " + member.getRole() + "，无法" + action + "工作区标签");
            }
            return;
        }
        // 个人空间标签：保持原有归属校验
        if (tag.getUserId() == null || !tag.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能" + action + "自己的标签");
        }
    }

    @Override
    @Transactional
    public void delete(Long tagId, Long userId, Long workspaceId) {
        KnowledgeTag tag = knowledgeTagMapper.selectById(tagId);
        if (tag == null || tag.getDeleted() == 1) {
            throw new BusinessException(400, "标签不存在");
        }
        checkManageable(tag, userId, workspaceId, "删除");
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
    public KnowledgeTag update(Long tagId, String tagName, String tagColor, Long parentId, Long userId, Long workspaceId) {
        KnowledgeTag tag = knowledgeTagMapper.selectById(tagId);
        if (tag == null || tag.getDeleted() == 1) {
            throw new BusinessException(400, "标签不存在");
        }
        checkManageable(tag, userId, workspaceId, "修改");
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
    public List<KnowledgeTag> listTreeByUser(Long userId, Long workspaceId) {
        List<KnowledgeTag> allTags = knowledgeTagMapper.selectList(
                scopeWrapper(userId, workspaceId)
                        .orderByAsc(KnowledgeTag::getCreateTime));

        // 构建 tagId → tag 映射
        Map<Long, KnowledgeTag> tagMap = new HashMap<>();
        for (KnowledgeTag tag : allTags) {
            tag.setChildren(new ArrayList<>());
            tag.setNodeCount(0);
            tagMap.put(tag.getId(), tag);
        }

        // 统计范围限定在当前可见的知识点内：
        // 工作区只算该工作区的知识点，个人空间只算本人的知识点
        List<Object> scopedNodeIds = knowledgeNodeMapper.selectObjs(
                new LambdaQueryWrapper<KnowledgeNode>()
                        .select(KnowledgeNode::getId)
                        .eq(KnowledgeNode::getDeleted, 0)
                        .eq(workspaceId != null, KnowledgeNode::getWorkspaceId, workspaceId)
                        .isNull(workspaceId == null, KnowledgeNode::getWorkspaceId)
                        .eq(workspaceId == null, KnowledgeNode::getUserId, userId));

        // tagId → 该标签下直接挂靠的知识点ID集合（Set 天然去重，同一知识点重复关联只算一次）
        Map<Long, Set<Long>> directNodes = new HashMap<>();
        if (scopedNodeIds != null && !scopedNodeIds.isEmpty()) {
            List<KnowledgeNodeTagRelation> relations = relationMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeNodeTagRelation>()
                            .in(KnowledgeNodeTagRelation::getNodeId, scopedNodeIds));
            for (KnowledgeNodeTagRelation rel : relations) {
                directNodes.computeIfAbsent(rel.getTagId(), k -> new HashSet<>()).add(rel.getNodeId());
            }
        }

        // 先设置直接知识点数
        for (KnowledgeTag tag : allTags) {
            tag.setNodeCount(directNodes.getOrDefault(tag.getId(), Set.of()).size());
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

        // 递归汇总 nodeCount（子标签的知识点计入父标签，用集合并集去重）
        for (KnowledgeTag root : roots) {
            aggregateNodeIds(root, directNodes);
        }

        return roots;
    }

    /**
     * 递归汇总知识点：返回该标签及其所有子标签关联的知识点ID集合（去重）.
     * <p>同一个知识点同时挂在父标签和子标签上时，向上汇总只计一次。</p>
     */
    private Set<Long> aggregateNodeIds(KnowledgeTag tag, Map<Long, Set<Long>> directNodes) {
        Set<Long> ids = new HashSet<>(directNodes.getOrDefault(tag.getId(), Set.of()));
        if (tag.getChildren() != null) {
            for (KnowledgeTag child : tag.getChildren()) {
                ids.addAll(aggregateNodeIds(child, directNodes));
            }
        }
        tag.setNodeCount(ids.size());
        return ids;
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
    public List<TagSuggestion> suggestTags(String title, String summary, Long userId, Long workspaceId) {
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
            return parseTagSuggestions(aiResponse, userId, workspaceId);
        } catch (Exception e) {
            log.warn("AI标签建议失败 title={} userId={}", title, userId, e);
            return List.of();
        }
    }

    /**
     * 解析AI返回的标签建议，匹配已有标签.
     */
    private List<TagSuggestion> parseTagSuggestions(String aiResponse, Long userId, Long workspaceId) {
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
            List<KnowledgeTag> existingTags = listByUser(userId, workspaceId);

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
