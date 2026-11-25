package com.logy.pocketpay.common.response;

import com.logy.pocketpay.common.exception.ApiErrorCode;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        ApiErrorCode code,
        String message,
        String path,
        String traceId,
        List<FieldError> fieldErrors
) {
}