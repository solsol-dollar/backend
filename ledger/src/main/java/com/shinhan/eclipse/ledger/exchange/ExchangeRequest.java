package com.shinhan.eclipse.ledger.exchange;

import java.math.BigDecimal;

public record ExchangeRequest(
        String     direction,   // "KRW_TO_USD" or "USD_TO_KRW"
        BigDecimal sourceAmount
) {}
