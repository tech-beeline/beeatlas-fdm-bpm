package ru.beeline.fdmbpm.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.fdmbpm.dto.PostImportServiceDTO;
import ru.beeline.fdmbpm.service.CapabilityService;
import ru.beeline.fdmbpm.service.ExportProcessService;
import ru.beeline.fdmbpm.service.ImportProcessService;
import ru.beeline.fdmbpm.service.InfrastructureService;
import ru.beeline.fdmbpm.service.RelationsService;

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
}
