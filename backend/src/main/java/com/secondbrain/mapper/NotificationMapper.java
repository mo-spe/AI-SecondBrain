package com.secondbrain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.secondbrain.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper.
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
