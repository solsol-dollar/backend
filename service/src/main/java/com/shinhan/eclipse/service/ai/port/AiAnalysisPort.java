package com.shinhan.eclipse.service.ai.port;

import com.shinhan.eclipse.service.ai.dto.RiskAnalysisResult;

public interface AiAnalysisPort {
    RiskAnalysisResult analyzeIpoRisk(String companyName, String newsContent);
}
