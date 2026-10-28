/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.graph.GraphDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("LocalGraphCheckDelegate")
public class LocalGraphCheckDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    GraphClient graphClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer localGraphCount = (Integer) delegateExecution.getVariable("localGraphCount");
        log.info("ℹ️ [pipeline] Шаг: Проверка готовности локального графа | process_id={}, localGraphCount={}",
                processId, localGraphCount);
        if (localGraphCount != null) {
            if (localGraphCount > 3) {
                camundaProcessRepository.markAsyncTrueIfFalse(processId);
            }
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            try {
                GraphDTO localGraphDTO = graphClient.getLocalGraph(processId);
                String status = localGraphDTO.getStatus();
                log.info("ℹ️ [pipeline/graph] local graph status={} process_id={} attempt={}",
                        status, processId, localGraphCount);
                if (status.equals("PROCESS") || status.equals("QUEUE")) {
                    delegateExecution.setVariable("localGraphCount", localGraphCount + 1);
                }
                if (status.equals("DONE")) {
                    delegateExecution.setVariable("doneLocalGraph", true);
                    saveAlias(processId, "lclgrph", typeProcess);
                    log.info("ℹ️ [pipeline] Локальный граф READY | process_id={}", processId);
                }
            } catch (Exception e) {
                log.error("ℹ️ [pipeline] Ошибка проверки локального графа | process_id={}", processId, e);
                TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tt.execute(status -> {
                    StatusProcess statusProcess =
                            statusProcessRepository.findByAliasAndTypeProcessId("errlclgrph", typeProcess.getId());
                    if (camundaProcessStatusRepository.findByCamundaProcessIdAndStatusProcessId(processId,
                            statusProcess.getId()).isEmpty()) {
                        saveAlias(processId, "errlclgrph", typeProcess);
                    }
                    return null;
                });
                throw new ProcessException("❌ Ошибка процесса на шаге: Проверка готовности локального графа");
            }
        } else {
            log.info("ℹ️ [pipeline/graph] localGraphCount=null → ставим 1 (первый опрос)");
            delegateExecution.setVariable("localGraphCount", 1);
        }
    }
}
