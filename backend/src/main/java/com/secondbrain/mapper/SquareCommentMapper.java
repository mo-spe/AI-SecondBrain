package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.SquareComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper.
 */
@Mapper
public interface SquareCommentMapper extends BaseMapper<SquareComment> {
}
