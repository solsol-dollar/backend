package com.shinhan.eclipse.domain.product;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "investment_products")
public class InvestmentProduct extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String productType;

    @Column(length = 20)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(length = 50)
    private String exchangeName;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(length = 100)
    private String sector;

    public static InvestmentProduct ofSeed(String productType, String ticker,
                                           String productName, String exchangeName,
                                           String currency, String sector) {
        InvestmentProduct p = new InvestmentProduct();
        p.productType = productType;
        p.ticker = ticker;
        p.productName = productName;
        p.exchangeName = exchangeName;
        p.currency = currency;
        p.sector = sector;
        return p;
    }

    public void enrichFromApi(String productName, String sector) {
        if (productName != null && !productName.isBlank()) this.productName = productName;
        if (sector != null && !sector.isBlank()) this.sector = sector;
    }
}
