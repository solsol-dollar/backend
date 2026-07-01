package com.shinhan.eclipse.ledger.returnplan.internal;

import com.shinhan.eclipse.domain.transaction.TransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReturnPlanTransferRepository extends JpaRepository<TransferTransaction, Long> {}
