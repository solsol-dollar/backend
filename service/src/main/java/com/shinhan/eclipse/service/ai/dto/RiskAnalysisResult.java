package com.shinhan.eclipse.service.ai.dto;

import java.math.BigDecimal;
import java.util.List;

// Spring AI .entity() 파싱용 DTO
public record RiskAnalysisResult(
        BigDecimal score,
        String summary,
        List<String> reasons
) {}
