package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;

/** 用户关注关系数据访问接口。 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {
}
