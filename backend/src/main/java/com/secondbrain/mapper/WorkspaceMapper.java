package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.Workspace;
import org.apache.ibatis.annotations.Mapper;

/** 工作区数据访问接口. <p>提供工作区表的数据库操作</p> */
@Mapper
public interface WorkspaceMapper extends BaseMapper<Workspace> {
}
