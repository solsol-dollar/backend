package com.shinhan.eclipse.service.securities;

import java.util.List;

public interface SecuritiesService {
    List<ProductListItem>   listProducts(String type, String keyword, String sort, int offset, int limit);
    ProductDetail           getProduct(Long id);
    OrderBookResponse       getOrderBook(Long id);
    HoldingsSummary         getHoldings(Long userId);
    List<RecommendedProduct> getRecommended(Long userId);
    List<RecommendedProduct> getRecommended(Long userId, Long ipoId);
    List<MarketIndex>       getMarketIndices();
    ProductStats            getProductStats(Long id);
    List<RankingItem>       getRanking(String type, int limit, String productType);
}
