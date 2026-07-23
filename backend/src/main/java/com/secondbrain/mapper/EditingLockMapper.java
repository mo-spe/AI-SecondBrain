package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.EditingLock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 编辑锁 Mapper.
 * <p>提供编辑锁的数据库操作</p>
 */
@Mapper
public interface EditingLockMapper extends BaseMapper<EditingLock> {
}
