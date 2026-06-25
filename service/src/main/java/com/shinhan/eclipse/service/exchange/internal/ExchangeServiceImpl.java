package com.shinhan.eclipse.service.exchange.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
class ExchangeServiceImpl implements ExchangeService {

    private final ExchangeRateApiClient apiClient;
    private final ExchangeRateCache     rateCache;

    @Override
    public ExchangeRateInfo getExchangeRate(String currencyCode) {
        return rateCache.get(currencyCode).orElseGet(() -> fetchAndCache(currencyCode));
    }

    @Override
    public Optional<ExchangeRateInfo> getPreviousExchangeRate(String currencyCode) {
        return rateCache.getPrev(currencyCode);
    }

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private ExchangeRateInfo fetchAndCache(String currencyCode) {
        try {
            ExchangeRateInfo rate = apiClient.fetchOne(currencyCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE,
                            "지원하지 않는 통화: " + currencyCode));
            rateCache.put(rate);
            CompletableFuture.runAsync(() -> cachePrevIfMissing(currencyCode));
            return rate;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("환율 API 호출 실패, 캐시 폴백 시도: {}", e.getMessage());
            return rateCache.get(currencyCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE));
        }
    }

    private void cachePrevIfMissing(String currencyCode) {
        if (rateCache.getPrev(currencyCode).isPresent()) return;
        LocalDate date = LocalDate.now(KST).minusDays(1);
        for (int i = 0; i < 7; i++, date = date.minusDays(1)) {
            Optional<ExchangeRateInfo> prev = apiClient.fetchOne(currencyCode, date);
            if (prev.isPresent()) {
                rateCache.putPrev(prev.get());
                log.info("[환율] 전날 캐시 복구: {}={} ({})", currencyCode, prev.get().baseRate(), date);
                return;
            }
        }
        log.warn("[환율] 전날 환율 조회 실패: {}", currencyCode);
    }
}