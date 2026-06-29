package com.shinhan.eclipse.service.ipo;

import java.time.LocalDateTime;

public record IpoNewsDetailItem(
        Long id,
        String title,
        String titleKo,
        String source,
        LocalDateTime publishedAt,
        String url,
        String summary,
        String content,
        String contentKo
) {}
