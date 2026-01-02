package com.pullwise.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Configuração do OpenAPI/Swagger para documentação da API.
 */
@Configuration
@Profile("!prod") // Desabilita em produção
public class OpenApiConfig {

    @Bean
    public OpenAPI pullwiseOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Pullwise API")
                        .description("API para análise de código com IA - Code Review automatizado com SAST + LLM")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Pullwise")
                                .email("contact@pullwise.ai")
                                .url("https://pullwise.ai"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Servidor Local"),
                        new Server().url("https://api.pullwise.ai").description("Produção")
                ))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Autenticação JWT - Use o token obtido em /oauth2/authorization/github")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch(
                        "/oauth2/**",
                        "/webhooks/**",
                        "/actuator/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi billingApi() {
        return GroupedOpenApi.builder()
                .group("billing")
                .pathsToMatch("/billing/**")
                .build();
    }

    @Bean
    public GroupedOpenApi analyticsApi() {
        return GroupedOpenApi.builder()
                .group("analytics")
                .pathsToMatch("/analytics/**")
                .build();
    }
}
