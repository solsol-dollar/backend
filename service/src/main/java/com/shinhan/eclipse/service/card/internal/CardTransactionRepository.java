package com.shinhan.eclipse.service.card.internal;

import com.shinhan.eclipse.domain.account.CardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    List<CardTransaction> findByUserIdAndTransactedAtBetweenOrderByTransactedAtDesc(
            Long userId, LocalDateTime from, LocalDateTime to);

    List<CardTransaction> findByUserIdAndTransactedAtAfterOrderByTransactedAtAsc(
            Long userId, LocalDateTime from);
}