package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.CommunityAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社区回答数据访问接口。
 *
 * @author AI
 */
@Mapper
public interface CommunityAnswerMapper extends BaseMapper<CommunityAnswer> {
}
