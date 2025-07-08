package ru.beeline.fdmbpm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Кастомная аннотация для добавления общих заголовков к операциям Swagger/OpenAPI.
 * Используется для избежания дублирования аннотаций @Parameter в каждом методе.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Operation(
        parameters = {
                @Parameter(
                        in = ParameterIn.HEADER,
                        name = "user-id",
                        required = true,
                        schema = @Schema(type = "string"),
                        description = "ID пользователя"
                ),
                @Parameter(
                        in = ParameterIn.HEADER,
                        name = "user-permission",
                        required = true,
                        array = @ArraySchema(schema = @Schema(type = "string")),
                        description = "Права пользователя"
                ),
                @Parameter(
                        in = ParameterIn.HEADER,
                        name = "user-products-ids",
                        required = true,
                        array = @ArraySchema(schema = @Schema(type = "integer")),
                        description = "ID продуктов пользователя"
                ),
                @Parameter(
                        in = ParameterIn.HEADER,
                        name = "user-roles",
                        required = true,
                        array = @ArraySchema(schema = @Schema(type = "string")),
                        description = "Роли пользователя"
                )
        }
)
public @interface SwaggerCommonHeaders {
}