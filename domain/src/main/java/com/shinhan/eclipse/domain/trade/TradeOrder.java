package com.shinhan.eclipse.domain.trade;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "trade_orders")
public class TradeOrder extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 20)
    private String orderSide;

    @Column(nullable = false, length = 20)
    private String orderType = "MARKET";

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal requestedPrice;

    @Column(precision = 18, scale = 4)
    private BigDecimal executedPrice;

    @Column(precision = 18, scale = 4)
    private BigDecimal executedAmount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false, length = 30)
    private String orderStatus = "COMPLETED";

    @Column(nullable = false, length = 20)
    private String executionMode = "MOCK";

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime executedAt;

    @Column(length = 255)
    private String failureReason;

    public static TradeOrder mockFill(Long userId, Long productId, Long accountId,
                                      String orderSide, Integer quantity, BigDecimal requestedPrice) {
        TradeOrder o = new TradeOrder();
        o.userId = userId;
        o.productId = productId;
        o.accountId = accountId;
        o.orderSide = orderSide;
        o.quantity = quantity;
        o.requestedPrice = requestedPrice;
        o.executedPrice = requestedPrice;
        o.executedAmount = requestedPrice.multiply(BigDecimal.valueOf(quantity));
        o.orderedAt = LocalDateTime.now();
        o.executedAt = LocalDateTime.now();
        return o;
    }
}
