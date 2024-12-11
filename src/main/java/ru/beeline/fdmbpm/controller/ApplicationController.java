package ru.beeline.fdmbpm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmbpm.service.CapabilityService;
import ru.beeline.fdmbpm.service.RelationsService;

@Slf4j
@RestController
public class ApplicationController {
    @Autowired
    RelationsService relationsService;
    @Autowired
    CapabilityService capabilityService;

    @GetMapping("/send-business")
    public String sendBusinessCapability() {
        log.info("running process CapabilityUpload, step: send business capability");
        return capabilityService.sendBusinessCapability().toString();
    }

    @GetMapping("/create-relations")
    public String ct() {
        log.info("running process CapabilityUpload, step: send business capability");
         relationsService.createRelations();
        return null;
    }
    @GetMapping("/send-tech")
    public String sendTechCapability() {
        log.info("running process CapabilityUpload, step: send tech capability");
        return capabilityService.sendTechCapability().toString();
    }
}
