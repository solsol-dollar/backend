package com.shinhan.eclipse.worker.candle.repository;

import com.shinhan.eclipse.domain.product.InvestmentProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkerProductRepository extends JpaRepository<InvestmentProduct, Long> {

    List<InvestmentProduct> findByStatus(String status);
}
