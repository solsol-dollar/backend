package com.shinhan.eclipse.ledger.accountlink.internal;

import com.shinhan.eclipse.domain.account.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserId(Long userId);
}
