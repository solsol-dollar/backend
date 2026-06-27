package com.shinhan.eclipse.ledger.transaction.internal;

import com.shinhan.eclipse.domain.account.BalanceHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface TxBalanceHoldRepository extends JpaRepository<BalanceHold, Long> {

    /** 계좌 목록에 해당하는 모든 청약 홀딩 내역 (회사명 포함) */
    @Query("""
            SELECT bh, i.companyName
            FROM BalanceHold bh
            JOIN IpoSubscription s ON s.id = bh.subscriptionId
            JOIN Ipo i ON i.id = s.ipoId
            WHERE bh.accountId IN :accountIds
            ORDER BY bh.createdAt DESC
            """)
    List<Object[]> findWithCompanyNameByAccountIds(@Param("accountIds") List<Long> accountIds);

    /** 출금(청약 신청): LOCKED 또는 SETTLED */
    @Query("""
            SELECT bh, i.companyName
            FROM BalanceHold bh
            JOIN IpoSubscription s ON s.id = bh.subscriptionId
            JOIN Ipo i ON i.id = s.ipoId
            WHERE bh.accountId IN :accountIds AND bh.holdStatus IN ('LOCKED', 'SETTLED')
            ORDER BY bh.createdAt DESC
            """)
    List<Object[]> findOutgoingWithCompanyName(@Param("accountIds") List<Long> accountIds);

    /** 입금(청약 취소): RELEASED */
    @Query("""
            SELECT bh, i.companyName
            FROM BalanceHold bh
            JOIN IpoSubscription s ON s.id = bh.subscriptionId
            JOIN Ipo i ON i.id = s.ipoId
            WHERE bh.accountId IN :accountIds AND bh.holdStatus = 'RELEASED'
            ORDER BY bh.createdAt DESC
            """)
    List<Object[]> findIncomingWithCompanyName(@Param("accountIds") List<Long> accountIds);
}
