package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.UserReviewPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户复习偏好数据访问接口。
 */
@Mapper
public interface UserReviewPreferenceMapper extends BaseMapper<UserReviewPreference> {
}
