package com.shinhan.eclipse.service.securities;

import com.shinhan.eclipse.domain.product.InvestmentProduct;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDetail(
        Long       id,
        String     ticker,
        String     productName,
        String     productType,
        String     exchangeName,
        String     currency,
        String     sector,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changeRate,
        String     sign,
        long       volume,
        Instant    updatedAt
) {
    public static ProductDetail of(InvestmentProduct p, QuoteSnapshot quote) {
        return new ProductDetail(
                p.getId(), p.getTicker(), p.getProductName(),
                p.getProductType(), p.getExchangeName(), p.getCurrency(), p.getSector(),
                quote != null ? quote.price()      : null,
                quote != null ? quote.change()     : null,
                quote != null ? quote.changeRate() : null,
                quote != null ? quote.sign()       : null,
                quote != null ? quote.volume()     : 0L,
                quote != null ? quote.updatedAt()  : null
        );
    }
}
