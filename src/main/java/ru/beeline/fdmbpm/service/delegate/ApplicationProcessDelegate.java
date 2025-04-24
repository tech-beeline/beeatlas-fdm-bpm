package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.ApplicationProcessService;

@Slf4j
@Component("ApplicationProcessService")
public class ApplicationProcessDelegate implements JavaDelegate {

    @Autowired
    ApplicationProcessService applicationProcessService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("running process ApplicationProcessService");
        String processInstanceId = (String) delegateExecution.getVariable("processInstanceId");
        String businessKey = (String) delegateExecution.getVariable("businessKey");
        Integer authorId = (Integer) delegateExecution.getVariable("authorId");
        String type = (String) delegateExecution.getVariable("type");
        String comment = (String) delegateExecution.getVariable("comment");
        Integer entityId = (Integer) delegateExecution.getVariable("entityId");
        String name = (String) delegateExecution.getVariable("name");
        applicationProcessService.applicationProcess(processInstanceId, businessKey, authorId, type,
                comment, entityId, name);
    }
}