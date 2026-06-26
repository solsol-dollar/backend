package com.shinhan.eclipse.service.ipo.internal;

record InternalApiEnvelope<T>(String code, String message, T data) {
}
