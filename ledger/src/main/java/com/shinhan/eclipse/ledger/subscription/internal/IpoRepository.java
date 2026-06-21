package com.shinhan.eclipse.ledger.subscription.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;

interface IpoRepository extends JpaRepository<Ipo, Long> {
}
