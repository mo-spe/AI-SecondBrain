package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ShareLink;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.ShareLinkMapper;
import com.secondbrain.service.ShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识节点分享服务实现.
 * <p>使用SHA-256生成随机分享令牌，支持三种有效期类型</p>
 */
@Service
public class ShareServiceImpl implements ShareService {

    private static final Logger log = LoggerFactory.getLogger(ShareServiceImpl.class);

    private final ShareLinkMapper shareLinkMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;

    public ShareServiceImpl(ShareLinkMapper shareLinkMapper,
                            KnowledgeNodeMapper knowledgeNodeMapper) {
        this.shareLinkMapper = shareLinkMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
    }

    /**
     * 创建分享链接.
     * 根据expireType计算过期时间：permanent→null，7d→+7天，24h→+24小时。
     */
    @Override
    public Map<String, Object> createShareLink(Long nodeId, Long ownerId, String expireType) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("知识节点不存在: " + nodeId);
        }

        String token = generateToken();

        ShareLink link = new ShareLink();
        link.setNodeId(nodeId);
        link.setOwnerId(ownerId);
        link.setToken(token);
        link.setExpireType(expireType);
        link.setAccessCount(0);
        link.setIsRevoked(0);
        link.setCreatedAt(LocalDateTime.now());

        if ("7d".equals(expireType)) {
            link.setExpiresAt(LocalDateTime.now().plusDays(7));
        } else if ("24h".equals(expireType)) {
            link.setExpiresAt(LocalDateTime.now().plusHours(24));
        }

        shareLinkMapper.insert(link);

        log.info("share_link_created nodeId={} ownerId={} expireType={}", nodeId, ownerId, expireType);

        Map<String, Object> result = new HashMap<>();
        result.put("id", link.getId());
        result.put("token", token);
        result.put("nodeId", nodeId);
        result.put("nodeTitle", node.getTitle());
        result.put("expireType", expireType);
        result.put("expiresAt", link.getExpiresAt());
        result.put("createdAt", link.getCreatedAt());
        return result;
    }

    /**
     * 通过token获取分享内容.
     * 校验有效性（未撤销、未过期），每次访问自动+1。
     */
    @Override
    public Map<String, Object> getShareContent(String token) {
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getToken, token);
        ShareLink link = shareLinkMapper.selectOne(wrapper);

        if (link == null || link.getIsRevoked() == 1) {
            return null;
        }

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }

        KnowledgeNode node = knowledgeNodeMapper.selectById(link.getNodeId());
        if (node == null) {
            return null;
        }

        // 访问计数+1
        ShareLink updateLink = new ShareLink();
        updateLink.setId(link.getId());
        updateLink.setAccessCount(link.getAccessCount() + 1);
        shareLinkMapper.updateById(updateLink);

        Map<String, Object> result = new HashMap<>();
        result.put("title", node.getTitle());
        result.put("summary", node.getSummary());
        result.put("contentMd", node.getContentMd());
        result.put("shareOwnerId", link.getOwnerId());
        result.put("accessCount", link.getAccessCount() + 1);
        result.put("createdAt", link.getCreatedAt());
        return result;
    }

    /**
     * 撤销分享链接.
     * 仅分享者本人可撤销。
     */
    @Override
    public void revokeShare(Long shareId, Long ownerId) {
        ShareLink link = shareLinkMapper.selectById(shareId);
        if (link == null) {
            throw new IllegalArgumentException("分享记录不存在");
        }
        if (!link.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("无权撤销此分享");
        }

        ShareLink updateLink = new ShareLink();
        updateLink.setId(shareId);
        updateLink.setIsRevoked(1);
        shareLinkMapper.updateById(updateLink);

        log.info("share_link_revoked id={} ownerId={}", shareId, ownerId);
    }

    /**
     * 查询我的分享列表.
     */
    @Override
    public List<ShareLink> listMyShares(Long ownerId) {
        LambdaQueryWrapper<ShareLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShareLink::getOwnerId, ownerId)
                .eq(ShareLink::getIsRevoked, 0)
                .orderByDesc(ShareLink::getCreatedAt);
        return shareLinkMapper.selectList(wrapper);
    }

    /**
     * 生成SHA-256随机令牌.
     * 使用UUID + 时间戳组合后哈希，64字符十六进制，不可猜测。
     */
    private String generateToken() {
        try {
            String raw = UUID.randomUUID().toString() + System.nanoTime();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }
}
