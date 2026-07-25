package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.dto.CommunityLabelDTO;
import com.secondbrain.entity.UserReviewCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户复习卡片（个人副本）Mapper.
 */
@Mapper
public interface UserReviewCardMapper extends BaseMapper<UserReviewCard> {

    /**
     * 批量获取池子题目的社区统计数据.
     * <p>统计每个 pool 的复习人数和已掌握人数，用于计算社区标签</p>
     *
     * @param poolIds 池子题目ID列表
     * @return 社区统计列表
     */
    @Select("<script>"
            + "SELECT pool_id, COUNT(*) AS member_count, "
            + "SUM(CASE WHEN mastery_level >= 4 THEN 1 ELSE 0 END) AS mastered_count "
            + "FROM user_review_card "
            + "WHERE is_archived = 0 "
            + "AND pool_id IN "
            + "<foreach collection='poolIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY pool_id"
            + "</script>")
    List<CommunityLabelDTO> batchGetCommunityStats(@Param("poolIds") List<Long> poolIds);
}
