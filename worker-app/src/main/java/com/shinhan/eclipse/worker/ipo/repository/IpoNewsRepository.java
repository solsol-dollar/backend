package com.shinhan.eclipse.worker.ipo.repository;

import com.shinhan.eclipse.domain.ipo.IpoNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IpoNewsRepository extends JpaRepository<IpoNews, Long> {

    @Query("SELECT MAX(n.publishedAt) FROM IpoNews n WHERE n.ipoId = :ipoId")
    Optional<LocalDateTime> findMaxPublishedAt(@Param("ipoId") Long ipoId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO ipo_news
                (ipo_id, title, source, published_at, url, phase, content, created_at, updated_at, status)
            VALUES
                (:ipoId, :title, :source, :publishedAt, :url, :phase, :content, NOW(), NOW(), 'ACTIVE')
            """, nativeQuery = true)
    int insertIgnore(@Param("ipoId") Long ipoId,
                     @Param("title") String title,
                     @Param("source") String source,
                     @Param("publishedAt") LocalDateTime publishedAt,
                     @Param("url") String url,
                     @Param("phase") String phase,
                     @Param("content") String content);

    @Query(value = """
            SELECT * FROM ipo_news WHERE id IN (
                SELECT id FROM (
                    SELECT id,
                           ROW_NUMBER() OVER (PARTITION BY ipo_id ORDER BY published_at DESC) AS rn
                    FROM ipo_news
                    WHERE title_ko IS NULL AND content IS NOT NULL AND status = 'ACTIVE'
                      AND ipo_id IN (SELECT id FROM ipos WHERE status = 'ACTIVE')
                ) ranked WHERE rn <= 3
            )
            """, nativeQuery = true)
    List<IpoNews> findAllForTranslation();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE ipo_news SET title_ko = :titleKo, summary = :summary, updated_at = NOW() WHERE id = :id",
           nativeQuery = true)
    int updateTranslation(@Param("id") Long id, @Param("titleKo") String titleKo, @Param("summary") String summary);
}
