#!/usr/bin/env python3
"""
증권탭 price_candles 시드 데이터 생성기

출력: INSERT IGNORE SQL (stdout)
실행: python3 scripts/gen_price_candles.py > scripts/price_candles_seed.sql
적용: mysql -h 127.0.0.1 -P 3307 -u root -peclipse123 eclipse < scripts/price_candles_seed.sql
예상 행수: 20종목 × (약 260일봉 + 약 54주봉 + 약 13월봉) ≈ 6,700행
"""

import random
import sys
from datetime import date, timedelta
from itertools import groupby

SEED = 42
random.seed(SEED)

# 기준가(USD) — price_candles 생성 기준점
TICKERS = {
    'AAPL':  185.0,
    'NVDA':  950.0,
    'TSLA':  250.0,
    'MSFT':  420.0,
    'QQQ':   455.0,
    'GOOGL': 175.0,
    'META':  510.0,
    'AMZN':  190.0,
    'NFLX':  960.0,
    'AMD':   165.0,
    'INTC':   22.0,
    'JPM':   245.0,
    'V':     355.0,
    'JNJ':   155.0,
    'PLTR':   25.0,
    'IONQ':   38.0,
    'RIVN':   14.0,
    'SPY':   595.0,
    'SCHD':   29.0,
    'VTI':   290.0,
}

DAYS = 365


def is_weekday(d: date) -> bool:
    return d.weekday() < 5  # 월(0)~금(4)


def gen_day_candles(base_price: float) -> list[tuple]:
    """영업일 기준 DAY 캔들 생성. (date, open, high, low, close, volume, sign) 반환"""
    candles = []
    price = base_price
    today = date.today()
    for i in range(DAYS, -1, -1):
        d = today - timedelta(days=i)
        if not is_weekday(d):
            continue
        change_pct = random.uniform(-0.03, 0.03)
        open_p  = round(price * (1 + random.uniform(-0.01, 0.01)), 4)
        close_p = round(price * (1 + change_pct), 4)
        high_p  = round(max(open_p, close_p) * (1 + random.uniform(0, 0.005)), 4)
        low_p   = round(min(open_p, close_p) * (1 - random.uniform(0, 0.005)), 4)
        volume  = random.randint(500_000, 5_000_000)
        sign    = '2' if close_p > open_p else ('5' if close_p < open_p else '3')
        candles.append((d, open_p, high_p, low_p, close_p, volume, sign))
        price = close_p
    return candles


def gen_week_candles(day_candles: list[tuple]) -> list[tuple]:
    """DAY → 주봉 집계 (ISO 주차 기준, 마지막 거래일 날짜 사용)"""
    def iso_week(c):
        return c[0].isocalendar()[:2]

    result = []
    for _, group in groupby(day_candles, key=iso_week):
        week = list(group)
        open_p  = week[0][1]
        high_p  = max(c[2] for c in week)
        low_p   = min(c[3] for c in week)
        close_p = week[-1][4]
        volume  = sum(c[5] for c in week)
        sign    = '2' if close_p > open_p else ('5' if close_p < open_p else '3')
        result.append((week[-1][0], open_p, high_p, low_p, close_p, volume, sign))
    return result


def gen_month_candles(day_candles: list[tuple]) -> list[tuple]:
    """DAY → 월봉 집계 (월 마지막 거래일 날짜 사용)"""
    def ym(c):
        return (c[0].year, c[0].month)

    result = []
    for _, group in groupby(day_candles, key=ym):
        month = list(group)
        open_p  = month[0][1]
        high_p  = max(c[2] for c in month)
        low_p   = min(c[3] for c in month)
        close_p = month[-1][4]
        volume  = sum(c[5] for c in month)
        sign    = '2' if close_p > open_p else ('5' if close_p < open_p else '3')
        result.append((month[-1][0], open_p, high_p, low_p, close_p, volume, sign))
    return result


def emit_sql(ticker: str, candle_type: str, candles: list[tuple]) -> None:
    for (d, o, h, l, c, v, s) in candles:
        print(
            f"INSERT IGNORE INTO price_candles "
            f"(product_id, candle_type, candle_at, open_price, high_price, low_price, close_price, volume, sign, created_at, updated_at, status) "
            f"SELECT id, '{candle_type}', '{d}', {o}, {h}, {l}, {c}, {v}, '{s}', @now, @now, 'ACTIVE' "
            f"FROM investment_products WHERE ticker = '{ticker}' LIMIT 1;"
        )


def main() -> None:
    print("-- =================================================================")
    print(f"-- price_candles 시드 데이터 ({DAYS}일, 20종목, DAY/WEEK/MONTH)")
    print(f"-- 생성 기준일: {date.today()}")
    print("-- 적용: mysql -h 127.0.0.1 -P 3307 -u root -peclipse123 eclipse < this_file.sql")
    print("-- =================================================================")
    print()
    print("SET @now = NOW();")
    print()

    total = 0
    for ticker, base in TICKERS.items():
        day_candles   = gen_day_candles(base)
        week_candles  = gen_week_candles(day_candles)
        month_candles = gen_month_candles(day_candles)

        print(f"-- {ticker} (기준가 {base}): DAY {len(day_candles)}건 / WEEK {len(week_candles)}건 / MONTH {len(month_candles)}건")
        emit_sql(ticker, 'DAY',   day_candles)
        emit_sql(ticker, 'WEEK',  week_candles)
        emit_sql(ticker, 'MONTH', month_candles)
        print()

        total += len(day_candles) + len(week_candles) + len(month_candles)

    print(f"-- 총 INSERT 예상: {total}건")
    print("SELECT 'price_candles' AS tbl, COUNT(*) AS cnt FROM price_candles;")


if __name__ == '__main__':
    main()
