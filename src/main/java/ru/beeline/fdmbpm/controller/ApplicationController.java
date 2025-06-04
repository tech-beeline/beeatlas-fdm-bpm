package ru.beeline.fdmbpm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmbpm.dto.ApplicationProcessDTO;
import ru.beeline.fdmbpm.dto.PostImportServiceDTO;
import ru.beeline.fdmbpm.service.CapabilityService;
import ru.beeline.fdmbpm.service.ExportProcessService;
import ru.beeline.fdmbpm.service.ImportProcessService;
import ru.beeline.fdmbpm.service.InfrastructureService;
import ru.beeline.fdmbpm.service.RelationsService;
import ru.beeline.fdmbpm.service.appservice.ApplicationProcessService;

@Slf4j
@RestController
public class ApplicationController {
    @Autowired
    RelationsService relationsService;
    @Autowired
    CapabilityService capabilityService;
    @Autowired
    ImportProcessService importProcessService;
    @Autowired
    ExportProcessService exportProcessService;
    @Autowired
    InfrastructureService infrastructureService;

    @Autowired
    ApplicationProcessService applicationProcessService;

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

    @PostMapping("/import-process")
    public String startImportService(@RequestBody PostImportServiceDTO postImportServiceDTO) {
        log.info("running process start Import Service");
        Integer id = importProcessService.uploadingDataFromExcel(postImportServiceDTO.getEntityType(),
                postImportServiceDTO.getSync(),
                postImportServiceDTO.getDocId());
        return String.format("the process has been completed, packageId: %s ", id.toString());
    }

    @PostMapping("/export-process")
    public String startExportProcess(@RequestBody PostImportServiceDTO postImportServiceDTO) {
        log.info("running process start Export process");
        exportProcessService.processExportingToExcel(postImportServiceDTO.getEntityType(),
                postImportServiceDTO.getDocId());
        return "the process has been completed";
    }

    @PostMapping("/infrastructure-process/{product}")
    public String startInfrastructureProcess(@PathVariable String product) {
        log.info("running process start cmdb process");
        infrastructureService.gettingApplicationData(product);
        return "the process has been completed";
    }

    @PostMapping("/application-process")
    public String startInfrastructureProcess(@RequestBody ApplicationProcessDTO body) {
        log.info("running application process1");
        applicationProcessService.applicationProcess(body.getProcessInstanceId(), body.getBusinessKey(), body.getAuthorId(),
                body.getType(), body.getComment(), body.getEntityId(), body.getName());
        return "the process has been completed";
    }

    @GetMapping("/application-process/{applicationId}/{type}/{typeId}")
    public String startInfrastructureProcess(@PathVariable Integer applicationId,
                                             @PathVariable String type,
                                             @PathVariable Integer typeId) {
        log.info("running application process 2");
        applicationProcessService.sendGroupNotifications(applicationId, type, typeId);
        return "the process 2 has been completed";
    }

    @GetMapping("/application-process/{applicationId}/{type}")
    public String startNotifyResponsible(@PathVariable Integer applicationId,
                                         @PathVariable String type) {
        log.info("running application process 3");
        applicationProcessService.notifyResponsible(applicationId, type);
        return "the process 3 has been completed";
    }

    @GetMapping("/application-process/target-action/{typeId}/{entityId}")
    public String startPerformTargetAction(@PathVariable Integer typeId,
                                           @PathVariable Integer entityId) {
        log.info("running application process 3");
        applicationProcessService.performTargetAction(null, typeId, entityId);
        return "the process 3 has been completed";
    }
}
