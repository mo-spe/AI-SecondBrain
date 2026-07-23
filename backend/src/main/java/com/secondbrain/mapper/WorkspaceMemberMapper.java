package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.WorkspaceMember;
import org.apache.ibatis.annotations.Mapper;

/** 工作区成员数据访问接口. <p>提供工作区成员表的数据库操作</p> */
@Mapper
public interface WorkspaceMemberMapper extends BaseMapper<WorkspaceMember> {
}
