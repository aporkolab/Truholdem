package com.truholdem.config.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    
    
    private static final String V1_PREFIX = "/v1";
    private static final String V2_PREFIX = "/v2";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(V1_PREFIX,
            HandlerTypePredicate.forAnnotation(ApiV1Config.class));
        configurer.addPathPrefix(V2_PREFIX,
            HandlerTypePredicate.forAnnotation(ApiV2Config.class));
    }
}
