package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口.
 * <p>提供用户表的数据库操作</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
