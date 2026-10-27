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
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("PublicToFdmDelegate")
public class PublicToFdmDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    ArchClient archClient;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        log.info("ℹ️ [pipeline] Шаг: Публикация в ФДМ | process_id={}, docId={}", processId, docId);
        TypeProcess typeProcess = null;
        try {
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            archClient.publicFdm(docId);
            saveAlias(processId, "btls", typeProcess);
            log.info("ℹ️ [pipeline] Шаг завершён: Публикация в ФДМ | process_id={}, docId={}", processId, docId);
        } catch (Exception e) {
            log.error("ℹ️ [pipeline] Ошибка на шаге Публикация в ФДМ | process_id={}, docId={}",
                    processId, docId, e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TypeProcess finalTypeProcess = typeProcess;
            tt.execute(status -> {
                saveAlias(processId, "btlserr", finalTypeProcess);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Публикация в ФДМ");
        }
    }
}
