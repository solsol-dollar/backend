# 알림(Push Notification) 설계 문서

> 작성일: 2026-06-23
> 구현 범위: MVP — 배정 완료 / 환불금 / 쉬는 달러 (3종)
> Push 방식: FCM (Firebase Cloud Messaging)

---

## 1. 아키텍처 결정

### 왜 Outbox + Polling인가

worker-app(배치)과 service-app(API 서버)은 **별개 JVM 프로세스**다.

- **Spring Events** → 같은 JVM 안에서만 동작. 크로스 프로세스 불가.
- **Kafka / RabbitMQ** → 가능하지만 MVP에 과함.
- **Outbox + Polling** → DB를 통신 채널로 사용. 추가 인프라 없음. 현재 구조에 맞는 유일한 선택.

### 앱별 역할

```
worker-app (8082)   배치 실행 → notifications 테이블 INSERT (sentAt = NULL)
service-app (8081)  매 10초 polling → sentAt IS NULL 조회 → FCM 발송 → sentAt 업데이트
```

worker-app은 알림 데이터만 만든다. 발송 방식은 모른다.
service-app이 발송 전담. 발송 로직이 한 곳에만 존재한다.

### Outbox Pattern 흐름

```
이벤트 발생 (배정 완료 / 환불금 / 쉬는 달러)
  ↓
notifications INSERT  (sent_at = NULL)
  ↓
service-app PushSendingScheduler (10초마다)
  ↓
sent_at IS NULL AND status = 'ACTIVE' 조회
  ↓
userId → notification_settings 조회
  → fcmToken 없으면 스킵
  → 타입별 enabled 꺼져 있으면 스킵
  ↓
FCM 발송
  ↓
성공 → sentAt = now()
실패 → sentAt 유지 (다음 주기 자동 재시도)
```

---

## 2. DB 테이블

### notifications

```sql
CREATE TABLE notifications (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    notification_type VARCHAR(30)  NOT NULL,  -- IPO_ALLOCATION | IPO_REFUND | IDLE_DOLLAR
    title             VARCHAR(100) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    target_type       VARCHAR(50)  NULL,      -- IPO | ACCOUNT
    target_id         BIGINT       NULL,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at           DATETIME     NULL,      -- NULL = 미발송
    read_at           DATETIME     NULL,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id)
);
```

### notification_settings

유저당 1행. FCM 토큰 + 타입별 ON/OFF 설정.

```sql
CREATE TABLE notification_settings (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    user_id                BIGINT       NOT NULL,
    fcm_token              VARCHAR(500) NULL,         -- NULL = 기기 미등록 (전체 알림 꺼짐)
    ipo_allocation_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    ipo_refund_enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    idle_dollar_enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             DATETIME     NOT NULL,
    updated_at             DATETIME     NOT NULL,
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY UK_notification_settings_user (user_id)
);
```

---

## 3. notification_type 3종

| type | 생성 위치 | 발생 시점 | target_type | target_id |
|------|-----------|-----------|-------------|-----------|
| `IPO_ALLOCATION` | worker-app | 배정 완료 배치 (매일 11:00 KST) | `IPO` | ipo_id |
| `IPO_REFUND` | worker-app | refundDate 당일 감지 배치 | `IPO` | ipo_id |
| `IDLE_DOLLAR` | service-app | 쉬는 달러 감지 스케줄러 | `ACCOUNT` | account_id |

---

## 4. API 목록 (service-app :8081)

| Method | Endpoint | 설명 | 구현 |
|--------|----------|------|------|
| GET | `/api/v1/mypage/notifications` | 알림 목록 조회 (MY-003) | ✅ |
| PUT | `/api/v1/mypage/notifications/{id}/read` | 알림 읽음 처리 (MY-004) | ✅ |
| GET | `/api/v1/mypage/notification-settings` | 알림 설정 조회 | ✅ |
| PUT | `/api/v1/mypage/notification-settings` | 타입별 ON/OFF 설정 | ✅ |
| POST | `/api/v1/mypage/push-subscriptions` | FCM 토큰 등록 | ✅ |
| DELETE | `/api/v1/mypage/push-subscriptions` | FCM 토큰 해제 | ✅ |

---

## 5. 구현 현황

### 완료

- [x] `notification_settings` 테이블 schema.sql 추가
- [x] `Notification` 엔티티 factory method 추가
- [x] `NotificationSettings` 엔티티 생성
- [x] service-app API 전체 구현 (6개)
- [x] Firebase Admin SDK 설정 (`FirebaseConfig`)
- [x] FCM 발송 polling 스케줄러 (`PushSendingScheduler`, 10초 간격)
- [x] 온보딩 완료 시 `notification_settings` 자동 생성 (`OnboardingService`)

### 미완료

- [ ] `IpoAllocationJob` — 배정 완료 후 notifications INSERT
- [ ] 환불금 감지 배치 신규 구현 + notifications INSERT
- [ ] 쉬는 달러 감지 스케줄러 notifications INSERT (service-app)

---

## 6. 남은 구현 상세

### IpoAllocationJob — 배정 완료 알림

`allocateForIpo()` 완료 후 배정받은 유저마다 INSERT.

```java
// 배정받은 subscription들 순회하며
notificationRepository.save(Notification.create(
    subscription.getUserId(),
    "IPO_ALLOCATION",
    "IPO 배정 완료",
    ipo.getCompanyName() + " IPO 배정이 완료됐어요.",
    "IPO", ipo.getId()
));
```

### 환불금 감지 배치 (신규)

매일 refundDate = 오늘인 IPO 조회 → 청약자 조회 → INSERT.
중복 방지: 이미 `IPO_REFUND` 알림이 있는 유저는 스킵.

```java
@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
public void run() {
    // refundDate = 오늘인 IPO 조회
    // 해당 IPO 청약자 조회
    // 중복 체크 후 notifications INSERT
}
```

### 쉬는 달러 알림 (service-app)

기존 쉬는 달러 감지 스케줄러에 notifications INSERT 추가.

---

## 7. FCM 토큰 만료 처리

FCM 발송 실패 시 `MessagingErrorCode.UNREGISTERED` 에러가 오면
해당 유저의 `fcmToken`을 `null`로 업데이트한다.
이후 polling에서 자동으로 스킵된다.

```java
} catch (FirebaseMessagingException e) {
    if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
        // notification_settings.fcm_token = null
    }
}
```

---

## 8. 미결 사항

- [ ] 환불금 배치 실행 시간 확정 (refundDate 당일 몇 시?)
- [ ] 쉬는 달러 감지 스케줄러 위치 확인 (service-app 어느 패키지?)
- [ ] FCM 토큰 갱신 처리 (프론트에서 토큰 갱신 시 재등록 필요)
