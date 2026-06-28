package com.shinhan.eclipse.service.ipo;

import java.util.List;
import java.util.Optional;

public interface IpoExplorationService {
    IpoListResult getIpos(String status, boolean favoriteOnly, Long userId, int page, int size);
    IpoDetailResult getIpoDetail(Long ipoId, Long userId);
    List<IpoNewsItem> getIpoNews(Long ipoId, int size);
    FavoriteIpoResponse addFavorite(Long userId, Long ipoId);
    void removeFavorite(Long userId, Long ipoId);
    List<FavoriteIpoItem> getFavoriteIpos(Long userId, Integer limit);
    Optional<IpoScoreResult> getIpoScore(Long ipoId);
    List<IpoFinancialItem> getIpoFinancials(Long ipoId);
}
