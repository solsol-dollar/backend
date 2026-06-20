package com.shinhan.eclipse.auth;

public interface TokenIssuer {
    String issue(AuthUser authUser);
    long getExpirationMs();
}
