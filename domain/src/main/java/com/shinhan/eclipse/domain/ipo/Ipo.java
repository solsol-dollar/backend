package com.shinhan.eclipse.domain.ipo;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ipos")
public class Ipo extends BaseEntity {

    private Long productId;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(length = 50)
    private String exchangeName;

    @Column(length = 100)
    private String sector;

    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private LocalDate listingDate;
    private LocalDate refundDate;
    private LocalDate depositDate;

    @Column(precision = 18, scale = 4)
    private BigDecimal offerPriceMin;

    @Column(precision = 18, scale = 4)
    private BigDecimal offerPriceMax;

    @Column(precision = 18, scale = 4)
    private BigDecimal confirmedOfferPrice;

    @Column(precision = 18, scale = 4)
    private BigDecimal minimumSubscriptionAmount;

    @Column(nullable = false, length = 30)
    private String ipoStatus = "UPCOMING";
}
