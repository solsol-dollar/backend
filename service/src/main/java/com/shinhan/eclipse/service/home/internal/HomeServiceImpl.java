package com.shinhan.eclipse.service.home.internal;

import com.shinhan.eclipse.service.home.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class HomeServiceImpl implements HomeService {

    @Override
    public Object getDashboard(Long userId) {
        // TODO: 홈 대시보드 집계 (구독 현황, IPO 일정, 보유 종목, 유휴 달러)
        throw new UnsupportedOperationException("TODO");
    }
}
