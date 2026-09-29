package com.pgs.assistant.web;

import com.pgs.assistant.chat.RateLimiter.RateLimitExceededException;
import com.pgs.assistant.gateway.GatewayClient.InvalidApiKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidApiKeyException.class)
    ProblemDetail unauthorized(InvalidApiKeyException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ProblemDetail tooMany(RateLimitExceededException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    ProblemDetail gatewayDown(RestClientException e) {
        log.warn("Gateway call failed: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "The payment gateway is unavailable");
    }

    /** Model provider errors (throttling, credentials, timeouts) must not leak internals to the merchant. */
    @ExceptionHandler(RuntimeException.class)
    ProblemDetail unexpected(RuntimeException e) {
        log.error("Assistant request failed", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "The assistant couldn't answer right now. Please try again in a moment.");
    }
}
