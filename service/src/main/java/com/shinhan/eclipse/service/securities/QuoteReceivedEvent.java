package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteReceivedEvent(
        String ticker,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        long      volume,
        String    sign,
        Instant   receivedAt
) {
    public static QuoteReceivedEvent of(String ticker, BigDecimal price,
                                        BigDecimal change, BigDecimal changeRate,
                                        long volume, String sign) {
        return new QuoteReceivedEvent(ticker, price, change, changeRate,
                volume, sign, Instant.now());
    }
}
