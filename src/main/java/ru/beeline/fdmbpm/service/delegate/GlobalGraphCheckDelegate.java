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
@Component("GlobalGraphCheckDelegate")
public class GlobalGraphCheckDelegate extends StatusLogic implements JavaDelegate {

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
        Integer globalGraphCount = (Integer) delegateExecution.getVariable("globalGraphCount");
        log.info("ℹ️ [pipeline] Шаг: Проверка готовности глобального графа | process_id={}, globalGraphCount={}",
                processId, globalGraphCount);
        if (globalGraphCount != null) {
            if (globalGraphCount > 3) {
                camundaProcessRepository.markAsyncTrueIfFalse(processId);
            }
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            try {
                GraphDTO localGraphDTO = graphClient.getGlobalGraph(processId);
                String status = localGraphDTO.getStatus();
                log.info("ℹ️ [pipeline/graph] global graph status={} process_id={} attempt={}",
                        status, processId, globalGraphCount);
                if (status.equals("PROCESS") || status.equals("QUEUE")) {
                    delegateExecution.setVariable("globalGraphCount", globalGraphCount + 1);
                }
                if (status.equals("DONE")) {
                    delegateExecution.setVariable("doneGlobalGraph", true);
                    saveAlias(processId, "glblgrph", typeProcess);
                    log.info("ℹ️ [pipeline] Глобальный граф READY | process_id={}", processId);
                }
            } catch (Exception e) {
                log.error("ℹ️ [pipeline] Ошибка проверки глобального графа | process_id={}", processId, e);
                TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tt.execute(status -> {
                    StatusProcess statusProcess =
                            statusProcessRepository.findByAliasAndTypeProcessId("errglblgrph", typeProcess.getId());
                    if (camundaProcessStatusRepository.findByCamundaProcessIdAndStatusProcessId(processId,
                            statusProcess.getId()).isEmpty()) {
                        saveAlias(processId, "errglblgrph", typeProcess);
                    }
                    return null;
                });
                throw new ProcessException("❌ Ошибка процесса на шаге: Проверка готовности глобального графа");
            }
        } else {
            log.info("ℹ️ [pipeline/graph] globalGraphCount=null → ставим 1 (первый опрос)");
            delegateExecution.setVariable("globalGraphCount", 1);
        }
    }
}
