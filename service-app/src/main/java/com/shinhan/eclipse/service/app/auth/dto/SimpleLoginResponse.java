package com.shinhan.eclipse.service.app.auth.dto;

public record SimpleLoginResponse(String accessToken, String tokenType, long expiresIn) {}