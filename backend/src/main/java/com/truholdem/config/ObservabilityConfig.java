package com.truholdem.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.semconv.ResourceAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@Configuration
@ConditionalOnProperty(name = "otel.enabled", havingValue = "true", matchIfMissing = true)
public class ObservabilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

    @Value("${otel.service.name:truholdem-backend}")
    private String serviceName;

    @Value("${otel.service.version:1.0.0}")
    private String serviceVersion;

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Value("${otel.exporter.otlp.timeout:10000}")
    private long exporterTimeout;

    @Value("${otel.metrics.export.interval:60000}")
    private long metricsExportInterval;

    
    @Bean
    public Resource otelResource() {
        return Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, serviceName)
                        .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
                        .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, 
                             System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "development"))
                        .put("service.namespace", "truholdem")
                        .put("service.instance.id", java.util.UUID.randomUUID().toString())
                        .build()));
    }

    
    @Bean
    public OtlpGrpcSpanExporter otlpSpanExporter() {
        logger.info("Configuring OTLP span exporter with endpoint: {}", otlpEndpoint);
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofMillis(exporterTimeout))
                .build();
    }

    
    @Bean
    public OtlpGrpcMetricExporter otlpMetricExporter() {
        logger.info("Configuring OTLP metric exporter with endpoint: {}", otlpEndpoint);
        return OtlpGrpcMetricExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setTimeout(Duration.ofMillis(exporterTimeout))
                .build();
    }

    
    @Bean
    public SdkTracerProvider tracerProvider(Resource resource, OtlpGrpcSpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .setScheduleDelay(Duration.ofMillis(5000))
                        .setExporterTimeout(Duration.ofMillis(exporterTimeout))
                        .build())
                .build();
    }

    
    @Bean
    public SdkMeterProvider meterProvider(Resource resource, OtlpGrpcMetricExporter metricExporter) {
        return SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                        .setInterval(Duration.ofMillis(metricsExportInterval))
                        .build())
                .build();
    }

    
    @Bean
    public OpenTelemetry openTelemetry(SdkTracerProvider tracerProvider, SdkMeterProvider meterProvider) {
        logger.info("Initializing OpenTelemetry for service: {}", serviceName);
        
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down OpenTelemetry...");
            tracerProvider.close();
            meterProvider.close();
        }));

        return sdk;
    }

    
    @Bean
    public Tracer otelTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("com.truholdem.game");
    }

    
    @Bean
    public Meter gameMeter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter("com.truholdem.game");
    }
}
