package com.shinhan.eclipse.worker.ipo.repository;

import com.shinhan.eclipse.domain.ipo.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IpoNewsIpoRepository extends JpaRepository<Ipo, Long> {

    List<Ipo> findByStatus(String status);

    @Modifying
    @Query(value = """
            UPDATE ipos i
            SET i.status = 'INACTIVE', i.updated_at = NOW()
            WHERE i.status = 'ACTIVE'
              AND i.confirmed_offer_price IS NOT NULL
              AND i.listing_date <= CURDATE()
              AND (
                  (SELECT COUNT(*) FROM ipo_news n WHERE n.ipo_id = i.id AND n.phase = 'PRE') <= 3
                  OR
                  (SELECT COUNT(*) FROM ipo_news n WHERE n.ipo_id = i.id AND n.phase = 'POST') <= 3
              )
            """, nativeQuery = true)
    int deactivateIposWithInsufficientNews();
}
