package ru.beeline.fdmbpm.service.appservice.appdelegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.appservice.ApplicationProcessService;

@Slf4j
@Component("sendNotificationsDelegate")
public class SendNotificationsDelegate implements JavaDelegate {
    @Autowired
    ApplicationProcessService applicationProcessService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("старт процесса: Разослать уведомления ответсвенным");
        Integer applicationId = (Integer) delegateExecution.getVariable("applicationId");
        Integer typeId = (Integer) delegateExecution.getVariable("typeId");
        String type = (String) delegateExecution.getVariable("type");
        String name = (String) delegateExecution.getVariable("name");
        applicationProcessService.sendGroupNotifications(applicationId, type, typeId, name);
    }
}
