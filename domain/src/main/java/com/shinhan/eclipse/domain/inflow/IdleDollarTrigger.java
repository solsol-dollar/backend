package com.shinhan.eclipse.domain.inflow;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "idle_dollar_triggers")
public class IdleDollarTrigger extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    private Long accountId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal idleBalance;

    private Integer idleDays;

    @Column(nullable = false, length = 30)
    private String triggerStatus = "TRIGGERED";

    @Column(length = 50)
    private String suppressionReason;

    private Long notificationId;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    private LocalDateTime notifiedAt;
    private LocalDateTime invalidatedAt;
}
