package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.ReviewLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 复习日志数据访问接口.
 * <p>提供复习日志表的数据库操作</p>
 */
@Mapper
public interface ReviewLogMapper extends BaseMapper<ReviewLog> {
}
