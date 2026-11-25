
package com.logy.pocketpay.config;

import com.logy.pocketpay.security.AccessTokenCookieResolver;
import com.logy.pocketpay.security.AuthRateLimitFilter;
import com.logy.pocketpay.security.JwtAccessDeniedHandler;
import com.logy.pocketpay.security.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            String tokenType = jwt.getClaimAsString("token_type");

            if ("PASSWORD_CHANGE".equals(tokenType)) {
                return List.of(
                        new SimpleGrantedAuthority("PASSWORD_CHANGE")
                );
            }

            String role = jwt.getClaimAsString("role");

            if (role == null) {
                return List.of();
            }

            return List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );
        });

        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AccessTokenCookieResolver accessTokenCookieResolver,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            ObjectMapper objectMapper,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) throws Exception {

        http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/users/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/actuator/*",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/change-password"
                        ).hasAnyAuthority(
                                "ROLE_USER",
                                "ROLE_ADMIN",
                                "PASSWORD_CHANGE"
                        )
                        .anyRequest().hasAnyRole("USER", "ADMIN")
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        .bearerTokenResolver(accessTokenCookieResolver)
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )

                .csrf(csrf -> {

                    csrf.spa();

                    /* Enforce CSRF protection for cookie-based JWT authentication. */
                    csrf.withObjectPostProcessor(
                            new ObjectPostProcessor<CsrfFilter>() {

                                @Override
                                public <O extends CsrfFilter> O postProcess(O filter) {

                                    filter.setRequireCsrfProtectionMatcher(
                                            CsrfFilter.DEFAULT_CSRF_MATCHER
                                    );

                                    return filter;
                                }
                            }
                    );
                })

                .addFilterBefore(
                        new AuthRateLimitFilter(objectMapper),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
