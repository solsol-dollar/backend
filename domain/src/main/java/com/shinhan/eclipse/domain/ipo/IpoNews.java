package com.shinhan.eclipse.domain.ipo;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "ipo_news")
public class IpoNews extends BaseEntity {

    @Column(nullable = false)
    private Long ipoId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String source;

    private LocalDateTime publishedAt;

    @Column(length = 500)
    private String url;

    @Column(nullable = false, length = 10)
    private String phase;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String contentKo;

    @Column(length = 255)
    private String titleKo;

    @Column(columnDefinition = "TEXT")
    private String summary;

    public static IpoNews create(Long ipoId, String title, String source,
                                 LocalDateTime publishedAt, String url, String phase, String content) {
        IpoNews news = new IpoNews();
        news.ipoId = ipoId;
        news.title = title;
        news.source = source;
        news.publishedAt = publishedAt;
        news.url = url;
        news.phase = phase;
        news.content = content;
        return news;
    }

    public void updateTranslation(String titleKo, String summary) {
        this.titleKo = titleKo;
        this.summary = summary;
    }
}
