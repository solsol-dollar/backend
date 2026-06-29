package com.shinhan.eclipse.service.ipo;

import java.util.List;

public record IpoTopNewsResult(
        List<IpoNewsItem> pre,
        List<IpoNewsItem> post
) {}
