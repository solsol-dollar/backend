package com.shinhan.eclipse.service.mypage;

import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {
    void initSettings(Long userId);
    List<PendingPush> getPendingPushes();
    void markAsSent(Long notificationId);
    Long createNotification(Long userId, String notificationType, String title, String message, String targetType, Long targetId);
    NotificationSettingsItem getNotificationSettings(Long userId);
    Page<NotificationItem> getNotifications(Long userId, Boolean isRead, int page, int size);
    void markAsRead(Long userId, Long notificationId);
    void registerFcmToken(Long userId, String fcmToken);
    void unregisterFcmToken(Long userId);
    void updateNotificationSettings(Long userId, Boolean ipoAllocation, Boolean ipoRefund, Boolean idleDollar, Boolean spendingReport);

    List<Long> getAllUserIdsWithFcmToken();
}
