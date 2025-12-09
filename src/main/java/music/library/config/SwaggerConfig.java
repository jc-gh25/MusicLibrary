package music.library.config;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Configures Springdoc OpenAPI (Swagger) for the Music Library project.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Defines a named group of API paths and tags.
     *
     * @return GroupedOpenApi instance that tells Springdoc where to find your routes.
     */
    @Bean
    public GroupedOpenApi musicLibraryGroup() {
        return GroupedOpenApi.builder()
                .group("Music Library")          // Name shown in Swagger UI sidebar
                .pathsToMatch("/api/**")         // All endpoints live under /api/
                .build();
    }

    /**
     * Optional: customizes the OpenAPI metadata (title, description, version).
     *
     * @return An OpenAPI instance.
     */
    @Bean
    public OpenAPI openApiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Library API")
                        .description(
                                "RESTful endpoints for managing artists, albums and genres.\n" +
                                        "\n" +
                                        "**Important:** All paths start with `/api/`.")
                        .version("1.0"));
    }
}
