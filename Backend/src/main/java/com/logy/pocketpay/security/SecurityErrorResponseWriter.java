package com.logy.pocketpay.security;

import com.logy.pocketpay.common.response.ApiError;
import com.logy.pocketpay.common.exception.ApiErrorCode;
import com.logy.pocketpay.common.tracing.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final TraceContext traceContext;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            ApiErrorCode code,
            String message
    ) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                traceContext.getTraceId(),
                List.of()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
