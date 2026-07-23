package com.secondbrain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.secondbrain.entity.Notification;

/**
 * 广场互动通知服务接口.
 *
 * <p>负责广场互动通知（点赞、评论、举报结果）的创建、查询和已读标记。</p>
 */
public interface SquareNotificationService {

    /**
     * 创建通知.
     *
     * @param userId     接收通知的用户ID
     * @param type       通知类型
     * @param title      通知标题
     * @param content    通知内容
     * @param targetType 关联目标类型
     * @param targetId   关联目标ID
     */
    void create(Long userId, String type, String title, String content, String targetType, Long targetId);

    /**
     * 通知列表（按时间倒序）.
     *
     * @param userId  当前用户ID
     * @param current 当前页
     * @param size    每页大小
     * @return 分页通知列表
     */
    IPage<Notification> list(Long userId, Integer current, Integer size);

    /**
     * 未读通知数.
     *
     * @param userId 当前用户ID
     * @return 未读数量
     */
    long getUnreadCount(Long userId);

    /**
     * 标记单条通知已读.
     *
     * @param notificationId 通知ID
     * @param userId         当前用户ID
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 全部标记已读.
     *
     * @param userId 当前用户ID
     */
    void markAllAsRead(Long userId);
}
