package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
class ProductDataInitializer {

    private final ProductRepository productRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        if (productRepository.countActive() > 0) {
            log.info("investment_products 이미 존재 ({} 건) — 시드 스킵", productRepository.countActive());
            return;
        }

        List<ProductSeedCatalog.Seed> all = Stream.concat(
                Stream.concat(
                        ProductSeedCatalog.NASDAQ_100.stream(),
                        ProductSeedCatalog.SP500_NYSE.stream()
                ),
                ProductSeedCatalog.ETF_LIST.stream()
        ).toList();

        List<InvestmentProduct> products = new ArrayList<>();
        for (ProductSeedCatalog.Seed seed : all) {
            if (productRepository.existsByTicker(seed.ticker())) continue;
            products.add(InvestmentProduct.ofSeed(
                    seed.productType(),
                    seed.ticker(),
                    seed.productName(),
                    seed.exchangeName(),
                    "USD",
                    seed.sector()
            ));
        }

        productRepository.saveAll(products);
        log.info("investment_products 시드 완료: {} 건 (NASDAQ100 + ETF)", products.size());
    }
}
