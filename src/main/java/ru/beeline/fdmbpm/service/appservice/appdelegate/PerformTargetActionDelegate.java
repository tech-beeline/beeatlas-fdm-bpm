package ru.beeline.fdmbpm.service.appservice.appdelegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.appservice.ApplicationProcessService;

@Slf4j
@Component("performTargetActionDelegate")
public class PerformTargetActionDelegate implements JavaDelegate {

    @Autowired
    ApplicationProcessService applicationProcessService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Старт процесса: Целевое действие по заявке");
        Integer typeId = (Integer) delegateExecution.getVariable("typeId");
        Integer entityId = (Integer) delegateExecution.getVariable("entityId");
        applicationProcessService.performTargetAction(typeId, entityId);
    }
}
