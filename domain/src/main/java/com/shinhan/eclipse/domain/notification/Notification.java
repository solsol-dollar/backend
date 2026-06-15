package com.shinhan.eclipse.domain.notification;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 30)
    private String notificationType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 50)
    private String targetType;

    private Long targetId;

    @Column(nullable = false)
    private Boolean isRead = false;

    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
