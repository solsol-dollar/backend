package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.notification.Notification;
import com.shinhan.eclipse.domain.notification.NotificationSettings;
import com.shinhan.eclipse.service.mypage.NotificationItem;
import com.shinhan.eclipse.service.mypage.NotificationService;
import com.shinhan.eclipse.service.mypage.NotificationSettingsItem;
import com.shinhan.eclipse.service.mypage.PendingPush;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;

    @Override
    @Transactional
    public void initSettings(Long userId) {
        if (notificationSettingsRepository.findByUserId(userId).isEmpty()) {
            notificationSettingsRepository.save(NotificationSettings.create(userId));
        }
    }

    @Override
    public NotificationSettingsItem getNotificationSettings(Long userId) {
        NotificationSettings settings = getSettingsOrThrow(userId);
        return new NotificationSettingsItem(
                settings.getFcmToken() != null,
                settings.getIpoAllocationEnabled(),
                settings.getIpoRefundEnabled(),
                settings.getIdleDollarEnabled()
        );
    }

    @Override
    public Page<NotificationItem> getNotifications(Long userId, Boolean isRead, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Notification> result = (isRead == null)
                ? notificationRepository.findByUserIdAndStatusAndSentAtIsNotNull(userId, "ACTIVE", pageable)
                : notificationRepository.findByUserIdAndIsReadAndStatusAndSentAtIsNotNull(userId, isRead, "ACTIVE", pageable);
        return result.map(this::toItem);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "알림을 찾을 수 없습니다.");
        }
    }

    @Override
    @Transactional
    public void registerFcmToken(Long userId, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "FCM 토큰이 유효하지 않습니다.");
        }
        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return notificationSettingsRepository.save(NotificationSettings.create(userId));
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        return notificationSettingsRepository.findByUserId(userId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "알림 설정을 찾을 수 없습니다."));
                    }
                });
        settings.updateFcmToken(fcmToken);
    }

    @Override
    @Transactional
    public void unregisterFcmToken(Long userId) {
        notificationSettingsRepository.findByUserId(userId).ifPresent(s -> s.updateFcmToken(null));
    }

    @Override
    @Transactional
    public void updateNotificationSettings(Long userId, Boolean ipoAllocation, Boolean ipoRefund, Boolean idleDollar) {
        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return notificationSettingsRepository.save(NotificationSettings.create(userId));
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        return notificationSettingsRepository.findByUserId(userId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "알림 설정을 찾을 수 없습니다."));
                    }
                });
        settings.updateSettings(ipoAllocation, ipoRefund, idleDollar);
    }

    @Override
    public List<PendingPush> getPendingPushes() {
        List<Notification> pending = notificationRepository.findBySentAtIsNullAndStatus("ACTIVE", PageRequest.of(0, 100, Sort.by("createdAt").ascending()));
        if (pending.isEmpty()) return List.of();

        List<Long> userIds = pending.stream().map(Notification::getUserId).distinct().toList();
        Map<Long, NotificationSettings> settingsByUserId = notificationSettingsRepository
                .findByUserIdInAndFcmTokenIsNotNull(userIds)
                .stream()
                .collect(Collectors.toMap(NotificationSettings::getUserId, s -> s));

        List<PendingPush> result = new ArrayList<>();
        for (Notification n : pending) {
            NotificationSettings s = settingsByUserId.get(n.getUserId());
            if (s != null && isTypeEnabled(s, n.getNotificationType())) {
                result.add(new PendingPush(n.getId(), n.getUserId(), s.getFcmToken(), n.getTitle(), n.getMessage(), n.getNotificationType(), n.getTargetId()));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void markAsSent(Long notificationId) {
        notificationRepository.updateSentAt(notificationId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public Long createNotification(Long userId, String notificationType, String title, String message, String targetType, Long targetId) {
        return notificationRepository.save(Notification.create(userId, notificationType, title, message, targetType, targetId)).getId();
    }

    @Override
    public List<Long> getAllUserIdsWithFcmToken() {
        return notificationSettingsRepository.findAllUserIdsWithFcmToken();
    }

    private boolean isTypeEnabled(NotificationSettings settings, String notificationType) {
        return switch (notificationType) {
            case "IPO_ALLOCATION" -> settings.getIpoAllocationEnabled();
            case "IPO_REFUND" -> settings.getIpoRefundEnabled();
            case "IDLE_DOLLAR" -> settings.getIdleDollarEnabled();
            case "SPENDING_REPORT" -> true;
            default -> false;
        };
    }

    private NotificationSettings getSettingsOrThrow(Long userId) {
        return notificationSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "알림 설정을 찾을 수 없습니다."));
    }

    private NotificationItem toItem(Notification n) {
        return new NotificationItem(
                n.getId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.getTargetType(),
                n.getTargetId(),
                n.getIsRead(),
                n.getSentAt()
        );
    }
}
