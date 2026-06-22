package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteSnapshot(
        String     ticker,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        long       volume,
        String     sign,
        Instant    updatedAt
) {
    public static QuoteSnapshot from(QuoteReceivedEvent e) {
        return new QuoteSnapshot(
                e.ticker(), e.price(), e.change(), e.changeRate(),
                e.volume(), e.sign(), e.receivedAt()
        );
    }
}
