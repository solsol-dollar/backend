package com.shinhan.eclipse.service.exchange;

import com.shinhan.eclipse.common.exchange.ExchangeRateInfo;

public interface ExchangeService {

    /**
     * Redis 캐시의 최신 환율을 반환한다. 캐시 miss 시 API를 호출하고,
     * API도 실패하면 BusinessException(EXCHANGE_RATE_UNAVAILABLE)을 던진다.
     */
    ExchangeRateInfo getExchangeRate(String currencyCode);
}
