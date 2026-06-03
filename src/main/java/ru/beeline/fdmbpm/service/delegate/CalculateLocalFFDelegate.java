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
import ru.beeline.fdmbpm.client.ArchClient;
import ru.beeline.fdmbpm.client.FfManagerClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.ffmanager.PostFfManagerDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("CalculateLocalFFDelegate")
public class CalculateLocalFFDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    ArchClient archClient;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private FfManagerClient ffManagerClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Расчет Фитнес функции");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        log.info("process_id: {}, docId: {}, cmdb: {}", processId, docId, cmdb);
        TypeProcess typeProcess = null;
        try {
            ffManagerClient.postFfManager(PostFfManagerDTO.builder().app(cmdb).build(), docId, processId);
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            log.info("camundaProcess: processId={}, procId={}, businessKey={}, typeProcessId={}",
                    processId, camundaProcess.getProcId(), camundaProcess.getBusinessKey(), camundaProcess.getTypeProcessId());
            typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            archClient.postFitnessFunction(docId, processId);
            saveAlias(processId, "ffdn", typeProcess);
            log.info("Шаг: Расчет Фитнес функции, успешно завершен. typeProcess: {} ", typeProcess);
        } catch (Exception e) {
            log.error("Ошибка при Расчете Фитнес функции. Создание записи с ошибкой", e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TypeProcess finalTypeProcess = typeProcess;
            tt.execute(status -> {
                saveAlias(processId, "fferrcd", finalTypeProcess);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Расчет Фитнес функции");
        }
    }
}
