package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.ShareLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分享链接 Mapper（预留V1.1）.
 * <p>提供分享链接的数据库操作</p>
 */
@Mapper
public interface ShareLinkMapper extends BaseMapper<ShareLink> {
}
