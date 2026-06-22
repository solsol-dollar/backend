package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.transaction.TransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

interface IdleDetectionTransferRepository extends JpaRepository<TransferTransaction, Long> {

    @Query("""
            SELECT MAX(t.requestedAt) FROM TransferTransaction t
            WHERE (t.fromAccountId = :accountId OR t.toAccountId = :accountId)
              AND t.status = 'ACTIVE'
            """)
    Optional<LocalDateTime> findLatestRequestedAt(@Param("accountId") Long accountId);
}