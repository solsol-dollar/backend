package com.shinhan.eclipse.ledger.returnplan.dto;

import com.shinhan.eclipse.domain.returnplan.ReturnPlanPreset;

import java.math.BigDecimal;

public record ReturnPlanPresetRes(
        String presetCode,
        String presetName,
        BigDecimal securitiesRatio,
        BigDecimal savingsRatio,
        BigDecimal accountRatio,
        String description
) {
    public static ReturnPlanPresetRes from(ReturnPlanPreset preset) {
        return new ReturnPlanPresetRes(
                preset.getPresetCode(),
                preset.getPresetName(),
                preset.getSecuritiesRatio(),
                preset.getSavingsRatio(),
                preset.getAccountRatio(),
                preset.getDescription());
    }
}
