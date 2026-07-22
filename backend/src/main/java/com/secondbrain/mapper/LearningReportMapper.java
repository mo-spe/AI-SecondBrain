package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.LearningReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学习报告数据访问接口.
 * <p>提供学习报告表的数据库操作</p>
 */
@Mapper
public interface LearningReportMapper extends BaseMapper<LearningReport> {
}
