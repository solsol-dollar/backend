package com.shinhan.eclipse.service.securities;

import java.math.BigDecimal;
import java.util.List;

public record OrderBookResponse(
        String       ticker,
        BigDecimal   price,
        List<Level>  askLevels,
        List<Level>  bidLevels
) {
    public record Level(BigDecimal price, long volume) {}
}
