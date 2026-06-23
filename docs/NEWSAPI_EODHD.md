# EODHD 뉴스 파이프라인 설계 결정

> 최초 작성: 2026-06-22 / 최종 수정: 2026-06-23  
> 대상 테이블: `ipo_news`, `ipos`

---

## 1. 스키마 변경

### ipo_news 컬럼 매핑

| 컬럼 | EODHD 필드 | 비고 |
|------|-----------|------|
| `title` | `title` | 기존 유지 |
| `source` | `link` 파싱 | URL에서 도메인 추출 (예: "finance.yahoo.com") |
| `published_at` | `date` | ET 기준 날짜 파싱 |
| `url` | `link` | 기존 유지 |
| `phase` | — | `PRE` / `POST` (상장 전·후 구분, 아래 §2 참고) |
| `content` | `content` | 영어 원문 풀 본문 (3,000~7,000자). RAG 임베딩 입력값 |
| `title_ko` | — | 한국어 번역 제목 (Step2에서 Claude 생성) |
| `summary` | — | 한국어 요약 2~3문장 (Step2에서 Claude 생성). IPO-007 API 응답에 노출 |

### source_tier는 파이프라인에서 처리하지 않음

EODHD는 `source: "Nasdaq"` 문자열만 제공함. TIER 분류 기준(TIER1/2/3)과 매핑 로직은 AI팀이 점수 계산 시 처리. 파이프라인이 TIER 기준을 갖는 건 책임 분리 위반이고, AI팀이 기준을 바꿀 때마다 파이프라인도 수정해야 하는 결합이 생김.

---

## 2. PRE / POST 수집 범위 (phase 설계)

### 배경

IPO 전후 뉴스를 **구분해서** 비교 분석하기 위해 `phase` 컬럼 도입. 상장 전 기사(기대감·공모가·리스크)와 상장 후 기사(실제 주가·시장 반응)를 별도로 노출할 수 있음.

### 수집 창

```
PRE  : listingDate - 365일  →  listingDate - 1일   (상장 1년 전부터 전날까지)
POST : listingDate           →  listingDate + 90일  (상장일부터 3개월)
```

- POST 90일 제한 이유: 상장 후 기사는 PRE보다 수십 배 많아질 수 있음. 90일이면 IPO 초기 시장 반응을 충분히 포착하면서 수집량을 제어할 수 있음.
- POST 상한이 오늘 이후면 `today`로 클리핑.

### phase 결정 로직

Reader에서 기사 `publishedAt`와 `listingDate`를 비교해 결정:

```java
String phase = publishedAt.toLocalDate().isBefore(ipo.getListingDate()) ? "PRE" : "POST";
```

Processor(`IpoNewsItemConverter`)는 Reader가 세팅한 phase를 그대로 전달.

---

## 3. 이중 수집 전략

### 문제

EODHD `s=TICKER.US` 기반 검색은 해당 종목이 **상장된 후** 티커가 부여돼야 기사가 뜸. 상장 전 언론 보도(IPO 준비·공모가 발표 등)는 종목 티커가 없어 개별 티커 검색으로 잡히지 않는 경우가 있음.

### 해결: 두 가지 수집 방식 병행

| Reader | API 파라미터 | 특징 |
|--------|------------|------|
| `IpoNewsFetchReader` | `s=TICKER.US` | 종목 단위 증분 수집. cursor(`MAX(published_at)`) 기반 |
| `IpoNewsTagReader` | `t=IPO` | IPO 태그 전체 기사 수집 → 메모리 내 키워드 필터링 |

**IpoNewsTagReader 키워드 추출 로직**

회사명에서 법인 형태 접미사(Inc., Corp., Ltd. 등)를 제거 → 첫 번째 단어를 검색 키워드로 사용. 최소 2글자 이상이어야 유효.

```
"Cerebras Systems Inc."  →  "Cerebras"
"MiniMed Group, Inc."    →  "MiniMed"
```

예외 종목은 `KEYWORD_OVERRIDE` 맵에 수동 등록:
```java
"OFRM" → "Once Upon a Farm"
"REA"  → "Rare Earths Americas"
```

단어가 너무 짧거나 일반 단어라 오탐률이 높은 종목은 `TAG_SEARCH_EXCLUDED`로 제외:
```java
TAG_SEARCH_EXCLUDED = Set.of("SPCX")  // "Space"가 너무 일반적
```

---

## 4. Dedup 전략

### EODHD 데이터 분석 결과 (CBRS, limit=1000 기준)

- 총 기사 수: 261건
- EODHD 응답 내 article id 필드 없음 (`date, title, content, link, symbols, tags, sentiment` 뿐)
- URL 중복: 0건
- title 중복: 23건 → 신디케이션 (Nasdaq 기사를 Yahoo Finance가 재게재)
  - 20건: 제목 + 날짜 동일
  - 3건: 제목 동일, 날짜 1~2시간 차이 (별개 기사로 판단)

### UNIQUE 제약 2개

```sql
UNIQUE KEY uq_ipo_url  (ipo_id, url(255))    -- 파이프라인 재실행 안전망
UNIQUE KEY uq_ipo_news (ipo_id, title(191), published_at)  -- 신디케이션 방지
```

**url 단독이 아닌 이유:** URL이 달라도 같은 기사인 신디케이션 케이스를 못 잡음.  
**title 단독이 아닌 이유:** "Markets Close Higher" 같은 제목이 다른 날 다시 나오면 다른 기사인데 막혀버림. `published_at`을 붙여 "같은 날 같은 제목"만 중복으로 판단.  
**prefix 길이 유지 이유:** `title(191)` — utf8mb4에서 191×4=764 bytes, 767-byte InnoDB 인덱스 상한 이하. `url(255)` — VARCHAR(500)이라 full-index 불가.

### 수집 필터 (DB 삽입 전)

- `title == null || blank` → 제거
- `content.length() < 500` → 페이월 기사 (본문이 잘린 것) → 제거
- `publishedAt`가 수집 창(windowStart ~ windowEnd) 밖 → 제거

### INSERT 방식

```sql
INSERT IGNORE INTO ipo_news (ipo_id, title, source, published_at, url, phase, content, ...)
VALUES (...)
```

`INSERT IGNORE`로 UNIQUE 위반 시 오류 없이 skip. `@Modifying` 쿼리에 `@Transactional` 필수 (Spring Data JPA가 자동으로 트랜잭션을 열지 않음).

---

## 5. 증분 수집 전략

### MAX(published_at) cursor 방식

```
[1] SELECT MAX(published_at) FROM ipo_news WHERE ipo_id = ?

    ┌─ 결과 있음 (기존에 수집한 적 있음)
    │   → from = MAX 날짜 (날짜만 추출, EODHD는 시간 포함 from 미지원)
    │
    └─ 결과 없음 (신규)
        → from = listingDate - 365일

[2] EODHD 호출: from ~ windowEnd 범위 기사 반환
    (from 당일 기사가 일부 중복 반환될 수 있음)

[3] INSERT IGNORE로 중복 자동 처리
```

### 왜 배치 시작 시각이 아닌 ticker별 MAX인가

배치 중간 실패 시, 다음 실행에서 각 ticker가 **자신의 마지막 데이터**를 기준으로 이어받음. 배치 전체 시작 시각을 쓰면 앞서 성공한 ticker도 처음부터 다시 가져옴.

---

## 6. Spring Batch 구조

### Job 구성

```
ipoNewsFetchOnlyJob  [매일 03:00 스케줄]
  Step 1: ipoNewsSyncStep   (ticker 기반 수집)
  Step 2: ipoNewsTagStep    (IPO 태그 기반 수집)

ipoNewsSyncJob  [RAG 연동 후 스케줄 추가 예정]
  Step 1: ipoNewsSyncStep   (ticker 기반 수집)
  Step 2: ipoNewsTagStep    (IPO 태그 기반 수집)
  Step 3: ipoNewsKoSummaryStep  (한국어 번역)
```

### 청크 설정

| Step | chunk size | 이유 |
|------|-----------|------|
| ipoNewsSyncStep | 50 | 종목당 최대 1,000건 × 38종목, 부분 커밋으로 실패 복구 |
| ipoNewsTagStep | 50 | 동일 |
| ipoNewsKoSummaryStep | 1 | OpenAI API 호출 1건 = 1 트랜잭션, 번역 실패가 다른 기사에 영향 없도록 |

### 배치 안정성 설계

| 항목 | 구현 |
|------|------|
| `@StepScope` | Reader 3개 모두 적용. Job 재실행 시 iterator 상태 격리 |
| date 역전 guard | `from >= to`이면 수집 skip (cursor가 windowEnd를 넘어선 경우) |
| API 예외 처리 | `IpoNewsTagReader.fetchAllPages()` try-catch → Step FAILED 방지, 빈 리스트 반환 |
| `@BeforeStep` API key 체크 | key 미설정 시 빈 iterator 반환, Step은 0건으로 정상 종료 |

---

## 7. 한국어 번역 파이프라인

### 대상 선별

`IpoNewsKoSummaryReader`: 각 종목별 `title_ko IS NULL` 기사 중 `published_at` 최신 3건.  
RAG 연동 후에는 `rag_rank <= 3` 조건으로 교체 예정.

### 번역 처리

- 모델: `claude-haiku-4-5-20251001` (속도·비용 균형)
- 입력: 영어 원문 `title` + `content`
- 출력: `title_ko` (한국어 제목), `summary` (2~3문장 요약)
- 프롬프트 규칙:
  - 한국 경제 신문 보도체 (~했다, ~밝혔다, ~예정이다)
  - 회사명·티커·거래소명은 영어 유지
  - 달러 금액·주식 수는 숫자 표현 ($18, $200M)
  - 공모가·조달금액·상장일 등 핵심 수치 우선 포함
  - 본문에 없는 정보 추가 금지

---

## 8. 전체 파이프라인 흐름

```
[1단계] 뉴스 수집                                          ✅ 구현 완료
  EODHD API → ipo_news 저장 (영어 원문)
  매일 새벽 3시 실행
  이중 수집: ticker 기반 + IPO 태그 기반
  PRE(상장 전 1년) / POST(상장 후 3개월) 구분 저장
  INSERT IGNORE + UNIQUE 제약으로 멱등성 보장

      ↓

[2단계] 벡터 임베딩 저장 (RAG 준비)                        ⬜ 미구현
  ipo_news.content → text-embedding-3-small (1,536차원)
  → Vector DB (pgvector) 저장
  신규 기사(embedding 없는 것)만 처리

      ↓

[3단계] 관련 기사 검색 + AI 분석 (RAG)                     ⬜ 미구현
  쿼리: "{ticker} IPO news stock market analysis"
  → Top-10 선별 (코사인 유사도 0.3 이상)
  → GPT-4o-mini가 감성 방향·핵심 신호·위험 요인 분석 → signalSummary 생성
  상세 설계: docs/NEWSCORE_AI.md §3~4 참고

    ↓ (병렬)

[3-1단계] 한국어 번역 + 요약                               ✅ 구현 완료 (RAG 연동 전)
  현재: title_ko IS NULL 최신 3건 번역
  RAG 연동 후: rag_rank <= 3 기사로 교체
  모델: claude-haiku-4-5, 출력: title_ko + summary
  노출: IPO-007 API 응답

      ↓

[4단계] 점수 계산                                          ⬜ 미구현
  signalSummary + 뉴스 메타데이터 → 0~100점 → ipo_risk_scores 저장
  상세 설계: docs/NEWSCORE_AI.md §5~6 참고
```

### 현황

| 단계 | 항목 | 상태 | 비고 |
|------|------|------|------|
| 1단계 | 뉴스 수집 (EODHD) | ✅ 완료 | PRE/POST 구분, 이중 수집 |
| 2단계 | 벡터 임베딩 저장 | ⬜ 미구현 | pgvector 인프라 구성 필요 |
| 3단계 | RAG 검색 + AI 분석 | ⬜ 미구현 | 2단계 완료 후 구현 |
| 3-1단계 | 한국어 번역 + 요약 | ✅ 완료 (RAG 연동 전) | RAG 완료 시 선별 기준 교체 |
| 4단계 | 점수 계산 | ⬜ 미구현 | AI팀 협의 필요 |

---

## 9. Spring Batch 선택 이유

**대량 초기 적재**

첫 실행 시 종목당 최대 1,000건(365일치)을 적재. 38개 종목이면 수만 건이 한 번에 들어옴. 전체를 단일 트랜잭션으로 처리하면 OOM 또는 실패 시 전량 롤백.

**청크 트랜잭션으로 부분 커밋 보장**

50건 단위로 커밋. 3만 번째 기사에서 실패해도 앞서 커밋된 2,950건은 보존. 다음 실행에서 `MAX(published_at)` 기준으로 이어서 수집.

**실행 이력 자동 관리**

`BATCH_JOB_EXECUTION` 테이블에 성공/실패/소요시간 자동 기록. 별도 모니터링 테이블 없이 운영 가시성 확보.

**멱등성 보장**

동일 Job을 여러 번 실행해도 데이터 정합성 유지. `INSERT IGNORE` + UNIQUE 제약으로 중복 삽입 차단. 배치 중간 실패 후 재실행해도 동일 결과 보장.
