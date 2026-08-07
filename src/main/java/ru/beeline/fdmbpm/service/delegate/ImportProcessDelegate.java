/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.ImportProcessService;

@Slf4j
@Component("ImportProcess")
public class ImportProcessDelegate implements JavaDelegate {

    @Autowired
    ImportProcessService importProcessService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        String entityType = (String) delegateExecution.getVariable("entityType");
        Boolean sync = (Boolean) delegateExecution.getVariable("sync");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        importProcessService.uploadingDataFromExcel(entityType, sync, docId);
    }
}
