package com.shinhan.eclipse.service.ipo.internal;

public record InternalApiEnvelope<T>(String code, String message, T data) {
}
