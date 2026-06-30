package com.shinhan.eclipse.service.app.api;

import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.response.ApiResponse;
import com.shinhan.eclipse.service.mypage.NotificationItem;
import com.shinhan.eclipse.service.mypage.NotificationService;
import com.shinhan.eclipse.service.mypage.NotificationSettingsItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** MY-003: 알림 목록 조회 */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthUser authUser) {
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상이어야 합니다.");
        if (size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1~100 사이어야 합니다.");
        List<NotificationItem> notifications = notificationService.getNotifications(authUser.userId(), isRead, page, size).getContent();
        return ResponseEntity.ok(ApiResponse.success(new NotificationListResponse(notifications)));
    }

    /** MY-004: 알림 읽음 처리 */
    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal AuthUser authUser) {
        notificationService.markAsRead(authUser.userId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** FCM 토큰 등록 */
    @PostMapping("/push-subscriptions")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @RequestBody FcmTokenRequest request,
            @AuthenticationPrincipal AuthUser authUser) {
        notificationService.registerFcmToken(authUser.userId(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** FCM 토큰 해제 */
    @DeleteMapping("/push-subscriptions")
    public ResponseEntity<ApiResponse<Void>> unregisterFcmToken(
            @AuthenticationPrincipal AuthUser authUser) {
        notificationService.unregisterFcmToken(authUser.userId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** 알림 설정 조회 */
    @GetMapping("/notification-settings")
    public ResponseEntity<ApiResponse<NotificationSettingsItem>> getNotificationSettings(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getNotificationSettings(authUser.userId())));
    }

    /** 알림 타입별 ON/OFF 설정 */
    @PutMapping("/notification-settings")
    public ResponseEntity<ApiResponse<Void>> updateNotificationSettings(
            @RequestBody NotificationSettingsRequest request,
            @AuthenticationPrincipal AuthUser authUser) {
        notificationService.updateNotificationSettings(
                authUser.userId(),
                request.ipoAllocationEnabled(),
                request.ipoRefundEnabled(),
                request.idleDollarEnabled(),
                request.spendingReportEnabled());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    record NotificationListResponse(List<NotificationItem> notifications) {}

    record FcmTokenRequest(String fcmToken) {}

    record NotificationSettingsRequest(
            Boolean ipoAllocationEnabled,
            Boolean ipoRefundEnabled,
            Boolean idleDollarEnabled,
            Boolean spendingReportEnabled
    ) {}
}
