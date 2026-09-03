package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.UserBlock;
import org.apache.ibatis.annotations.Mapper;

/** 用户拉黑关系数据访问接口。 */
@Mapper
public interface UserBlockMapper extends BaseMapper<UserBlock> {
}
