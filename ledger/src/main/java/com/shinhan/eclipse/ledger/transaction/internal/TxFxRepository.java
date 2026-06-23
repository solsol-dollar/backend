package com.shinhan.eclipse.ledger.transaction.internal;

import com.shinhan.eclipse.domain.transaction.FxExchangeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface TxFxRepository extends JpaRepository<FxExchangeTransaction, Long> {

    @Query("SELECT t FROM FxExchangeTransaction t WHERE t.userId = :userId AND (t.fromAccountId IN :ids OR t.toAccountId IN :ids) ORDER BY t.requestedAt DESC")
    List<FxExchangeTransaction> findAllByAccounts(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}