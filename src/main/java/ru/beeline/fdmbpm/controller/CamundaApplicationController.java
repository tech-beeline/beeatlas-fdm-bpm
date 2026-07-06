/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.controller;


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

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("camunda-process/api/v1")
public class CamundaApplicationController {

    @Autowired
    ApplicationService applicationService;

    @SwaggerCommonHeaders
    @GetMapping("/application/nobody")
    public ResponseEntity<List<ApplicationDTO>> getAssignedApplications(
            @RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getAssignedApplications(userId));
    }

    @SwaggerCommonHeaders
    @GetMapping("/application/author")
    public ResponseEntity<List<ApplicationDTO>> getApplicationsByAuthor(
            @RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getApplicationsByAuthor(Integer.valueOf(userId)));
    }

    @SwaggerCommonHeaders
    @GetMapping("/application/executor")
    public ResponseEntity<List<ApplicationDTO>> getApplicationsByExecutor(
            @RequestHeader(value = USER_ID_HEADER) String userId) {
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getApplicationsByExecutor(Integer.valueOf(userId)));
    }

    @SwaggerCommonHeaders
    @PatchMapping("/application/{business_key}/executor")
    public ResponseEntity patchExecutorProcess(@PathVariable("business_key") String businessKey,
                                               @RequestParam(value = "next_status") String nextStatus,
                                               @RequestHeader(value = USER_ID_HEADER) String userId) {
        return applicationService.patchExecutorProcess(businessKey, nextStatus, userId);
    }

    @GetMapping("/application/{business_key}")
    public ApplicationExtendedDTO getApplicationsByBusinessKey(@PathVariable("business_key") String businessKey) {
        return applicationService.getApplications(null, businessKey);
    }

    @GetMapping("/application")
    public ApplicationExtendedDTO getApplicationsById(@RequestParam (required = false) Integer id,
                                                      @RequestParam (value = "business-key", required = false) String businessKey) {
        return applicationService.getApplications(id, businessKey);
    }

    @SwaggerCommonHeaders
    @PatchMapping("/application/{business_key}/change-status/{status_alias}")
    public ResponseEntity patchChangeStatus(@PathVariable("business_key") String businessKey,
                                            @PathVariable("status_alias") String statusAlias,
                                            @RequestBody(required = false) CommentDTO commentDTO,
                                            @RequestHeader(value = USER_ID_HEADER) String userId) {
        return applicationService.patchChangeStatus(businessKey, statusAlias, userId, commentDTO);
    }

    @SwaggerCommonHeaders
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