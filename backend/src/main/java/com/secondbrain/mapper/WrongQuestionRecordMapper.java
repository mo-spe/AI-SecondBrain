package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.WrongQuestionRecord;
import org.apache.ibatis.annotations.Mapper;

/** 错题档案数据访问接口。 */
@Mapper
public interface WrongQuestionRecordMapper extends BaseMapper<WrongQuestionRecord> {
}
