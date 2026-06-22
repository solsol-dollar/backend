package com.shinhan.eclipse.ledger.balance;

import java.math.BigDecimal;

public interface BalanceAdjustFacade {
    BigDecimal adjust(BalanceAdjustRequest request);
}
