package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

interface IpoRepository extends JpaRepository<Ipo, Long> {

    @Query("SELECT i.ticker FROM Ipo i WHERE i.ticker IN :tickers")
    Set<String> findExistingTickers(@Param("tickers") List<String> tickers);

    @Query("SELECT i FROM Ipo i WHERE i.listingDate IS NOT NULL AND i.listingDate <= CURRENT_DATE AND i.status = 'ACTIVE'")
    List<Ipo> findListedIpos();

    @Query(value = """
        SELECT i FROM Ipo i
        WHERE i.status = 'ACTIVE'
        AND (
            :status IS NULL
            OR (:status = 'UPCOMING' AND i.subscriptionStartDate IS NOT NULL AND CURRENT_DATE < i.subscriptionStartDate)
            OR (:status = 'OPEN'     AND i.subscriptionStartDate IS NOT NULL AND i.subscriptionEndDate IS NOT NULL
                AND CURRENT_DATE >= i.subscriptionStartDate AND CURRENT_DATE <= i.subscriptionEndDate)
            OR (:status = 'CLOSED'   AND i.listingDate IS NOT NULL AND CURRENT_DATE >= i.listingDate)
        )
        AND (
            :favoriteOnly = false
            OR EXISTS (SELECT f FROM FavoriteIpo f WHERE f.ipoId = i.id AND f.userId = :userId)
        )
        ORDER BY i.listingDate ASC
        """,
        countQuery = """
        SELECT COUNT(i) FROM Ipo i
        WHERE i.status = 'ACTIVE'
        AND (
            :status IS NULL
            OR (:status = 'UPCOMING' AND i.subscriptionStartDate IS NOT NULL AND CURRENT_DATE < i.subscriptionStartDate)
            OR (:status = 'OPEN'     AND i.subscriptionStartDate IS NOT NULL AND i.subscriptionEndDate IS NOT NULL
                AND CURRENT_DATE >= i.subscriptionStartDate AND CURRENT_DATE <= i.subscriptionEndDate)
            OR (:status = 'CLOSED'   AND i.listingDate IS NOT NULL AND CURRENT_DATE >= i.listingDate)
        )
        AND (
            :favoriteOnly = false
            OR EXISTS (SELECT f FROM FavoriteIpo f WHERE f.ipoId = i.id AND f.userId = :userId)
        )
        """)
    Page<Ipo> findWithFilters(
            @Param("status") String status,
            @Param("favoriteOnly") boolean favoriteOnly,
            @Param("userId") Long userId,
            Pageable pageable
    );
}
