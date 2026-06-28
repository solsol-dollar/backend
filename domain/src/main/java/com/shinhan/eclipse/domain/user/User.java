package com.shinhan.eclipse.domain.user;

import com.shinhan.eclipse.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 30)
    private String onboardingStatus = "REQUIRED";

    @Column(nullable = false, length = 30)
    private String investmentStatus = "REQUIRED";

    @Column(nullable = false, length = 255)
    private String simplePassword;

    public void completeOnboarding() {
        this.onboardingStatus = "COMPLETED";
    }

    public void completeInvestmentDiagnosis() {
        this.investmentStatus = "COMPLETED";
    }
}
