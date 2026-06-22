package com.shinhan.eclipse.service.securities.internal;

import com.shinhan.eclipse.domain.holding.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByUserIdAndProductId(Long userId, Long productId);
    List<Holding> findByUserIdAndStatus(Long userId, String status);
}
