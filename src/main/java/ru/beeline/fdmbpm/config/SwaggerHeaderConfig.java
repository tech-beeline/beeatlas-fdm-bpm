package ru.beeline.fdmbpm.config;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerHeaderConfig {

    @Bean
    public OpenApiCustomizer globalHeaderOpenApiCustomiser() {
        return openApi -> openApi.getPaths().values().forEach(pathItem ->
                                                                      pathItem.readOperations().forEach(operation -> {
                                                                          operation.addParametersItem(new Parameter()
                                                                                                              .in("Header")
                                                                                                              .name("user-id")
                                                                                                              .schema(new StringSchema())
                                                                                                              .description("ID пользователя"));
                                                                          operation.addParametersItem(new Parameter()
                                                                                                              .in("Header")
                                                                                                              .name("user-permission")
                                                                                                              .schema(new StringSchema())
                                                                                                              .description("Права пользователя"));
                                                                          operation.addParametersItem(new Parameter()
                                                                                                              .in("Header")
                                                                                                              .name("user-products-ids")
                                                                                                              .schema(new StringSchema())
                                                                                                              .description("ID продуктов пользователя"));
                                                                          operation.addParametersItem(new Parameter()
                                                                                                              .in("Header")
                                                                                                              .name("user-roles")
                                                                                                              .schema(new StringSchema())
                                                                                                              .description("Роли пользователя"));
                                                                      })
        );
    }
}