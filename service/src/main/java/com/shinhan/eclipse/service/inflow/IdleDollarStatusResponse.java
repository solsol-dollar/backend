package com.shinhan.eclipse.service.inflow;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IdleDollarStatusResponse(
        boolean isIdle,
        Long accountId,
        BigDecimal idleBalance,
        int idleDays,
        LocalDateTime detectedAt
) {
    public static IdleDollarStatusResponse none() {
        return new IdleDollarStatusResponse(false, null, null, 0, null);
    }
}