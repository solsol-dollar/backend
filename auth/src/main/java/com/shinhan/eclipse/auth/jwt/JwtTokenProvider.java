package com.shinhan.eclipse.auth.jwt;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.shinhan.eclipse.auth.AuthUser;
import com.shinhan.eclipse.auth.TokenIssuer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

public class JwtTokenProvider implements TokenIssuer {

    private final JwtEncoder encoder;
    private final long expirationMs;

    public JwtTokenProvider(RSAPublicKey publicKey, RSAPrivateKey privateKey, long expirationMs) {
        var jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        this.expirationMs = expirationMs;
    }

    @Override
    public long getExpirationMs() {
        return expirationMs;
    }

    @Override
    public String issue(AuthUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.userId()))
                .claim("name", user.name())
                .claim("role", user.role())
                .issuedAt(now)
                .expiresAt(now.plusMillis(expirationMs))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}