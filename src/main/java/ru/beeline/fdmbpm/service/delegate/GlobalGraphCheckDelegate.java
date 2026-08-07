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
        log.info("ℹ️ Проверка готовности глобального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer globalGraphCount = (Integer) delegateExecution.getVariable("globalGraphCount");
        if (globalGraphCount != null) {
            log.info("ℹ️ globalGraphCount: {}", globalGraphCount);
            if (globalGraphCount > 3) {
                camundaProcessRepository.markAsyncTrueIfFalse(processId);
            }
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            try {
                GraphDTO localGraphDTO = graphClient.getGlobalGraph(processId);
                if (localGraphDTO.getStatus().equals("PROCESS") || localGraphDTO.getStatus().equals("QUEUE")) {
                    delegateExecution.setVariable("globalGraphCount", globalGraphCount + 1);
                }
                if (localGraphDTO.getStatus().equals("DONE")) {
                    delegateExecution.setVariable("doneGlobalGraph", true);
                    saveAlias(processId, "glblgrph", typeProcess);
                }
            } catch (Exception e) {
                log.info("❌ Ошибка при проверке готовности глобального графа. Создание записи с ошибкой");
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
            log.info("ℹ️ globalGraphCount = null , устанавливаем globalGraphCount = 1");
            delegateExecution.setVariable("globalGraphCount", 1);
        }
    }
}
