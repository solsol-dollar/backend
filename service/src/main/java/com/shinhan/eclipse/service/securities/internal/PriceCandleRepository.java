package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.product.PriceCandle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

interface PriceCandleRepository extends JpaRepository<PriceCandle, Long> {

    List<PriceCandle> findByProductIdAndCandleTypeAndCandleAtBetween(
            Long productId,
            String candleType,
            LocalDate from,
            LocalDate to
    );
}
