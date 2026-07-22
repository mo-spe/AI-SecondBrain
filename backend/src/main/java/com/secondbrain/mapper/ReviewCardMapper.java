package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.ReviewCard;
import org.apache.ibatis.annotations.Mapper;

/**
 * 复习卡片数据访问接口.
 * <p>提供复习卡片表的数据库操作</p>
 */
@Mapper
public interface ReviewCardMapper extends BaseMapper<ReviewCard> {
}
