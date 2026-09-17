package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.CommunityQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区问题数据访问接口。
 *
 * @author AI
 */
@Mapper
public interface CommunityQuestionMapper extends BaseMapper<CommunityQuestion> {
}
