package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.dto.UpdateCommunityProfileRequest;
import com.secondbrain.vo.CommunityUserListItemVO;
import com.secondbrain.vo.CommunityUserProfileVO;

/** 社区用户资料与关系服务。 */
public interface CommunityUserService {

    /**
     * 获取公开个人主页。
     *
     * @param profileUserId 主页用户ID
     * @param viewerId 当前访问者ID
     * @return 公开主页
     */
    CommunityUserProfileVO getProfile(Long profileUserId, Long viewerId);

    /**
     * 更新当前用户的社区公开资料。
     *
     * @param userId 当前用户ID
     * @param request 公开资料
     * @return 更新后的主页
     */
    CommunityUserProfileVO updateProfile(Long userId, UpdateCommunityProfileRequest request);

    /**
     * 关注指定用户。
     *
     * @param followerId 当前用户ID
     * @param followedId 目标用户ID
     */
    void follow(Long followerId, Long followedId);

    /**
     * 取消关注指定用户。
     *
     * @param followerId 当前用户ID
     * @param followedId 目标用户ID
     */
    void unfollow(Long followerId, Long followedId);

    /**
     * 分页查询粉丝。
     *
     * @param userId 主页用户ID
     * @param viewerId 当前访问者ID
     * @param current 当前页
     * @param size 每页大小
     * @return 粉丝分页
     */
    IPage<CommunityUserListItemVO> listFollowers(Long userId, Long viewerId, Integer current, Integer size);

    /**
     * 分页查询关注用户。
     *
     * @param userId 主页用户ID
     * @param viewerId 当前访问者ID
     * @param current 当前页
     * @param size 每页大小
     * @return 关注分页
     */
    IPage<CommunityUserListItemVO> listFollowing(Long userId, Long viewerId, Integer current, Integer size);

    /**
     * 拉黑指定用户并解除双方关注。
     *
     * @param blockerId 当前用户ID
     * @param blockedId 目标用户ID
     */
    void block(Long blockerId, Long blockedId);

    /**
     * 解除当前用户发起的拉黑。
     *
     * @param blockerId 当前用户ID
     * @param blockedId 目标用户ID
     */
    void unblock(Long blockerId, Long blockedId);
}
