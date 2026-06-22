package com.shinhan.eclipse.service.ipo;

import java.time.LocalDateTime;

public record FavoriteIpoResponse(Long ipoId, boolean isFavorite, LocalDateTime createdAt) {}
