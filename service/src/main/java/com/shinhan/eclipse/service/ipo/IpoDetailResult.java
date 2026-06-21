package com.shinhan.eclipse.service.ipo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IpoDetailResult(
        Long id,
        String ticker,
        String companyName,
        String exchangeName,
        String sector,
        String ipoStatus,
        LocalDate subscriptionStartDate,
        LocalDate subscriptionEndDate,
        LocalDate listingDate,
        LocalDate refundDate,
        LocalDate depositDate,
        BigDecimal offerPriceMin,
        BigDecimal offerPriceMax,
        BigDecimal confirmedOfferPrice,
        BigDecimal minimumSubscriptionAmount,
        boolean isFavorite
) {}
