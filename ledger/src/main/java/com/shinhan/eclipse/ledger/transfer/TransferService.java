package com.shinhan.eclipse.ledger.transfer;

public interface TransferService {
    TransferResult transfer(TransferRequest request, Long userId);
}