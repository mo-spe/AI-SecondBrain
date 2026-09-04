package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.SquarePostNode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 广场帖子知识节点关联数据访问接口。
 *
 * <p>通过独立关联表支持一个帖子发布多个知识点，而不影响旧帖的单节点字段。</p>
 */
@Mapper
public interface SquarePostNodeMapper extends BaseMapper<SquarePostNode> {
}
