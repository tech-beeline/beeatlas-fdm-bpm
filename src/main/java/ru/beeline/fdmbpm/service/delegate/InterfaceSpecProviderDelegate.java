package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.MapicSpecService;

@Slf4j
@Component("InterfaceSpecProviderDelegate")
public class InterfaceSpecProviderDelegate implements JavaDelegate {

    @Autowired
    MapicSpecService mapicSpecService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        Integer apiId = (Integer) delegateExecution.getVariable("apiId");
        log.info("run MapicSpecDelegate with id:" + apiId);
        mapicSpecService.uploadSpec(apiId);
    }
}
