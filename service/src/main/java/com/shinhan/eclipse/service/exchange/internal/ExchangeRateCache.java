package com.shinhan.eclipse.service.exchange.internal;

import com.shinhan.eclipse.common.exchange.ExchangeRateInfo;

import java.util.Optional;

interface ExchangeRateCache {
    void put(ExchangeRateInfo info);
    Optional<ExchangeRateInfo> get(String currencyCode);
}