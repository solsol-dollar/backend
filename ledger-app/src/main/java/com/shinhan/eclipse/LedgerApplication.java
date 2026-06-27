package com.shinhan.eclipse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = {
                "com.shinhan.eclipse.ledger",
                "com.shinhan.eclipse.common",
                "com.shinhan.eclipse.auth"
        },
        exclude = UserDetailsServiceAutoConfiguration.class
)
@EnableJpaAuditing
@EnableScheduling
public class LedgerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }

}
