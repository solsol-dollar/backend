package com.shinhan.eclipse.worker.candle.repository;

import com.shinhan.eclipse.domain.product.PriceCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkerPriceCandleRepository extends JpaRepository<PriceCandle, Long> {

    @Query("SELECT c FROM PriceCandle c WHERE c.productId = :productId " +
           "AND c.candleType = :candleType AND c.candleAt = :candleAt")
    java.util.Optional<PriceCandle> findByProductIdAndCandleTypeAndCandleAt(
            @Param("productId") Long productId,
            @Param("candleType") String candleType,
            @Param("candleAt") LocalDate candleAt
    );

    /**
     * UPSERT — MySQL ON DUPLICATE KEY UPDATE 를 네이티브 쿼리로 실행.
     * UQ_candle(product_id, candle_type, candle_at) 기준 충돌 시 OHLCV 갱신.
     */
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO price_candles
                (product_id, candle_type, candle_at,
                 open_price, high_price, low_price, close_price,
                 volume, amount, sign,
                 created_at, updated_at)
            VALUES
                (:productId, :candleType, :candleAt,
                 :openPrice, :highPrice, :lowPrice, :closePrice,
                 :volume, :amount, :sign,
                 NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                open_price  = VALUES(open_price),
                high_price  = VALUES(high_price),
                low_price   = VALUES(low_price),
                close_price = VALUES(close_price),
                volume      = VALUES(volume),
                amount      = VALUES(amount),
                sign        = VALUES(sign),
                updated_at  = NOW()
            """)
    void upsert(
            @Param("productId")  Long productId,
            @Param("candleType") String candleType,
            @Param("candleAt")   LocalDate candleAt,
            @Param("openPrice")  java.math.BigDecimal openPrice,
            @Param("highPrice")  java.math.BigDecimal highPrice,
            @Param("lowPrice")   java.math.BigDecimal lowPrice,
            @Param("closePrice") java.math.BigDecimal closePrice,
            @Param("volume")     Long volume,
            @Param("amount")     java.math.BigDecimal amount,
            @Param("sign")       String sign
    );
}
