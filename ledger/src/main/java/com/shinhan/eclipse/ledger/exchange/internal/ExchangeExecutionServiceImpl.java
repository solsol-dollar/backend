package com.shinhan.eclipse.ledger.exchange.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.exchange.ExchangeRateInfo;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.transaction.FxExchangeTransaction;
import com.shinhan.eclipse.ledger.exchange.ExchangeExecutionService;
import com.shinhan.eclipse.ledger.exchange.ExchangeRequest;
import com.shinhan.eclipse.ledger.exchange.ExchangeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
class ExchangeExecutionServiceImpl implements ExchangeExecutionService {

    private final LedgerRateReader                rateReader;
    private final LedgerExchangeAccountRepository accountRepository;
    private final LedgerFxTransactionRepository   txRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ExchangeResult execute(ExchangeRequest request, Long userId) {
        validateDirection(request.direction());

        if (request.sourceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "환전 금액은 0보다 커야 합니다.");
        }

        boolean krwToUsd     = "KRW_TO_USD".equals(request.direction());
        String  fromCurrency = krwToUsd ? "KRW" : "USD";
        String  toCurrency   = krwToUsd ? "USD" : "KRW";

        ExchangeRateInfo rate = rateReader.read("USD")
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE));

        // 데드락 방지: KRW 계좌를 항상 먼저 락 (알파벳순 KRW < USD)
        FinancialAccount krwAccount = lockAccount(userId, "KRW");
        FinancialAccount usdAccount = lockAccount(userId, "USD");

        FinancialAccount fromAccount = krwToUsd ? krwAccount : usdAccount;
        FinancialAccount toAccount   = krwToUsd ? usdAccount : krwAccount;

        // KRW→USD: 고객이 달러 구매 → 은행 매도율(tts) 적용
        // USD→KRW: 고객이 달러 판매 → 은행 매수율(ttb) 적용
        BigDecimal appliedRate = krwToUsd ? rate.sellingRate() : rate.buyingRate();
        if (appliedRate == null || appliedRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.EXCHANGE_RATE_UNAVAILABLE, "유효하지 않은 환율입니다.");
        }
        BigDecimal targetAmount = krwToUsd
                ? request.sourceAmount().divide(appliedRate, 4, RoundingMode.HALF_UP)
                : request.sourceAmount().multiply(appliedRate).setScale(4, RoundingMode.HALF_UP);

        if (fromAccount.getBalance().compareTo(request.sourceAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE,
                    "잔액 부족 (보유: %s %s, 요청: %s)".formatted(
                            fromAccount.getBalance(), fromCurrency, request.sourceAmount()));
        }

        FxExchangeTransaction tx = FxExchangeTransaction.create(
                userId,
                fromAccount.getId(), toAccount.getId(),
                fromCurrency, toCurrency,
                appliedRate, request.sourceAmount(), targetAmount
        );
        tx = txRepository.save(tx);

        fromAccount.deductBalance(request.sourceAmount());
        toAccount.addBalance(targetAmount);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        tx.complete(fromAccount.getBalance(), toAccount.getBalance());
        tx = txRepository.save(tx);

        log.info("환전 완료: userId={} {}{} → {}{} (rate={})",
                userId, request.sourceAmount(), fromCurrency, targetAmount, toCurrency, appliedRate);

        return new ExchangeResult(
                tx.getId(),
                appliedRate,
                request.sourceAmount(), fromCurrency,
                targetAmount, toCurrency,
                tx.getExchangeStatus(),
                tx.getCompletedAt()
        );
    }

    private FinancialAccount lockAccount(Long userId, String currency) {
        return accountRepository.findLinkedAccountWithLock(userId, currency)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_ACCOUNT_NOT_FOUND,
                        "연동된 " + currency + " 계좌가 없습니다."));
    }

    private void validateDirection(String direction) {
        if (!"KRW_TO_USD".equals(direction) && !"USD_TO_KRW".equals(direction)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "올바르지 않은 환전 방향입니다: " + direction);
        }
    }
}