package com.shinhan.eclipse.service.ipo;

import java.util.List;

public record IpoListResult(
        List<IpoItem> ipos,
        long total,
        int page,
        int size
) {}
