package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.domain.user.InvestmentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface InvestmentProfileRepository extends JpaRepository<InvestmentProfile, Long> {
    Optional<InvestmentProfile> findTopByUserIdOrderByDiagnosedAtDesc(Long userId);
}
