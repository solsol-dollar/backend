# Eclipse API 명세서

| 항목 | 내용 |
|------|------|
| 프로젝트 | Eclipse — 신한 그룹 연계 해외 공모주 청약 앱 |
| 작성일 | 2026-06-16 |
| 최종 수정 | 2026-06-24 |
| 버전 | v1.1 (AI Score API 추가) |

---

## 에러코드

| No | 구분 | 에러코드 | HTTP 상태 | 메시지 | 발생 상황 | 연관 REQ |
|----|------|---------|-----------|--------|----------|---------|
| 1 | 공통 | SUCCESS | 200 | 성공 | 정상 처리 | - |
| 2 | 공통 | C001 | 400 | 잘못된 입력입니다. | 필수 파라미터 누락, 유효성 검증 실패 | REQ-00-91-01 |
| 3 | 공통 | C002 | 404 | 리소스를 찾을 수 없습니다. | 요청한 ID에 해당하는 데이터 없음 | - |
| 4 | 공통 | C003 | 500 | 서버 내부 오류입니다. | 예상치 못한 서버 오류 | REQ-00-91-01 |
| 5 | 인증 | AUTH_001 | 401 | 인증이 필요합니다. | 헤더 누락 또는 유효하지 않은 userId | REQ-00-91-01 |
| 6 | 인증 | AUTH_002 | 401 | 인증에 실패했습니다. | SSO 토큰 유효하지 않음 | REQ-01-91-01 |
| 7 | 청약/원장 | L001 | 422 | 잔액이 부족합니다. | 청약 금액 > 가용 잔액 | REQ-05-92-01 |
| 8 | 청약/원장 | L002 | 409 | 동시 청약이 진행 중입니다. | 동일 IPO 중복 청약 시도 | REQ-05-93-01 |
| 9 | 청약/원장 | L003 | 422 | 청약 가능 기간이 아닙니다. | 청약 기간 외 요청 | REQ-05-03-01 |
| 10 | 청약/원장 | L004 | 404 | 청약 정보를 찾을 수 없습니다. | 존재하지 않는 subscriptionId | - |
| 11 | 청약/원장 | L005 | 422 | 연동된 계좌가 없습니다. | 증권 계좌 미연동 상태에서 청약 시도 | REQ-01-01-02 |
| 12 | 외부 | S002 | 503 | 외부 서비스 오류입니다. | Finnhub/LS증권/환율 API 장애 | - |
| 13 | 외부 | S003 | 404 | IPO 정보를 찾을 수 없습니다. | 조회한 ipoId 미존재 | - |

---

## Response 공통 구조

모든 API의 응답은 아래 공통 구조를 따릅니다.

```json
{
  "code": "SUCCESS",
  "message": "성공",
  "data": { ... }
}
```

---

## 온보딩 (01)

> **서버:** service-app `:8081`

### AUTH-001 — 신한 통합 SSO 로그인

| 항목 | 내용 |
|------|------|
| Method | POST |
| Endpoint | `/api/v1/auth/sso` |
| 인증 필요 | N |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-01-01-01, REQ-01-91-01, REQ-01-91-02 |
| 구현 범위 | MVP |

**Header**
```
Content-Type: application/json
```

**Request Body**
```json
{
  "ssoToken": "string (신한SSO 토큰)"
}
```

**Response data**
```
userId: Long
onboardingStatus: String (REQUIRED|COMPLETED)
접속 토큰은 헤더 반환: X-USER-ID: {userId}
```

**에러코드:** `C001` — 토큰 유효하지 않음 / `AUTH_001` — 인증 실패/타임아웃

**비고:** 평가 환경에서 ssoToken 더미값 허용. 실서비스 전환 시 신한 SSO OAuth2 연동 필요

---

### AUTH-002 — 내 정보 / 온보딩 상태 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/auth/me` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-01-05-01 |
| 구현 범위 | MVP |

**Header**
```
X-USER-ID: {userId} (Long)
Content-Type: application/json
```

**Response data**
```
userId: Long
name: String
onboardingStatus: String (REQUIRED|COMPLETED)
```

**에러코드:** `AUTH_001`

**비고:** 재진입 시 onboardingStatus 확인 후 IPO 캘린더로 스킵

---

### ACC-001 — 그룹사 계좌·카드 자동 스캔

> **서버:** ledger-app `:8080`

| 항목 | 내용 |
|------|------|
| Method | POST |
| Endpoint | `/api/v1/accounts/scan` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-01-01-02, REQ-01-01-03 |
| 구현 범위 | MVP |

**Response data**
```
accounts: [
  {
    id: Long,
    accountType: String (SECURITIES|FX_SAVINGS|FX_ACCOUNT|CHECK_CARD),
    accountName: String,
    accountNumberMasked: String,
    balance: BigDecimal,
    currency: String,
    linked: Boolean,
    isBlocking: Boolean
  }
]
```

**에러코드:** `S002` — 외부 API 오류 / `L005` — 연동된 계좌 없음

**비고:** 증권계좌 미조회 시 isBlocking=true 반환. 신한 그룹사 내부 API 호출

---

### ACC-002 — 연동 계좌·카드 목록 조회

> **서버:** ledger-app `:8080`

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/accounts` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-01-01-03, REQ-01-07-01 |
| 구현 범위 | MVP |

**Query Parameter**
```
accountType: String (optional) — SECURITIES|FX_SAVINGS|FX_ACCOUNT|CHECK_CARD
```

**Response data**
```
accounts: [
  {
    id: Long,
    accountType: String,
    institutionName: String,
    accountName: String,
    accountNumberMasked: String,
    balance: BigDecimal,
    currency: String,
    linked: Boolean,
    linkedAt: String (ISO8601),
    isBlocking: Boolean
  }
]
```

**에러코드:** `AUTH_001`

**비고:** 마이페이지 '내 계좌' 화면에서도 동일 API 사용

---

## IPO 탐색 (02-03)

> **서버:** service-app `:8081`

### IPO-001 — IPO 캘린더 목록 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/ipos` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-02-01-01~02, REQ-02-02-01, REQ-02-03-01~03, REQ-02-05-02, REQ-02-06-01, REQ-02-91-01 |
| 구현 범위 | MVP |

**Query Parameter**
```
status: String (optional) — OPEN|UPCOMING|CLOSED
favoriteOnly: Boolean (optional)
page: int (default 0)
size: int (default 20)
```

**Response data**
```
ipos: [
  {
    id: Long,
    ticker: String,
    companyName: String,
    ipoStatus: String,
    subscriptionStartDate: String,
    subscriptionEndDate: String,
    listingDate: String,
    offerPriceMin: BigDecimal,
    offerPriceMax: BigDecimal,
    confirmedOfferPrice: BigDecimal,
    isFavorite: Boolean,
    statusBadge: String
  }
],
total: Long,
page: int,
size: int
```

**에러코드:** `S002` — Finnhub API 오류 / `S003` — IPO 없음 (빈 배열 반환)

**비고:** Finnhub 캐시 테이블(ipos) 기반 조회. 빈 결과는 200 + 빈 배열로 반환

---

### IPO-002 — IPO 종목 상세 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/ipos/{ipoId}` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-02-07-01, REQ-03-01-01~04, REQ-03-05-01~02, REQ-03-91-01 |
| 구현 범위 | MVP |

**Path Parameter**
```
ipoId: Long
```

**Response data**
```
{
  id: Long,
  ticker: String,
  companyName: String,
  exchangeName: String,
  sector: String,
  ipoStatus: String (UPCOMING|OPEN|CLOSED|LISTED),
  subscriptionStartDate: String,
  subscriptionEndDate: String,
  listingDate: String,
  refundDate: String,
  offerPriceMin: BigDecimal,
  offerPriceMax: BigDecimal,
  confirmedOfferPrice: BigDecimal (null=미확정),
  minimumSubscriptionAmount: BigDecimal,
  isFavorite: Boolean
}
```

**에러코드:** `S003` — IPO 없음

**비고:** confirmedOfferPrice=null이면 프론트에서 '미정' 표시

---

### IPO-003 — 관심 IPO 등록 (하트 토글)

| 항목 | 내용 |
|------|------|
| Method | POST |
| Endpoint | `/api/v1/ipos/{ipoId}/favorites` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-02-05-01, REQ-02-05-02 |
| 구현 범위 | MVP |

**Response data**
```
{
  ipoId: Long,
  isFavorite: true,
  createdAt: String
}
```

**에러코드:** `S003` — IPO 없음 / `C001` — 이미 등록

**비고:** favorite_ipos 테이블 UNIQUE(user_id, ipo_id) 제약

---

### IPO-004 — 관심 IPO 해제

| 항목 | 내용 |
|------|------|
| Method | DELETE |
| Endpoint | `/api/v1/ipos/{ipoId}/favorites` |
| 인증 필요 | Y |
| 성공 HTTP | 204 |
| 연관 REQ | REQ-02-05-01 |
| 구현 범위 | MVP |

**에러코드:** `S003` — IPO 없음

---

### SCORE-001 — IPO 뉴스 기반 기대 신호 점수 조회 ✨ NEW

> **서버:** eclipse-ai `:8083`

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/ipo/{ipoId}/score` |
| 인증 필요 | N |
| 성공 HTTP | 200 |
| 연관 REQ | - |
| 구현 범위 | MVP |

**Path Parameter**
```
ipoId: Long
```

**Response data**
```json
{
  "ipoId": 1,
  "ticker": "BOBS",
  "finalScore": 40,
  "grade": "NEUTRAL",
  "reason": "IPO 시장의 전반적인 분위기가 중립적이다.",
  "summary": "Bob's Discount Furniture는 미국에서 IPO를 추진하며...",
  "topNewsIds": [25, 26],
  "newsCount": 3,
  "scoredAt": "2026-06-20T05:30:00"
}
```

| 필드 | 타입 | Nullable | 설명 |
|------|------|:--------:|------|
| `ipoId` | Long | N | IPO 식별자 |
| `ticker` | String | N | 종목 티커 심볼 |
| `finalScore` | Integer | Y | 뉴스 기반 기대 신호 점수 (0~100). 스코어 없을 때 null |
| `grade` | String | Y | STRONG_POSITIVE / POSITIVE / NEUTRAL / NEGATIVE / STRONG_NEGATIVE |
| `reason` | String | Y | 등급 판단 근거 한 문장 (한국어, 40자 이내). AI 생성 |
| `summary` | String | Y | 핵심 뉴스 2건 기반 통합 요약 3~5문장. 한국경제·매일경제 보도체. AI 생성 |
| `topNewsIds` | List\<Long\> | Y | 스코어 산출에 사용된 핵심 뉴스 ID 2건 |
| `newsCount` | Integer | Y | 스코어 산출에 사용된 총 뉴스 수 |
| `scoredAt` | LocalDateTime | Y | 스코어 산출 시각 |

**에러코드:** `C002` — 해당 ipoId의 점수 미존재 (404)

**비고:**
- finalScore, grade, reason, summary, topNewsIds는 스코어 미산출 시 null
- 스코어는 상장 전 뉴스만 기반으로 산출 (publishedAt < listingDate)
- 산출 로직 상세: `docs/NEWSCORE_AI.md` 참조
- 매일 05:30 자동 산출 (Spring Batch)

---

### NEWS-001 — IPO 뉴스 상세 조회 (topNewsIds용) ✨ NEW

> **서버:** eclipse-ai `:8083`

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/ipo/{ipoId}/news/{newsId}` |
| 인증 필요 | N |
| 성공 HTTP | 200 |
| 구현 범위 | 추후 연동 |

**비고:** `topNewsIds`로 전달된 뉴스 ID로 원문 및 한국어 번역 전문 조회 예정. 현재 DB에는 `ipo_news.content_ko` 저장 완료

---

## 청약 (05)

> **서버:** ledger-app `:8080`

### SUB-001 — IPO 청약 신청

| 항목 | 내용 |
|------|------|
| Method | POST |
| Endpoint | `/api/v1/subscriptions` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-05-03-01~03, REQ-05-92-01, REQ-05-93-01 |
| 구현 범위 | MVP |

**Request Body**
```json
{
  "ipoId": "Long",
  "securitiesAccountId": "Long",
  "shares": "Integer",
  "offerPrice": "BigDecimal"
}
```

**Response data**
```
{
  subscriptionId: Long,
  ipoId: Long,
  ticker: String,
  requestedShares: Integer,
  subscriptionAmount: BigDecimal,
  currency: "USD",
  subscriptionStatus: "REQUESTED",
  subscribedAt: String
}
```

**에러코드:** `L001` — 잔액 부족 / `L002` — 동시 청약 충돌 / `L003` — 청약 기간 아님 / `L005` — 계좌 미연동

**비고:** MVP: execution_mode=MOCK. 청약 직전 잔액 재검증 포함 (REQ-05-92-01)

---

### SUB-002 — 청약 확정

| 항목 | 내용 |
|------|------|
| Method | PUT |
| Endpoint | `/api/v1/subscriptions/{subscriptionId}/confirm` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-05-03-04, REQ-05-92-01~02 |
| 구현 범위 | MVP |

**Response data**
```
{
  subscriptionId: Long,
  subscriptionStatus: "CONFIRMED",
  confirmedAt: String
}
```

**에러코드:** `L001` — 재검증 실패(잔액 변동) / `L002` — 충돌 / `L003` — 마감 / `L004` — 청약 없음

**비고:** 확정 시 SubscriptionConfirmedEvent 발행 → 배정 레코드 생성

---

### SUB-003 — 청약 취소

| 항목 | 내용 |
|------|------|
| Method | DELETE |
| Endpoint | `/api/v1/subscriptions/{subscriptionId}` |
| 인증 필요 | Y |
| 성공 HTTP | 204 |
| 연관 REQ | REQ-05-91-01 |
| 구현 범위 | MVP |

**에러코드:** `L003` — 청약 기간 아님 / `L004` — 청약 없음

---

### SUB-004 — 내 청약 목록 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/subscriptions` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-05-91-01 |
| 구현 범위 | MVP |

**Query Parameter**
```
ipoId: Long (optional)
status: String (optional) — REQUESTED|CONFIRMED|ALLOCATED
```

**Response data**
```
subscriptions: [
  {
    subscriptionId: Long,
    ipoId: Long,
    ticker: String,
    companyName: String,
    requestedShares: Integer,
    subscriptionAmount: BigDecimal,
    subscriptionStatus: String,
    subscribedAt: String
  }
]
```

**에러코드:** `AUTH_001`

---

## 청약 후 · 리턴플랜 (06-07)

> **서버:** ledger-app `:8080`

### ALLOC-001 — 배정 결과 목록 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/subscription-results` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-06-01-01~02, REQ-06-02-01, REQ-06-91-01 |
| 구현 범위 | MVP |

**Query Parameter**
```
subscriptionId: Long (optional)
```

**Response data**
```
results: [
  {
    subscriptionResultId: Long,
    subscriptionId: Long,
    ipoId: Long,
    ticker: String,
    companyName: String,
    subscriptionAmount: BigDecimal,
    allocatedAmount: BigDecimal,
    refundAmount: BigDecimal,
    allocationRate: BigDecimal,
    allocatedShares: Integer,
    currentPrice: BigDecimal,
    pnlUsd: BigDecimal,
    allocationStatus: String (PENDING|COMPLETED),
    allocatedAt: String
  }
]
```

**에러코드:** `AUTH_001`

**비고:** allocationStatus=PENDING이면 '배정 대기' 표시. currentPrice는 실시간 시세 API 조회

---

### ALLOC-002 — 배정 결과 상세 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/subscription-results/{subscriptionResultId}` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-06-01-01, REQ-06-02-01, REQ-06-03-01 |
| 구현 범위 | MVP |

**Response data**
```
{
  subscriptionResultId: Long,
  subscriptionAmount: BigDecimal,
  allocatedAmount: BigDecimal,
  refundAmount: BigDecimal,
  allocationRate: BigDecimal,
  allocatedShares: Integer,
  currentPrice: BigDecimal,
  pnlUsd: BigDecimal,
  listingDate: String,
  hasReturnPlan: Boolean
}
```

**에러코드:** `C002` — 배정 없음

**비고:** refundAmount>0이면 리턴플랜 배너 표시 (REQ-06-03-01)

---

### RP-001 — 리턴 플랜 생성

| 항목 | 내용 |
|------|------|
| Method | POST |
| Endpoint | `/api/v1/return-plans` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-07-00-01~02, REQ-07-01-01 |
| 구현 범위 | MVP |

**Request Body**
```json
{
  "subscriptionResultId": "Long"
}
```

**Response data**
```
{
  returnPlanId: Long,
  subscriptionResultId: Long,
  totalRefundAmount: BigDecimal,
  nextIpoInfo: {
    ipoId: Long,
    ticker: String,
    subscriptionStartDate: String
  },
  savingsRate: BigDecimal,
  allocations: [
    {
      destinationType: String (SECURITIES|FX_SAVINGS|FX_ACCOUNT),
      ratio: Integer (0),
      amount: BigDecimal (0)
    }
  ]
}
```

**에러코드:** `L004` — 배정 없음 / `C001` — 이미 생성된 플랜

**비고:** 생성 시 비율 전부 0으로 초기화. 맥락정보(다음 IPO 일정, 외화적금금리) 포함 반환

---

### RP-002 — 리턴 플랜 비율 수정

| 항목 | 내용 |
|------|------|
| Method | PUT |
| Endpoint | `/api/v1/return-plans/{returnPlanId}` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-07-01-02~04, REQ-07-04-03 |
| 구현 범위 | MVP |

**Request Body**
```json
{
  "allocations": [
    { "destinationType": "SECURITIES", "ratio": 40 },
    { "destinationType": "FX_SAVINGS", "ratio": 40 },
    { "destinationType": "FX_ACCOUNT", "ratio": 20 }
  ]
}
```
> 3개 ratio 합산 = 100 필수. 5단위 (0, 5, 10 ...)

**Response data**
```
{
  returnPlanId: Long,
  allocations: [
    {
      destinationType: String,
      ratio: Integer,
      amount: BigDecimal
    }
  ]
}
```

**에러코드:** `C001` — ratio 합산 ≠ 100 / `C002` — 플랜 없음

**비고:** 3개 ratio 합 = 100 서버 검증

---

## 증권 (09)

> **서버:** service-app `:8081`

### SEC-001 — 종목 리스트 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/securities/products` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-09-02-01~05 |
| 구현 범위 | MVP |

**Query Parameter**
```
type: String (required) — OVERSEAS|ETF
sort: String (optional) — TRADING_VALUE|TRADING_VOLUME|RISE|FALL
keyword: String (optional) — 검색
page: int
size: int
```

**Response data**
```
products: [
  {
    productId: Long,
    ticker: String,
    productName: String,
    productType: String,
    currentPrice: BigDecimal,
    changeRate: BigDecimal,
    changeAmount: BigDecimal,
    currency: "USD",
    rank: Integer
  }
],
total: Long
```

**에러코드:** `S002` — 시세 API 오류

**비고:** LS증권 실시간 시세 API 연동. keyword 검색 시 ticker/productName 일치 검색

---

### SEC-002 — 종목 상세 (현재가·차트)

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/securities/products/{productId}` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-09-05-01~03 |
| 구현 범위 | MVP |

**Query Parameter**
```
chartPeriod: String (default 1M) — 1D|1W|1M|3M
```

**Response data**
```
{
  productId: Long,
  ticker: String,
  productName: String,
  currentPrice: BigDecimal,
  changeRate: BigDecimal,
  changeAmount: BigDecimal,
  currency: "USD",
  chartData: [
    {
      date: String,
      closePrice: BigDecimal
    }
  ],
  sector: String,
  exchangeName: String
}
```

**에러코드:** `C002` — 종목 없음 / `S002`

**비고:** 1개월 선형 차트 기본 제공

---

### SEC-003 — 실시간 5호가 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/securities/products/{productId}/quotes` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-09-05-04~05 |
| 구현 범위 | MVP |

**Response data**
```
{
  ticker: String,
  currentPrice: BigDecimal,
  bids: [ { price: BigDecimal, quantity: Integer } x5 ],
  asks: [ { price: BigDecimal, quantity: Integer } x5 ],
  updatedAt: String
}
```

**에러코드:** `S002` — 시세 API 오류

**비고:** LS증권 WebSocket 스냅샷 → REST 반환. 시장 마감 시 최종 호가 반환

---

### SEC-004 — 보유 종목 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/securities/holdings` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-09-03-01 |
| 구현 범위 | MVP |

**Response data**
```
holdings: [
  {
    holdingId: Long,
    productId: Long,
    ticker: String,
    productName: String,
    totalQuantity: Integer,
    averagePrice: BigDecimal,
    currentPrice: BigDecimal,
    evaluatedValue: BigDecimal,
    unrealizedPnl: BigDecimal,
    returnRate: BigDecimal,
    currency: "USD"
  }
]
```

**에러코드:** `AUTH_001`

**비고:** 현재가는 LS증권 실시간 시세 기반

---

## 홈 · 마이 · 유입 (10-11-08)

> **서버:** service-app `:8081`

### HOME-001 — 홈 대시보드 통합 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/home/dashboard` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-10-01-02, REQ-10-02-01, REQ-10-03-01, REQ-10-04-01 |
| 구현 범위 | MVP |

**Response data**
```
{
  assetSummary: {
    securitiesBalance: BigDecimal,
    fxSavingsBalance: BigDecimal,
    fxAccountBalance: BigDecimal,
    totalUsd: BigDecimal
  },
  exchangeRate: {
    usdKrw: BigDecimal,
    changeRate: BigDecimal,
    updatedAt: String
  },
  accounts: [
    {
      accountId: Long,
      accountName: String,
      accountNumberMasked: String,
      balance: BigDecimal
    }
  ],
  favoriteIpos: [ (max 3) ... ]
}
```

**에러코드:** `AUTH_001` / `S002` — 환율 API 오류

**비고:** 한 번의 호출로 홈 전체 구성. 환율 데이터는 인메모리 캐시 (TTL 30초)

---

### HOME-002 — 실시간 USD/KRW 환율 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/home/exchange-rate` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-10-02-01, REQ-09-02-02 |
| 구현 범위 | MVP |

**Response data**
```
{
  usdKrw: BigDecimal,
  changeRate: BigDecimal,
  changeAmount: BigDecimal,
  updatedAt: String
}
```

**에러코드:** `S002` — 환율 API 오류

---

### MY-001 — 투자 성향 진단 제출

| 항목 | 내용 |
|------|------|
| Method | POST |
| Endpoint | `/api/v1/mypage/investment-profile` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-11-01-01~04 |
| 구현 범위 | MVP |

**Request Body**
```json
{
  "surveyAnswers": [
    { "questionId": 1, "answerId": 3 }
  ]
}
```
> 7문항 필수

**Response data**
```
{
  profileId: Long,
  riskTolerance: String
    (CONSERVATIVE|MODERATE_CONSERVATIVE|NEUTRAL|AGGRESSIVE|VERY_AGGRESSIVE),
  scoreBreakdown: {
    investmentExperience: Integer,
    returnExpectation: Integer,
    lossToleranceScore: Integer
  },
  recommendedAllocation: {
    ipo: Integer,
    etf: Integer,
    savings: Integer,
    rp: Integer
  },
  diagnosedAt: String
}
```

**에러코드:** `C001` — 7문항 미완성

**비고:** 진단 결과를 리턴플랜 프리셋으로 연동 가능 (REQ-11-01-05)

---

### MY-002 — 최근 투자 성향 진단 결과 조회

| 항목 | 내용 |
|------|------|
| Method | GET |
| Endpoint | `/api/v1/mypage/investment-profile` |
| 인증 필요 | Y |
| 성공 HTTP | 200 |
| 연관 REQ | REQ-11-01-01~05 |
| 구현 범위 | MVP |

**Response data**
```
{
  profileId: Long,
  riskTolerance: String,
  scoreBreakdown: {...},
  recommendedAllocation: {...},
  diagnosedAt: String
}
```

**에러코드:** `C002` — 진단 이력 없음

**비고:** 미진단 시 C002, 프론트에서 진단 유도

---

## 서버 포트 요약

| 서버 | 포트 | 담당 도메인 |
|------|:----:|-----------|
| service-app | 8081 | 온보딩, IPO 탐색, 증권, 홈·마이 |
| ledger-app | 8080 | 계좌, 청약, 배정, 리턴플랜 |
| eclipse-ai | 8083 | IPO 뉴스 스코어, 뉴스 번역 |
