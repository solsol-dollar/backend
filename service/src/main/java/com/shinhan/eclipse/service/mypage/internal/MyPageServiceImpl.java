package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.account.Card;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.domain.user.InvestmentProfile;
import com.shinhan.eclipse.service.mypage.MyPageAccountsResponse;
import com.shinhan.eclipse.service.mypage.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
class MyPageServiceImpl implements MyPageService {

    private final InvestmentProfileRepository investmentProfileRepository;
    private final MyPageAccountRepository accountRepository;
    private final MyPageCardRepository cardRepository;

    @Override
    public InvestmentProfile diagnoseInvestmentProfile(Long userId, String surveyJson) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "미구현 기능입니다.");
    }

    @Override
    public InvestmentProfile getLatestProfile(Long userId) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "미구현 기능입니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public MyPageAccountsResponse getLinkedAccounts(Long userId) {
        List<FinancialAccount> accounts = accountRepository.findByUserIdAndLinkedTrueAndStatus(userId, "ACTIVE");
        List<Card> cards = cardRepository.findByUserIdAndLinkedTrueAndStatus(userId, "ACTIVE");

        List<MyPageAccountsResponse.AccountItem> accountItems = accounts.stream()
                .map(a -> new MyPageAccountsResponse.AccountItem(
                        a.getId(), a.getAccountType(), a.getAccountName(),
                        a.getAccountNumberMasked(), a.getCurrency(),
                        a.getBalance(), a.getInterestRate(), a.getMaturityDate()))
                .toList();

        List<MyPageAccountsResponse.CardItem> cardItems = cards.stream()
                .map(c -> new MyPageAccountsResponse.CardItem(
                        c.getId(), c.getCardName(), c.getCardNumberMasked(), c.getIssuerName()))
                .toList();

        return new MyPageAccountsResponse(accountItems, cardItems);
    }

    @Override
    @Transactional
    public MyPageAccountsResponse.AccountItem createDepositAccount(Long userId) {
        if (accountRepository.existsByUserIdAndAccountTypeAndStatus(userId, "DEPOSIT", "ACTIVE")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 예금 계좌가 존재합니다.");
        }

        FinancialAccount account = accountRepository.save(FinancialAccount.createDepositAccount(userId));
        return new MyPageAccountsResponse.AccountItem(
                account.getId(), account.getAccountType(), account.getAccountName(),
                account.getAccountNumberMasked(), account.getCurrency(),
                account.getBalance(), account.getInterestRate(), account.getMaturityDate());
    }

    @Override
    @Transactional
    public MyPageAccountsResponse.AccountItem createSavingsAccount(Long userId) {
        if (accountRepository.existsByUserIdAndAccountTypeAndStatus(userId, "SAVINGS", "ACTIVE")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 적금 계좌가 존재합니다.");
        }

        FinancialAccount account = accountRepository.save(FinancialAccount.createSavingsAccount(userId));
        return new MyPageAccountsResponse.AccountItem(
                account.getId(), account.getAccountType(), account.getAccountName(),
                account.getAccountNumberMasked(), account.getCurrency(),
                account.getBalance(), account.getInterestRate(), account.getMaturityDate());
    }

    @Override
    @Transactional
    public MyPageAccountsResponse.CardItem createCard(Long userId) {
        if (cardRepository.existsByUserIdAndStatus(userId, "ACTIVE")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 카드가 존재합니다.");
        }

        FinancialAccount cmaAccount = accountRepository
                .findFirstByUserIdAndAccountTypeAndStatus(userId, "DEPOSIT", "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "연동된 외화 예금 계좌가 없습니다."));

        Card card = cardRepository.save(Card.issue(userId, cmaAccount.getId()));
        return new MyPageAccountsResponse.CardItem(
                card.getId(), card.getCardName(), card.getCardNumberMasked(), card.getIssuerName());
    }
}