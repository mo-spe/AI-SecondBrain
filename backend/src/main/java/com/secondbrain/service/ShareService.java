package com.secondbrain.service;

import com.secondbrain.entity.ShareLink;

import java.util.List;
import java.util.Map;

/**
 * 知识节点分享服务接口.
 * <p>提供分享链接的创建、访问、撤销和查询功能</p>
 */
public interface ShareService {

    /**
     * 创建分享链接.
     *
     * @param nodeId     知识节点ID
     * @param ownerId    分享者用户ID
     * @param expireType 有效期类型：permanent/7d/24h
     * @return 分享链接信息（含完整URL）
     */
    Map<String, Object> createShareLink(Long nodeId, Long ownerId, String expireType);

    /**
     * 通过token获取分享内容（公开访问）.
     * 每次访问自动增加计数。
     *
     * @param token 分享令牌
     * @return 分享的知识节点内容 + 分享信息，已过期/已撤销返回null
     */
    Map<String, Object> getShareContent(String token);

    /**
     * 撤销分享链接.
     * 仅分享者可撤销。
     *
     * @param shareId 分享记录ID
     * @param ownerId 操作用户ID
     */
    void revokeShare(Long shareId, Long ownerId);

    /**
     * 查询我的分享列表（含访问统计）.
     *
     * @param ownerId 分享者用户ID
     * @return 分享记录列表
     */
    List<ShareLink> listMyShares(Long ownerId);
}
