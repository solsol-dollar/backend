# 뉴스 API 테스트 결과

> 테스트 일자: 2026-06-22  
> 대상: DB 저장 IPO 종목 중 대형주 4개 + 소형/중소형 4개  
> 평가 기준: NEWSCORE_AI.md 점수 산출 요소 기준  
> 테스트 API: EODHD / GNews / Finnhub+Diffbot / Marketaux

---

## 1. EODHD Financial News

### 수집 현황 (limit=50 기준)

| 티커 | 회사명 | 규모 | 건수 | 평균 본문 | 출처 |
|------|--------|------|:----:|:--------:|------|
| CBRS | Cerebras Systems | 대형 $185 | 5건+ | ~7,000자 | Nasdaq, Seeking Alpha |
| SPCX | SpaceX | 대형 $135 | 10건+ | 5,416자 | Yahoo Finance, Nasdaq, Seeking Alpha |
| QNT | Quantinuum | 대형 $60 | 10건+ | 4,937자 | Nasdaq, Yahoo Finance, Seeking Alpha |
| PS | Pershing Square | 대형 $50 | 확인됨 | 미측정 | — |
| MOBI | Mobia Medical | 소형 $15 | 10건+ | 4,613자 | Nasdaq, Yahoo Finance, Seeking Alpha |
| BXDC | Blackstone Digital | 중소 $20 | 10건+ | 3,238자 | Yahoo Finance, Seeking Alpha |
| ELOX | Eloxx Pharmaceuticals | 소형 $11 | **50건+** | **7,777자** | Nasdaq, Yahoo Finance, GlobeNewsWire |
| BOBS | Bob's Discount Furniture | 중소 $17 | 0건 | — | 미인덱싱 |

### 점수 요소별 평가

| 점수 요소 | 평점 | 판단 |
|---------|:----:|------|
| **① 감성 (25%)** | ★★★★★ | 평균 본문 3,000~7,000자. sentiment 점수 내장 (`polarity`, `pos`, `neg`) |
| **② 최신성 (15%)** | ★★★★☆ | timezone 포함 datetime 정밀 제공. 일부 종목 IPO 이전 기사 혼입 → 날짜 필터 필수 |
| **③ 뉴스 양 (10%)** | ★★★★☆ | limit=50 기준 충분. BOBS만 미인덱싱. 운영 시 limit=100~500 권장 |
| **④ 출처 신뢰도 (12%)** | ★★★★★ | Seeking Alpha, Nasdaq, Yahoo Finance, GlobeNewsWire — TIER1/TIER2 금융 매체 |
| **⑤ 신호 강도 (13%)** | ★★★★☆ | 풀 본문으로 LLM keySignals/riskFactors 추출 가능. BXDC 일부 단신(300자) 혼입 |
| **⑦ 일관성 (7%)** | ★★★★☆ | 내장 sentiment로 감성 분포 계산 가능 |

### 파이프라인 적용 시 주의사항

- **날짜 노이즈**: MOBI(2016년), ELOX(2022년)처럼 IPO 이전 기사 혼입 → `IPO일 ±365일` 필터 필수
- **단신 필터**: 본문 500자 미만 기사 제외 권장
- **Rate Limit**: API 호출 1번 = 1 call 차감. 기사가 50개 반환되어도 call 소비는 1회. 무료 플랜 ~20 call/일이므로 **47개 종목 전체 조회 불가** → 유료 플랜 필수 ($19.99/월)

---

## 2. GNews

### 수집 현황

| 티커 | 총건수(reported) | 실제 반환 | content | 비고 |
|------|:---------------:|:--------:|:-------:|------|
| CBRS | 150건 | 10건 | **266자 (truncated)** | 나머지 최대 6,882자 잘림 |
| SPCX | — | 0건 | — | Rate limit |
| QNT | — | 0건 | — | Rate limit |
| PS | — | 0건 | — | Rate limit |
| MOBI | — | 0건 | — | Rate limit |
| BXDC | 2건 | 0건 | — | 30일 초과 기사 전부 제거 |
| ELOX | 6건 | 0건 | — | 30일 초과 기사 전부 제거 |
| BOBS | — | 0건 | — | Rate limit |

출처(CBRS 기준): Markets Insider, Times of India, Finbold, Benzinga, Barchart — TIER2 혼재

### 핵심 문제

- **본문 266자 고정 truncated** — 무료 플랜은 snippet만 제공. 나머지는 `[X chars]` 표시로 잘림
- **30일 이전 기사 접근 불가** — BXDC, ELOX 기사 모두 제거됨
- **12시간 delay** — 실시간 뉴스 접근 불가
- **Rate limit** — 첫 번째 티커(CBRS) 이후 대부분 0건

### 점수 요소별 평가

| 점수 요소 | 평점 | 판단 |
|---------|:----:|------|
| **① 감성 (25%)** | ★★☆☆☆ | 266자로 AI 감성 분석 불가 수준 |
| **② 최신성 (15%)** | ★★★☆☆ | datetime 있음. 12시간 delay + 30일 이전 접근 불가 |
| **③ 뉴스 양 (10%)** | ★★☆☆☆ | 총량은 있으나 Rate limit으로 1개 티커만 성공 |
| **④ 출처 신뢰도 (12%)** | ★★★☆☆ | TIER2 혼재 |
| **⑤ 신호 강도 (13%)** | ★★☆☆☆ | 266자로 keySignals/riskFactors 추출 불가 |
| **⑦ 일관성 (7%)** | ★★☆☆☆ | 본문 없으면 감성 분포 계산 의미 없음 |

---

## 3. Finnhub + Diffbot

### Finnhub 건수 현황

| 티커 | Finnhub 총건수 | summary 길이 | 주요 출처 |
|------|:------------:|:----------:|---------|
| CBRS | **91건** | 100~200자 | Seeking Alpha, Benzinga |
| SPCX | **249건** | 100~200자 | Seeking Alpha, Benzinga |
| QNT | 23건 | 100~150자 | Benzinga, Seeking Alpha |
| PS | 34건 | 90~150자 | Benzinga |
| GMRS | 21건 | 150~160자 | Seeking Alpha, Benzinga |
| MOBI | 11건 | 140~220자 | Seeking Alpha, Benzinga |
| BXDC | 28건 | 100~140자 | Seeking Alpha, Benzinga |
| ELOX | 4건 | 1~5자 | Benzinga |

### Diffbot 추출 결과 — 성공률 4/24건 (17%)

| 성공 케이스 | 본문 길이 | 비고 |
|-----------|:--------:|------|
| QNT Benzinga | 3,008자 | 공개 기사 |
| GMRS Benzinga | 365자 | 단신 수준 |
| MOBI Benzinga | 1,646자 | 공개 기사 |
| ELOX Benzinga | 1,891자 | 시황 roundup |

**실패 원인**: Seeking Alpha 전량 paywall, Benzinga 대부분 유료 기사

### 점수 요소별 평가

| 점수 요소 | 평점 | 판단 |
|---------|:----:|------|
| **① 감성 (25%)** | ★★☆☆☆ | 성공률 17%, 본문 365~3,008자 — 불안정 |
| **② 최신성 (15%)** | ★★★★☆ | datetime 정밀 |
| **③ 뉴스 양 (10%)** | ★★★★☆ | Finnhub 건수 풍부 (249건). 추출 실패로 실 활용 건수 적음 |
| **④ 출처 신뢰도 (12%)** | ★★★☆☆ | Seeking Alpha, Benzinga — TIER1이나 paywall로 접근 불가 |
| **⑤ 신호 강도 (13%)** | ★★☆☆☆ | 성공 기사도 대부분 단신 수준 |
| **⑦ 일관성 (7%)** | ★★☆☆☆ | 추출 성공률 낮아 일관성 측정 불안정 |

---

## 4. Marketaux

### 수집 현황

| 티커 | found | 반환 | 평균 description | 비고 |
|------|:-----:|:----:|:---------------:|------|
| CBRS | 0 | 0 | — | 미인덱싱 |
| SPCX | 1 | 1 | 22자 | theetfeducator.com (TIER3) |
| QNT | 0 | 0 | — | 미인덱싱 |
| PS | 1 | 1 | 68자 | **2021년, 전혀 다른 회사 매칭** |
| GMRS | 0 | 0 | — | 미인덱싱 |
| MOBI | 0 | 0 | — | 미인덱싱 |
| BXDC | 0 | 0 | — | 미인덱싱 |
| ELOX | 137 | 3 | 88자 | 2022년 기사, etfdailynews.com |

6/8 미인덱싱. 잡히는 기사도 TIER3 소스에 22~88자 description 수준. PS는 전혀 다른 회사 기사 매칭.

### 점수 요소별 평가

| 점수 요소 | 평점 | 판단 |
|---------|:----:|------|
| **① 감성 (25%)** | ★☆☆☆☆ | 22~88자로 AI 분석 불가 |
| **② 최신성 (15%)** | ★★☆☆☆ | datetime 있음. 2021~2022년 기사 혼입 |
| **③ 뉴스 양 (10%)** | ★☆☆☆☆ | 6/8 미인덱싱 |
| **④ 출처 신뢰도 (12%)** | ★☆☆☆☆ | TIER3 위주 |
| **⑤ 신호 강도 (13%)** | ★☆☆☆☆ | 추출 불가 수준 |
| **⑦ 일관성 (7%)** | ★☆☆☆☆ | 건수 부족으로 측정 불가 |

---

## 5. 4개 API 종합 비교

| 항목 | EODHD | GNews | Finnhub+Diffbot | Marketaux |
|------|:-----:|:-----:|:--------------:|:---------:|
| **본문 품질** | ★★★★★ 3K~7K자 | ★★☆☆☆ 266자 truncated | ★★☆☆☆ paywall 80% | ★☆☆☆☆ 22~88자 |
| **커버리지** | ★★★★☆ | ★★☆☆☆ | ★★★★★ Finnhub 풍부 | ★☆☆☆☆ 6/8 미인덱싱 |
| **출처 신뢰도** | ★★★★★ | ★★★☆☆ | ★★★☆☆ | ★☆☆☆☆ TIER3 |
| **노이즈** | 낮음 | 낮음 | 중간 | **높음 (엉뚱한 매칭)** |
| **Rate limit** | ★★☆☆☆ 무료 빡빡 | ★★☆☆☆ | ★★★★★ | ★★★☆☆ |
| **운영 복잡도** | 낮음 | 낮음 | **높음 (2-step)** | 낮음 |
| **비용** | $19.99/월 | 유료 전환 필요 | 무료 | 무료 |

---

## 6. 최종 결론

**EODHD 유료 플랜($19.99/월)을 메인 소스로 채택.**

- 본문 품질, 출처 신뢰도, sentiment 내장 모든 면에서 압도적
- API 호출 1번 = 1 call 차감 (기사 50건 반환해도 1 call) → 유료 플랜으로 47개 종목 일별 수집 충분
- 미인덱싱 종목(BOBS 등)은 **Finnhub Company News summary**로 보완 (건수 풍부, 무료)

**파이프라인 필수 필터 2가지**
1. `IPO 상장일 ±365일` 날짜 필터 — 이전 동명 회사 기사 노이즈 제거
2. `본문 500자 미만` 제외 — 단신/roundup 노이즈 제거
