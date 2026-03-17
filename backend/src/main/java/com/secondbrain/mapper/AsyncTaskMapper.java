package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.AsyncTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AsyncTaskMapper extends BaseMapper<AsyncTask> {
}