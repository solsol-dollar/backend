package com.shinhan.eclipse.worker.repository;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpoNewsRepository extends JpaRepository<IpoNews, Long> {
    Optional<IpoNews> findByUrl(String url);
    boolean existsByUrl(String url);
}
