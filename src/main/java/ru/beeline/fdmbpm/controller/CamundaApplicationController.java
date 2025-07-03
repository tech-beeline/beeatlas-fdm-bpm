package ru.beeline.fdmbpm.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationDTO;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationExtendedDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.CommentDTO;
import ru.beeline.fdmbpm.service.ApplicationService;

import java.util.List;

import static ru.beeline.fdmbpm.utils.Constants.USER_ID_HEADER;
import static ru.beeline.fdmbpm.utils.Constants.USER_ROLES_HEADER;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("camunda-process/api/v1")
public class CamundaApplicationController {

    @Autowired
    ApplicationService applicationService;

    @Operation(parameters = {@Parameter(in = ParameterIn.HEADER, name = USER_ROLES_HEADER, required = true, array = @ArraySchema(schema = @Schema(type = "string")))})
    @GetMapping("/application/nobody")
    public ResponseEntity<List<ApplicationDTO>> getAssignedApplications() {
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getAssignedApplications());
    }

    @Operation(parameters = {@Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true, schema = @Schema(type = "string"))})
    @GetMapping("/application/author")
    public ResponseEntity<List<ApplicationDTO>> getApplicationsByAuthor(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getApplicationsByAuthor(userId));
    }

    @Operation(parameters = {@Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true, schema = @Schema(type = "string"))})
    @GetMapping("/application/executor")
    public ResponseEntity<List<ApplicationDTO>> getApplicationsByExecutor(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getApplicationsByExecutor(userId));
    }

    @Operation(parameters = {@Parameter(in = ParameterIn.HEADER, name = USER_ROLES_HEADER, required = true, array = @ArraySchema(schema = @Schema(type = "string")), description = "Роли пользователя"),
            @Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true, schema = @Schema(type = "string"), description = "ID пользователя")})
    @PatchMapping("/application/{business_key}/executor")
    public ResponseEntity patchExecutorProcess(@PathVariable("business_key") String businessKey,
                                               @RequestParam(value = "next_status") String nextStatus,
                                               HttpServletRequest request) {
        return applicationService.patchExecutorProcess(businessKey, nextStatus, request);
    }

    @GetMapping("/application/{business_key}")
    public ApplicationExtendedDTO getApplicationsByBusinessKey(@PathVariable("business_key") String businessKey) {
        return applicationService.getApplicationsByBusinessKey(businessKey);
    }

    @Operation(parameters = {@Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, required = true, schema = @Schema(type = "string"))})
    @PatchMapping("/application/{business_key}/change-status/{status_alias}")
    public ResponseEntity patchChangeStatus(@PathVariable("business_key") String businessKey,
                                            @PathVariable("status_alias") String statusAlias,
                                            @RequestBody(required = false) CommentDTO commentDTO,
                                            HttpServletRequest request) {
        return applicationService.patchChangeStatus(businessKey, statusAlias, request, commentDTO);
    }

    @Operation(parameters = {@Parameter(in = ParameterIn.HEADER, name = USER_ID_HEADER, schema = @Schema(type = "integer"))})
    @PatchMapping("/application/{business_key}/executor/{new_executor_id}")
    public ResponseEntity changeExecutor(@PathVariable(name = "business_key") String businessKey,
                                         @PathVariable(name = "new_executor_id") Integer newExecutorId,
                                         @RequestHeader(value = USER_ID_HEADER, required = false) Integer userId) {
        applicationService.changeExecutor(businessKey, newExecutorId, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/application/{business_key}/sync-order")
    public ResponseEntity syncOrder(@PathVariable(name = "business_key") String businessKey) {
        applicationService.syncOrder(businessKey);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}