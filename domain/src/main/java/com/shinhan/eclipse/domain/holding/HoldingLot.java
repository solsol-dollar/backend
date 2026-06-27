package com.shinhan.eclipse.domain.holding;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "holding_lots")
public class HoldingLot extends BaseEntity {

    @Column(nullable = false)
    private Long holdingId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    // 폴리모픽 참조 (IPO_ALLOCATION / TRADE_ORDER) — FK 없음
    @Column(nullable = false, length = 30)
    private String sourceType;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal acquisitionPrice;

    @Column(nullable = false)
    private LocalDateTime acquiredAt;

    public static HoldingLot ofBuy(Long holdingId, Long userId, Long productId,
                                    Long tradeOrderId, Integer quantity, BigDecimal price) {
        HoldingLot lot = new HoldingLot();
        lot.holdingId = holdingId;
        lot.userId = userId;
        lot.productId = productId;
        lot.sourceType = "TRADE_ORDER";
        lot.sourceId = tradeOrderId;
        lot.quantity = quantity;
        lot.remainingQuantity = quantity;
        lot.acquisitionPrice = price;
        lot.acquiredAt = LocalDateTime.now();
        return lot;
    }

    public static HoldingLot ofIpoAllocation(Long holdingId, Long userId, Long productId,
                                              Long subscriptionId, Integer quantity, BigDecimal price) {
        HoldingLot lot = new HoldingLot();
        lot.holdingId = holdingId;
        lot.userId = userId;
        lot.productId = productId;
        lot.sourceType = "IPO_ALLOCATION";
        lot.sourceId = subscriptionId;
        lot.quantity = quantity;
        lot.remainingQuantity = quantity;
        lot.acquisitionPrice = price;
        lot.acquiredAt = LocalDateTime.now();
        return lot;
    }
}
