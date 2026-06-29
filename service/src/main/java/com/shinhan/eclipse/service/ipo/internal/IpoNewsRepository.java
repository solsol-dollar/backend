package com.shinhan.eclipse.service.ipo.internal;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface IpoNewsRepository extends JpaRepository<IpoNews, Long> {
    List<IpoNews> findByIpoIdOrderByPublishedAtDesc(Long ipoId, Pageable pageable);
    java.util.Optional<IpoNews> findByIdAndIpoId(Long id, Long ipoId);
}
