package com.shinhan.eclipse.worker.ipo.dto;

import java.time.LocalDateTime;

public record NewsItem(
        Long ipoId,
        String title,
        String source,
        LocalDateTime publishedAt,
        String url,
        String phase,
        String content
) {}
