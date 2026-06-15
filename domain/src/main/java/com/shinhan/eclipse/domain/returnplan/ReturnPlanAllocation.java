package com.shinhan.eclipse.domain.returnplan;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "return_plan_allocations")
public class ReturnPlanAllocation extends BaseEntity {

    @Column(nullable = false)
    private Long returnPlanId;

    @Column(nullable = false, length = 30)
    private String destinationType;

    private Long destinationAccountId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationRatio;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal allocationAmount;

    @Column(nullable = false, length = 30)
    private String allocationStatus = "PENDING";
}
