package com.shinhan.eclipse.service.inflow.internal;

import com.shinhan.eclipse.domain.ipo.FavoriteIpo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

interface IdleDetectionFavoriteIpoRepository extends JpaRepository<FavoriteIpo, Long> {

    boolean existsByUserId(Long userId);

    @Query("""
            SELECT COUNT(f) > 0 FROM FavoriteIpo f
            JOIN Ipo i ON i.id = f.ipoId
            WHERE f.userId = :userId
              AND i.subscriptionEndDate >= :cutoff
            """)
    boolean existsFavoriteWithSubscriptionEndAfter(@Param("userId") Long userId,
                                                   @Param("cutoff") LocalDate cutoff);
}