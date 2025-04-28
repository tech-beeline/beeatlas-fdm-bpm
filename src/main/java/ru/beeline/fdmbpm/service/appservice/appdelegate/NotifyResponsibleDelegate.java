package ru.beeline.fdmbpm.service.appservice.appdelegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.appservice.ApplicationProcessService;

@Slf4j
@Component("notifyResponsibleDelegate")
public class NotifyResponsibleDelegate implements JavaDelegate {

    @Autowired
    ApplicationProcessService applicationProcessService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Старт процесса: Нотификация ответственному");
        Integer applicationId = (Integer) delegateExecution.getVariable("applicationId");
        String type = (String) delegateExecution.getVariable("type");
        applicationProcessService.notifyResponsible(applicationId, type);
    }
}
