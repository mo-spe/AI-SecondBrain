package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.KnowledgeReviewReminder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识点指定复习提醒数据访问接口。
 */
@Mapper
public interface KnowledgeReviewReminderMapper extends BaseMapper<KnowledgeReviewReminder> {
}
