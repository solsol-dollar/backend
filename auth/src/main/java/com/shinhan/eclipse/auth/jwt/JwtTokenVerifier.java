package com.shinhan.eclipse.auth.jwt;

import com.shinhan.eclipse.auth.AuthUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPublicKey;

public class JwtTokenVerifier {

    private final JwtDecoder decoder;

    public JwtTokenVerifier(RSAPublicKey publicKey) {
        this.decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    public AuthUser verify(String token) {
        Jwt jwt = decoder.decode(token);
        return new AuthUser(
                Long.parseLong(jwt.getSubject()),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("role")
        );
    }
}