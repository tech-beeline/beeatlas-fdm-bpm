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
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;
import ru.beeline.fdmbpm.utils.StepErrorMessage;

@Slf4j
@Component("LocalGraphCheckResultDelegate")
public class LocalGraphCheckResultDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Object doneLocalGraph = delegateExecution.getVariable("doneLocalGraph");
        log.info("ℹ️ [pipeline] Шаг: Проверка результата локального графа | process_id={}, doneLocalGraph={}",
                processId, doneLocalGraph);
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        if (!Boolean.parseBoolean(String.valueOf(doneLocalGraph))) {
            String message = StepErrorMessage.of("Проверка результата локального графа", null);
            saveAlias(processId, "errlclgrph", typeProcess, message);
            log.error("ℹ️ [pipeline] Локальный граф НЕ готов | process_id={}, doneLocalGraph={}",
                    processId, doneLocalGraph);
            throw new ProcessException("Ошибка процесса на шаге: Проверка результата локального графа");
        }
        log.info("ℹ️ [pipeline] Шаг завершён: Проверка результата локального графа | process_id={}", processId);
    }
}
