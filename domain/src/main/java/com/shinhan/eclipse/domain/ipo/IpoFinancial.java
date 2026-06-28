package com.shinhan.eclipse.domain.ipo;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ipo_financials")
public class IpoFinancial extends BaseEntity {

    @Column(nullable = false)
    private Long ipoId;

    @Column(nullable = false)
    private Integer fiscalYear;

    private Long revenue;
    private Long operatingIncome;
    private Long netIncome;

    @Column(nullable = false, length = 3)
    private String currency;

    public static IpoFinancial create(Long ipoId, int fiscalYear,
                                      Long revenue, Long operatingIncome, Long netIncome,
                                      String currency) {
        IpoFinancial f = new IpoFinancial();
        f.ipoId = ipoId;
        f.fiscalYear = fiscalYear;
        f.revenue = revenue;
        f.operatingIncome = operatingIncome;
        f.netIncome = netIncome;
        f.currency = currency;
        return f;
    }
}
