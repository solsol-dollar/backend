package com.shinhan.eclipse.service.app.config;

/**
 * LedgerClient 빈은 service 모듈의 LedgerClientConfig (package-private) 에서 등록된다.
 * service-app 은 eclipse.ledger.url 프로퍼티를 application.yml 에 선언하는 것으로 충분하다.
 *
 * 이 클래스는 향후 ledger-app 연동에 대한 추가 설정(타임아웃, 재시도 정책 등)이
 * 필요할 때 사용하기 위해 보존한다.
 */
public final class LedgerClientConfig {
    private LedgerClientConfig() {}
}
