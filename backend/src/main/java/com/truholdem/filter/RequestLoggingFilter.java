package com.truholdem.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    private static final int MAX_PAYLOAD_LENGTH = 1000;

    private static final int MAX_CACHE_SIZE = 50 * 1024 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isActuatorEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String correlationId = getOrCreateCorrelationId(request);
        MDC.put("correlationId", correlationId);
        MDC.put("requestUri", request.getRequestURI());
        MDC.put("requestMethod", request.getMethod());

        
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, MAX_CACHE_SIZE);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Instant startTime = Instant.now();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();

            logRequest(requestWrapper, responseWrapper, durationMs, correlationId);

            responseWrapper.copyBodyToResponse();

            MDC.clear();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            long durationMs,
                            String correlationId) {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        int status = response.getStatus();
        String clientIp = getClientIp(request);

        logger.info("HTTP {} {} - {} - {}ms [correlationId={}]",
                method,
                queryString != null ? uri + "?" + queryString : uri,
                status,
                durationMs,
                correlationId);

        if (isGameAction(uri)) {
            String requestBody = getRequestBody(request);
            auditLogger.info("GAME_ACTION: {} {} - status={} - duration={}ms - ip={} - correlationId={} - body={}",
                    method, uri, status, durationMs, clientIp, correlationId,
                    truncate(requestBody, MAX_PAYLOAD_LENGTH));
        }

        if (durationMs > 1000) {
            logger.warn("Slow request detected: {} {} took {}ms", method, uri, durationMs);
        }

        if (status >= 400) {
            String responseBody = getResponseBody(response);
            logger.warn("Request failed: {} {} - {} - body: {}",
                    method, uri, status, truncate(responseBody, MAX_PAYLOAD_LENGTH));
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }
        return correlationId;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isActuatorEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/api/actuator");
    }

    private boolean isGameAction(String uri) {
        return uri.contains("/poker/game") &&
                (uri.contains("/action") || uri.contains("/start") || uri.contains("/new-hand"));
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return "";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...[truncated]";
    }
}