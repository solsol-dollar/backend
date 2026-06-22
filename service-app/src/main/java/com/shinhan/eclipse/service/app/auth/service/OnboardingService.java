package com.shinhan.eclipse.service.app.auth.service;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.Card;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.user.User;
import com.shinhan.eclipse.service.app.auth.dto.OnboardingAccountsResponse;
import com.shinhan.eclipse.service.app.auth.dto.OnboardingAccountsResponse.AccountInfo;
import com.shinhan.eclipse.service.app.auth.dto.OnboardingAccountsResponse.CardInfo;
import com.shinhan.eclipse.service.app.auth.internal.CardRepository;
import com.shinhan.eclipse.service.app.auth.internal.OnboardingAccountRepository;
import com.shinhan.eclipse.service.app.auth.internal.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final OnboardingAccountRepository accountRepository;
    private final CardRepository cardRepository;

    @Transactional
    public void complete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if ("COMPLETED".equals(user.getOnboardingStatus())) {
            return;
        }

        // CMA 계좌 생성 (항상)
        String cmaNumber = generateAccountNumber();
        accountRepository.save(FinancialAccount.createCmaAccount(userId, cmaNumber, "USD"));
        accountRepository.save(FinancialAccount.createCmaAccount(userId, cmaNumber, "KRW"));

        // 기존 예금/적금 계좌 자동 연동
        accountRepository.findByUserIdAndAccountTypeInAndLinkedFalse(userId, List.of("DEPOSIT", "SAVINGS"))
                .forEach(FinancialAccount::link);

        // 기존 카드 자동 연동
        List<Card> cards = cardRepository.findByUserIdAndLinkedFalse(userId);
        cards.forEach(Card::link);

        user.completeOnboarding();
    }

    @Transactional(readOnly = true)
    public OnboardingAccountsResponse getAvailableAccounts(Long userId) {
        List<AccountInfo> accounts = accountRepository
                .findByUserIdAndAccountTypeInAndLinkedFalse(userId, List.of("DEPOSIT", "SAVINGS"))
                .stream()
                .map(a -> new AccountInfo(a.getId(), a.getAccountType(), a.getAccountName(),
                        a.getAccountNumberMasked(), a.getCurrency(), a.getBalance()))
                .toList();

        List<CardInfo> cards = cardRepository.findByUserIdAndLinkedFalse(userId)
                .stream()
                .map(c -> new CardInfo(c.getId(), c.getCardName(), c.getCardNumberMasked()))
                .toList();

        return new OnboardingAccountsResponse(accounts, cards);
    }

    private String generateAccountNumber() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "****-****-" + suffix;
    }
}