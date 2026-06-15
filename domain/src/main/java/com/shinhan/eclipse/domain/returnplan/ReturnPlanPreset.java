package com.shinhan.eclipse.domain.returnplan;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "return_plan_presets")
public class ReturnPlanPreset extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String presetCode;

    @Column(nullable = false, length = 50)
    private String presetName;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal securitiesRatio;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal savingsRatio;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal accountRatio;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer displayOrder = 0;
}
