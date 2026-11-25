package com.logy.pocketpay.common.exception;

import com.logy.pocketpay.auth.exception.*;
import com.logy.pocketpay.auth.service.AuthCookieService;
import com.logy.pocketpay.common.response.ApiError;
import com.logy.pocketpay.common.response.FieldError;
import com.logy.pocketpay.common.tracing.TraceContext;
import com.logy.pocketpay.user.exception.AccountNotActiveException;
import com.logy.pocketpay.user.exception.EmailAlreadyRegisteredException;
import com.logy.pocketpay.user.exception.PhoneNumberAlreadyRegisteredException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final TraceContext traceContext;
    private final AuthCookieService authCookieService;


    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.USER_EMAIL_ALREADY_REGISTERED,
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(PhoneNumberAlreadyRegisteredException.class)
    public ResponseEntity<ApiError> handlePhoneNumberAlreadyRegistered(
            PhoneNumberAlreadyRegisteredException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.USER_PHONE_ALREADY_REGISTERED,
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiError> handleAccountNotActive(
            AccountNotActiveException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.AUTH_ACCESS_DENIED,
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.AUTH_INVALID_CREDENTIALS,
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiError> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request
    ) {
        ResponseEntity<ApiError> errorResponse = buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.AUTH_INVALID_CREDENTIALS,
                exception.getMessage(),
                request,
                List.of()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        authCookieService.clearAccessTokenCookie(),
                        authCookieService.clearRefreshTokenCookie()
                )
                .body(errorResponse.getBody());
    }

    @ExceptionHandler(ActiveSessionExistsException.class)
    public ResponseEntity<ApiError> handleActiveSessionExists(
            ActiveSessionExistsException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.AUTH_ACTIVE_SESSION_EXISTS,
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ApiError> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.AUTH_INVALID_PASSWORD_RESET_TOKEN,
                exception.getMessage(),
                request,
                null
        );
    }

    @ExceptionHandler(PasswordUnchangedException.class)
    public ResponseEntity<ApiError> handlePasswordUnchanged(
            PasswordUnchangedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.AUTH_PASSWORD_UNCHANGED,
                exception.getMessage(),
                request,
                null
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        List<FieldError> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_FAILED,
                "One or more fields contain invalid values.",
                request,
                fieldErrors
        );
    }


    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ApiError> handleEndpointNotFound(
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.RESOURCE_NOT_FOUND,
                "The requested endpoint does not exist.",
                request,
                List.of()
        );
    }


    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {

        ResponseEntity<ApiError> errorResponse = buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                ApiErrorCode.METHOD_NOT_ALLOWED,
                "The HTTP method is not supported for this endpoint.",
                request,
                List.of()
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(exception.getHeaders())
                .body(errorResponse.getBody());
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequestBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.MALFORMED_REQUEST_BODY,
                "The request body is missing, malformed or contains invalid values.",
                request,
                List.of()
        );
    }




    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unhandled exception for method={} path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request,
                null
        );
    }


    private ResponseEntity<ApiError> buildErrorResponse(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request,
            List<FieldError> fieldErrors
    ) {

        String traceId = traceContext.getTraceId();

        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                traceId,
                fieldErrors
        );

        return ResponseEntity
                .status(status)
                .body(apiError);
    }
}