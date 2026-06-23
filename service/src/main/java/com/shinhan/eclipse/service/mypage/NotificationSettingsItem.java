package com.shinhan.eclipse.service.mypage;

public record NotificationSettingsItem(
        Boolean pushEnabled,
        Boolean ipoAllocationEnabled,
        Boolean ipoRefundEnabled,
        Boolean idleDollarEnabled
) {}
