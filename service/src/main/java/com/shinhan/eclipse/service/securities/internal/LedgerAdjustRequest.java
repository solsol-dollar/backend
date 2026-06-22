package com.shinhan.eclipse.service.securities.internal;

import java.math.BigDecimal;

record LedgerAdjustRequest(Long userId, Long accountId, BigDecimal amount, String type) {}
