# Eclipse Backend

신한 그룹 연계 해외 공모주(IPO) 청약 자동화 앱 Eclipse의 백엔드 서버입니다.
내부 평가용 프로젝트로, 실제 금융 거래는 MOCK 모드로 동작합니다.

---

## 아키텍처

### 배포 구조 (3개 앱)

```
[Client]
   |
   +---> ledger-app  :8080  (자금/원장)
   |       - 청약 (ipo_subscriptions)
   |       - 리턴플랜 (return_plans)
   |       - 계좌 연동 (financial_accounts)
   |       - 증권 주문 (trade_orders) [MOCK 즉시 체결]
   |
   +---> service-app :8081  (조회/AI)
   |       - IPO 탐색 (ipos)
   |       - 홈 대시보드
   |       - 마이페이지 / 투자성향
   |       - 쉬는 달러 감지
   |       - AI 리스크 분석 (Claude)
   |
   +---> worker-app  :8082  (배치)
           - Finnhub 뉴스 수집 (IpoNewsSyncJob)
           - Claude AI 뉴스 요약

              공유 DB
         [MySQL 8.0 :3306]
```

### Gradle 모듈 의존 방향

```
ledger-app ──depends──> ledger ─┐
                                 ├──> domain ──> common
service-app ─depends──> service ─┘
worker-app  ─────────────────────────> domain ──> common
```

| 앱 모듈 | 포트 | 역할 | 의존 라이브러리 모듈 |
|---------|------|------|-------------------|
| ledger-app | 8080 | 자금/원장 트랜잭션 처리 | ledger, domain, common |
| service-app | 8081 | 조회·탐색·AI 분석 | service, domain, common |
| worker-app | 8082 | 배치 뉴스 수집·요약 | domain, common |

---

## 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 21 |
| 프레임워크 | Spring Boot | 3.3.5 |
| 빌드 | Gradle | 8.10 |
| 모듈 경계 | Spring Modulith | 1.2.3 |
| AI 통합 | Spring AI | 1.0.4 |
| 배치 | Spring Batch | (Boot 3.3.5 관리) |
| DB | MySQL | 8.0 |
| 유틸 | Lombok | (Boot BOM 관리) |
| AI 모델 (service-app) | Claude Sonnet 4.6 | claude-sonnet-4-6 |
| AI 모델 (worker-app) | Claude Haiku 4.5 | claude-haiku-4-5-20251001 |

---

## 로컬 개발 환경 셋업

### 사전 요구사항

- Java 21 (JAVA_HOME 설정 필요)
- Docker & Docker Compose

### 1단계: 저장소 클론

```bash
git clone <repository-url>
cd eclipse-backend
```

### 2단계: MySQL 실행

```bash
docker-compose up -d
```

`docker-compose.yml`이 `schema.sql`을 자동으로 실행하여 테이블을 생성합니다.
컨테이너 이름: `eclipse_mysql`, DB: `eclipse`, 비밀번호: `eclipse123`

### 3단계: 환경변수 설정

service-app과 worker-app은 Claude AI를 사용합니다.

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

Finnhub, LS증권 연동은 현재 stub 상태이므로 해당 키 없이도 실행됩니다.

### 4단계: 앱 실행

각 앱을 별도 터미널에서 실행합니다.

```bash
# ledger-app (포트 8080)
./gradlew :ledger-app:bootRun

# service-app (포트 8081)
ANTHROPIC_API_KEY=sk-ant-... ./gradlew :service-app:bootRun

# worker-app (포트 8082)
ANTHROPIC_API_KEY=sk-ant-... ./gradlew :worker-app:bootRun
```

로컬 프로파일(ddl-auto: create-drop, SQL 로그 출력)을 사용하려면:

```bash
./gradlew :ledger-app:bootRun --args='--spring.profiles.active=local'
```

> 주의: `local` 프로파일에서 ledger-app과 service-app은 `ddl-auto: create-drop`으로 동작합니다.
> worker-app `local` 프로파일은 `validate`를 유지합니다.
> Docker MySQL을 먼저 띄운 후 `schema.sql`이 적용된 상태에서 실행해야 합니다.

### 5단계: 동작 확인

```bash
curl -H "X-User-Id: 1" http://localhost:8080/actuator/health
curl -H "X-User-Id: 1" http://localhost:8081/actuator/health
curl -H "X-User-Id: 1" http://localhost:8082/actuator/health
```

---

## 모듈 구조

| 모듈 | 타입 | 역할 | 핵심 패키지 |
|------|------|------|------------|
| common | 라이브러리 | 공통 유틸 (BaseEntity, 예외, 응답 형식, UserHeader) | `common.entity`, `common.exception`, `common.resolver`, `common.response` |
| domain | 라이브러리 | JPA 엔티티 정의 (비즈니스 로직 없음) | `domain.account`, `domain.ipo`, `domain.subscription`, `domain.returnplan`, `domain.trade`, `domain.transaction`, `domain.holding`, `domain.user`, `domain.inflow`, `domain.notification`, `domain.product` |
| ledger | 라이브러리 | 자금/원장 비즈니스 로직 (Spring Modulith 경계 적용) | `ledger.subscription`, `ledger.returnplan`, `ledger.accountlink`, `ledger.event` |
| service | 라이브러리 | 조회/AI 비즈니스 로직 (Spring Modulith 경계 적용) | `service.ipo`, `service.securities`, `service.home`, `service.mypage`, `service.inflow`, `service.ai` |
| ledger-app | Spring Boot 앱 | ledger 모듈을 서빙하는 실행 단위 | `LedgerApplication` |
| service-app | Spring Boot 앱 | service 모듈을 서빙하는 실행 단위 | `ServiceApplication` |
| worker-app | Spring Boot 앱 | Spring Batch 배치 잡 실행 단위 | `WorkerApplication`, `worker.job`, `worker.reader`, `worker.processor`, `worker.writer` |

### domain 모듈 패키지 트리

```
domain/
  account/        FinancialAccount, Card
  holding/        Holding, HoldingLot
  inflow/         IdleDollarTrigger
  ipo/            Ipo, IpoNews, IpoRiskScore, FavoriteIpo
  notification/   Notification
  product/        InvestmentProduct
  returnplan/     ReturnPlan, ReturnPlanAllocation, ReturnPlanPreset
  subscription/   IpoSubscription
  trade/          TradeOrder, RpContract
  transaction/    TransferTransaction, FxExchangeTransaction
  user/           User, InvestmentProfile
```

---

## Spring Modulith 규칙

ledger 모듈과 service 모듈은 Spring Modulith로 경계가 강제됩니다.
다음 규칙을 반드시 지켜야 합니다. 위반 시 `ModularityTest`가 빌드에서 실패합니다.

### 모듈 = 1차 하위 패키지

```
com.shinhan.eclipse.ledger.subscription   <- 모듈 1
com.shinhan.eclipse.ledger.returnplan     <- 모듈 2
com.shinhan.eclipse.ledger.accountlink    <- 모듈 3
```

### public API 위치

모듈 루트에 `interface`만 선언합니다.

```java
// 허용: com.shinhan.eclipse.ledger.subscription.SubscriptionFacade (public interface)
public interface SubscriptionFacade {
    IpoSubscription requestSubscription(...);
}
```

### 구현체와 Repository 위치

반드시 `internal/` 하위 패키지에, **package-private 클래스**로 작성합니다.

```java
// 허용: com.shinhan.eclipse.ledger.subscription.internal.SubscriptionFacadeImpl
class SubscriptionFacadeImpl implements SubscriptionFacade { ... }  // package-private

// 허용: com.shinhan.eclipse.ledger.subscription.internal.IpoSubscriptionRepository
interface IpoSubscriptionRepository extends JpaRepository<IpoSubscription, Long> { ... }  // package-private
```

### 금지 사항

- `ledger` 모듈에서 `service` 모듈 직접 의존 금지 (반대 방향도 금지)
- 한 모듈의 `internal/` 패키지를 다른 모듈에서 직접 참조 금지
- `internal/` 패키지 클래스를 `public`으로 선언하면 Modulith 검증 실패

### Modulith 검증 테스트 실행

```bash
./gradlew :ledger-app:test
./gradlew :service-app:test
```

---

## 새 기능 추가 가이드

### 예시: ledger 모듈에 송금(Transfer) 기능 추가

**1. Entity 추가 → domain 모듈**

```
domain/src/main/java/com/shinhan/eclipse/domain/transaction/TransferTransaction.java
```
이미 존재. 없으면 `domain/transaction/` 패키지에 `@Entity` 클래스 추가.

**2. Repository 추가 → 해당 비즈니스 모듈의 internal/**

```java
// ledger/src/main/java/com/shinhan/eclipse/ledger/transfer/internal/TransferRepository.java
interface TransferRepository extends JpaRepository<TransferTransaction, Long> { }
```

**3. Service 인터페이스 → 모듈 루트 (public)**

```java
// ledger/src/main/java/com/shinhan/eclipse/ledger/transfer/TransferService.java
public interface TransferService {
    TransferTransaction transfer(Long userId, Long fromAccountId, Long toAccountId, BigDecimal amount);
}
```

**4. 구현체 → internal/ (package-private)**

```java
// ledger/src/main/java/com/shinhan/eclipse/ledger/transfer/internal/TransferServiceImpl.java
@Service
@RequiredArgsConstructor
class TransferServiceImpl implements TransferService { ... }
```

**5. Controller → *-app 모듈**

현재 scaffold에 Controller 클래스가 없습니다. Controller는 각 `*-app` 모듈에 추가해야 합니다.
예: `ledger-app/src/main/java/com/shinhan/eclipse/ledger/app/transfer/TransferController.java`

---

## X-User-Id 헤더 (인증 대체)

이 프로젝트는 인증/인가 없이 HTTP 헤더로 사용자를 구분합니다.

```http
X-User-Id: 1
```

- 헤더가 없으면 기본값 `1L`로 처리됩니다.
- `@UserHeader` 어노테이션으로 Controller 파라미터에 주입합니다.

```java
@GetMapping("/subscriptions")
public ApiResponse<?> getSubscriptions(@UserHeader Long userId) {
    ...
}
```

`UserIdArgumentResolver`가 `X-User-Id` 헤더 값을 `Long`으로 파싱하여 주입합니다.
등록은 각 `*-app` 모듈의 `WebMvcConfigurer`에서 해야 합니다. (현재 scaffold에 없음 — TODO)

---

## worker-app 배치 가이드

### IpoNewsSyncJob 파이프라인

```
IpoNewsFetchReader  ->  IpoNewsAiSummaryProcessor  ->  IpoNewsWriter
(Finnhub API 수집)      (Claude Haiku AI 요약)         (DB 저장)
```

- **chunk(5)**: 5건씩 묶어 트랜잭션 처리. 대량 요청 시 API 부하 분산.
- **skip 정책**: `Exception.class` 전체를 skip 대상으로 등록, `skipLimit(20)`. AI 실패 또는 개별 기사 오류 시 해당 기사만 건너뛰고 계속 진행합니다.
- **자동 실행**: `@Scheduled(cron = "0 0 */6 * * *")` — 매 6시간마다 `IpoNewsSyncScheduler`가 잡을 기동합니다.
- **시작 시 자동 실행 방지**: `spring.batch.job.enabled: false` 설정으로 앱 구동 시 즉시 실행되지 않습니다.
- **메타데이터 테이블**: `spring.batch.jdbc.initialize-schema: always`로 Spring Batch 메타데이터 테이블이 자동 생성됩니다.

### 수동 실행 방법

현재 수동 트리거 엔드포인트가 없습니다. (TODO: Actuator Batch Endpoint 추가)
임시 방편으로 `IpoNewsSyncScheduler.triggerIpoNewsSync()`를 직접 호출하거나, `spring.batch.job.enabled: true`로 변경 후 앱을 재시작합니다.

### AI 모델 설정

worker-app은 비용 절감을 위해 Haiku 모델을 사용합니다.

```yaml
# worker-app/src/main/resources/application.yml
spring:
  ai:
    anthropic:
      chat:
        options:
          model: claude-haiku-4-5-20251001
          temperature: 0.1
          max-tokens: 512
```

---

## 외부 API 설정

| API | 용도 | 환경변수 키 | 현재 상태 |
|-----|------|------------|---------|
| Finnhub | IPO 캘린더 및 뉴스 수집 | `FINNHUB_API_KEY` | stub (TODO) |
| LS증권 WebSocket | 실시간 해외주식 시세 | `LS_SECURITIES_APP_KEY`, `LS_SECURITIES_SECRET_KEY` | stub (TODO) |
| Claude (Anthropic) | 리스크 분석(service-app), 뉴스 요약(worker-app) | `ANTHROPIC_API_KEY` | 연동 구현됨 |

Finnhub와 LS증권은 현재 stub 구현 상태입니다. 실제 API 키 없이 실행 가능하지만 해당 기능은 빈 결과를 반환합니다.

---

## TODO 목록

현재 scaffold에서 구현되지 않은 항목입니다.

- **Controller 클래스**: 각 비즈니스 모듈의 엔드포인트가 없습니다. `ledger-app`, `service-app` 각각에 Controller 추가 필요.
- **WebMvcConfigurer**: `UserIdArgumentResolver` 등록을 위한 MVC 설정 클래스 (각 `*-app`에 추가 필요).
- **Service impl 비즈니스 로직**: `SubscriptionFacadeImpl`, `ReturnPlanFacadeImpl` 등 실제 로직이 stub 상태.
- **Finnhub API 연동**: `IpoNewsFetchReader.fetchFromFinnhub()`에 실제 HTTP 클라이언트 구현 필요.
- **LS증권 WebSocket 연동**: `LsSecuritiesWebSocketHandler`에 실제 연결 로직 구현 필요.
- **IpoNews 엔티티 생성자/setter**: `IpoNewsAiSummaryProcessor`의 필드 설정 코드가 주석 처리 상태.
- **CORS 설정**: 프론트엔드 연동 시 필요.
- **Worker-app 수동 실행 엔드포인트**: Actuator Batch Endpoint 또는 별도 API 추가.

---

## 참고 문서

- [모듈 상세 설명](docs/MODULES.md)
- [DB 스키마 문서](docs/DATABASE.md)
