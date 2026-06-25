package com.shinhan.eclipse.service.home;

import com.shinhan.eclipse.service.ipo.FavoriteIpoItem;

import java.util.List;

public interface HomeService {
    AssetsSummaryResponse getAssets(Long userId);
    List<FavoriteIpoItem> getRandomFavoriteIpos(Long userId);
    Object getDashboard(Long userId);
}
