package com.shinhan.eclipse.ledger.returnplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AllocationItem(
        @NotBlank String destinationType,
        @NotNull Integer ratio
) {}
