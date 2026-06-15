# DB 스키마 문서

Eclipse는 공유 MySQL 8.0 DB(`eclipse`)를 사용합니다. 평가용 단순화 구조로, 모든 앱(ledger-app, service-app, worker-app)이 동일한 DB에 접근합니다.

스키마 파일: `eclipse-backend/schema.sql` (현재 버전: v1.1.0)

---

## 개발 시 주의사항

### ddl-auto 설정

기본 프로파일에서 모든 앱은 `ddl-auto: validate`를 사용합니다.

```yaml
# 기본 (application.yml) — 스키마 검증만 수행
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

이는 **Hibernate가 테이블을 생성하지 않는다**는 뜻입니다. 반드시 MySQL을 먼저 올리고 `schema.sql`이 적용된 후 앱을 실행해야 합니다.

```bash
# 올바른 순서
docker-compose up -d     # MySQL 컨테이너 시작 + schema.sql 자동 적용
./gradlew :ledger-app:bootRun  # 이후 앱 실행
```

로컬 프로파일(ledger-app, service-app)에서는 `ddl-auto: create-drop`으로 오버라이드됩니다.

### Spring Batch 메타데이터 테이블

worker-app 실행 시 Spring Batch 메타데이터 테이블이 자동 생성됩니다.

```yaml
# worker-app/src/main/resources/application.yml
spring:
  batch:
    jdbc:
      initialize-schema: always
```

`BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION` 등의 테이블이 eclipse DB에 추가됩니다. 애플리케이션 테이블과 함께 동일한 DB에 생성됩니다.

---

## 테이블 목록 (21개)

schema.sql v1.1.0 기준입니다. v1.0.x에 있던 `subscription_results`는 `ipo_subscriptions`에 통합되어 제거되었습니다.

### 1. 사용자 / 계좌 / 카드

| 테이블 | 설명 |
|--------|------|
| `users` | 앱 사용자. `onboarding_status`로 온보딩 완료 여부 추적. |
| `financial_accounts` | 외화 증권 계좌, 외화 적금, 외화 통장 등 연동 금융 계좌. `account_type`으로 종류 구분. |
| `cards` | 연동 카드 정보. `card_status`: UNISSUED / ISSUED / LINKED. |
| `investment_profiles` | 투자성향 진단 결과 이력. 이력 보존형으로 최신은 `diagnosed_at` 기준 조회. |

### 2. 투자 상품 / IPO

| 테이블 | 설명 |
|--------|------|
| `investment_products` | 주식, ETF, RP 등 투자 상품 마스터 데이터. |
| `ipos` | 해외 공모주 IPO 정보. 청약 기간, 공모가 범위, 상장일 등. `ipo_status`: UPCOMING / OPEN / CLOSED 등. |
| `ipo_risk_scores` | IPO별 AI 리스크 분석 결과. `risk_score`는 숫자형(등급 문자 A/B/C 금지 — 투자자문업 규제). |
| `ipo_news` | IPO 관련 뉴스. Finnhub 수집 후 Claude AI 요약 저장. |
| `favorite_ipos` | 사용자별 관심 IPO 목록. (user_id, ipo_id) 복합 유니크 키. |

### 3. 청약 / 리턴플랜

| 테이블 | 설명 |
|--------|------|
| `ipo_subscriptions` | IPO 청약 신청 및 배정 결과 통합 테이블. 배정 전 `allocated_shares` 등이 NULL. `result_status`: PENDING / ALLOCATED / REFUNDED 등. |
| `return_plans` | 배정 환불금 운용 계획. `plan_status`: DRAFT / CONFIRMED / EXECUTED. |
| `return_plan_allocations` | 리턴플랜 내 계좌별 배분 명세. `destination_type`: SECURITIES / FX_SAVINGS / FX_ACCOUNT. |
| `return_plan_presets` | 리턴플랜 프리셋 정의(DB 관리). `preset_code`: IPO_FOCUS / STABLE_SAVING / BALANCED. |

### 4. 거래 / 이체 / 환전

| 테이블 | 설명 |
|--------|------|
| `transfer_transactions` | 계좌 간 이체 내역. 리턴플랜 실행 시 `allocation_id`로 `return_plan_allocations` 참조. |
| `fx_exchange_transactions` | 원화-외화 환전 내역. 환율, 환전 전후 금액, 계좌 잔액 스냅샷 포함. |
| `trade_orders` | 해외 주식 매수/매도 주문. `execution_mode`: MOCK (평가용). |
| `rp_contracts` | RP(환매조건부채권) 계약. 원금, 금리, 만기일 관리. |

### 5. 보유 종목

| 테이블 | 설명 |
|--------|------|
| `holdings` | 사용자별 종목 보유 현황. (user_id, product_id) 복합 유니크 키. 평균단가 관리. |
| `holding_lots` | 개별 매수 단위(Lot) 관리. `source_type`으로 취득 경로 구분 (IPO_ALLOCATION / TRADE_ORDER). source_id는 폴리모픽으로 FK 미선언. |

### 6. 알림 / 쉬는 달러

| 테이블 | 설명 |
|--------|------|
| `notifications` | 사용자 푸시/인앱 알림 내역. `target_type` + `target_id`로 관련 객체 연결. |
| `idle_dollar_triggers` | 쉬는 달러(유휴 외화 잔액) 감지 이력. `trigger_status`: TRIGGERED / SUPPRESSED / INVALIDATED. 억제 조건(관심 IPO 14일 이내, 최근 환불 7일 이내) 기록. |

---

## 주요 관계 (청약 플로우 중심)

청약부터 리턴플랜 실행까지의 데이터 흐름입니다.

```
users (1)
  |
  +---> financial_accounts (N)          # 연동 외화 계좌
  |
  +---> ipo_subscriptions (N)           # IPO 청약 신청
  |       | ipo_id ──────────────────>  ipos
  |       | securities_account_id ────> financial_accounts
  |
  +---> return_plans (N)                # 리턴플랜 (환불금 운용 계획)
          | subscription_id ──────────> ipo_subscriptions
          |
          +---> return_plan_allocations (N)   # 계좌별 배분
                  |
                  +---> transfer_transactions  # 이체 실행 내역
```

### 보유 종목 관계

```
trade_orders ──> holdings (holding_lots.source_type = TRADE_ORDER)
ipo_subscriptions ──> holdings (holding_lots.source_type = IPO_ALLOCATION)
```

`holding_lots.source_id`는 `source_type`에 따라 참조 대상이 달라지는 폴리모픽 컬럼으로, FK 제약 없이 애플리케이션 레이어에서 무결성을 보장합니다.

### v1.0.x → v1.1.0 변경 사항 요약

- `subscription_results` 테이블 제거
- 배정 결과 필드(`allocated_shares`, `allocated_amount`, `refund_amount`, `allocation_rate`, `result_status`, `confirmed_at`)가 `ipo_subscriptions`에 통합
- 배정 전에는 해당 필드가 NULL
- `return_plans.subscription_result_id` → `return_plans.subscription_id` (FK 대상이 `ipo_subscriptions`로 변경)

---

## FK 제약 정책

- 모든 FK는 `ON DELETE RESTRICT`, `ON UPDATE RESTRICT` (기본값)
- 운영 삭제/수정 정책 확정 후 조정 예정
- `holding_lots.source_id`는 폴리모픽이므로 FK 미선언 (의도적)
