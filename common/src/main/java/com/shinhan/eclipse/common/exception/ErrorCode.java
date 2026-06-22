package com.shinhan.eclipse.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT("C001", "잘못된 입력입니다.", 400),
    NOT_FOUND("C002", "리소스를 찾을 수 없습니다.", 404),
    INTERNAL_ERROR("C003", "서버 내부 오류입니다.", 500),
    UNAUTHORIZED("C004", "인증에 실패했습니다.", 401),

    // Ledger
    INSUFFICIENT_BALANCE("L001", "잔액이 부족합니다.", 422),
    SUBSCRIPTION_CONFLICT("L002", "동시 청약 충돌입니다.", 409),
    SUBSCRIPTION_PERIOD_INVALID("L003", "청약 기간이 아닙니다.", 422),
    SUBSCRIPTION_NOT_FOUND("L004", "청약 정보를 찾을 수 없습니다.", 404),
    ACCOUNT_NOT_LINKED("L005", "연동된 계좌가 없습니다.", 422),
    ACCOUNT_NOT_FOUND("L006", "계좌를 찾을 수 없습니다.", 404),
    ALLOCATION_NOT_FOUND("L007", "배정 결과가 없습니다.", 404),
    RETURN_PLAN_ALREADY_EXISTS("L008", "이미 생성된 리턴 플랜이 있습니다.", 409),
    RETURN_PLAN_NOT_FOUND("L009", "리턴 플랜을 찾을 수 없습니다.", 404),
    RATIO_SUM_INVALID("L010", "비율의 합이 100이 아닙니다.", 422),
    RETURN_PLAN_CONFLICT("L011", "이미 확정된 리턴 플랜입니다.", 409),

    // Service
    AI_ANALYSIS_FAILED("S001", "AI 분석에 실패했습니다.", 503),
    EXTERNAL_API_ERROR("S002", "외부 API 오류입니다.", 502),
    IPO_NOT_FOUND("S003", "IPO 정보를 찾을 수 없습니다.", 404),
    PRODUCT_NOT_FOUND("S004", "종목을 찾을 수 없습니다.", 404),
    INSUFFICIENT_HOLDING("S005", "보유 수량이 부족합니다.", 422),
    INVALID_ORDER("S006", "유효하지 않은 주문입니다.", 400),
    QUOTE_UNAVAILABLE("S007", "현재가를 조회할 수 없습니다. 가격을 직접 입력해 주세요.", 422),

    // Exchange
    EXCHANGE_RATE_UNAVAILABLE("E001", "환율 정보를 조회할 수 없습니다.", 503),
    EXCHANGE_ACCOUNT_NOT_FOUND("E003", "환전 대상 계좌를 찾을 수 없습니다.", 404);

    private final String code;
    private final String message;
    private final int status;
}
