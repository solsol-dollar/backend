package com.shinhan.eclipse.service.card.internal;

import com.shinhan.eclipse.service.card.SpendingReportService;
import com.shinhan.eclipse.service.mypage.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class SpendingReportServiceImpl implements SpendingReportService {

    private final NotificationService notificationService;

    @Override
    public void generateAll(int year, int month) {
        List<Long> userIds = notificationService.getAllUserIdsWithFcmToken();
        log.info("[소비리포트] {}년 {}월 발송 시작: 대상 {}명", year, month, userIds.size());

        int success = 0;
        for (Long userId : userIds) {
            try {
                sendNotification(userId, month);
                success++;
            } catch (Exception e) {
                log.warn("[소비리포트] userId={} 알림 생성 실패: {}", userId, e.getMessage());
            }
        }
        log.info("[소비리포트] 완료: 성공={} / 전체={}", success, userIds.size());
    }

    private void sendNotification(Long userId, int month) {
        notificationService.createNotification(
                userId, "SPENDING_REPORT",
                month + "월 소비 리포트가 도착했어요!",
                "체인지업 체크카드 사용현황을 확인해보세요.",
                "CARD", null);
    }
}
