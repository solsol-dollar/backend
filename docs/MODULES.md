# 모듈 상세 설명

Eclipse 백엔드는 7개의 Gradle 모듈로 구성됩니다.
라이브러리 모듈 4개(common, domain, ledger, service)와 실행 가능한 앱 모듈 3개(ledger-app, service-app, worker-app)입니다.

---

## common

**역할**: 모든 모듈이 공통으로 사용하는 유틸리티와 인프라 코드.

**의존 모듈**: 없음 (최상위 라이브러리)

**의존 라이브러리**: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`

**패키지 구조**:

```
com.shinhan.eclipse.common
  entity/
    BaseEntity              - @MappedSuperclass. id, createdAt, updatedAt, status(ACTIVE) 공통 필드
  exception/
    BusinessException       - RuntimeException 상속, ErrorCode를 인자로 받는 비즈니스 예외
    ErrorCode               - 에러 코드 열거형 (TODO: 코드 정의 필요)
    GlobalExceptionHandler  - @RestControllerAdvice. BusinessException → ApiResponse 변환
  resolver/
    UserHeader              - @interface. Controller 파라미터에 선언하는 어노테이션
    UserIdArgumentResolver  - X-User-Id 헤더를 Long으로 변환. 없으면 기본값 1L 반환
  response/
    ApiResponse             - 공통 응답 래퍼
```

**Spring Modulith 미적용**: common은 순수 유틸 모듈로 모듈 경계 검증 대상이 아닙니다.

---

## domain

**역할**: JPA 엔티티 정의 전담 모듈. 비즈니스 로직 없음. 모든 엔티티는 `BaseEntity`를 상속합니다.

**의존 모듈**: common

**의존 라이브러리**: (common을 통해 spring-boot-starter-data-jpa 상속)

**포함된 엔티티**:

| 패키지 | 엔티티 클래스 | 대응 테이블 |
|--------|-------------|-----------|
| domain.account | FinancialAccount | financial_accounts |
| domain.account | Card | cards |
| domain.holding | Holding | holdings |
| domain.holding | HoldingLot | holding_lots |
| domain.inflow | IdleDollarTrigger | idle_dollar_triggers |
| domain.ipo | Ipo | ipos |
| domain.ipo | IpoNews | ipo_news |
| domain.ipo | IpoRiskScore | ipo_risk_scores |
| domain.ipo | FavoriteIpo | favorite_ipos |
| domain.notification | Notification | notifications |
| domain.product | InvestmentProduct | investment_products |
| domain.returnplan | ReturnPlan | return_plans |
| domain.returnplan | ReturnPlanAllocation | return_plan_allocations |
| domain.returnplan | ReturnPlanPreset | return_plan_presets |
| domain.subscription | IpoSubscription | ipo_subscriptions |
| domain.trade | TradeOrder | trade_orders |
| domain.trade | RpContract | rp_contracts |
| domain.transaction | TransferTransaction | transfer_transactions |
| domain.transaction | FxExchangeTransaction | fx_exchange_transactions |
| domain.user | User | users |
| domain.user | InvestmentProfile | investment_profiles |

**Spring Modulith 미적용**: domain은 엔티티 저장소로 모듈 경계 검증 대상이 아닙니다.

---

## ledger

**역할**: 자금/원장 트랜잭션 처리 비즈니스 로직. Spring Modulith 경계 적용.

**의존 모듈**: common, domain

**의존 라이브러리**: `spring-boot-starter-data-jpa`, `spring-modulith-starter-core`

**Spring Modulith 모듈 목록**:

### ledger.trade

해외 주식 매수/매도 주문 처리. 평가용 MOCK 즉시 체결 방식으로 동작합니다. 실제 자금이 `financial_accounts`에서 차감되므로 원장 모듈에 위치합니다.

**public API**:
```java
// com.shinhan.eclipse.ledger.trade.TradeOrderFacade
public interface TradeOrderFacade {
    TradeOrder placeOrder(Long userId, Long productId, Long accountId,
                          String orderSide, Integer quantity, BigDecimal requestedPrice);
    List<TradeOrder> getOrders(Long userId);
}
```

**Repository (internal)**:
- `TradeOrderRepository` — JpaRepository<TradeOrder, Long>

**구현체 (internal)**:
- `TradeOrderFacadeImpl` — package-private. `TradeOrder.mockFill()`로 즉시 체결 처리 (`executionMode = "MOCK"`, `orderStatus = "COMPLETED"`).

---

### ledger.subscription

청약 신청 및 확정 처리.

**public API**:
```java
// com.shinhan.eclipse.ledger.subscription.SubscriptionFacade
public interface SubscriptionFacade {
    IpoSubscription requestSubscription(Long userId, Long ipoId, Long securitiesAccountId, Integer shares, BigDecimal offerPrice);
    IpoSubscription confirmSubscription(Long subscriptionId, Long userId);
    List<IpoSubscription> getSubscriptions(Long userId);
}
```

**Repository (internal)**:
- `IpoSubscriptionRepository` — JpaRepository<IpoSubscription, Long>

**구현체 (internal)**:
- `SubscriptionFacadeImpl` — package-private

---

### ledger.returnplan

리턴플랜 생성·확정·조회.

**public API**:
```java
// com.shinhan.eclipse.ledger.returnplan.ReturnPlanFacade
public interface ReturnPlanFacade {
    ReturnPlan createReturnPlan(Long userId, Long subscriptionId);
    ReturnPlan confirmReturnPlan(Long returnPlanId, Long userId);
    List<ReturnPlan> getReturnPlans(Long userId);
}
```

**Repository (internal)**:
- `ReturnPlanRepository` — JpaRepository<ReturnPlan, Long>
- `ReturnPlanAllocationRepository` — JpaRepository<ReturnPlanAllocation, Long>

**구현체 (internal)**:
- `ReturnPlanFacadeImpl` — package-private

---

### ledger.accountlink

외화 계좌 및 카드 연동/해제.

**public API**:
```java
// com.shinhan.eclipse.ledger.accountlink.AccountLinkService
public interface AccountLinkService {
    List<FinancialAccount> getLinkedAccounts(Long userId);
    FinancialAccount linkAccount(Long userId, String accountType, String institutionName, String accountNumberMasked);
    void unlinkAccount(Long userId, Long accountId);
}
```

**Repository (internal)**:
- `FinancialAccountRepository` — JpaRepository<FinancialAccount, Long>
- `CardRepository` — JpaRepository<Card, Long>

**구현체 (internal)**:
- `AccountLinkServiceImpl` — package-private

---

### ledger.event

모듈 간 통신에 사용하는 도메인 이벤트 레코드. (Spring Modulith Application Events 활용 예정)

| 이벤트 레코드 | 발생 시점 | 주요 필드 |
|-------------|---------|---------|
| `SubscriptionConfirmedEvent` | 청약 확정 시 | subscriptionId, userId, ipoId, amountUsd |
| `AllocationCompletedEvent` | 배정 완료 시 | subscriptionId, userId, refundAmount |
| `RemittanceFailedEvent` | 송금 실패 시 | subscriptionId, userId, reason |

현재 이벤트 외부화(`modulith.events.externalization.enabled: false`)는 비활성화 상태입니다.

---

## service

**역할**: 조회·탐색·AI 분석 비즈니스 로직. Spring Modulith 경계 적용.

**의존 모듈**: common, domain

**의존 라이브러리**: `spring-boot-starter-data-jpa`, `spring-modulith-starter-core`, `spring-ai-client-chat`

**Spring Modulith 모듈 목록**:

### service.ipo

IPO 목록 조회, 상세 정보, 관심 IPO 관리.

**public API**:
```java
// com.shinhan.eclipse.service.ipo.IpoExplorationService
public interface IpoExplorationService {
    List<Ipo> getUpcomingIpos();
    Ipo getIpoDetail(Long ipoId);
    FavoriteIpo addFavorite(Long userId, Long ipoId);
    void removeFavorite(Long userId, Long ipoId);
    List<FavoriteIpo> getFavorites(Long userId);
}
```

**Repository (internal)**:
- `IpoRepository` — JpaRepository<Ipo, Long>
- `IpoNewsRepository` — JpaRepository<IpoNews, Long>
- `FavoriteIpoRepository` — JpaRepository<FavoriteIpo, Long>

**구현체 (internal)**:
- `IpoExplorationServiceImpl` — package-private
- `FinnhubSyncScheduler` — Finnhub API 동기화 스케줄러 (현재 stub, TODO)

---

### service.home

홈 화면 대시보드 데이터 집계.

**public API**:
```java
// com.shinhan.eclipse.service.home.HomeService
public interface HomeService {
    Object getDashboard(Long userId);
}
```

**구현체 (internal)**:
- `HomeServiceImpl` — package-private

---

### service.mypage

투자성향 진단 및 마이페이지.

**public API**:
```java
// com.shinhan.eclipse.service.mypage.MyPageService
public interface MyPageService {
    InvestmentProfile diagnoseInvestmentProfile(Long userId, String surveyJson);
    InvestmentProfile getLatestProfile(Long userId);
}
```

**Repository (internal)**:
- `InvestmentProfileRepository` — JpaRepository<InvestmentProfile, Long>

**구현체 (internal)**:
- `MyPageServiceImpl` — package-private

---

### service.inflow

쉬는 달러(유휴 외화 잔액) 감지 및 알림.

**public API**:
```java
// com.shinhan.eclipse.service.inflow.IdleDollarService
public interface IdleDollarService {
    void detectAndNotify(Long userId);
    List<IdleDollarTrigger> getTriggerHistory(Long userId);
}
```

**Repository (internal)**:
- `IdleDollarTriggerRepository` — JpaRepository<IdleDollarTrigger, Long>

**구현체 (internal)**:
- `IdleDollarServiceImpl` — package-private

---

### service.ai

Claude AI를 이용한 IPO 리스크 분석.

**public API**:
```java
// com.shinhan.eclipse.service.ai.RiskScoreService
public interface RiskScoreService {
    IpoRiskScore analyzeRisk(Long ipoId);
    IpoRiskScore getRiskScore(Long ipoId);
}
```

**포트 인터페이스**:
- `AiAnalysisPort` — AI 호출을 추상화하는 포트 (Port & Adapter 패턴)

**DTO**:
- `RiskAnalysisResult` — AI 응답을 담는 레코드

**Repository (internal)**:
- `IpoRiskScoreRepository` — JpaRepository<IpoRiskScore, Long>

**구현체 (internal)**:
- `RiskScoreServiceImpl` — package-private. `spring-ai-client-chat`의 `ChatClient` 사용.

---

## ledger-app

**역할**: ledger 모듈을 서빙하는 Spring Boot 실행 단위.

**포트**: 8080

**의존 모듈**: ledger, domain, common

**의존 라이브러리**: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `mysql-connector-j`

**빌드 산출물**: `ledger-app.jar`

**주요 클래스**: `LedgerApplication` (main 클래스)

**테스트**: `ModularityTest` — `ApplicationModules.of(LedgerApplication.class).verify()`로 ledger 모듈 구조 검증

**현재 미구현**: Controller 클래스 없음 (TODO)

---

## service-app

**역할**: service 모듈을 서빙하는 Spring Boot 실행 단위. Claude AI 연동 포함.

**포트**: 8081

**의존 모듈**: service, domain, common

**의존 라이브러리**: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-ai-starter-model-anthropic`, `mysql-connector-j`

**빌드 산출물**: `service-app.jar`

**주요 클래스**:
- `ServiceApplication` (main 클래스)
- `config/SpringAiConfig` — `ChatClient` Bean 설정

**AI 설정**: `claude-sonnet-4-6`, temperature: 0.2, max-tokens: 2048

**테스트**: `ModularityTest` — `ApplicationModules.of(ServiceApplication.class).verify()`로 service 모듈 구조 검증

**현재 미구현**: Controller 클래스 없음 (TODO)

---

## worker-app

**역할**: Finnhub 뉴스 수집 + Claude AI 요약 배치 잡 실행 단위. Spring Modulith 미적용.

**포트**: 8082

**의존 모듈**: domain, common (ledger, service 미의존)

**의존 라이브러리**: `spring-boot-starter-batch`, `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `spring-ai-starter-model-anthropic`, `spring-ai-client-chat`, `mysql-connector-j`

**빌드 산출물**: `worker-app.jar`

**AI 설정**: `claude-haiku-4-5-20251001`, temperature: 0.1, max-tokens: 512

**주요 클래스**:

| 클래스 | 패키지 | 역할 |
|--------|--------|------|
| `WorkerApplication` | root | main 클래스 |
| `IpoNewsSyncJobConfig` | worker.job | Job/Step Bean 정의 |
| `IpoNewsFetchReader` | worker.reader | Finnhub API 수집 (ItemReader) |
| `IpoNewsAiSummaryProcessor` | worker.processor | Claude AI 요약 (ItemProcessor) |
| `IpoNewsWriter` | worker.writer | DB 저장 (ItemWriter) |
| `IpoNewsSyncScheduler` | worker.scheduler | 6시간 주기 잡 기동 |
| `IpoNewsRepository` | worker.repository | IpoNews JPA Repository |
| `SpringAiConfig` | worker.config | ChatClient Bean 설정 |
| `NewsItem` | worker.dto | Reader → Processor 전달 데이터 |
| `NewsSummaryResult` | worker.dto | AI 응답 파싱 레코드 |
