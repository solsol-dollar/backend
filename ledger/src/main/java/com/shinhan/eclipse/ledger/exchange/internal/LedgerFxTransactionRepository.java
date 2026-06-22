package com.shinhan.eclipse.ledger.exchange.internal;

import com.shinhan.eclipse.domain.transaction.FxExchangeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

interface LedgerFxTransactionRepository extends JpaRepository<FxExchangeTransaction, Long> {}
