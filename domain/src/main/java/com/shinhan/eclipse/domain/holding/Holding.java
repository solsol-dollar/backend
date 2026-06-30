package com.shinhan.eclipse.domain.holding;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "holdings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Holding extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal averagePrice;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    public static Holding create(Long userId, Long productId, Integer quantity, BigDecimal price) {
        Holding h = new Holding();
        h.userId = userId;
        h.productId = productId;
        h.totalQuantity = quantity;
        h.averagePrice = price;
        return h;
    }

    public void addBuy(Integer quantity, BigDecimal price) {
        BigDecimal totalCost = this.averagePrice.multiply(BigDecimal.valueOf(this.totalQuantity))
                .add(price.multiply(BigDecimal.valueOf(quantity)));
        this.totalQuantity += quantity;
        this.averagePrice = totalCost.divide(BigDecimal.valueOf(this.totalQuantity), 4, RoundingMode.HALF_UP);
        if ("CLOSED".equals(getStatus())) {
            activate();
        }
    }

    public void addSell(Integer quantity) {
        if (quantity > this.totalQuantity) throw new IllegalArgumentException("보유 수량 부족");
        this.totalQuantity -= quantity;
        if (this.totalQuantity == 0) {
            deactivate();
        }
    }
}
