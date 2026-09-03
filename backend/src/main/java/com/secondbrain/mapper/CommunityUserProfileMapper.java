package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.CommunityUserProfile;
import org.apache.ibatis.annotations.Mapper;

/** 社区公开资料数据访问接口。 */
@Mapper
public interface CommunityUserProfileMapper extends BaseMapper<CommunityUserProfile> {
}
