package com.shinhan.eclipse.domain.product;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주가 캔들 데이터 엔티티.
 * candle_type: DAY / WEEK / MONTH / YEAR
 * candle_at: 캔들 기준일 (주봉=해당주 마지막 거래일, 월봉=말일)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "price_candles",
    uniqueConstraints = @UniqueConstraint(name = "UQ_candle", columnNames = {"product_id", "candle_type", "candle_at"})
)
public class PriceCandle extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** DAY / WEEK / MONTH / YEAR */
    @Column(name = "candle_type", nullable = false, length = 10)
    private String candleType;

    @Column(name = "candle_at", nullable = false)
    private LocalDate candleAt;

    @Column(name = "open_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal closePrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "amount", precision = 24, scale = 4)
    private BigDecimal amount;

    /** 등락구분: 2=상승, 3=보합, 5=하락 */
    @Column(name = "sign", length = 1)
    private String sign;

    public static PriceCandle of(Long productId, String candleType, LocalDate candleAt,
                                 BigDecimal openPrice, BigDecimal highPrice,
                                 BigDecimal lowPrice, BigDecimal closePrice,
                                 Long volume, BigDecimal amount, String sign) {
        PriceCandle c = new PriceCandle();
        c.productId  = productId;
        c.candleType = candleType;
        c.candleAt   = candleAt;
        c.openPrice  = openPrice;
        c.highPrice  = highPrice;
        c.lowPrice   = lowPrice;
        c.closePrice = closePrice;
        c.volume     = volume;
        c.amount     = amount;
        c.sign       = sign;
        return c;
    }
}
