package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductDataInitializerTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductDataInitializer initializer;

    @Test
    void 테이블이_비어있으면_전체_시드를_저장한다() {
        given(productRepository.countActive()).willReturn(0L);
        given(productRepository.existsByTicker(anyString())).willReturn(false);

        initializer.init();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InvestmentProduct>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());

        int expectedSize = ProductSeedCatalog.NASDAQ_100.size() + ProductSeedCatalog.ETF_LIST.size();
        assertThat(captor.getValue()).hasSize(expectedSize);
    }

    @Test
    void 데이터가_이미_존재하면_시드를_건너뛴다() {
        given(productRepository.countActive()).willReturn(10L);

        initializer.init();

        verify(productRepository, never()).saveAll(anyList());
    }

    @Test
    void 이미_존재하는_ticker는_중복_저장하지_않는다() {
        given(productRepository.countActive()).willReturn(0L);
        // 기본적으로 존재하지 않음, AAPL만 이미 존재
        given(productRepository.existsByTicker(anyString())).willReturn(false);
        given(productRepository.existsByTicker("AAPL")).willReturn(true);

        initializer.init();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InvestmentProduct>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());

        boolean hasAapl = captor.getValue().stream()
                .anyMatch(p -> "AAPL".equals(p.getTicker()));
        assertThat(hasAapl).isFalse();
    }
}
