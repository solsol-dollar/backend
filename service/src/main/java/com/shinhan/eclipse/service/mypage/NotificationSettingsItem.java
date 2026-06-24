package com.shinhan.eclipse.service.mypage;

public record NotificationSettingsItem(
        Boolean fcmRegistered,
        Boolean ipoAllocationEnabled,
        Boolean ipoRefundEnabled,
        Boolean idleDollarEnabled
) {}
