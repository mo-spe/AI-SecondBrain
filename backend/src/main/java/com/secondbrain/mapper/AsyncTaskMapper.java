package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.AsyncTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异步任务数据访问接口.
 * <p>提供异步任务表的数据库操作</p>
 */
@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTask> {
}
