package com.shinhan.eclipse.domain.account;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "cards")
public class Card extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    private Long linkedAccountId;

    @Column(nullable = false, length = 30)
    private String cardType = "CHECK_CARD";

    @Column(nullable = false, length = 30)
    private String cardStatus = "UNISSUED";

    @Column(length = 100)
    private String cardName;

    @Column(length = 50)
    private String cardNumberMasked;

    @Column(length = 50)
    private String issuerName;

    @Column(nullable = false)
    private Boolean linked;

    private LocalDateTime linkedAt;
}
