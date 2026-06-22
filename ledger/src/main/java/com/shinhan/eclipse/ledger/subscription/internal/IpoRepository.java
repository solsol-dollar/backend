package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

interface IpoRepository extends JpaRepository<Ipo, Long> {
    Optional<Ipo> findFirstByIpoStatusOrderBySubscriptionStartDateAsc(String ipoStatus);
}
