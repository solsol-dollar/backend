package com.shinhan.eclipse.service.home.internal;

import com.shinhan.eclipse.domain.account.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AssetCardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserIdAndLinkedTrueAndStatus(Long userId, String status);
}