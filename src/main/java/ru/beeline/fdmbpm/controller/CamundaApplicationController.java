package ru.beeline.fdmbpm.controller;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationDTO;
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

    @GetMapping("/application/nobody")
    public ResponseEntity<List<ApplicationDTO>> getAssignedApplications() {
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getAssignedApplications());
    }

    @GetMapping("/application/author")
    public ResponseEntity<List<ApplicationDTO>> getApplicationsByAuthor(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getApplicationsByAuthor(userId));
    }

    @GetMapping("/application/executor")
    public ResponseEntity<List<ApplicationDTO>> getApplicationsByExecutor(HttpServletRequest request) {
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        return ResponseEntity.status(HttpStatus.OK).body(applicationService.getApplicationsByExecutor(userId));
    }

    @PatchMapping("/application/{business_key}/executor")
    public ResponseEntity<Boolean> patchExecutorProcess(@PathVariable("business_key") String businessKey,
                                                        @RequestParam(value = "next_status") String nextStatus,
                                                        HttpServletRequest request) {
        return applicationService.patchExecutorProcess(businessKey, nextStatus, request);
    }

    @PatchMapping("/application/{business_key}/change-status/{status_alias}")
    public ResponseEntity<Boolean> patchChangeStatus(@PathVariable("business_key") String businessKey,
                                                     @PathVariable("status_alias") String statusAlias,
                                                     @RequestBody(required = false) CommentDTO commentDTO,
                                                     HttpServletRequest request) {
        return applicationService.patchChangeStatus(businessKey, statusAlias, request, commentDTO);
    }
}
