package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.IpoScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface IpoScoreRepository extends JpaRepository<IpoScore, Long> {
    Optional<IpoScore> findByIpoId(Long ipoId);
}
