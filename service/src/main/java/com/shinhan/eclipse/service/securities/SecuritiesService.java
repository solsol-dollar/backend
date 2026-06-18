package com.shinhan.eclipse.service.securities;

import java.util.List;

public interface SecuritiesService {
    List<ProductListItem>   listProducts(String type, String keyword);
    ProductDetail           getProduct(Long id);
    OrderBookResponse       getOrderBook(Long id);
    List<HoldingItem>       getHoldings(Long userId);
    List<RecommendedProduct> getRecommended(Long userId);
}
