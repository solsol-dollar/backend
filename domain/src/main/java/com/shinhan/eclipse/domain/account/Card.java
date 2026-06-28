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

    public static Card issue(Long userId, Long linkedAccountId) {
        Card c = new Card();
        c.userId = userId;
        c.linkedAccountId = linkedAccountId;
        c.cardType = "CHECK_CARD";
        c.cardStatus = "LINKED";
        c.cardName = "신한 체인지업 체크카드";
        c.cardNumberMasked = generateMaskedNumber();
        c.issuerName = "신한카드";
        c.linked = true;
        c.linkedAt = LocalDateTime.now();
        return c;
    }

    public void link() {
        this.linked = true;
        this.linkedAt = LocalDateTime.now();
    }

    private static String generateMaskedNumber() {
        int suffix = (int) (Math.random() * 9000) + 1000;
        return "****-****-****-" + suffix;
    }
}
