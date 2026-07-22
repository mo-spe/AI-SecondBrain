package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.ResearchHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 研究历史数据访问接口.
 * <p>提供研究历史表的数据库操作</p>
 */
@Mapper
public interface ResearchHistoryMapper extends BaseMapper<ResearchHistory> {
}
