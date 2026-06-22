package com.shinhan.eclipse.service.exchange.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.service.exchange.ExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    private ExchangeRateInfo fetchAndCache(String currencyCode) {
        try {
            ExchangeRateInfo rate = apiClient.fetchOne(currencyCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE,
                            "지원하지 않는 통화: " + currencyCode));
            rateCache.put(rate);
            return rate;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("환율 API 호출 실패, 캐시 폴백 시도: {}", e.getMessage());
            return rateCache.get(currencyCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE));
        }
    }
}