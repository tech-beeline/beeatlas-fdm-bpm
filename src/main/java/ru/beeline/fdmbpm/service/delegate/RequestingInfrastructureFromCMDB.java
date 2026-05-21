/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.InfrastructureService;

@Slf4j
@Component("RequestingInfrastructureFromCMDB")
public class RequestingInfrastructureFromCMDB implements JavaDelegate {

    @Autowired
    InfrastructureService infrastructureService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("ℹ️ Старт процесса requesting infrastructure from CMDB");
        String product = (String) delegateExecution.getVariable("product");
        infrastructureService.gettingApplicationData(product);
    }
}
