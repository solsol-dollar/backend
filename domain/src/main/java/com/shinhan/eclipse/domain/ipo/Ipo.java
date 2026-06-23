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

    public static Ipo create(
            String ticker,
            String companyName,
            String exchangeName,
            String sector,
            LocalDate subscriptionStartDate,
            LocalDate subscriptionEndDate,
            LocalDate listingDate,
            LocalDate refundDate,
            LocalDate depositDate,
            BigDecimal offerPriceMin,
            BigDecimal offerPriceMax,
            BigDecimal confirmedOfferPrice,
            BigDecimal minimumSubscriptionAmount,
            String ipoStatus
    ) {
        Ipo ipo = new Ipo();
        ipo.ticker = ticker;
        ipo.companyName = companyName;
        ipo.exchangeName = exchangeName;
        ipo.sector = sector;
        ipo.subscriptionStartDate = subscriptionStartDate;
        ipo.subscriptionEndDate = subscriptionEndDate;
        ipo.listingDate = listingDate;
        ipo.refundDate = refundDate;
        ipo.depositDate = depositDate;
        ipo.offerPriceMin = offerPriceMin;
        ipo.offerPriceMax = offerPriceMax;
        ipo.confirmedOfferPrice = confirmedOfferPrice;
        ipo.minimumSubscriptionAmount = minimumSubscriptionAmount;
        ipo.ipoStatus = ipoStatus;
        return ipo;
    }
}
