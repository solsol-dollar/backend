package com.shinhan.eclipse.service.securities;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import com.shinhan.eclipse.domain.product.PriceCandle;

import java.math.BigDecimal;
import java.util.List;

public record ProductListItem(
        Long               id,
        String             ticker,
        String             productName,
        String             productType,
        String             exchangeName,
        String             currency,
        String             sector,
        BigDecimal         price,
        BigDecimal         changeRate,
        String             sign,
        Long               volume,
        BigDecimal         tradeAmount,
        List<BigDecimal>   sparkPrices
) {
    public static ProductListItem of(InvestmentProduct p, QuoteSnapshot quote) {
        return of(p, quote, null, List.of());
    }

    public static ProductListItem of(InvestmentProduct p, QuoteSnapshot quote, PriceCandle candle) {
        return of(p, quote, candle, List.of());
    }

    public static ProductListItem of(InvestmentProduct p, QuoteSnapshot quote, PriceCandle candle, List<BigDecimal> sparkPrices) {
        // 가격/등락: 실시간 > 캔들 종가
        BigDecimal price      = quote != null ? quote.price()      : (candle != null ? candle.getClosePrice() : null);
        BigDecimal changeRate = quote != null ? quote.changeRate() : null;
        String     sign       = quote != null ? quote.sign()       : (candle != null ? candle.getSign()       : null);

        // 거래량: 실시간(> 0) > 캔들 — Long.valueOf()로 명시 boxing해서 타입 promote 방지
        long rawVol = (quote != null) ? quote.volume() : 0L;
        Long vol = rawVol > 0L ? Long.valueOf(rawVol) : (candle != null ? candle.getVolume() : null);

        // 거래대금: 실시간(price × vol) > 캔들 amount
        BigDecimal tradeAmount = null;
        if (price != null && vol != null && vol > 0) {
            tradeAmount = price.multiply(BigDecimal.valueOf(vol));
        } else if (candle != null) {
            tradeAmount = candle.getAmount();
        }

        return new ProductListItem(
                p.getId(), p.getTicker(), p.getProductName(),
                p.getProductType(), p.getExchangeName(), p.getCurrency(), p.getSector(),
                price, changeRate, sign, vol, tradeAmount,
                sparkPrices != null ? sparkPrices : List.of()
        );
    }
}
