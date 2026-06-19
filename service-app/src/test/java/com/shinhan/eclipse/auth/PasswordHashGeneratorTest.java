package com.shinhan.eclipse.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashGeneratorTest {

    @Test
    void printHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("김하늘 (123456): " + encoder.encode("123456"));
        System.out.println("이서준 (654321): " + encoder.encode("654321"));
        System.out.println("박지민 (111111): " + encoder.encode("111111"));
    }
}
