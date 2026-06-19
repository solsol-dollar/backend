package com.shinhan.eclipse.auth.config;

import com.shinhan.eclipse.auth.filter.JwtAuthenticationFilter;
import com.shinhan.eclipse.auth.jwt.JwtProperties;
import com.shinhan.eclipse.auth.jwt.JwtTokenProvider;
import com.shinhan.eclipse.auth.jwt.JwtTokenVerifier;
import com.shinhan.eclipse.auth.jwt.RsaKeyLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties props;
    private final ResourceLoader resourceLoader;

    @Bean
    public JwtTokenVerifier jwtTokenVerifier() throws Exception {
        return new JwtTokenVerifier(
                RsaKeyLoader.loadPublicKey(resourceLoader, props.getPublicKey()));
    }

    @Bean
    @ConditionalOnProperty(name = "jwt.provider-enabled", havingValue = "true")
    public JwtTokenProvider jwtTokenProvider() throws Exception {
        return new JwtTokenProvider(
                RsaKeyLoader.loadPublicKey(resourceLoader, props.getPublicKey()),
                RsaKeyLoader.loadPrivateKey(resourceLoader, props.getPrivateKey()),
                props.getExpirationMs());
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        return new JwtAuthenticationFilter(jwtTokenVerifier());
    }
}