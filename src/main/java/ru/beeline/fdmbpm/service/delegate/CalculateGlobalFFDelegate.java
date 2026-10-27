/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ArchClient;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("CalculateGlobalFFDelegate")
public class CalculateGlobalFFDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    ArchClient archClient;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        log.info("ℹ️ [pipeline] Шаг: Расчёт глобальных FF | cmdb={}, process_id={}, docId={} — TODO: заглушка, расчёт не выполняется",
                cmdb, processId, docId);
        log.info("ℹ️ [pipeline] Шаг завершён: Расчёт глобальных FF (no-op)");
    }
}
