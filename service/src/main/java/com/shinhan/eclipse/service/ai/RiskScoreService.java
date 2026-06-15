package com.shinhan.eclipse.service.ai;

import com.shinhan.eclipse.domain.ipo.IpoRiskScore;

public interface RiskScoreService {
    IpoRiskScore analyzeRisk(Long ipoId);
    IpoRiskScore getRiskScore(Long ipoId);
}
