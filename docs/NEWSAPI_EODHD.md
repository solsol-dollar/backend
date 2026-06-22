# EODHD 뉴스 파이프라인 설계 결정

> 결정 일자: 2026-06-22  
> 대상 테이블: `ipo_news`, `ipos`

---

## 1. 스키마 변경

### ipo_news 컬럼 매핑

| 컬럼 | EODHD 필드 | 비고 |
|------|-----------|------|
| `title` | `title` | 기존 유지 |
| `source` | `source` | 기존 유지 (예: "Nasdaq", "Seeking Alpha") |
| `published_at` | `date` | 기존 유지 |
| `url` | `link` | 기존 유지 |
| `content` | `content` | 영어 원문 풀 본문 (3,000~7,000자). RAG 임베딩 입력값 |
| `summary` | — | 한국어 요약 2~3문장 (Step2에서 Claude Haiku 생성). IPO-007 API 응답에 노출 |

### source_tier는 파이프라인에서 처리하지 않음

EODHD는 `source: "Nasdaq"` 문자열만 제공함. TIER 분류 기준(TIER1/2/3)과 매핑 로직은 AI팀이 점수 계산 시 처리. 파이프라인이 TIER 기준을 갖는 건 책임 분리 위반이고, AI팀이 기준을 바꿀 때마다 파이프라인도 수정해야 하는 결합이 생김.

---

## 2. Dedup 전략

### EODHD 데이터 분석 결과 (CBRS, limit=1000 기준)

- 총 기사 수: 261건 (limit=1000 줘도 261건이 상한 — 실제 보유량 전부)
- EODHD 응답 내 article id 필드 없음 (`date, title, content, link, symbols, tags, sentiment` 뿐)
- URL 중복: 0건
- title 중복: 23건 → 신디케이션 (Nasdaq 기사를 Yahoo Finance가 재게재)
  - 20건: 제목 + 날짜 동일
  - 3건: 제목 동일, 날짜 1~2시간 차이 (별개 기사로 판단)

### 신디케이션 기사 처리

같은 기사가 Nasdaq과 Yahoo Finance 두 URL로 존재. 내용이 약간 다르지만 (Nasdaq은 Key Points 섹션 추가) 본질적으로 동일 기사. 둘 다 저장하면 AI 감성·일관성 점수에 같은 신호가 두 번 반영되어 왜곡.

### UNIQUE 제약 2개

```sql
ALTER TABLE ipo_news
  ADD UNIQUE KEY uq_url  (url(255)),             -- 파이프라인 재실행 안전망
  ADD UNIQUE KEY uq_news (title(191), published_at);  -- 신디케이션 방지
```

**url 단독이 아닌 이유:** URL이 달라도 같은 기사인 신디케이션 케이스를 못 잡음.  
**title 단독이 아닌 이유:** "Markets Close Higher" 같은 제목이 다른 날 다시 나오면 다른 기사인데 막혀버림. `published_at`을 붙여 "같은 날 같은 제목"만 중복으로 판단.

**날짜와 제목이 다른 본문 100자가 같은 기사 (같은 배경 문장으로 시작하는 다른 기사임) (9건 관측):** DB 제약으로 잡으려면 본문 유사도 분석이 필요해 파이프라인 레벨에서 처리하기 복잡하고, RAG 유사도 임계값(0.3)에서 자연스럽게 걸러짐. 허용.

---

## 3. 증분 수집 전략

### last_news_collected_at 컬럼 추가하지 않는 이유

`ipos` 테이블에 별도 컬럼을 추가하면 배치 실패 시 tracking 값 자체가 틀어짐. 실제 데이터에서 기준점을 뽑는 것이 배치 실패에도 항상 정확함.

### 선택한 방식: ticker별 MAX(published_at) 기준

#### 흐름

```
[1] DB에서 해당 ticker의 마지막 기사 날짜 조회
    SELECT MAX(published_at) FROM ipo_news WHERE ipo_id = ?

    ┌─ 결과가 있으면 (기존에 수집한 적 있음)
    │   → MAX = 2026-06-21 15:20:00
    │   → from = 2026-06-21 (날짜만 추출)
    │   → EODHD에 6월 21일부터 기사 요청 (21일 포함)
    │
    └─ 결과가 없으면 (신규 IPO, 처음 수집)
        → from = listing_date - 365일
        → 상장일 기준 1년치 전체 소급 수집

[2] EODHD 호출
    GET /api/news?s=CBRS.US&from=2026-06-21&limit=1000
    → 6월 21일부터 기사 전부 반환 (21일 포함)
      (이미 저장된 6월 21일 기사 + 새로운 6월 22일 기사)

[3] INSERT 시도
    → 6월 21일 기사: UNIQUE 제약에 걸려 자동 거절 (이미 있으니까)
    → 6월 22일 기사: 신규 저장 성공
```

#### 왜 ticker별로 MAX를 뽑는가

배치가 47개 ticker를 순서대로 처리하다가 중간(예: 23번째)에 실패해도, 다음 실행 때 각 ticker가 자신의 마지막 데이터 기준으로 독립적으로 이어받음. 배치 전체 시작 시각을 from으로 쓰면 앞서 성공한 ticker도 처음부터 다시 가져오게 됨.

#### EODHD `from` 파라미터 제약

`YYYY-MM-DD` 날짜 포맷만 지원 (시간 포함 불가). 이 때문에 from 날짜의 기사가 일부 중복 반환되고, UNIQUE 제약이 이를 처리함.

### 레이어 구조

```
1차: from 파라미터   → 이전 날짜 기사 네트워크 전송 자체를 차단
2차: UNIQUE 제약     → from 날짜 당일 기존 기사 중복 INSERT 차단
```

---

## 4. 전체 파이프라인 흐름

```
[파이프라인 A — 우리 담당]

  Step1: 뉴스 수집 (구현 완료)
    EODHD API → ipo_news.content 저장 (영어 원문)
    6시간마다 실행, IPO별 MAX(published_at) 증분 수집

        ↓

  Step2: 한국어 요약 생성 (구현 완료)
    IPO별 최신 3건 (summary IS NULL) → Claude Haiku → ipo_news.summary 저장
    IPO-007 API가 이 summary를 사용자에게 반환

[파이프라인 B — AI팀 담당]

  Step3: 벡터 임베딩
    ipo_news.content → text-embedding-3-small → pgvector 저장

        ↓

  Step4: RAG + AI 분석
    관련 기사 검색 → gpt-4o-mini → signalSummary 생성

        ↓

  Step5: 점수 계산
    8가지 요소 가중 합산 → ipo_risk_scores 저장
```

### 현황

| 항목 | 상태 | 담당 |
|------|------|------|
| 뉴스 수집 (EODHD) | ✅ 완료 | 우리 |
| 한국어 요약 생성 | ✅ 완료 | 우리 |
| 벡터 임베딩 | ⬜ 미확인 | AI팀 협의 필요 |
| RAG + 분석 + 점수 | ⬜ 미구현 | AI팀 |

---

## 5. Spring Batch 선택 이유

**대량 초기 적재**

첫 실행 시 종목당 최대 1,000건(365일치)을 적재. 47개 종목이면 수만 건이 한 번에 들어옴. 전체를 단일 트랜잭션으로 처리하면 OOM 또는 실패 시 전량 롤백.

**청크 트랜잭션으로 부분 커밋 보장**

50건 단위로 커밋. 3만 번째 기사에서 실패해도 앞서 커밋된 2,950건은 보존. 다음 실행에서 `MAX(published_at)` 기준으로 이어서 수집.

**실행 이력 자동 관리**

`BATCH_JOB_EXECUTION` 테이블에 성공/실패/소요시간 자동 기록. 별도 모니터링 테이블 없이 운영 가시성 확보.

