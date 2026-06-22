package com.shinhan.eclipse.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashGeneratorTest {

    @Test
    void printHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
assertThat(encoder.matches("123456", encoder.encode("123456"))).isTrue();
        assertThat(encoder.matches("654321", encoder.encode("654321"))).isTrue();
        assertThat(encoder.matches("111111", encoder.encode("111111"))).isTrue();
        assertThat(encoder.matches("000000", encoder.encode("123456"))).isFalse();
    }
}