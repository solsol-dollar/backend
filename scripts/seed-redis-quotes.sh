#!/usr/bin/env bash
# =====================================================================
# Eclipse 로컬 테스트용 Redis 시세 픽스처
# 사용법: bash scripts/seed-redis-quotes.sh [redis-cli 옵션]
#   예)  bash scripts/seed-redis-quotes.sh
#         → localhost:6379 기본 접속
#   예)  bash scripts/seed-redis-quotes.sh -p 6380
#
# 장중이 아닐 때 WebSocket 시세가 들어오지 않아도 Redis 에 가짜 시세를
# 넣어두면 SEC-001·002·004 enrichment 경로를 테스트할 수 있다.
# TTL=3600s (1시간) — 실제 장중 시세가 들어오면 60s TTL 로 덮어씌워진다.
# =====================================================================

REDIS_CLI="redis-cli $*"
NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
TTL=3600

seed() {
  local TICKER="$1"
  local PRICE="$2"
  local CHANGE="$3"
  local CHANGE_RATE="$4"
  local VOLUME="$5"
  local SIGN="$6"   # 2=상승, 5=하락, 3=보합

  local JSON=$(cat <<EOF
{"ticker":"${TICKER}","price":${PRICE},"change":${CHANGE},"changeRate":${CHANGE_RATE},"volume":${VOLUME},"sign":"${SIGN}","updatedAt":"${NOW}"}
EOF
)
  $REDIS_CLI SET "quote:${TICKER}" "${JSON}" EX ${TTL}
  echo "  SET quote:${TICKER} -> \$${PRICE} (${CHANGE_RATE}%) TTL=${TTL}s"
}

echo "=== Eclipse Redis 시세 픽스처 시드 ==="
echo "대상: ${REDIS_CLI}"
echo ""

# Technology
seed  AAPL   195.50   2.30   1.19  58432100  2
seed  MSFT   415.80   3.60   0.87  22156700  2
seed  NVDA   875.20  -8.50  -0.96  41328900  5
seed  META   512.30   6.10   1.20  18745200  2
seed  GOOGL  178.40   1.20   0.68   9823400  2
seed  ADBE   535.70  -2.40  -0.45   3214800  5
seed  CSCO    54.30   0.40   0.74   8921300  2
seed  INTC    31.20  -0.80  -2.50  21543700  5

# Consumer Discretionary
seed  AMZN   192.30   2.10   1.10  31245600  2
seed  TSLA   245.80  -4.30  -1.72  89342100  5
seed  SBUX    78.50  -0.60  -0.76   9123400  5
seed  ABNB   148.70   1.90   1.30   4532100  2

# Communication Services
seed  NFLX   682.40   8.70   1.29  13256800  2
seed  TMUS   176.20   1.10   0.63   6432100  2

# Healthcare
seed  AMGN   285.60   2.20   0.78   3214500  2
seed  GILD    89.30  -0.50  -0.56   7832100  5
seed  ISRG   428.90   4.30   1.01   1923400  2

# Consumer Staples
seed  PEP   170.40  -0.80  -0.47   8234500  5
seed  COST  869.50   5.60   0.65   2134600  2

# ETF
seed  QQQ   468.30   3.80   0.82  38234500  2
seed  SOXX  222.10  -1.90  -0.85   8923400  5
seed  SPY   548.70   2.40   0.44  61234500  2
seed  ARKK   53.20   0.90   1.72   9234500  2
seed  VOO   503.80   2.20   0.44  12345600  2

echo ""
echo "=== 완료: $(redis-cli $* KEYS 'quote:*' | wc -l | tr -d ' ')개 키 저장됨 ==="
echo ""
echo "확인: ${REDIS_CLI} GET quote:AAPL"
