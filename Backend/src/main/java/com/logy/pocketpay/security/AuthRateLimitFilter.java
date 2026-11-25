package com.logy.pocketpay.security;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import com.logy.pocketpay.common.exception.ApiErrorCode;
import com.logy.pocketpay.common.response.ApiError;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.time.Instant;
import java.util.List;

@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private record RateLimitPolicy(
            int capacity,
            Duration refillPeriod
    ) {
    }

    private static final Map<String, RateLimitPolicy> POLICIES = Map.of(
            "/api/v1/auth/login",
            new RateLimitPolicy(10, Duration.ofMinutes(1)),

            "/api/v1/users/register",
            new RateLimitPolicy(5, Duration.ofHours(1)),

            "/api/v1/auth/refresh",
            new RateLimitPolicy(10, Duration.ofMinutes(1)),

            "/api/v1/auth/forgot-password",
            new RateLimitPolicy(3, Duration.ofMinutes(15)),

            "/api/v1/auth/reset-password",
            new RateLimitPolicy(5, Duration.ofMinutes(15)),

            "/api/v1/auth/change-password",
            new RateLimitPolicy(5, Duration.ofMinutes(15))
    );

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    private Bucket createBucket(RateLimitPolicy policy) {

        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.capacity())
                .refillGreedy(
                        policy.capacity(),
                        policy.refillPeriod()
                )
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        RateLimitPolicy policy = POLICIES.get(request.getServletPath());

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || policy == null) {

            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getRemoteAddr();
        String cacheKey = request.getServletPath() + ":" + clientIp;

        Bucket bucket = buckets.get(
                cacheKey,
                key -> createBucket(policy)
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {

            long retryAfterSeconds = Math.max(
                    1,
                    (long) Math.ceil(
                            probe.getNanosToWaitForRefill() / 1_000_000_000.0
                    )
            );

            log.warn(
                    "Rate limit exceeded: method={} path={} clientIp={} retryAfterSeconds={}",
                    request.getMethod(),
                    request.getServletPath(),
                    clientIp,
                    retryAfterSeconds
            );

            int status = HttpStatus.TOO_MANY_REQUESTS.value();

            ApiError apiError = new ApiError(
                    Instant.now(),
                    status,
                    ApiErrorCode.RATE_LIMIT_EXCEEDED,
                    "Too many requests. Please try again later.",
                    request.getRequestURI(),
                    org.slf4j.MDC.get("traceId"),
                    List.of()
            );

            response.setStatus(status);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            objectMapper.writeValue(response.getOutputStream(), apiError);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
