package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeRevision;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.KnowledgeRevisionMapper;
import com.secondbrain.service.KnowledgeRevisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识节点版本历史服务实现.
 * <p>每次更新知识节点后自动保存版本快照，支持历史查询和回滚</p>
 */
@Service
public class KnowledgeRevisionServiceImpl implements KnowledgeRevisionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRevisionServiceImpl.class);

    private final KnowledgeRevisionMapper revisionMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    public KnowledgeRevisionServiceImpl(KnowledgeRevisionMapper revisionMapper,
                                         KnowledgeNodeMapper knowledgeNodeMapper) {
        this.revisionMapper = revisionMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    /**
     * 保存版本快照.
     * 查询当前节点最大版本号+1作为新版本号，将传入的内容写入knowledge_revision表。
     */
    @Override
    public void saveRevision(Long nodeId, Long userId, String title, String contentMd, String summary) {
        int nextRevisionNum = getNextRevisionNum(nodeId);

        KnowledgeRevision revision = new KnowledgeRevision();
        revision.setNodeId(nodeId);
        revision.setUserId(userId);
        revision.setTitle(title);
        revision.setContentMd(contentMd);
        revision.setSummary(summary);
        revision.setRevisionNum(nextRevisionNum);
        revision.setCreatedAt(LocalDateTime.now());

        revisionMapper.insert(revision);
        log.info("revision_saved nodeId={} revisionNum={} userId={}", nodeId, nextRevisionNum, userId);
    }

    /**
     * 获取版本历史列表（按版本号降序）.
     */
    @Override
    public List<KnowledgeRevision> getRevisionList(Long nodeId) {
        LambdaQueryWrapper<KnowledgeRevision> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRevision::getNodeId, nodeId)
                .orderByDesc(KnowledgeRevision::getRevisionNum);
        return revisionMapper.selectList(wrapper);
    }

    /**
     * 获取版本详情.
     */
    @Override
    public KnowledgeRevision getRevisionDetail(Long revisionId) {
        return revisionMapper.selectById(revisionId);
    }

    /**
     * 回滚到指定版本.
     * 读取目标版本内容覆盖knowledge_node，同时创建新版本记录标记回滚操作。
     */
    @Override
    public void rollback(Long nodeId, Long revisionId, Long userId) {
        KnowledgeRevision targetRevision = revisionMapper.selectById(revisionId);
        if (targetRevision == null) {
            throw new IllegalArgumentException("版本不存在: " + revisionId);
        }
        if (!targetRevision.getNodeId().equals(nodeId)) {
            throw new IllegalArgumentException("版本不属于该知识节点");
        }

        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("知识节点不存在: " + nodeId);
        }

        KnowledgeNode updateNode = new KnowledgeNode();
        updateNode.setId(nodeId);
        updateNode.setTitle(targetRevision.getTitle());
        updateNode.setContentMd(targetRevision.getContentMd());
        updateNode.setSummary(targetRevision.getSummary());
        knowledgeNodeMapper.updateById(updateNode);

        saveRevision(nodeId, userId, targetRevision.getTitle(), targetRevision.getContentMd(),
                "[回滚] 恢复到版本 #" + targetRevision.getRevisionNum());

        log.info("revision_rollback nodeId={} fromRevision={} userId={}", nodeId, targetRevision.getRevisionNum(), userId);
    }

    /**
     * 获取下一个版本号.
     */
    private int getNextRevisionNum(Long nodeId) {
        LambdaQueryWrapper<KnowledgeRevision> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeRevision::getNodeId, nodeId)
                .orderByDesc(KnowledgeRevision::getRevisionNum)
                .last("LIMIT 1");
        KnowledgeRevision latest = revisionMapper.selectOne(wrapper);
        return latest != null ? latest.getRevisionNum() + 1 : 1;
    }
}
