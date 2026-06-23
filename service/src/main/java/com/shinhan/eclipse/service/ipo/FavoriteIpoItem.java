package com.shinhan.eclipse.service.ipo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FavoriteIpoItem(
        Long id,
        Long ipoId,
        String ticker,
        String companyName,
        String logoUrl,
        String ipoStatus,
        LocalDate subscriptionStartDate,
        LocalDate subscriptionEndDate,
        BigDecimal confirmedOfferPrice
) {}
