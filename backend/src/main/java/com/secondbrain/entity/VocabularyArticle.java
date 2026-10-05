package com.secondbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 用户私有的词表文章，原始截图不进入持久化记录。 */
@Getter
@Setter
@TableName("vocabulary_article")
public class VocabularyArticle {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 创建者。 */
    private Long userId;
    /** 用户确认后的词表 JSON。 */
    private String wordsJson;
    /** 英文正文。 */
    private String article;
    /** 目标词的语境释义 JSON。 */
    private String meaningsJson;
    /** 程序复核后仍缺失的词 JSON。 */
    private String missingWordsJson;
    /** 文章题材。 */
    private String topic;
    /** 阅读难度。 */
    private String difficulty;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
