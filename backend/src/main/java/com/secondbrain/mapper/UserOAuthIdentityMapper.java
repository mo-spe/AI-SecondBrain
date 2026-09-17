package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.UserOAuthIdentity;
import org.apache.ibatis.annotations.Mapper;

/** 第三方登录身份映射数据访问接口。 */
@Mapper
public interface UserOAuthIdentityMapper extends BaseMapper<UserOAuthIdentity> {
}
