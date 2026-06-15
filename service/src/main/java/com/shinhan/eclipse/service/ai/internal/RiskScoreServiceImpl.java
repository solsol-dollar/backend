package com.shinhan.eclipse.service.ai.internal;

import com.shinhan.eclipse.domain.ipo.IpoRiskScore;
import com.shinhan.eclipse.service.ai.RiskScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class RiskScoreServiceImpl implements RiskScoreService {

    private final ChatClient chatClient;
    private final IpoRiskScoreRepository riskScoreRepository;

    @Override
    public IpoRiskScore analyzeRisk(Long ipoId) {
        // TODO: ipoId로 뉴스/공시 조회 → ChatClient 호출 → .entity(RiskAnalysisResult.class) → 저장
        // 실패 시 graceful degradation (AI_ANALYSIS_FAILED 로깅 후 null 반환)
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public IpoRiskScore getRiskScore(Long ipoId) {
        // TODO: DB 캐시 조회, 없으면 analyzeRisk 호출
        throw new UnsupportedOperationException("TODO");
    }
}
