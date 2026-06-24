package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.domain.notification.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByUserId(Long userId);

    List<NotificationSettings> findByUserIdInAndFcmTokenIsNotNull(List<Long> userIds);
}
