package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.domain.ipo.FavoriteIpo;
import com.shinhan.eclipse.domain.ipo.Ipo;
import com.shinhan.eclipse.domain.ipo.IpoNews;
import com.shinhan.eclipse.service.exchange.ExchangeService;
import com.shinhan.eclipse.service.ipo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class IpoExplorationServiceImpl implements IpoExplorationService {

    private final IpoRepository ipoRepository;
    private final IpoNewsRepository ipoNewsRepository;
    private final FavoriteIpoRepository favoriteIpoRepository;
    private final IpoFinancialRepository ipoFinancialRepository;
    private final IpoScoreRepository ipoScoreRepository;
    private final ExchangeService exchangeService;

    @Override
    public IpoListResult getIpos(String status, boolean favoriteOnly, Long userId, int page, int size, String keyword) {
        Page<Ipo> ipoPage = ipoRepository.findWithFilters(
                status, favoriteOnly, userId, keyword, PageRequest.of(page, size));

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
    public List<IpoNewsItem> getIpoNews(Long ipoId, int size) {
        if (!ipoRepository.existsById(ipoId)) {
            throw new BusinessException(ErrorCode.IPO_NOT_FOUND);
        }
        return ipoNewsRepository
                .findByIpoIdOrderByPublishedAtDesc(ipoId, PageRequest.of(0, size))
                .stream()
                .map(this::toNewsItem)
                .toList();
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
        try {
            FavoriteIpo saved = favoriteIpoRepository.save(FavoriteIpo.create(userId, ipoId));
            return new FavoriteIpoResponse(saved.getIpoId(), true, saved.getCreatedAt());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 찜한 IPO입니다.");
        }
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
                            ipo.getLogoUrl(),
                            computeStatus(ipo),
                            ipo.getSubscriptionStartDate(),
                            ipo.getSubscriptionEndDate(),
                            ipo.getListingDate(),
                            ipo.getConfirmedOfferPrice()
                    );
                })
                .toList();

        if (limit != null && limit > 0 && result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    @Override
    public Optional<IpoScoreResult> getIpoScore(Long ipoId) {
        return ipoScoreRepository.findByIpoId(ipoId)
                .map(score -> new IpoScoreResult(
                        score.getIpoId(),
                        score.getTicker(),
                        score.getFinalScore(),
                        score.getGrade(),
                        score.getReason(),
                        score.getSummary(),
                        parseTopNewsIds(score.getTopNewsIds()),
                        score.getNewsCount(),
                        score.getScoredAt(),
                        score.getPostFinalScore(),
                        score.getPostGrade(),
                        score.getPostReason(),
                        score.getPostSummary(),
                        parseTopNewsIds(score.getPostTopNewsIds()),
                        score.getPostNewsCount()
                ));
    }

    @Override
    public List<IpoFinancialItem> getIpoFinancials(Long ipoId) {
        if (!ipoRepository.existsById(ipoId)) {
            throw new BusinessException(ErrorCode.IPO_NOT_FOUND);
        }
        return ipoFinancialRepository.findByIpoIdOrderByFiscalYearDesc(ipoId).stream()
                .map(f -> {
                    BigDecimal rate = safeGetRate(f.getCurrency());
                    return new IpoFinancialItem(
                            f.getFiscalYear(),
                            f.getRevenue(), f.getOperatingIncome(), f.getNetIncome(),
                            f.getCurrency(),
                            toKrw(f.getRevenue(), rate),
                            toKrw(f.getOperatingIncome(), rate),
                            toKrw(f.getNetIncome(), rate)
                    );
                })
                .toList();
    }

    private BigDecimal safeGetRate(String currency) {
        try {
            return exchangeService.getExchangeRate(currency).baseRate();
        } catch (Exception e) {
            log.warn("환율 조회 실패 ({}), KRW 환산 생략: {}", currency, e.getMessage());
            return null;
        }
    }

    private Long toKrw(Long amount, BigDecimal rate) {
        if (amount == null || rate == null) return null;
        return BigDecimal.valueOf(amount).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private List<Long> parseTopNewsIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            String cleaned = json.trim().replaceAll("[\\[\\]\\s]", "");
            if (cleaned.isEmpty()) return List.of();
            return Arrays.stream(cleaned.split(","))
                    .map(Long::parseLong)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
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

    @Override
    public IpoTopNewsResult getTopIpoNews(Long ipoId) {
        return ipoScoreRepository.findByIpoId(ipoId)
                .map(score -> new IpoTopNewsResult(
                        fetchNewsByIds(parseTopNewsIds(score.getTopNewsIds())),
                        fetchNewsByIds(parseTopNewsIds(score.getPostTopNewsIds()))
                ))
                .orElse(new IpoTopNewsResult(List.of(), List.of()));
    }

    @Override
    public IpoNewsDetailItem getIpoNewsDetail(Long ipoId, Long newsId) {
        IpoNews news = ipoNewsRepository.findByIdAndIpoId(newsId, ipoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IPO_NOT_FOUND));
        return new IpoNewsDetailItem(
                news.getId(),
                news.getTitle(),
                news.getTitleKo(),
                news.getSource(),
                news.getPublishedAt(),
                news.getUrl(),
                news.getSummary(),
                news.getContent(),
                news.getContentKo()
        );
    }

    private List<IpoNewsItem> fetchNewsByIds(List<Long> ids) {
        return ids.stream()
                .limit(2)
                .map(id -> ipoNewsRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(this::toNewsItem)
                .toList();
    }

    private IpoNewsItem toNewsItem(IpoNews news) {
        return new IpoNewsItem(
                news.getId(),
                news.getTitleKo() != null ? news.getTitleKo() : news.getTitle(),
                news.getSource(),
                news.getPublishedAt(),
                news.getUrl(),
                news.getSummary()
        );
    }

    private IpoItem toItem(Ipo ipo, boolean isFavorite) {
        BigDecimal currentPrice = ipo.getCurrentPrice();
        BigDecimal confirmedPrice = ipo.getConfirmedOfferPrice();
        BigDecimal priceChange = null;
        BigDecimal priceChangePercent = null;
        if (currentPrice != null && confirmedPrice != null
                && confirmedPrice.compareTo(BigDecimal.ZERO) > 0) {
            priceChange = currentPrice.subtract(confirmedPrice).setScale(4, RoundingMode.HALF_UP);
            priceChangePercent = priceChange.divide(confirmedPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }
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
                confirmedPrice,
                isFavorite,
                ipo.getLogoUrl(),
                currentPrice,
                priceChange,
                priceChangePercent
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
