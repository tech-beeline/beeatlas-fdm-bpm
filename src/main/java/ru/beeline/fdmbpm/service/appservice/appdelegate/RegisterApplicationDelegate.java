package ru.beeline.fdmbpm.service.appservice.appdelegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.appservice.ApplicationProcessService;

@Slf4j
@Component("registerApplicationDelegate")
public class RegisterApplicationDelegate implements JavaDelegate {

    @Autowired
    ApplicationProcessService applicationProcessService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("старт процесса: Регистрация заявки");
        String processInstanceId = delegateExecution.getProcessInstanceId();
        String businessKey = delegateExecution.getProcessBusinessKey();
        Integer authorId = (Integer) delegateExecution.getVariable("authorId");
        String type = (String) delegateExecution.getVariable("type");
        String comment = (String) delegateExecution.getVariable("comment");
        Integer entityId = (Integer) delegateExecution.getVariable("entityId");
        String name = (String) delegateExecution.getVariable("name");
        log.info("переменные процесса: processInstanceId= {}, businessKey= {}, authorId= {}, type= {}, comment= {}, entityId= {}, name= {}",
                processInstanceId, businessKey, authorId, type, comment, entityId, name);
        applicationProcessService.applicationProcess(processInstanceId, businessKey, authorId, type,
                comment, entityId, name);
    }
}
