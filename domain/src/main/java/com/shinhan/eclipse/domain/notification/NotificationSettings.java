package com.shinhan.eclipse.domain.notification;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "notification_settings")
public class NotificationSettings extends BaseEntity {

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(length = 500)
    private String fcmToken;

    @Column(nullable = false)
    private Boolean ipoAllocationEnabled = true;

    @Column(nullable = false)
    private Boolean ipoRefundEnabled = true;

    @Column(nullable = false)
    private Boolean idleDollarEnabled = true;

    @Column(nullable = false)
    private Boolean spendingReportEnabled = true;

    public static NotificationSettings create(Long userId) {
        NotificationSettings settings = new NotificationSettings();
        settings.userId = userId;
        return settings;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateSettings(Boolean ipoAllocation, Boolean ipoRefund, Boolean idleDollar, Boolean spendingReport) {
        if (ipoAllocation != null) this.ipoAllocationEnabled = ipoAllocation;
        if (ipoRefund != null) this.ipoRefundEnabled = ipoRefund;
        if (idleDollar != null) this.idleDollarEnabled = idleDollar;
        if (spendingReport != null) this.spendingReportEnabled = spendingReport;
    }
}
