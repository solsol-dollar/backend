package com.shinhan.eclipse.service.ai.internal;

import com.shinhan.eclipse.domain.ipo.IpoRiskScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface IpoRiskScoreRepository extends JpaRepository<IpoRiskScore, Long> {
    Optional<IpoRiskScore> findTopByIpoIdOrderByCreatedAtDesc(Long ipoId);
}
