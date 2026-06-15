package com.shinhan.eclipse.worker.dto;

// Spring AI .entity() 파싱용 — Claude가 JSON으로 반환
public record NewsSummaryResult(
        String summary
) {}
