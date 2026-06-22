package com.shinhan.eclipse.ledger.transfer.internal;

import com.shinhan.eclipse.domain.transaction.TransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

interface LedgerTransferTransactionRepository extends JpaRepository<TransferTransaction, Long> {}