package com.shinhan.eclipse.service.ipo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IpoItem(
        Long id,
        String ticker,
        String companyName,
        String ipoStatus,
        LocalDate subscriptionStartDate,
        LocalDate subscriptionEndDate,
        LocalDate listingDate,
        BigDecimal offerPriceMin,
        BigDecimal offerPriceMax,
        BigDecimal confirmedOfferPrice,
        boolean isFavorite
) {}
