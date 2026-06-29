package com.shinhan.eclipse.service.exchange.market;

import org.springframework.context.ApplicationEvent;

public class LsTokenRefreshedEvent extends ApplicationEvent {

    private final String newToken;

    public LsTokenRefreshedEvent(Object source, String newToken) {
        super(source);
        this.newToken = newToken;
    }

    public String getNewToken() {
        return newToken;
    }
}
