package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.FavoriteIpo;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.service.ipo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class IpoExplorationServiceImpl implements IpoExplorationService {

    private final IpoRepository ipoRepository;
    private final FavoriteIpoRepository favoriteIpoRepository;

    @Override
    public IpoListResult getIpos(String status, boolean favoriteOnly, Long userId, int page, int size) {
        Page<Ipo> ipoPage = ipoRepository.findWithFilters(
                status, favoriteOnly, userId, PageRequest.of(page, size));

        Set<Long> favoriteIds = favoriteIpoRepository.findByUserId(userId).stream()
                .map(FavoriteIpo::getIpoId)
                .collect(Collectors.toSet());

        List<IpoItem> items = ipoPage.getContent().stream()
                .map(ipo -> toItem(ipo, favoriteIds.contains(ipo.getId())))
                .toList();

        return new IpoListResult(items, ipoPage.getTotalElements(), page, size);
    }

    @Override
    public IpoDetailResult getIpoDetail(Long ipoId, Long userId) {
        Ipo ipo = ipoRepository.findById(ipoId)
                .orElseThrow(() -> new NoSuchElementException("IPO not found: " + ipoId));
        boolean isFavorite = favoriteIpoRepository.findByUserIdAndIpoId(userId, ipoId).isPresent();
        return toDetail(ipo, isFavorite);
    }

    @Override
    public FavoriteIpoResponse addFavorite(Long userId, Long ipoId) {
        if (favoriteIpoRepository.findByUserIdAndIpoId(userId, ipoId).isPresent()) {
            throw new IllegalStateException("Already favorited");
        }
        FavoriteIpo saved = favoriteIpoRepository.save(FavoriteIpo.create(userId, ipoId));
        return new FavoriteIpoResponse(saved.getIpoId(), true, saved.getCreatedAt());
    }

    @Override
    public void removeFavorite(Long userId, Long ipoId) {
        FavoriteIpo favorite = favoriteIpoRepository.findByUserIdAndIpoId(userId, ipoId)
                .orElseThrow(() -> new NoSuchElementException("Favorite not found"));
        favoriteIpoRepository.delete(favorite);
    }

    @Override
    public List<FavoriteIpoItem> getFavoriteIpos(Long userId, Integer limit) {
        List<FavoriteIpo> favorites = favoriteIpoRepository.findByUserId(userId);

        List<Long> ipoIds = favorites.stream().map(FavoriteIpo::getIpoId).toList();
        if (ipoIds.isEmpty()) return List.of();

        List<Ipo> ipos = ipoRepository.findAllById(ipoIds);

        List<FavoriteIpoItem> result = favorites.stream()
                .flatMap(fav -> ipos.stream()
                        .filter(ipo -> ipo.getId().equals(fav.getIpoId()))
                        .map(ipo -> new FavoriteIpoItem(
                                fav.getId(),
                                ipo.getId(),
                                ipo.getTicker(),
                                ipo.getCompanyName(),
                                computeStatus(ipo),
                                ipo.getSubscriptionStartDate(),
                                ipo.getSubscriptionEndDate(),
                                ipo.getConfirmedOfferPrice()
                        )))
                .toList();

        if (limit != null && limit > 0 && result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    private String computeStatus(Ipo ipo) {
        LocalDate today = LocalDate.now();
        if (ipo.getSubscriptionStartDate() == null || today.isBefore(ipo.getSubscriptionStartDate())) {
            return "UPCOMING";
        }
        if (ipo.getSubscriptionEndDate() != null && !today.isAfter(ipo.getSubscriptionEndDate())) {
            return "OPEN";
        }
        if (ipo.getListingDate() != null && !today.isBefore(ipo.getListingDate())) {
            return "CLOSED";
        }
        return "UPCOMING";
    }

    private IpoItem toItem(Ipo ipo, boolean isFavorite) {
        return new IpoItem(
                ipo.getId(),
                ipo.getTicker(),
                ipo.getCompanyName(),
                computeStatus(ipo),
                ipo.getSubscriptionStartDate(),
                ipo.getSubscriptionEndDate(),
                ipo.getListingDate(),
                ipo.getOfferPriceMin(),
                ipo.getOfferPriceMax(),
                ipo.getConfirmedOfferPrice(),
                isFavorite
        );
    }

    private IpoDetailResult toDetail(Ipo ipo, boolean isFavorite) {
        return new IpoDetailResult(
                ipo.getId(),
                ipo.getTicker(),
                ipo.getCompanyName(),
                ipo.getExchangeName(),
                ipo.getSector(),
                computeStatus(ipo),
                ipo.getSubscriptionStartDate(),
                ipo.getSubscriptionEndDate(),
                ipo.getListingDate(),
                ipo.getRefundDate(),
                ipo.getDepositDate(),
                ipo.getOfferPriceMin(),
                ipo.getOfferPriceMax(),
                ipo.getConfirmedOfferPrice(),
                ipo.getMinimumSubscriptionAmount(),
                isFavorite
        );
    }
}
