package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.product.PriceCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface PriceCandleRepository extends JpaRepository<PriceCandle, Long> {

    List<PriceCandle> findByProductIdAndCandleTypeAndCandleAtBetween(
            Long productId,
            String candleType,
            LocalDate from,
            LocalDate to
    );

    Optional<PriceCandle> findByProductIdAndCandleTypeAndCandleAt(
            Long productId,
            String candleType,
            LocalDate candleAt
    );

    /** 스파크라인용: productId별 최근 N일 종가 (product_id, close_price) 쌍 반환 */
    @Query(value = """
            SELECT product_id, close_price
            FROM price_candles
            WHERE candle_type = 'DAY'
              AND product_id IN :ids
              AND candle_at >= :from
            ORDER BY product_id ASC, candle_at ASC
            """, nativeQuery = true)
    List<Object[]> findDailyClosePricesForSpark(@Param("ids") List<Long> ids, @Param("from") LocalDate from);

    @Query(value = """
            SELECT MAX(high_price) FROM price_candles
            WHERE product_id = :id AND candle_type = 'DAY' AND candle_at >= :from
            """, nativeQuery = true)
    Optional<BigDecimal> findWeek52High(@Param("id") Long id, @Param("from") LocalDate from);

    @Query(value = """
            SELECT MIN(low_price) FROM price_candles
            WHERE product_id = :id AND candle_type = 'DAY' AND candle_at >= :from
            """, nativeQuery = true)
    Optional<BigDecimal> findWeek52Low(@Param("id") Long id, @Param("from") LocalDate from);

    Optional<PriceCandle> findFirstByProductIdAndCandleTypeAndCandleAtGreaterThanEqualOrderByCandleAtAsc(
            Long productId, String candleType, LocalDate from);

    Optional<PriceCandle> findFirstByProductIdAndCandleTypeOrderByCandleAtDesc(
            Long productId, String candleType);

    /** productId별 가장 최근 DAY 캔들 1건씩 벌크 조회 (GROUP BY + JOIN으로 DEPENDENT SUBQUERY 제거) */
    @Query(value = """
            SELECT pc.* FROM price_candles pc
            INNER JOIN (
                SELECT product_id, MAX(candle_at) AS max_at
                FROM price_candles
                WHERE candle_type = 'DAY' AND product_id IN :ids
                GROUP BY product_id
            ) latest ON pc.product_id = latest.product_id
                     AND pc.candle_at = latest.max_at
                     AND pc.candle_type = 'DAY'
            """, nativeQuery = true)
    List<PriceCandle> findLatestDailyByProductIds(@Param("ids") List<Long> ids);

    /** productId별 최근 DAY 캔들 2건씩 벌크 조회 — 전일 대비 변동률 계산용 */
    @Query(value = """
            SELECT pc.* FROM price_candles pc
            INNER JOIN (
                SELECT product_id, candle_at
                FROM (
                    SELECT product_id, candle_at,
                           ROW_NUMBER() OVER (PARTITION BY product_id ORDER BY candle_at DESC) AS rn
                    FROM price_candles
                    WHERE candle_type = 'DAY' AND product_id IN :ids
                ) ranked
                WHERE rn <= 2
            ) top2 ON pc.product_id = top2.product_id
                   AND pc.candle_at = top2.candle_at
                   AND pc.candle_type = 'DAY'
            """, nativeQuery = true)
    List<PriceCandle> findTop2DailyByProductIds(@Param("ids") List<Long> ids);
}
