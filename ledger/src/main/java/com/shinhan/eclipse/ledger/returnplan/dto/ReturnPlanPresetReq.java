package com.shinhan.eclipse.ledger.returnplan.dto;

import jakarta.validation.constraints.NotBlank;

public record ReturnPlanPresetReq(
        @NotBlank String presetCode
) {}
