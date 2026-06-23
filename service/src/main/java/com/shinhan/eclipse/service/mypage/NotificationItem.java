package com.shinhan.eclipse.service.mypage;

import java.time.LocalDateTime;

public record NotificationItem(
        Long notificationId,
        String notificationType,
        String title,
        String message,
        String targetType,
        Long targetId,
        Boolean isRead,
        LocalDateTime sentAt
) {}
