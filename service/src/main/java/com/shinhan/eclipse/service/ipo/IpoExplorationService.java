package com.shinhan.eclipse.service.ipo;

import com.shinhan.eclipse.domain.ipo.FavoriteIpo;
import com.shinhan.eclipse.domain.ipo.Ipo;

import java.util.List;

public interface IpoExplorationService {
    List<Ipo> getUpcomingIpos();
    Ipo getIpoDetail(Long ipoId);
    FavoriteIpo addFavorite(Long userId, Long ipoId);
    void removeFavorite(Long userId, Long ipoId);
    List<FavoriteIpo> getFavorites(Long userId);
}
