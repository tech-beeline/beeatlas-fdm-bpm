package ru.beeline.fdmbpm.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmbpm.dto.ProcessDTO;
import ru.beeline.fdmbpm.service.ProcessService;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("camunda-process/api/v1")
public class CamundaProcessController {

    @Autowired
    ProcessService processService;

    @GetMapping("/status/{id_enum}/{id}/now")
    public ResponseEntity<ProcessDTO> getProcess(@PathVariable String id_enum,
                                                 @PathVariable String id) {
        return ResponseEntity.status(HttpStatus.OK).body(processService.getProcess(id_enum, id));
    }
}
