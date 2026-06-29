package com.shinhan.eclipse.service.home.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.service.exchange.market.MarketRateData;
import com.shinhan.eclipse.service.exchange.market.MarketRateRedisStore;
import lombok.extern.slf4j.Slf4j;
import com.shinhan.eclipse.domain.account.Card;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.service.card.CardService;
import com.shinhan.eclipse.service.home.AssetsSummaryResponse;
import com.shinhan.eclipse.service.home.HomeService;
import com.shinhan.eclipse.service.ipo.FavoriteIpoItem;
import com.shinhan.eclipse.service.ipo.IpoExplorationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
class HomeServiceImpl implements HomeService {

    private final AssetAccountRepository accountRepository;
    private final AssetCardRepository cardRepository;
    private final IpoExplorationService ipoExplorationService;
    private final CardService cardService;
    private final MarketRateRedisStore marketRateRedisStore;

    @Override
    @Transactional(readOnly = true)
    public AssetsSummaryResponse getAssets(Long userId) {
        List<FinancialAccount> accounts = accountRepository.findByUserIdAndLinkedTrueAndStatus(userId, "ACTIVE");
        List<Card> cards = cardRepository.findByUserIdAndLinkedTrueAndStatus(userId, "ACTIVE");

        // 실시간 시장 환율 (Redis)
        MarketRateData marketRate = marketRateRedisStore.get().orElse(null);
        BigDecimal exchangeRate   = marketRate != null ? marketRate.price()      : null;
        BigDecimal changeAmount   = marketRate != null ? marketRate.change()     : null;
        BigDecimal changeRate     = marketRate != null ? marketRate.changeRate() : null;
        BigDecimal prevRate       = (exchangeRate != null && changeAmount != null)
                ? exchangeRate.subtract(changeAmount) : null;

        // 증권(CMA) 계좌 분리
        List<FinancialAccount> securities = accounts.stream()
                .filter(a -> "SECURITIES".equals(a.getAccountType()))
                .toList();

        BigDecimal cmaUsd = securities.stream()
                .filter(a -> "USD".equals(a.getCurrency()))
                .map(FinancialAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cmaKrw = securities.stream()
                .filter(a -> "KRW".equals(a.getCurrency()))
                .map(FinancialAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 사용가능잔액(= 실제잔액 - 청약 등으로 잠긴 reservedBalance)
        BigDecimal cmaUsdAvailable = securities.stream()
                .filter(a -> "USD".equals(a.getCurrency()))
                .map(FinancialAccount::availableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cmaKrwAvailable = securities.stream()
                .filter(a -> "KRW".equals(a.getCurrency()))
                .map(FinancialAccount::availableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // KRW → USD 환산 (환율 없거나 0이면 null — 환산 불가 상태 명시)
        BigDecimal krwInUsd = (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) > 0)
                ? cmaKrwAvailable.divide(exchangeRate, 4, RoundingMode.HALF_UP)
                : null;
        BigDecimal securitiesTotalUsd = (krwInUsd != null) ? cmaUsdAvailable.add(krwInUsd) : null;

        Long usdAccountId = securities.stream()
                .filter(a -> "USD".equals(a.getCurrency()))
                .map(FinancialAccount::getId)
                .findFirst().orElse(null);
        Long krwAccountId = securities.stream()
                .filter(a -> "KRW".equals(a.getCurrency()))
                .map(FinancialAccount::getId)
                .findFirst().orElse(null);
        String cmaAccountNumber = securities.stream()
                .map(FinancialAccount::getAccountNumber)
                .findFirst()
                .orElse(null);

        AssetsSummaryResponse.SecuritiesAsset securitiesAsset =
                new AssetsSummaryResponse.SecuritiesAsset(
                        usdAccountId, krwAccountId, cmaAccountNumber, cmaUsd, cmaKrw, securitiesTotalUsd,
                        cmaUsdAvailable, cmaKrwAvailable);

        // 예금/적금 계좌
        List<AssetsSummaryResponse.AccountAsset> accountAssets = accounts.stream()
                .filter(a -> !("SECURITIES".equals(a.getAccountType())))
                .map(a -> new AssetsSummaryResponse.AccountAsset(
                        a.getId(),
                        a.getAccountType(),
                        a.getAccountName(),
                        a.getAccountNumber(),
                        a.getBalance(),
                        a.getInterestRate(),
                        a.getMaturityDate()
                ))
                .toList();

        // 이번 달 카드 소비 합계
        BigDecimal monthlySpend = BigDecimal.ZERO;
        int monthlyCount = 0;
        try {
            java.time.LocalDate today = java.time.LocalDate.now();
            var summary = cardService.getMonthlySummary(userId, today.getYear(), today.getMonthValue());
            monthlySpend = summary.totalAmount();
            monthlyCount = summary.totalCount();
        } catch (Exception e) {
            log.warn("[홈] 카드 소비 요약 조회 실패: {}", e.getMessage());
        }

        // 카드 (현재 유저당 카드 1장 기준, 소비 합계 공유)
        final BigDecimal finalMonthlySpend = monthlySpend;
        final int finalMonthlyCount = monthlyCount;
        List<AssetsSummaryResponse.CardAsset> cardAssets = cards.stream()
                .map(c -> new AssetsSummaryResponse.CardAsset(
                        c.getCardName(), c.getCardNumberMasked(), c.getIssuerName(),
                        finalMonthlySpend, "USD", finalMonthlyCount))
                .toList();

        // 전체 총합 (USD 기준)
        BigDecimal accountsTotalUsd = accountAssets.stream()
                .map(AssetsSummaryResponse.AccountAsset::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUsdBalance = (securitiesTotalUsd != null) ? securitiesTotalUsd.add(accountsTotalUsd) : null;

        AssetsSummaryResponse.ExchangeRateInfo exchangeRateInfo =
                new AssetsSummaryResponse.ExchangeRateInfo(exchangeRate, prevRate, changeAmount, changeRate);

        return new AssetsSummaryResponse(securitiesAsset, accountAssets, cardAssets, exchangeRateInfo, totalUsdBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteIpoItem> getRandomFavoriteIpos(Long userId) {
        List<FavoriteIpoItem> all = ipoExplorationService.getFavoriteIpos(userId, null);
        if (all.isEmpty()) return List.of();

        // 1순위: OPEN/UPCOMING → 마감일 가까운 순
        List<FavoriteIpoItem> result = new ArrayList<>(all.stream()
                .filter(i -> "OPEN".equals(i.ipoStatus()) || "UPCOMING".equals(i.ipoStatus()))
                .sorted(Comparator.comparing(FavoriteIpoItem::subscriptionEndDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(2)
                .toList());

        // 2개 미만이면 CLOSED로 나머지 채움 (최신순)
        if (result.size() < 2) {
            int needed = 2 - result.size();
            all.stream()
                    .filter(i -> "CLOSED".equals(i.ipoStatus()))
                    .sorted(Comparator.comparing(FavoriteIpoItem::subscriptionEndDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(needed)
                    .forEach(result::add);
        }

        return result;
    }

    @Override
    public Object getDashboard(Long userId) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "미구현 기능입니다.");
    }
}