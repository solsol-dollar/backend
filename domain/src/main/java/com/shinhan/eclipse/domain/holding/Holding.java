package com.shinhan.eclipse.domain.holding;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
