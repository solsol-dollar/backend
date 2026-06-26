# Eclipse 프로덕션 배포 트러블슈팅

> 기록일: 2026-06-25  
> 환경: AWS EC2(52.79.99.81) + nginx + RDS MySQL + S3 + CloudFront(E1988C8RQXMZIQ)

---

## 목차

1. [Firebase 흰 화면 (White Screen)](#1-firebase-흰-화면)
2. [LS WebSocket 타임아웃 루프](#2-ls-websocket-타임아웃-루프)
3. [CloudFront API 라우팅 누락](#3-cloudfront-api-라우팅-누락)
4. [DB Zero Date 오류 (로그인 500)](#4-db-zero-date-오류)
5. [Firebase 서비스 계정 파일 없음 (서버 크래시)](#5-firebase-서비스-계정-파일-없음)
6. [docker-compose 환경변수 미전달](#6-docker-compose-환경변수-미전달)
7. [CORS 403 오류 (로그인 후 화면 전환 안됨)](#7-cors-403-오류)
8. [홈 자산 조회 503 (환율 캐시 미초기화)](#8-홈-자산-조회-503-환율-캐시-미초기화)

---

## 1. Firebase 흰 화면

### 증상
CloudFront 배포 후 프론트엔드에서 아무 화면도 렌더링되지 않음.  
브라우저 콘솔에서 아래 오류 발생:
```
Uncaught FirebaseError: Installations: Missing App configuration value: 'projectId'
```

### 원인
`firebase.ts`에서 `initializeApp()`을 모듈 최상위에서 호출하는데, Vite 빌드 시 Firebase 환경변수(`VITE_FIREBASE_*`)가 주입되지 않아 `undefined`로 번들링됨.

GitHub Actions `deploy.yml`의 `pnpm build` step에 Firebase 관련 env가 없었음.

### 해결책
**`eclipse-frontend/.github/workflows/deploy.yml`** — `pnpm build` step에 환경변수 추가:
```yaml
- run: pnpm build
  env:
    VITE_FIREBASE_API_KEY: ${{ secrets.VITE_FIREBASE_API_KEY }}
    VITE_FIREBASE_AUTH_DOMAIN: ${{ secrets.VITE_FIREBASE_AUTH_DOMAIN }}
    VITE_FIREBASE_PROJECT_ID: ${{ secrets.VITE_FIREBASE_PROJECT_ID }}
    VITE_FIREBASE_MESSAGING_SENDER_ID: ${{ secrets.VITE_FIREBASE_MESSAGING_SENDER_ID }}
    VITE_FIREBASE_APP_ID: ${{ secrets.VITE_FIREBASE_APP_ID }}
    VITE_FIREBASE_VAPID_KEY: ${{ secrets.VITE_FIREBASE_VAPID_KEY }}
```

GitHub Repository → Settings → Secrets에 해당 값 등록 후 재배포.

### 주의
재배포 후에도 PWA Service Worker가 이전 번들을 캐싱하므로 **시크릿 창**에서 확인 필요.  
또는 DevTools → Application → Service Workers → Unregister 후 새로고침.

---

## 2. LS WebSocket 타임아웃 루프

### 증상
서버 기동 후 30초마다 아래 로그가 반복:
```
ERROR - LsWebSocketClient: WebSocket connection failed
```

### 원인
EC2에서 LS증권 WebSocket 서버(`openapi.ls-sec.co.kr:9443`)에 접근 불가.  
LS 방화벽이 EC2 IP 대역을 차단하고 있음(REST API 포트 8080은 허용, WebSocket 포트 9443만 차단).

### 해결책
현재 서비스가 KIS API만 사용하므로 LS 기능을 비활성화.  
AWS Secrets Manager `eclipse/prod/env`에서 `LS_APP_KEY`, `LS_APP_SECRET`를 빈 값으로 설정.

```
LS_APP_KEY=
LS_APP_SECRET=
```

`LsProperties.isConfigured()`가 false를 반환해 `LsWebSocketClient`의 WebSocket 연결 시도 자체가 스킵됨.

### 참고
`docker compose restart`는 환경변수를 다시 로드하지 않음.  
환경변수 변경 후에는 반드시 아래 명령으로 재시작:
```bash
docker compose up -d --force-recreate service-app
```

---

## 3. CloudFront API 라우팅 누락

### 증상
프론트엔드에서 API 호출 시 모두 S3 응답(403/404) 반환.  
`curl https://<cf-domain>/api/service/...` → S3 XML 오류 응답.

### 원인
CloudFront 배포에 CacheBehavior가 0개 — 모든 요청이 기본 S3 오리진으로 라우팅됨.  
EC2 백엔드로 향하는 경로 규칙이 누락되어 있었음.

### 해결책
AWS CLI로 CloudFront에 두 개의 CacheBehavior 추가:

| Path Pattern | Origin |
|---|---|
| `/api/service/*` | `ec2-eclipse-api` (ALB DNS) |
| `/api/ledger/*` | `ec2-eclipse-api` (ALB DNS) |

오리진 설정:
- 도메인: `eclipse-alb-250962440.ap-northeast-2.elb.amazonaws.com`
- 프로토콜: HTTP Only (EC2→nginx는 HTTP 80)
- 쿠키: Forward All (HttpOnly 인증 쿠키 전달 필요)
- 헤더: `Authorization`, `Content-Type`, `X-User-Id`, `Origin`

### 주의
- CloudFront 오리진 도메인에 IP 주소 직접 입력 불가 → ALB DNS 사용
- `OriginSSLProtocols`는 HTTPS 오리진에만 필요 (HTTP 오리진 시 제거)
- POST 요청은 CloudFront가 캐시하지 않으므로 TTL 0 설정 필수

---

## 4. DB Zero Date 오류

### 증상
로그인 시 500 오류 발생:
```
java.sql.SQLException: Zero date value prohibited
```

### 원인
DB에 `0000-00-00 00:00:00` 값이 저장된 datetime 컬럼 존재.  
MySQL 수동 INSERT 시 `NOW()` 누락으로 발생.

영향 테이블: `users`, `financial_accounts`, `cards`, `holdings`, `investment_profiles` 등

### 해결책
**1) DB 데이터 수정** — 각 테이블의 zero date 컬럼을 현재 시각으로 업데이트:
```sql
SET @now = NOW();
UPDATE users SET created_at = @now WHERE created_at = '0000-00-00 00:00:00';
-- 동일 패턴으로 전체 테이블 적용
```

**2) JDBC 안전망 추가** — Secrets Manager `eclipse/prod/env`의 `DB_URL`에 파라미터 추가:
```
zeroDateTimeBehavior=convertToNull
```

---

## 5. Firebase 서비스 계정 파일 없음

### 증상
새 버전 배포 후 service-app이 반복 재시작(restart loop):
```
Caused by: java.io.FileNotFoundException: ../config/firebase-service-account.json (No such file or directory)
```

### 원인
새 backend 코드에 Firebase Admin SDK가 추가되면서 `FirebaseConfig.java`가 서비스 계정 JSON 파일을 로드하려 하는데, EC2에 해당 파일이 없음.

### 해결책
파일을 EC2에 두는 대신 **AWS Secrets Manager**에서 직접 읽도록 변경.

**1) Secrets Manager에 시크릿 생성:**
```bash
aws secretsmanager create-secret \
  --name "eclipse/prod/firebase-credentials" \
  --secret-string "$(cat firebase-service-account.json)" \
  --region ap-northeast-2
```

**2) `FirebaseConfig.java` 수정** — `FIREBASE_SECRET_NAME` 환경변수가 있으면 Secrets Manager에서, 없으면 파일에서 읽도록 변경:
```java
private InputStream resolveCredentials() throws IOException {
    if (secretName != null && !secretName.isBlank()) {
        try (SecretsManagerClient client = SecretsManagerClient.builder()
                .region(Region.AP_NORTHEAST_2).build()) {
            String json = client.getSecretValue(
                GetSecretValueRequest.builder().secretId(secretName).build()
            ).secretString();
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
    }
    return new FileInputStream(credentialsPath);
}
```

**3) `service-app/build.gradle`에 의존성 추가:**
```gradle
implementation 'software.amazon.awssdk:secretsmanager:2.26.25'
```

**4) `eclipse/prod/env` 시크릿에 추가:**
```
FIREBASE_SECRET_NAME=eclipse/prod/firebase-credentials
```

### 참고
EC2 IAM Role(`eclipse-ec2-role`)의 정책이 `eclipse/*` 와일드카드를 허용하므로 추가 IAM 설정 불필요.

---

## 6. docker-compose 환경변수 미전달

### 증상
`.env`에 `FIREBASE_SECRET_NAME`이 있는데 service-app이 여전히 FileNotFoundException으로 크래시.

컨테이너 내부 환경변수 확인 시 `FIREBASE_SECRET_NAME` 없음.

### 원인
`docker-compose.yml`의 `service-app.environment` 섹션에 `FIREBASE_SECRET_NAME`이 없음.  
Docker Compose에서 `.env` 파일은 **compose 파일 내 변수 치환**에만 사용되며, 컨테이너에 자동으로 주입되지 않음.  
컨테이너에 전달하려면 `environment:` 섹션에 명시해야 함.

### 해결책
**`docker-compose.yml`** service-app environment에 추가:
```yaml
environment:
  FIREBASE_SECRET_NAME: ${FIREBASE_SECRET_NAME}
  # ... 기존 항목들
```

---

## 7. CORS 403 오류

### 증상
로그인 API가 서버에서 200을 반환하지만 브라우저에서는 화면 전환이 안됨.  
nginx 로그에서 POST 403 확인. CloudFront가 403을 S3 index.html(200)로 대체해 디버깅이 어려움.

### 원인 분석
```
브라우저 → CloudFront → nginx → service-app
                                    ↓
                              Spring Security CORS 체크
                              Origin: https://d3t0lih9ssyc42.cloudfront.net
                              허용 목록: ["http://localhost:5173"]
                              → 403 Forbidden
```

`WebMvcConfig.java`의 `allowedOriginPatterns`에 CloudFront 도메인이 없음.

axios `withCredentials: true`로 인해 same-origin 요청에도 `Origin` 헤더가 포함되고, Spring Security가 이를 CORS 검증 대상으로 처리함.

### 해결책
**`service-app/src/main/java/.../config/WebMvcConfig.java`** 수정:
```java
registry.addMapping("/**")
    .allowedOriginPatterns(
        "http://localhost:5173",
        "https://d3t0lih9ssyc42.cloudfront.net"
    )
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true);
```

### 참고
nginx CORS map(`map $http_origin $cors_origin`)은 수정 불필요.  
프론트엔드와 API가 동일한 CloudFront 도메인에서 서빙되므로 브라우저 관점에서 same-origin이며, nginx의 CORS 응답 헤더는 실질적 영향 없음.

---

## 8. 홈 자산 조회 503 (환율 캐시 미초기화)

### 증상
로그인 성공 후 홈 화면에서 503 Service Unavailable:
```
GET /api/service/api/v1/home/assets → 503
```

서버 로그:
```
WARN - GlobalExceptionHandler: BusinessException: 지원하지 않는 통화: USD
```

### 원인 분석
두 가지 원인이 복합적으로 작용:

**1) `EXIMBANK_AUTH_KEY=REPLACE_ME`** — 수출입은행 API 인증키 미설정  
`ExchangeRateApiClient.fetchAll()` → API 응답 결과 코드 3(인증코드 오류) → `isSuccess()` = false → `Optional.empty()`

**2) Redis 환율 캐시 스타트업 초기화 없음**  
`ExchangeRateScheduler`가 평일 11:10 KST에만 실행되므로, 서버 재시작 시 Redis에 데이터가 없음.  
API 키가 있더라도 그 사이(11:10 이전) 기간 동안 모든 홈 요청이 503 반환.

**오류 흐름:**
```
GET /api/v1/home/assets
  └─ ExchangeServiceImpl.getExchangeRate("USD")
       └─ Redis cache miss
            └─ fetchAndCache("USD")
                 └─ ExchangeRateApiClient.fetchOne("USD")
                      └─ Exim Bank API → 인증 오류 → Optional.empty()
                           └─ throw BusinessException(EXCHANGE_RATE_UNAVAILABLE, "지원하지 않는 통화: USD")
                                └─ GlobalExceptionHandler → 503 응답
```

### 즉시 조치 (임시)
Redis에 직접 시드 (TTL 72시간):
```bash
REDIS_HOST=$(grep '^REDIS_HOST' /app/eclipse/.env | cut -d= -f2)
docker run --rm redis:7-alpine redis-cli -h $REDIS_HOST -p 6379 \
  SET 'exchange:rate:USD' \
  '{"currencyCode":"USD","currencyName":"미국 달러","baseRate":1370.00,"buyingRate":1393.00,"sellingRate":1347.00,"fetchedAt":"2026-06-25T01:00:00Z"}' \
  EX 259200
```

### 근본 해결 (미완료 — 별도 이슈 처리)
1. **`EXIMBANK_AUTH_KEY` 발급 및 등록**
   - 수출입은행 OpenAPI (oapi.koreaexim.go.kr) 회원가입 후 API 키 발급
   - Secrets Manager `eclipse/prod/env` 수정: `EXIMBANK_AUTH_KEY=<실제키>`

2. **서버 시작 시 환율 자동 로드**  
   `ExchangeRateScheduler`에 `@EventListener(ApplicationReadyEvent.class)` 추가:
   ```java
   @EventListener(ApplicationReadyEvent.class)
   void loadOnStartup() {
       if (rateCache.get("USD").isPresent()) return;
       apiClient.fetchOne("USD").ifPresent(rateCache::put);
   }
   ```
   이 변경은 이번 세션에서 **작성 후 되돌렸음** — 다른 팀원이 별도 이슈로 처리 예정.

### 관련 파일
- `service/src/main/java/.../exchange/internal/ExchangeRateScheduler.java`
- `service/src/main/java/.../exchange/internal/ExchangeRateApiClient.java`
- `service/src/main/java/.../exchange/internal/ExchangeServiceImpl.java`
- `service/src/main/java/.../home/internal/HomeServiceImpl.java`

---

## 인프라 구성 요약

```
Browser
  └─► CloudFront (d3t0lih9ssyc42.cloudfront.net)
        ├─ /api/service/* → ALB → EC2 nginx:80 → service-app:8081
        ├─ /api/ledger/*  → ALB → EC2 nginx:80 → ledger-app:8080
        └─ /*             → S3 (정적 프론트엔드)

EC2 컨테이너 (docker compose)
  ├─ eclipse_nginx   (nginx:alpine)
  ├─ eclipse_service (service-app:8081)
  ├─ eclipse_ledger  (ledger-app:8080)
  └─ eclipse_worker  (worker-app:8082)

AWS
  ├─ RDS MySQL  : eclipse-db.chees648s7z0.ap-northeast-2.rds.amazonaws.com
  ├─ ElastiCache: eclipse-redis.gh7cab.0001.apn2.cache.amazonaws.com
  └─ Secrets Manager
       ├─ eclipse/prod/env                  — 앱 환경변수 전체
       ├─ eclipse/prod/firebase-credentials — Firebase 서비스 계정 JSON
       ├─ eclipse/prod/ec2-ssh-key          — EC2 SSH 키
       ├─ eclipse/prod/ghcr-pat             — GHCR PAT
       └─ eclipse/prod/jwt-private-key      — JWT RSA Private Key
```
