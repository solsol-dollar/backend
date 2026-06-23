package com.shinhan.eclipse.service.ipo;

import java.time.LocalDateTime;

public record IpoNewsItem(
        Long id,
        String title,
        String source,
        LocalDateTime publishedAt,
        String url,
        String summary
) {}
