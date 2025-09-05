package com.truholdem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class OpenApiConfig {

    @Value("${app.version:2.0.0}")
    private String appVersion;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    
    @Bean
    public OpenAPI truholdemOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .externalDocs(externalDocs())
            .servers(serverList())
            .components(securityComponents())
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    
    @Bean
    public GroupedOpenApi gameApi() {
        return GroupedOpenApi.builder()
            .group("1-game")
            .displayName("🎰 Game API")
            .pathsToMatch("/v1/poker/**", "/v2/poker/**", "/poker/**")
            .packagesToScan("com.truholdem.controller")
            .build();
    }

    
    @Bean
    public GroupedOpenApi tournamentApi() {
        return GroupedOpenApi.builder()
            .group("2-tournament")
            .displayName("🏆 Tournament API")
            .pathsToMatch("/tournaments/**", "/v1/tournaments/**", "/v2/tournaments/**")
            .packagesToScan("com.truholdem.controller")
            .build();
    }

    
    @Bean
    public GroupedOpenApi analysisApi() {
        return GroupedOpenApi.builder()
            .group("3-analysis")
            .displayName("📊 Analysis API")
            .pathsToMatch("/v1/analysis/**", "/v2/analysis/**")
            .packagesToScan("com.truholdem.controller")
            .build();
    }

    
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
            .group("4-auth")
            .displayName("🔐 Auth & User API")
            .pathsToMatch("/auth/**", "/users/**", "/v1/auth/**", "/v1/users/**")
            .packagesToScan("com.truholdem.controller")
            .build();
    }

    
    @Bean
    public GroupedOpenApi statisticsApi() {
        return GroupedOpenApi.builder()
            .group("5-statistics")
            .displayName("📈 Statistics & History API")
            .pathsToMatch("/statistics/**", "/history/**", 
                         "/v1/statistics/**", "/v1/history/**")
            .packagesToScan("com.truholdem.controller")
            .build();
    }

    private Info apiInfo() {
        return new Info()
            .title("TruHoldem Poker API")
            .version(appVersion)
            .description("""
                ## Texas Hold'em Poker Platform API
                
                A comprehensive poker platform featuring:
                
                ### 🎮 Core Features
                - **Game Engine**: Full Texas Hold'em rules with side pots
                - **AI Opponents**: Monte Carlo simulation with GTO strategies
                - **Tournaments**: Multi-table tournaments with blind structures
                - **Hand Analysis**: Equity calculator and optimal play suggestions
                
                ### 🔐 Authentication
                All protected endpoints require JWT Bearer token.
                Obtain token via `/api/auth/login` endpoint.
                
                ### 📖 API Versions
                - **v1**: Stable API for production use
                - **v2**: Enhanced API with HATEOAS and RFC 7807 error responses
                
                ### 🔗 Related Resources
                - [GitHub Repository](https://github.com/truholdem/poker)
                - [WebSocket Documentation](/docs/websocket)
                """)
            .contact(new Contact()
                .name("TruHoldem Team")
                .email("api@truholdem.com")
                .url("https://github.com/truholdem"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT"));
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
            .description("TruHoldem Full Documentation")
            .url("https://github.com/truholdem/poker/wiki");
    }

    private List<Server> serverList() {
        return List.of(
            new Server()
                .url(baseUrl)
                .description("Current Environment"),
            new Server()
                .url("http://localhost:8080")
                .description("Local Development"),
            new Server()
                .url("https://api.truholdem.com")
                .description("Production")
        );
    }

    private Components securityComponents() {
        return new Components()
            .addSecuritySchemes(SECURITY_SCHEME_NAME, 
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("""
                        JWT Authentication token.
                        
                        **How to obtain:**
                        1. Register: POST /api/auth/register
                        2. Login: POST /api/auth/login
                        3. Use returned token in Authorization header
                        
                        **Format:** `Bearer <token>`
                        """));
    }
}
