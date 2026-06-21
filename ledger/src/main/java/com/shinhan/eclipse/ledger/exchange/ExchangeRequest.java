package com.shinhan.eclipse.ledger.exchange;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExchangeRequest(
        @NotBlank String direction,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal sourceAmount
) {}
