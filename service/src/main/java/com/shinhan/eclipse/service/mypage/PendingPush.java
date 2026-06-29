package com.shinhan.eclipse.service.mypage;

public record PendingPush(
        Long notificationId,
        Long userId,
        String fcmToken,
        String title,
        String message,
        String notificationType,
        Long targetId
) {}
