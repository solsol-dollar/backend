package com.shinhan.eclipse.service.mypage;

import com.shinhan.eclipse.domain.user.InvestmentProfile;

public interface MyPageService {
    InvestmentProfile diagnoseInvestmentProfile(Long userId, String surveyJson);
    InvestmentProfile getLatestProfile(Long userId);
    MyPageAccountsResponse getLinkedAccounts(Long userId);
    MyPageAccountsResponse.AccountItem createDepositAccount(Long userId);
    MyPageAccountsResponse.AccountItem createSavingsAccount(Long userId);
    MyPageAccountsResponse.CardItem createCard(Long userId);
}
