package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.ipo.FavoriteIpo;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.service.ipo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
                .orElseThrow(() -> new BusinessException(ErrorCode.IPO_NOT_FOUND));
        boolean isFavorite = favoriteIpoRepository.findByUserIdAndIpoId(userId, ipoId).isPresent();
        return toDetail(ipo, isFavorite);
    }

    @Override
    @Transactional
    public FavoriteIpoResponse addFavorite(Long userId, Long ipoId) {
        if (!ipoRepository.existsById(ipoId)) {
            throw new BusinessException(ErrorCode.IPO_NOT_FOUND);
        }
        if (favoriteIpoRepository.findByUserIdAndIpoId(userId, ipoId).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 찜한 IPO입니다.");
        }
        FavoriteIpo saved = favoriteIpoRepository.save(FavoriteIpo.create(userId, ipoId));
        return new FavoriteIpoResponse(saved.getIpoId(), true, saved.getCreatedAt());
    }

    @Override
    @Transactional
    public void removeFavorite(Long userId, Long ipoId) {
        FavoriteIpo favorite = favoriteIpoRepository.findByUserIdAndIpoId(userId, ipoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "찜 정보를 찾을 수 없습니다."));
        favoriteIpoRepository.delete(favorite);
    }

    @Override
    public List<FavoriteIpoItem> getFavoriteIpos(Long userId, Integer limit) {
        List<FavoriteIpo> favorites = favoriteIpoRepository.findByUserId(userId);

        List<Long> ipoIds = favorites.stream().map(FavoriteIpo::getIpoId).toList();
        if (ipoIds.isEmpty()) return List.of();

        Map<Long, Ipo> ipoMap = ipoRepository.findAllById(ipoIds).stream()
                .collect(Collectors.toMap(Ipo::getId, ipo -> ipo));

        List<FavoriteIpoItem> result = favorites.stream()
                .filter(fav -> ipoMap.containsKey(fav.getIpoId()))
                .map(fav -> {
                    Ipo ipo = ipoMap.get(fav.getIpoId());
                    return new FavoriteIpoItem(
                            fav.getId(),
                            ipo.getId(),
                            ipo.getTicker(),
                            ipo.getCompanyName(),
                            computeStatus(ipo),
                            ipo.getSubscriptionStartDate(),
                            ipo.getSubscriptionEndDate(),
                            ipo.getConfirmedOfferPrice()
                    );
                })
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
                isFavorite,
                ipo.getLogoUrl()
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
                isFavorite,
                ipo.getNumberOfShares(),
                ipo.getLogoUrl()
        );
    }
}
