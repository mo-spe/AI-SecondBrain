package com.secondbrain.service;

import com.secondbrain.entity.ReviewCardPool;
import com.secondbrain.entity.UserReviewCard;
import com.secondbrain.vo.ReviewCardPoolVO;

import java.util.List;

/**
 * 题目池服务接口.
 * <p>管理工作区级复习卡片题目池，包括列表查询、加入复习、池子编辑等</p>
 */
public interface ReviewCardPoolService {

    /**
     * 获取工作区题目池列表（含社区标签和用户加入状态）.
     *
     * @param workspaceId 工作区ID
     * @param userId      当前用户ID
     * @return 题目池VO列表
     */
    List<ReviewCardPoolVO> getPoolList(Long workspaceId, Long userId);

    /**
     * 获取池子题目详情.
     *
     * @param poolId 池子题目ID
     * @param userId 当前用户ID
     * @return 题目池VO
     */
    ReviewCardPoolVO getPoolDetail(Long poolId, Long userId);

    /**
     * 加入复习（生成或返回个人副本）。
     * <p>重复加入保持幂等，已有非归档副本时直接返回原副本；重新开始需要独立的用户操作。</p>
     *
     * @param poolId 池子题目ID
     * @param userId 当前用户ID
     * @return 已存在或新创建的个人副本
     */
    UserReviewCard joinPool(Long poolId, Long userId);

    /**
     * 删除池子题目（仅 owner 可操作，不影响已有个人副本）.
     *
     * @param poolId 池子题目ID
     * @param userId 当前用户ID
     */
    void deletePoolItem(Long poolId, Long userId);

    /**
     * 编辑池子题目（仅 owner 可操作，不影响已有个人副本）.
     *
     * @param poolId   池子题目ID
     * @param question 新题目内容
     * @param answer   新正确答案
     * @param userId   当前用户ID
     * @return 更新后的池子题目
     */
    ReviewCardPool updatePoolItem(Long poolId, String question, String answer, Long userId);
}
