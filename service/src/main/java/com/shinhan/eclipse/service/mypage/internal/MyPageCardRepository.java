package com.shinhan.eclipse.service.mypage.internal;

import com.shinhan.eclipse.domain.account.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface MyPageCardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUserIdAndLinkedTrueAndStatus(Long userId, String status);
    boolean existsByUserIdAndStatus(Long userId, String status);
    Optional<Card> findByUserIdAndStatus(Long userId, String status);
}