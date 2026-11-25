package com.logy.pocketpay.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenCookieResolver implements BearerTokenResolver {

    @Override
    public String resolve(HttpServletRequest request) {

        String path = request.getServletPath();

        if ("/api/v1/auth/refresh".equals(path)
                || "/api/v1/auth/logout".equals(path)
                || "/api/v1/auth/forgot-password".equals(path)
                || "/api/v1/auth/reset-password".equals(path)) {

            return null;
        }

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}