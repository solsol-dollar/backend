package com.shinhan.eclipse.service.exchange.internal;

import com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * 수출입은행 환율은 영업일 오전에 하루 한 번 고시된다.
 * 평일 10:00에 1회 갱신하고, 공휴일은 스킵한다 (주말은 cron 표현식으로 제외).
 * 한국 공휴일 목록은 연도별로 업데이트 필요 (현재 2025·2026 하드코딩).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ExchangeRateScheduler {

    private final ExchangeRateApiClient apiClient;
    private final ExchangeRateCache     rateCache;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // 수출입은행 API cur_unit 코드 → 정규화 키 순서
    private static final List<String> API_CURRENCY_CODES    = List.of("USD", "JPY(100)", "BRL");
    private static final List<String> NORMALIZED_CURRENCIES = List.of("USD", "JPY",       "BRL");

    /** 앱 시작 시 즉시 캐시 초기화 (오늘 + 전날 환율) */
    @Scheduled(initialDelay = 0, fixedDelay = Long.MAX_VALUE)
    void initCacheOnStartup() {
        log.info("[환율 갱신] 앱 시작 초기화");
        LocalDate today = LocalDate.now(KST);
        LocalDate prevDay = prevBusinessDay(today);
        try {
            apiClient.fetchAll(today).ifPresentOrElse(
                    rates -> { storeRates(rates, false); log.info("[환율 갱신] 오늘 초기화 완료"); },
                    () -> log.warn("[환율 갱신] 오늘 환율 API 응답 없음")
            );
        } catch (Exception e) {
            log.error("[환율 갱신] 오늘 환율 초기화 실패: {}", e.getMessage());
        }
        try {
            apiClient.fetchAll(prevDay).ifPresentOrElse(
                    rates -> { storeRates(rates, true); log.info("[환율 갱신] 전날 초기화 완료 ({})", prevDay); },
                    () -> log.warn("[환율 갱신] 전날 환율 API 응답 없음 ({})", prevDay)
            );
        } catch (Exception e) {
            log.error("[환율 갱신] 전날 환율 초기화 실패: {}", e.getMessage());
        }
    }

    private void storeRates(List<ExchangeRateInfo> rates, boolean prev) {
        for (String apiCode : API_CURRENCY_CODES) {
            rates.stream()
                    .filter(r -> apiCode.equalsIgnoreCase(r.currencyCode()))
                    .findFirst()
                    .map(this::normalize)
                    .ifPresent(r -> { if (prev) rateCache.putPrev(r); else rateCache.put(r); });
        }
    }

    /** JPY(100) → JPY: 100엔 기준 환율을 1엔 기준으로 정규화 */
    private ExchangeRateInfo normalize(ExchangeRateInfo r) {
        if (r.currencyCode().toUpperCase().startsWith("JPY")) {
            BigDecimal hundred = BigDecimal.valueOf(100);
            return new ExchangeRateInfo(
                    "JPY", r.currencyName(),
                    r.baseRate().divide(hundred, 6, RoundingMode.HALF_UP),
                    r.buyingRate().divide(hundred, 6, RoundingMode.HALF_UP),
                    r.sellingRate().divide(hundred, 6, RoundingMode.HALF_UP),
                    r.fetchedAt()
            );
        }
        return r;
    }

    private LocalDate prevBusinessDay(LocalDate date) {
        LocalDate prev = date.minusDays(1);
        while (prev.getDayOfWeek() == DayOfWeek.SATURDAY
                || prev.getDayOfWeek() == DayOfWeek.SUNDAY
                || isKoreanHolidayDate(prev)) {
            prev = prev.minusDays(1);
        }
        return prev;
    }

    /** 평일 오전 11시 10분 1회 갱신 (KST 기준) */
    @Scheduled(cron = "0 10 11 * * MON-FRI", zone = "Asia/Seoul")
    void refreshDaily() {
        if (isKoreanHoliday()) {
            log.info("[환율 갱신] 공휴일 — 스킵");
            return;
        }
        log.info("[환율 갱신] 시작");
        try {
            apiClient.fetchAll().ifPresentOrElse(
                    rates -> {
                        NORMALIZED_CURRENCIES.forEach(code -> rateCache.get(code).ifPresent(rateCache::putPrev));
                        storeRates(rates, false);
                        log.info("[환율 갱신] 완료");
                    },
                    () -> log.warn("[환율 갱신] API 응답 없음 — 기존 캐시 유지")
            );
        } catch (Exception e) {
            log.error("[환율 갱신] 실패: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 공휴일 판별 (2025·2026 한국 법정공휴일 — 음력 기반 날짜는 고정 변환값 사용)
    // -----------------------------------------------------------------------

    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1,  1),   // 신정
            MonthDay.of(3,  1),   // 삼일절
            MonthDay.of(5,  5),   // 어린이날
            MonthDay.of(6,  6),   // 현충일
            MonthDay.of(8, 15),   // 광복절
            MonthDay.of(10, 3),   // 개천절
            MonthDay.of(10, 9),   // 한글날
            MonthDay.of(12, 25)   // 성탄절
    );

    private static final Set<LocalDate> LUNAR_HOLIDAYS = Set.of(
            // 설날 연휴 2025 (1/28~1/30)
            LocalDate.of(2025, 1, 28), LocalDate.of(2025, 1, 29), LocalDate.of(2025, 1, 30),
            // 부처님 오신 날 2025 (5/6 — 어린이날과 겹쳐 대체휴일)
            LocalDate.of(2025, 5,  6),
            // 추석 연휴 2025 (10/5~10/7)
            LocalDate.of(2025, 10, 5), LocalDate.of(2025, 10, 6), LocalDate.of(2025, 10, 7),
            // 설날 연휴 2026 (2/16~2/18)
            LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18),
            // 부처님 오신 날 2026 (5/24)
            LocalDate.of(2026, 5, 24),
            // 추석 연휴 2026 (9/24~9/26)
            LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26)
    );

    private boolean isKoreanHoliday() {
        return isKoreanHolidayDate(LocalDate.now(KST));
    }

    private boolean isKoreanHolidayDate(LocalDate date) {
        return FIXED_HOLIDAYS.contains(MonthDay.from(date))
                || LUNAR_HOLIDAYS.contains(date);
    }
}