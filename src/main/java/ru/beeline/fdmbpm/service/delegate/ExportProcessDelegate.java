/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.ExportProcessService;

@Slf4j
@Component("ExportProcess")
public class ExportProcessDelegate implements JavaDelegate {

    @Autowired
    ExportProcessService exportProcessService;
    @Override
    public void execute(DelegateExecution delegateExecution)  {
        String entityType = (String) delegateExecution.getVariable("entityType");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        exportProcessService.processExportingToExcel(entityType, docId);
    }
}
