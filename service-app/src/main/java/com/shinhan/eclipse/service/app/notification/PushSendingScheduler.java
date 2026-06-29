package com.shinhan.eclipse.service.app.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.shinhan.eclipse.service.mypage.NotificationService;
import com.shinhan.eclipse.service.mypage.PendingPush;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushSendingScheduler {

    private final NotificationService notificationService;
    private final FirebaseMessaging firebaseMessaging;

    private String resolveUrl(String targetType, Long targetId) {
        if (targetType == null) return "/home";
        return switch (targetType) {
            case "IPO_ALLOCATION" -> targetId != null ? "/ipo?scratchId=" + targetId : "/ipo";
            case "IPO_REFUND" -> targetId != null ? "/return-plan/result/" + targetId : "/return-plan";
            case "IDLE_DOLLAR" -> "/home/sleeping-dollar";
            case "SPENDING_REPORT" -> "/home/card/history";
            default -> "/home";
        };
    }

    @Scheduled(fixedDelay = 10_000)
    public void sendPendingPushes() {
        List<PendingPush> pushes = notificationService.getPendingPushes();
        if (pushes.isEmpty()) return;

        for (PendingPush push : pushes) {
            try {
                Message message = Message.builder()
                        .setToken(push.fcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(push.title())
                                .setBody(push.message())
                                .build())
                        .putData("url", resolveUrl(push.targetType(), push.targetId()))
                        .build();
                firebaseMessaging.send(message);
                notificationService.markAsSent(push.notificationId());
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    notificationService.unregisterFcmToken(push.userId());
                    log.warn("FCM 토큰 만료로 해제: userId={}", push.userId());
                } else {
                    log.error("FCM 발송 실패: notificationId={}, error={}", push.notificationId(), e.getMessage());
                }
            }
        }
    }
}
