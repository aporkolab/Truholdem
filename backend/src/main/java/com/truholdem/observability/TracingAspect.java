package com.truholdem.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Aspect
@Component
@Order(1)
@ConditionalOnBean(Tracer.class)
public class TracingAspect {

    private static final Logger logger = LoggerFactory.getLogger(TracingAspect.class);

    private final Tracer tracer;
    private final GameMetrics gameMetrics;

    public TracingAspect(Tracer tracer, GameMetrics gameMetrics) {
        this.tracer = tracer;
        this.gameMetrics = gameMetrics;
        logger.info("TracingAspect initialized for automatic instrumentation");
    }

    

    
    @Pointcut("execution(* com.truholdem.service.*Service.*(..))")
    public void serviceMethods() {}

    
    @Pointcut("execution(* com.truholdem.service.PokerGameService.*(..))")
    public void pokerGameServiceMethods() {}

    
    @Pointcut("execution(* com.truholdem.controller.*Controller.*(..))")
    public void controllerMethods() {}

    
    @Pointcut("execution(* com.truholdem.service.AdvancedBotAIService.*(..))")
    public void botAiMethods() {}

    

    
    @Around("serviceMethods()")
    public Object traceServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String spanName = className + "." + methodName;

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("code.function", methodName)
                .setAttribute("code.namespace", signature.getDeclaringType().getName())
                .startSpan();

        long startTime = System.nanoTime();
        
        try (Scope scope = span.makeCurrent()) {
            
            extractGameAttributes(joinPoint, span);
            
            Object result = joinPoint.proceed();
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            span.setAttribute("duration_ms", durationMs);
            span.end();
            
            logger.debug("Traced {}: {}ms", spanName, durationMs);
        }
    }

    
    @Around("pokerGameServiceMethods()")
    public Object tracePokerGameServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();

        Span span = tracer.spanBuilder("poker.game." + methodName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("component", "poker-game-service")
                .startSpan();

        long startTime = System.nanoTime();
        
        try (Scope scope = span.makeCurrent()) {
            extractGameAttributes(joinPoint, span);
            
            Object result = joinPoint.proceed();
            
            
            recordPokerGameMetrics(methodName, startTime);
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            span.setAttribute("error.type", e.getClass().getSimpleName());
            throw e;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            span.setAttribute("duration_ms", durationMs);
            span.end();
        }
    }

    
    @Around("controllerMethods()")
    public Object traceControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Span span = tracer.spanBuilder("http." + className + "." + methodName)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.route", className + "/" + methodName)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    
    @Around("botAiMethods()")
    public Object traceBotAiMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();

        Span span = tracer.spanBuilder("bot.ai." + methodName)
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute("component", "bot-ai")
                .startSpan();

        long startTime = System.nanoTime();
        
        try (Scope scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            
            
            if ("decide".equals(methodName)) {
                gameMetrics.recordBotDecision("advanced", durationMs, 
                        result != null ? result.toString() : "null");
            }
            
            span.setAttribute("thinking_time_ms", durationMs);
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    

    
    private void extractGameAttributes(ProceedingJoinPoint joinPoint, Span span) {
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        
        if (args == null || paramNames == null) {
            return;
        }

        for (int i = 0; i < args.length && i < paramNames.length; i++) {
            Object arg = args[i];
            String paramName = paramNames[i];
            
            if (arg instanceof UUID uuid) {
                if (paramName.toLowerCase().contains("game")) {
                    span.setAttribute("game.id", uuid.toString());
                } else if (paramName.toLowerCase().contains("player")) {
                    span.setAttribute("player.id", uuid.toString());
                } else {
                    span.setAttribute("param." + paramName, uuid.toString());
                }
            } else if (arg instanceof String str && !str.isEmpty()) {
                
                if (!paramName.toLowerCase().contains("password") && 
                    !paramName.toLowerCase().contains("token")) {
                    span.setAttribute("param." + paramName, str);
                }
            } else if (arg instanceof Number num) {
                span.setAttribute("param." + paramName, num.longValue());
            }
        }
    }

    
    private void recordPokerGameMetrics(String methodName, long startTimeNanos) {
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        
        switch (methodName) {
            case "createNewGame" -> gameMetrics.recordGameCreationTime(durationMs, 0);
            case "playerAct" -> gameMetrics.recordActionProcessingTime(durationMs, "player_action");
            case "executeBotAction" -> gameMetrics.recordActionProcessingTime(durationMs, "bot_action");
            default -> logger.trace("No specific metrics for method: {}", methodName);
        }
    }
}
