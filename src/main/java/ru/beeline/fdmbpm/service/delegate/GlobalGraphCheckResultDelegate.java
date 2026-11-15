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
@Component("GlobalGraphCheckResultDelegate")
public class GlobalGraphCheckResultDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Object doneGlobalGraph = delegateExecution.getVariable("doneGlobalGraph");
        log.info("ℹ️ [pipeline] Шаг: Проверка результата глобального графа | process_id={}, doneGlobalGraph={}",
                processId, doneGlobalGraph);
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        if (!Boolean.parseBoolean(String.valueOf(doneGlobalGraph))) {
            String message = StepErrorMessage.of("Проверка результата глобального графа", null);
            saveAlias(processId, "errglblgrph", typeProcess, message);
            log.error("ℹ️ [pipeline] Глобальный граф НЕ готов | process_id={}, doneGlobalGraph={}",
                    processId, doneGlobalGraph);
            throw new ProcessException("Ошибка процесса на шаге: Проверка результата глобального графа");
        }
        log.info("ℹ️ [pipeline] Шаг завершён: Проверка результата глобального графа | process_id={}", processId);
    }
}
