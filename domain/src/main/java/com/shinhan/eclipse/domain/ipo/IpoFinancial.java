package com.shinhan.eclipse.domain.ipo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ipo_financials")
public class IpoFinancial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ipoId;

    @Column(nullable = false)
    private Integer fiscalYear;

    private Long revenue;
    private Long operatingIncome;
    private Long netIncome;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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
