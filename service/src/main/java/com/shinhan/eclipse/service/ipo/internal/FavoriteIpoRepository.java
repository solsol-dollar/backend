package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.FavoriteIpo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface FavoriteIpoRepository extends JpaRepository<FavoriteIpo, Long> {
    List<FavoriteIpo> findByUserId(Long userId);
    Optional<FavoriteIpo> findByUserIdAndIpoId(Long userId, Long ipoId);
}
