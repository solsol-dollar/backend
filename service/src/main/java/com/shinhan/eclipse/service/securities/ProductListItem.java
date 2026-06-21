package com.shinhan.eclipse.service.securities;

import com.shinhan.eclipse.domain.product.InvestmentProduct;

import java.math.BigDecimal;

public record ProductListItem(
        Long       id,
        String     ticker,
        String     productName,
        String     productType,
        String     exchangeName,
        String     currency,
        String     sector,
        BigDecimal price,
        BigDecimal changeRate,
        String     sign
) {
    public static ProductListItem of(InvestmentProduct p, QuoteSnapshot quote) {
        return new ProductListItem(
                p.getId(), p.getTicker(), p.getProductName(),
                p.getProductType(), p.getExchangeName(), p.getCurrency(), p.getSector(),
                quote != null ? quote.price()      : null,
                quote != null ? quote.changeRate() : null,
                quote != null ? quote.sign()       : null
        );
    }
}
