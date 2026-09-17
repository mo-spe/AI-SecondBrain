package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.SquareCommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞记录 Mapper.
 */
@Mapper
public interface SquareCommentLikeMapper extends BaseMapper<SquareCommentLike> {
}
