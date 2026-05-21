/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmbpm.dto.ErrorResponse;
import ru.beeline.fdmbpm.dto.pipeline.ProcessStatusItemDTO;
import ru.beeline.fdmbpm.service.PipelineProcessStatusService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pipeline")
public class PipelineController {

    @Autowired
    private PipelineProcessStatusService pipelineProcessStatusService;

    @GetMapping(value = "/process-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Получить полный перечень статусов пайплайна по типу процесса")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProcessStatusItemDTO.class)),
                            examples = @ExampleObject(name = "Успешный ответ", value = """
                                    [
                                      {"id":1,"typeProcessId":1,"name":"Ожидание обработки","alias":"pending","isDone":false,"isError":false,"sequence":1},
                                      {"id":23,"typeProcessId":1,"name":"Неверный формат документа","alias":"vldterr","isDone":false,"isError":true,"sequence":2}
                                    ]
                                    """))),
            @ApiResponse(responseCode = "400", description = "Некорректный параметр typeProcessId",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "400 BAD REQUEST",
                                    value = "{\"errorMessage\":\"Параметр typeProcessId обязателен и должен быть целым числом.\"}"))),
            @ApiResponse(responseCode = "404", description = "Тип процесса не найден",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "404 NOT FOUND",
                                    value = "{\"errorMessage\":\"Тип процесса с указанным идентификатором не найден.\"}"))),
            @ApiResponse(responseCode = "405", description = "Метод не разрешён",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "405 METHOD NOT ALLOWED",
                                    value = "{\"errorMessage\":\"Метод не разрешён для данного ресурса.\"}")))
    })
    public List<ProcessStatusItemDTO> getProcessStatuses(@RequestParam("typeProcessId") Integer typeProcessId) {
        return pipelineProcessStatusService.getPipelineStatuses(typeProcessId);
    }
}

