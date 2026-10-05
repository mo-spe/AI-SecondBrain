package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.WrongQuestionReviewLog;
import org.apache.ibatis.annotations.Mapper;

/** 错题复盘历史数据访问接口。 */
@Mapper
public interface WrongQuestionReviewLogMapper extends BaseMapper<WrongQuestionReviewLog> {
}
