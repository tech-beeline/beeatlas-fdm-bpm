package ru.beeline.fdmbpm.controller;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.CommentDTO;
import ru.beeline.fdmbpm.service.ApplicationService;
import ru.beeline.fdmbpm.service.ProcessService;

import java.util.List;

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

    @PatchMapping("/application/{businessKey}/executor")
    public ResponseEntity<Void> patchExecutorProcess(@PathVariable String businessKey,
                                                     @RequestParam(value = "nextStatus") String nextStatus,
                                                     HttpServletRequest request) {
        applicationService.patchExecutorProcess(businessKey, nextStatus, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/application/{businessKey}/change-status/{statusAlias}")
    public ResponseEntity<Void> patchChangeStatus(@PathVariable String businessKey,
                                                  @PathVariable String statusAlias,
                                                  @RequestBody(required = false) CommentDTO commentDTO,
                                                  HttpServletRequest request) {
        applicationService.patchChangeStatus(businessKey, statusAlias, request, commentDTO);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
