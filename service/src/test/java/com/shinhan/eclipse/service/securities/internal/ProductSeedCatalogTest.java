package com.shinhan.eclipse.service.securities.internal;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSeedCatalogTest {

    @Test
    void NASDAQ_100_목록에_중복_ticker가_없다() {
        List<String> tickers = ProductSeedCatalog.NASDAQ_100.stream()
                .map(ProductSeedCatalog.Seed::ticker)
                .toList();

        Map<String, Long> duplicates = tickers.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates)
                .as("중복 ticker 발견: %s", duplicates)
                .isEmpty();
    }

    @Test
    void ETF_목록에_중복_ticker가_없다() {
        List<String> tickers = ProductSeedCatalog.ETF_LIST.stream()
                .map(ProductSeedCatalog.Seed::ticker)
                .toList();

        Map<String, Long> duplicates = tickers.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates)
                .as("중복 ticker 발견: %s", duplicates)
                .isEmpty();
    }

    @Test
    void NASDAQ_100과_ETF_목록_사이에_중복_ticker가_없다() {
        List<String> all = Stream.concat(
                ProductSeedCatalog.NASDAQ_100.stream(),
                ProductSeedCatalog.ETF_LIST.stream()
        ).map(ProductSeedCatalog.Seed::ticker).toList();

        Map<String, Long> duplicates = all.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates)
                .as("NASDAQ100-ETF 간 중복 ticker: %s", duplicates)
                .isEmpty();
    }

    @Test
    void 모든_seed_항목에_필수_필드가_채워져_있다() {
        Stream.concat(
                ProductSeedCatalog.NASDAQ_100.stream(),
                ProductSeedCatalog.ETF_LIST.stream()
        ).forEach(seed -> {
            assertThat(seed.ticker()).as("%s ticker", seed.ticker()).isNotBlank();
            assertThat(seed.productName()).as("%s productName", seed.ticker()).isNotBlank();
            assertThat(seed.sector()).as("%s sector", seed.ticker()).isNotBlank();
            assertThat(seed.productType()).as("%s productType", seed.ticker())
                    .isIn(ProductSeedCatalog.OVERSEAS, ProductSeedCatalog.ETF);
            assertThat(seed.exchangeName()).as("%s exchangeName", seed.ticker()).isNotBlank();
        });
    }

    @Test
    void NASDAQ_100_종목은_OVERSEAS_타입이다() {
        assertThat(ProductSeedCatalog.NASDAQ_100)
                .allMatch(s -> ProductSeedCatalog.OVERSEAS.equals(s.productType()));
    }

    @Test
    void ETF_목록_종목은_ETF_타입이다() {
        assertThat(ProductSeedCatalog.ETF_LIST)
                .allMatch(s -> ProductSeedCatalog.ETF.equals(s.productType()));
    }
}
