package com.truholdem.filter;

import com.truholdem.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;


@Component
@Order(1)
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> buckets;

    public RateLimitFilter(Map<String, Bucket> buckets) {
        this.buckets = buckets;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        
        if (isExcludedPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        String path = request.getRequestURI();
        
        Bucket bucket = resolveBucket(clientKey, path);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            
            logger.warn("Rate limit exceeded for client: {} on path: {}", clientKey, path);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitTimeSeconds));
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"retryAfterSeconds\":" + waitTimeSeconds + "}"
            );
        }
    }

    private String resolveClientKey(HttpServletRequest request) {
        
        String user = request.getRemoteUser();
        if (user != null) {
            return "user:" + user;
        }
        
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private Bucket resolveBucket(String clientKey, String path) {
        String bucketKey = clientKey + ":" + getBucketType(path);
        
        return buckets.computeIfAbsent(bucketKey, key -> {
            if (path.contains("/auth")) {
                return RateLimitConfig.createAuthBucket();
            } else if (path.contains("/start") || path.contains("/new")) {
                return RateLimitConfig.createGameCreationBucket();
            } else if (path.contains("/action")) {
                return RateLimitConfig.createActionBucket();
            } else {
                return RateLimitConfig.createStandardBucket();
            }
        });
    }

    private String getBucketType(String path) {
        if (path.contains("/auth")) return "auth";
        if (path.contains("/start") || path.contains("/new")) return "creation";
        if (path.contains("/action")) return "action";
        return "standard";
    }

    private boolean isExcludedPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || 
               path.startsWith("/api/actuator") ||
               path.equals("/health") ||
               path.contains("/swagger") ||
               path.contains("/api-docs");
    }
}
