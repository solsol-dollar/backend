package com.shinhan.eclipse.service.home.internal;

import com.shinhan.eclipse.common.exception.BusinessException;
import com.shinhan.eclipse.common.exception.ErrorCode;
import com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo;
import lombok.extern.slf4j.Slf4j;
import com.shinhan.eclipse.domain.account.Card;
import com.shinhan.eclipse.domain.account.FinancialAccount;
import com.shinhan.eclipse.service.exchange.ExchangeService;
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
    private final ExchangeService exchangeService;
    private final IpoExplorationService ipoExplorationService;

    @Override
    @Transactional(readOnly = true)
    public AssetsSummaryResponse getAssets(Long userId) {
        List<FinancialAccount> accounts = accountRepository.findByUserIdAndLinkedTrueAndStatus(userId, "ACTIVE");
        List<Card> cards = cardRepository.findByUserIdAndLinkedTrueAndStatus(userId, "ACTIVE");

        BigDecimal exchangeRate = null;
        try {
            ExchangeRateInfo rateInfo = exchangeService.getExchangeRate("USD");
            BigDecimal rate = rateInfo.baseRate();
            if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                exchangeRate = rate;
            }
        } catch (Exception e) {
            log.warn("[홈] 환율 조회 실패 — 환율 null 처리: {}", e.getMessage());
        }

        BigDecimal prevRate = null;
        try {
            prevRate = exchangeService.getPreviousExchangeRate("USD")
                    .map(com.shinhan.eclipse.common.redis.exchange.ExchangeRateInfo::baseRate)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[홈] 전날 환율 조회 실패: {}", e.getMessage());
        }
        BigDecimal changeAmount = (exchangeRate != null && prevRate != null) ? exchangeRate.subtract(prevRate) : null;
        BigDecimal changeRate = (changeAmount != null && prevRate != null && prevRate.compareTo(BigDecimal.ZERO) > 0)
                ? changeAmount.divide(prevRate, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : null;

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

        // KRW → USD 환산 (환율 없으면 null — 환산 불가 상태 명시)
        BigDecimal krwInUsd = (exchangeRate != null)
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

        // 카드
        List<AssetsSummaryResponse.CardAsset> cardAssets = cards.stream()
                .map(c -> new AssetsSummaryResponse.CardAsset(c.getCardName(), c.getCardNumberMasked(), c.getIssuerName()))
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