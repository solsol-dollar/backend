package com.shinhan.eclipse.domain.transaction;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "transfer_transactions")
public class TransferTransaction extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    private Long allocationId;
    private Long fromAccountId;
    private Long toAccountId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 30)
    private String transferType;

    @Column(nullable = false, length = 30)
    private String transferStatus;

    @Column(nullable = false, length = 20)
    private String executionMode;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    public static TransferTransaction create(Long userId, Long fromAccountId, Long toAccountId,
                                              String transferType, BigDecimal amount) {
        TransferTransaction tx = new TransferTransaction();
        tx.userId         = userId;
        tx.fromAccountId  = fromAccountId;
        tx.toAccountId    = toAccountId;
        tx.transferType   = transferType;
        tx.amount         = amount;
        tx.currency       = "USD";
        tx.transferStatus = "REQUESTED";
        tx.executionMode  = "REAL";
        tx.requestedAt    = LocalDateTime.now();
        return tx;
    }

    public void complete() {
        this.transferStatus = "COMPLETED";
        this.completedAt    = LocalDateTime.now();
    }
}
