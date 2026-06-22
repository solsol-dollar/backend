package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
class LedgerClient {

    private final LedgerApiClient apiClient;

    void deductBalance(Long userId, Long accountId, BigDecimal amount) {
        call(userId, accountId, amount, "DEDUCT");
    }

    void addBalance(Long userId, Long accountId, BigDecimal amount) {
        call(userId, accountId, amount, "ADD");
    }

    private void call(Long userId, Long accountId, BigDecimal amount, String type) {
        try {
            apiClient.adjust(new LedgerAdjustRequest(userId, accountId, amount, type));
            log.info("잔고 조정 완료: userId={} accountId={} amount={} type={}", userId, accountId, amount, type);
        } catch (FeignException.BadRequest | FeignException.UnprocessableEntity e) {
            log.warn("잔고 부족: userId={} accountId={}", userId, accountId);
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        } catch (FeignException e) {
            log.error("ledger-app 호출 실패: status={} body={}", e.status(), e.contentUTF8());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "ledger-app 호출 실패: " + e.status());
        }
    }
}
