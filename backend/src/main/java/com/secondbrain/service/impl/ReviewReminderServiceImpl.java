package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.secondbrain.dto.SetKnowledgeReviewReminderRequest;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.KnowledgeReviewReminder;
import com.secondbrain.entity.WorkspaceMember;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.mapper.KnowledgeReviewReminderMapper;
import com.secondbrain.mapper.WorkspaceMemberMapper;
import com.secondbrain.service.NotificationService;
import com.secondbrain.service.ReviewPreferenceService;
import com.secondbrain.service.ReviewReminderService;
import com.secondbrain.service.SquareNotificationService;
import com.secondbrain.vo.KnowledgeReviewReminderVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 知识点指定复习提醒服务实现。
 *
 * <p>数据库是提醒状态的唯一事实来源，避免缓存过期、服务重启或多实例调度造成漏发或重复发送。</p>
 */
@Service
public class ReviewReminderServiceImpl implements ReviewReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReviewReminderServiceImpl.class);

    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_SENT = "SENT";
    private static final String NOTIFICATION_TYPE = "review_reminder";
    private static final String NOTIFICATION_TARGET_TYPE = "knowledge";
    private static final int DUE_REMINDER_BATCH_SIZE = 100;
    private static final DateTimeFormatter REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final KnowledgeReviewReminderMapper reminderMapper;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;
    private final ReviewPreferenceService reviewPreferenceService;
    private final SquareNotificationService notificationCenterService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    public ReviewReminderServiceImpl(KnowledgeReviewReminderMapper reminderMapper,
                                     KnowledgeNodeMapper knowledgeNodeMapper,
                                     WorkspaceMemberMapper workspaceMemberMapper,
                                     ReviewPreferenceService reviewPreferenceService,
                                     SquareNotificationService notificationCenterService,
                                     NotificationService notificationService,
                                     TransactionTemplate transactionTemplate) {
        this.reminderMapper = reminderMapper;
        this.knowledgeNodeMapper = knowledgeNodeMapper;
        this.workspaceMemberMapper = workspaceMemberMapper;
        this.reviewPreferenceService = reviewPreferenceService;
        this.notificationCenterService = notificationCenterService;
        this.notificationService = notificationService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 获取当前用户尚未触发的指定提醒。
     *
     * @param userId 当前用户ID
     * @return 待触发提醒列表
     */
    @Override
    public List<KnowledgeReviewReminderVO> listReminders(Long userId) {
        List<KnowledgeReviewReminder> reminders = reminderMapper.selectList(
                new LambdaQueryWrapper<KnowledgeReviewReminder>()
                        .eq(KnowledgeReviewReminder::getUserId, userId)
                        .eq(KnowledgeReviewReminder::getStatus, STATUS_SCHEDULED)
                        .orderByAsc(KnowledgeReviewReminder::getScheduledAt));
        if (reminders.isEmpty()) {
            return List.of();
        }

        Map<Long, KnowledgeNode> nodeById = knowledgeNodeMapper.selectBatchIds(
                        reminders.stream().map(KnowledgeReviewReminder::getNodeId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(KnowledgeNode::getId, node -> node));
        return reminders.stream()
                .map(reminder -> toView(reminder, nodeById.get(reminder.getNodeId())))
                .toList();
    }

    /**
     * 新建或更新知识点的指定提醒。
     *
     * @param userId 当前用户ID
     * @param nodeId 知识点ID
     * @param request 指定提醒时间
     * @return 保存后的提醒
     */
    @Override
    @Transactional
    public KnowledgeReviewReminderVO saveReminder(Long userId, Long nodeId, SetKnowledgeReviewReminderRequest request) {
        LocalDateTime scheduledAt = request.getScheduledAt();
        if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "提醒时间必须晚于当前时间");
        }

        KnowledgeNode node = requireAccessibleNode(userId, nodeId);
        KnowledgeReviewReminder existing = reminderMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeReviewReminder>()
                        .eq(KnowledgeReviewReminder::getUserId, userId)
                        .eq(KnowledgeReviewReminder::getNodeId, nodeId));

        KnowledgeReviewReminder reminder;
        if (existing == null) {
            reminder = new KnowledgeReviewReminder();
            reminder.setUserId(userId);
            reminder.setNodeId(nodeId);
            reminder.setScheduledAt(scheduledAt);
            reminder.setStatus(STATUS_SCHEDULED);
            reminderMapper.insert(reminder);
        } else {
            // 重新预约时必须清掉历史发送标记，否则下一次扫描会误把它视为已经处理。
            reminderMapper.update(null, new LambdaUpdateWrapper<KnowledgeReviewReminder>()
                    .eq(KnowledgeReviewReminder::getId, existing.getId())
                    .set(KnowledgeReviewReminder::getScheduledAt, scheduledAt)
                    .set(KnowledgeReviewReminder::getStatus, STATUS_SCHEDULED)
                    .set(KnowledgeReviewReminder::getSentAt, null));
            existing.setScheduledAt(scheduledAt);
            existing.setStatus(STATUS_SCHEDULED);
            existing.setSentAt(null);
            reminder = existing;
        }

        log.info("knowledge_review_reminder_saved userId={} nodeId={} scheduledAt={}", userId, nodeId, scheduledAt);
        return toView(reminder, node);
    }

    /**
     * 取消当前用户对一个知识点的指定提醒。
     *
     * @param userId 当前用户ID
     * @param nodeId 知识点ID
     */
    @Override
    @Transactional
    public void cancelReminder(Long userId, Long nodeId) {
        reminderMapper.delete(new LambdaQueryWrapper<KnowledgeReviewReminder>()
                .eq(KnowledgeReviewReminder::getUserId, userId)
                .eq(KnowledgeReviewReminder::getNodeId, nodeId));
        log.info("knowledge_review_reminder_cancelled userId={} nodeId={}", userId, nodeId);
    }

    /**
     * 扫描并发送到期的指定提醒。
     */
    @Override
    public void processExpiredReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<KnowledgeReviewReminder> dueReminders = reminderMapper.selectList(
                new LambdaQueryWrapper<KnowledgeReviewReminder>()
                        .eq(KnowledgeReviewReminder::getStatus, STATUS_SCHEDULED)
                        .le(KnowledgeReviewReminder::getScheduledAt, now)
                        .orderByAsc(KnowledgeReviewReminder::getScheduledAt)
                        .last("LIMIT " + DUE_REMINDER_BATCH_SIZE));

        for (KnowledgeReviewReminder reminder : dueReminders) {
            try {
                ReminderDelivery delivery = transactionTemplate.execute(status -> claimAndCreateNotification(reminder));
                if (delivery != null && reviewPreferenceService.isReviewEmailEnabled(delivery.userId)) {
                    // 邮件在站内通知事务提交后发送，邮件服务异常不会回滚已经可靠持久化的提醒。
                    notificationService.sendReviewReminder(delivery.userId, delivery.nodeTitle,
                            delivery.scheduledAt.format(REMINDER_TIME_FORMATTER));
                }
            } catch (RuntimeException exception) {
                log.error("knowledge_review_reminder_delivery_failed reminderId={}", reminder.getId(), exception);
            }
        }

        if (!dueReminders.isEmpty()) {
            log.info("knowledge_review_reminder_due_processed count={}", dueReminders.size());
        }
    }

    /**
     * 发送每日复习摘要通知。
     */
    @Override
    public void sendDailyReviewNotification() {
        log.debug("daily_review_notification_delegated_to_existing_review_job");
    }

    /**
     * 生成每周复习报告。
     */
    @Override
    public void generateWeeklyReport() {
        log.debug("weekly_review_report_delegated_to_existing_review_job");
    }

    private ReminderDelivery claimAndCreateNotification(KnowledgeReviewReminder reminder) {
        LocalDateTime sentAt = LocalDateTime.now();
        int updated = reminderMapper.update(null, new LambdaUpdateWrapper<KnowledgeReviewReminder>()
                .eq(KnowledgeReviewReminder::getId, reminder.getId())
                .eq(KnowledgeReviewReminder::getStatus, STATUS_SCHEDULED)
                .le(KnowledgeReviewReminder::getScheduledAt, sentAt)
                .set(KnowledgeReviewReminder::getStatus, STATUS_SENT)
                .set(KnowledgeReviewReminder::getSentAt, sentAt));
        if (updated != 1) {
            return null;
        }

        KnowledgeNode node = knowledgeNodeMapper.selectById(reminder.getNodeId());
        String nodeTitle = node == null || node.getTitle() == null || node.getTitle().isBlank()
                ? "知识点"
                : node.getTitle();
        notificationCenterService.create(reminder.getUserId(), NOTIFICATION_TYPE, "知识点复习提醒",
                "你预定的知识点「" + nodeTitle + "」现在可以复习了", NOTIFICATION_TARGET_TYPE, reminder.getNodeId());
        return new ReminderDelivery(reminder.getUserId(), nodeTitle, reminder.getScheduledAt());
    }

    private KnowledgeNode requireAccessibleNode(Long userId, Long nodeId) {
        KnowledgeNode node = knowledgeNodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(404, "知识点不存在");
        }
        if (Objects.equals(node.getUserId(), userId) || isAcceptedWorkspaceMember(node.getWorkspaceId(), userId)) {
            return node;
        }
        throw new BusinessException(403, "无权为该知识点设置提醒");
    }

    private boolean isAcceptedWorkspaceMember(Long workspaceId, Long userId) {
        if (workspaceId == null || workspaceId <= 0) {
            return false;
        }
        WorkspaceMember member = workspaceMemberMapper.selectOne(new LambdaQueryWrapper<WorkspaceMember>()
                .eq(WorkspaceMember::getWorkspaceId, workspaceId)
                .eq(WorkspaceMember::getUserId, userId)
                // 旧数据没有状态值时代表已经生效的成员，pending 邀请则不能访问工作区知识。
                .and(status -> status
                        .eq(WorkspaceMember::getStatus, "accepted")
                        .or()
                        .isNull(WorkspaceMember::getStatus))
                .and(deleted -> deleted
                        .eq(WorkspaceMember::getDeleted, 0)
                        .or()
                        .isNull(WorkspaceMember::getDeleted)));
        return member != null;
    }

    private KnowledgeReviewReminderVO toView(KnowledgeReviewReminder reminder, KnowledgeNode node) {
        KnowledgeReviewReminderVO view = new KnowledgeReviewReminderVO();
        view.setId(reminder.getId());
        view.setNodeId(reminder.getNodeId());
        view.setNodeTitle(node == null || node.getTitle() == null ? "已删除知识点" : node.getTitle());
        view.setScheduledAt(reminder.getScheduledAt());
        view.setStatus(reminder.getStatus());
        view.setSentAt(reminder.getSentAt());
        return view;
    }

    private static final class ReminderDelivery {

        private final Long userId;
        private final String nodeTitle;
        private final LocalDateTime scheduledAt;

        private ReminderDelivery(Long userId, String nodeTitle, LocalDateTime scheduledAt) {
            this.userId = userId;
            this.nodeTitle = nodeTitle;
            this.scheduledAt = scheduledAt;
        }
    }
}
