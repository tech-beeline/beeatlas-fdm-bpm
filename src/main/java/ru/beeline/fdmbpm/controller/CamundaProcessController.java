package ru.beeline.fdmbpm.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.fdmbpm.dto.camundaProcess.GetContextDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.GetProcessDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ProcessDTO;
import ru.beeline.fdmbpm.service.ProcessService;

import java.util.List;

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

    @GetMapping("/status/{idEnum}/{id}")
    public ResponseEntity<List<GetProcessDTO>> getAllStatusProcess(@PathVariable String idEnum,
                                                                   @PathVariable String id) {
        return ResponseEntity.status(HttpStatus.OK).body(processService.getAllStatusProcess(idEnum, id));
    }

    @GetMapping("/processes/context/{name}/{value}")
    public ResponseEntity<List<GetContextDTO>> getAllProcessByContext(@PathVariable String name,
                                                                      @PathVariable String value) {
        return ResponseEntity.status(HttpStatus.OK).body(processService.getAllProcessByContext(name, value));
    }
}
