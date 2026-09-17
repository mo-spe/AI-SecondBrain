package com.secondbrain.service;

import com.secondbrain.dto.SetKnowledgeReviewReminderRequest;
import com.secondbrain.vo.KnowledgeReviewReminderVO;

import java.util.List;

/**
 * 知识点指定复习提醒服务。
 *
 * <p>提醒与复习算法的到期时间分离，用户的临时预约不会改写卡片排期。</p>
 */
public interface ReviewReminderService {

    /**
     * 获取当前用户尚未触发的指定提醒。
     *
     * @param userId 当前用户ID
     * @return 按计划时间升序的提醒列表
     */
    List<KnowledgeReviewReminderVO> listReminders(Long userId);

    /**
     * 新建或更新一个知识点的指定提醒。
     *
     * @param userId 当前用户ID
     * @param nodeId 知识点ID
     * @param request 指定提醒时间
     * @return 保存后的提醒
     */
    KnowledgeReviewReminderVO saveReminder(Long userId, Long nodeId, SetKnowledgeReviewReminderRequest request);

    /**
     * 取消当前用户对一个知识点的指定提醒。
     *
     * @param userId 当前用户ID
     * @param nodeId 知识点ID
     */
    void cancelReminder(Long userId, Long nodeId);

    /**
     * 扫描并发送已到期的指定提醒。
     *
     * <p>状态更新与站内通知写入在同一事务中完成，多个调度实例也只会有一个实例成功认领同一提醒。</p>
     */
    void processExpiredReminders();

    /**
     * 发送每日复习摘要通知。
     *
     * <p>当前保留该扩展点，以兼容既有定时任务。</p>
     */
    void sendDailyReviewNotification();

    /**
     * 生成每周复习报告。
     *
     * <p>当前保留该扩展点，以兼容既有定时任务。</p>
     */
    void generateWeeklyReport();
}
