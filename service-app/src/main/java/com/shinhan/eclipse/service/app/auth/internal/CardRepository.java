package com.shinhan.eclipse.service.app.auth.internal;

import com.shinhan.eclipse.domain.account.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserIdAndLinkedFalse(Long userId);
}