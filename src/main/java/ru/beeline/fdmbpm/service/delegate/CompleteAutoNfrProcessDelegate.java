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
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

@Slf4j
@Component("CompleteAutoNfrProcessDelegate")
public class CompleteAutoNfrProcessDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    private CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private TypeProcessRepository typeProcessRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution execution) {
        Integer processId = RegisterAutoNfrProcessDelegate.readInteger(execution.getVariable("process_id"));
        log.info("CompleteAutoNfrProcessDelegate: process_id={}", processId);
        try {
            TypeProcess typeProcess = typeOf(processId)
                    .orElseThrow(() -> new ProcessException("Процесс " + processId + " не найден"));
            saveAlias(processId, "nfrdn", typeProcess);
        } catch (Exception e) {
            log.error("CompleteAutoNfrProcessDelegate: ошибка статуса nfrdn, process_id={}", processId, e);
            saveError(processId);
            throw new ProcessException("Ошибка статуса завершения автоназначения НФТ", e);
        }
    }

    private void saveError(Integer processId) {
        if (processId == null) {
            return;
        }
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.execute(status -> {
            typeOf(processId).ifPresent(typeProcess -> saveAlias(processId, "nfrerr", typeProcess));
            return null;
        });
    }

    private java.util.Optional<TypeProcess> typeOf(Integer processId) {
        if (processId == null) {
            return java.util.Optional.empty();
        }
        return camundaProcessRepository.findById(processId)
                .map(CamundaProcess::getTypeProcessId)
                .flatMap(typeProcessRepository::findById);
    }
}
