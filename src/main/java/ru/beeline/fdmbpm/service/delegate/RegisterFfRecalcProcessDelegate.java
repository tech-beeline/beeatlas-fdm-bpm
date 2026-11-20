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
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.Optional;

@Slf4j
@Component("RegisterFfRecalcProcessDelegate")
public class RegisterFfRecalcProcessDelegate extends StatusLogic implements JavaDelegate {

    private static final String TYPE_ALIAS = "RecalcFf";

    @Autowired
    private TypeProcessRepository typeProcessRepository;
    @Autowired
    private CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private ContextRepository contextRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution execution) {
        String ffCode = readCode(execution.getVariable("ffCode"));
        String procId = execution.getProcessInstanceId();
        String businessKey = ffCode.isEmpty() ? "ff" : businessKey(execution, ffCode);
        log.info("RegisterFfRecalcProcessDelegate: ffCode={}, businessKey={}, procId={}", ffCode, businessKey, procId);
        TypeProcess typeProcess = typeProcessRepository.findByAlias(TYPE_ALIAS);
        try {
            if (typeProcess == null) {
                throw new ProcessException("Тип процесса RecalcFf не найден");
            }
            Optional<CamundaProcess> existing = camundaProcessRepository.findByProcIdAndBusinessKey(procId, businessKey);
            if (existing.isPresent()) {
                execution.setVariable("process_id", existing.get().getId());
                if (ffCode.isEmpty()) {
                    throw new ProcessException("Код fitness function не задан");
                }
                return;
            }
            if (ffCode.isEmpty()) {
                throw new ProcessException("Код fitness function не задан");
            }
            CamundaProcess camundaProcess = saveProcess(typeProcess.getId(), procId, businessKey);
            execution.setVariable("process_id", camundaProcess.getId());
            saveContext("ffCode", ffCode, camundaProcess);
            saveAlias(camundaProcess.getId(), "crt", typeProcess);
        } catch (ProcessException e) {
            saveErrStatus(procId, businessKey, typeProcess, ffCode);
            throw e;
        } catch (Exception e) {
            log.error("RegisterFfRecalcProcessDelegate: ошибка регистрации", e);
            saveErrStatus(procId, businessKey, typeProcess, ffCode);
            throw new ProcessException("Ошибка регистрации процесса перерасчёта fitness function", e);
        }
    }

    private void saveErrStatus(String procId, String businessKey, TypeProcess typeProcess, String ffCode) {
        if (typeProcess == null) {
            return;
        }
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.execute(status -> {
            if (camundaProcessRepository.findByProcIdAndBusinessKey(procId, businessKey).isPresent()) {
                return null;
            }
            CamundaProcess failed = saveProcess(typeProcess.getId(), procId, businessKey);
            if (!ffCode.isEmpty()) {
                saveContext("ffCode", ffCode, failed);
            }
            saveAlias(failed.getId(), "errcrt", typeProcess);
            return null;
        });
    }

    private CamundaProcess saveProcess(Integer typeProcessId, String procId, String businessKey) {
        return camundaProcessRepository.save(CamundaProcess.builder()
                .typeProcessId(typeProcessId)
                .procId(procId)
                .businessKey(businessKey)
                .isAsync(false)
                .build());
    }

    private void saveContext(String name, String value, CamundaProcess camundaProcess) {
        contextRepository.save(Context.builder()
                .name(name)
                .value(value)
                .camundaProcessId(camundaProcess.getId())
                .build());
    }

    static String readCode(Object raw) {
        if (raw == null) {
            return "";
        }
        return raw.toString().trim();
    }

    private static String businessKey(DelegateExecution execution, String fallback) {
        String key = execution.getBusinessKey();
        if (key == null || key.isBlank()) {
            return fallback;
        }
        return key;
    }
}
