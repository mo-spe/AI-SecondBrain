package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.VocabularyArticle;
import org.apache.ibatis.annotations.Mapper;

/** 用户词表文章的持久化入口。 */
@Mapper
public interface VocabularyArticleMapper extends BaseMapper<VocabularyArticle> {
}
