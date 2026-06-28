package com.shinhan.eclipse.ledger.transaction.internal;

import com.shinhan.eclipse.domain.account.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface TxCardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    @Query("SELECT c FROM CardTransaction c WHERE c.userId = :userId AND c.cardId IN (SELECT card.id FROM Card card WHERE card.linkedAccountId IN :accountIds) ORDER BY c.transactedAt DESC")
    List<CardTransaction> findAllByUserIdAndAccountIds(@Param("userId") Long userId, @Param("accountIds") List<Long> accountIds);
}
