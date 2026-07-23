package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.SquareLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞记录 Mapper.
 */
@Mapper
public interface SquareLikeMapper extends BaseMapper<SquareLike> {
}
