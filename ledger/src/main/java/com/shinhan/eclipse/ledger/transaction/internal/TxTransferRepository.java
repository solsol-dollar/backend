package com.shinhan.eclipse.ledger.transaction.internal;

import com.shinhan.eclipse.domain.transaction.TransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface TxTransferRepository extends JpaRepository<TransferTransaction, Long> {

    @Query("SELECT t FROM TransferTransaction t WHERE t.userId = :userId AND (t.fromAccountId IN :ids OR t.toAccountId IN :ids) ORDER BY t.requestedAt DESC")
    List<TransferTransaction> findAllByAccounts(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Query("SELECT t FROM TransferTransaction t WHERE t.userId = :userId AND t.toAccountId IN :ids ORDER BY t.requestedAt DESC")
    List<TransferTransaction> findIncoming(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Query("SELECT t FROM TransferTransaction t WHERE t.userId = :userId AND t.fromAccountId IN :ids AND t.transferType != 'CARD' ORDER BY t.requestedAt DESC")
    List<TransferTransaction> findOutgoing(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    @Query("SELECT t FROM TransferTransaction t WHERE t.userId = :userId AND t.fromAccountId IN :ids AND t.transferType = 'CARD' ORDER BY t.requestedAt DESC")
    List<TransferTransaction> findCard(@Param("userId") Long userId, @Param("ids") List<Long> ids);
}