package com.shinhan.eclipse.service.securities.internal;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Component
class LedgerFeignInterceptor implements RequestInterceptor {

    private static final String COOKIE_NAME = "access_token";

    @Override
    public void apply(RequestTemplate template) {
        String token = extractTokenFromCurrentRequest();
        if (token != null) {
            template.header("Authorization", "Bearer " + token);
        }
    }

    private String extractTokenFromCurrentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) return null;

        HttpServletRequest request = servletAttrs.getRequest();
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}