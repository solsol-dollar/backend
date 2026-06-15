package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.domain.user.InvestmentProfile;
import com.shinhan.eclipse.service.mypage.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class MyPageServiceImpl implements MyPageService {

    private final InvestmentProfileRepository investmentProfileRepository;

    @Override
    public InvestmentProfile diagnoseInvestmentProfile(Long userId, String surveyJson) {
        // TODO: 설문 점수 계산 → InvestmentProfile 저장
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public InvestmentProfile getLatestProfile(Long userId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }
}
