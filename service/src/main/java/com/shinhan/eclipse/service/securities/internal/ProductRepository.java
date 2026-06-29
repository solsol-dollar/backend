package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface ProductRepository extends JpaRepository<InvestmentProduct, Long> {

    List<InvestmentProduct> findByProductTypeAndStatus(String productType, String status);

    List<InvestmentProduct> findByStatus(String status);

    @Query("SELECT p FROM InvestmentProduct p WHERE p.status = 'ACTIVE' " +
           "AND (:type IS NULL OR p.productType = :type) " +
           "AND (:keyword IS NULL OR LOWER(p.ticker) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<InvestmentProduct> searchProducts(@Param("type") String type,
                                           @Param("keyword") String keyword);

    Optional<InvestmentProduct> findByTickerAndStatus(String ticker, String status);

    List<InvestmentProduct> findByTickerInAndStatus(List<String> tickers, String status);

    boolean existsByTicker(String ticker);

    @Query("SELECT COUNT(p) FROM InvestmentProduct p WHERE p.status = 'ACTIVE'")
    long countActive();
}
