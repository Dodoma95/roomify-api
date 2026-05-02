package com.roomify.configuration.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfiguration {

    private static final String DESCRIPTION = """
            API REST du backend Roomify — Architecture Hexagonale, Spring Boot 4, JWT stateless.
            
            ---
            
            ### Outils GraphQL
            
            | Outil | Rôle |
            |---|---|
            | [**GraphiQL**](/graphiql) | IDE interactif : écrire et exécuter des requêtes GraphQL |
            | [**Voyager**](/voyager) | Visualisation du schéma GraphQL sous forme de graphe |
            
            > **Authentification** : toutes les routes (REST et GraphQL) requièrent un Bearer JWT,\
             sauf les endpoints d'authentification publics listés ci-dessous.
            > Utilisez le bouton **Authorize** pour renseigner votre token.
            """;

    @Value("${api.version}")
    private String apiVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Roomify API")
                        .version(apiVersion)
                        .description(DESCRIPTION)
                        .contact(new Contact()
                                .name("Roomify Team")
                                .email("roomify.dev@proton.me")
                        )
                )
                .externalDocs(new ExternalDocumentation()
                        .description("Schéma GraphQL — Voyager")
                        .url("/voyager")
                )
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("users")
                .pathsToMatch("/api/v1/users/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .addTagsItem(new Tag()
                                .name("GraphQL")
                                .description(
                                        "La recherche des utilisateurs avec filtres est exposée via GraphQL — query `users` avec filtres partiels, filtre par rôle et pagination.")
                                .externalDocs(new ExternalDocumentation()
                                        .description("Ouvrir GraphiQL")
                                        .url("/graphiql")
                                )
                        )
                )
                .build();
    }

    @Bean
    public GroupedOpenApi placeApi() {
        return GroupedOpenApi.builder()
                .group("places")
                .pathsToMatch("/api/v1/places/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .addTagsItem(new Tag()
                                .name("GraphQL")
                                .description(
                                        "Les espaces sont également exposés via GraphQL — query `places`, `place` et `availableSlots` - filtres avancés et pagination.")
                                .externalDocs(new ExternalDocumentation()
                                        .description("Ouvrir GraphiQL")
                                        .url("/graphiql")
                                )
                        )
                )
                .build();
    }

    @Bean
    public GroupedOpenApi bookingApi() {
        return GroupedOpenApi.builder()
                .group("bookings")
                .pathsToMatch("/api/v1/bookings/**")
                .build();
    }

}
