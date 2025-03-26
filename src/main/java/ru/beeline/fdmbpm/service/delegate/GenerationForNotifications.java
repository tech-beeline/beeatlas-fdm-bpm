package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.ExportProcessService;

@Slf4j
@Component("GenerationNotificationProcess")
public class GenerationForNotifications implements JavaDelegate {

    @Autowired
    ExportProcessService exportProcessService;
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        String entityType = (String) delegateExecution.getVariable("entityType");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        Integer userId = (Integer) delegateExecution.getVariable("userId");
        exportProcessService.generationNotification(entityType, docId, userId);
    }
}
