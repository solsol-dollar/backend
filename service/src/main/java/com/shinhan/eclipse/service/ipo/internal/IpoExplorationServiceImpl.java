package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.FavoriteIpo;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.service.ipo.IpoExplorationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class IpoExplorationServiceImpl implements IpoExplorationService {

    private final IpoRepository ipoRepository;
    private final FavoriteIpoRepository favoriteIpoRepository;

    @Override
    public List<Ipo> getUpcomingIpos() {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Ipo getIpoDetail(Long ipoId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public FavoriteIpo addFavorite(Long userId, Long ipoId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void removeFavorite(Long userId, Long ipoId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<FavoriteIpo> getFavorites(Long userId) {
        // TODO: 구현
        throw new UnsupportedOperationException("TODO");
    }
}
