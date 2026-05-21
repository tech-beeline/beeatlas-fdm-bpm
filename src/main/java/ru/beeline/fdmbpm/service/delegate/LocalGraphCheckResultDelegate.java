/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("LocalGraphCheckResultDelegate")
public class LocalGraphCheckResultDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("Проверка результата локального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        log.info("delegateExecution.getVariable(\"doneLocalGraph\") =" + delegateExecution.getVariable("doneLocalGraph"));
        log.info("parse bool=" + Boolean.parseBoolean(delegateExecution.getVariable("doneLocalGraph").toString()));
        if (!Boolean.parseBoolean(delegateExecution.getVariable("doneLocalGraph").toString())) {
            saveAlias(processId, "errlclgrph", typeProcess);
            throw new Exception("doneLocalGraph is" + delegateExecution.getVariable("doneLocalGraph").toString());
        }
    }
}
