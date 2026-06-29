package com.shinhan.eclipse.service.ipo;

import java.time.LocalDateTime;
import java.util.List;

public record IpoScoreResult(
        Long ipoId,
        String ticker,
        Integer finalScore,
        String grade,
        String reason,
        String summary,
        List<Long> topNewsIds,
        Integer newsCount,
        LocalDateTime scoredAt,
        Integer postScore,
        String postGrade,
        String postReason,
        String postSummary,
        List<Long> postTopNewsIds,
        Integer postNewsCount
) {}
