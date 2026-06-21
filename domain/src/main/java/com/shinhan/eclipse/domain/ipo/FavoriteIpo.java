package com.shinhan.eclipse.domain.ipo;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "favorite_ipos",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ipo_id"}))
public class FavoriteIpo extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long ipoId;

    public static FavoriteIpo create(Long userId, Long ipoId) {
        FavoriteIpo f = new FavoriteIpo();
        f.userId = userId;
        f.ipoId = ipoId;
        return f;
    }
}
