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

import java.util.List;
import java.util.Optional;

@Slf4j
@Component("RegisterAutoNfrProcessDelegate")
public class RegisterAutoNfrProcessDelegate extends StatusLogic implements JavaDelegate {

    private static final String TYPE_ALIAS = "AutoNfr";

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
        Integer nfrId = readInteger(execution.getVariable("nfrId"));
        List<String> rule = readRule(execution.getVariable("rule"));
        String procId = execution.getProcessInstanceId();
        String businessKey = nfrId == null ? "nfr" : businessKey(execution, nfrId.toString());
        log.info("RegisterAutoNfrProcessDelegate: nfrId={}, businessKey={}, procId={}", nfrId, businessKey, procId);
        TypeProcess typeProcess = typeProcessRepository.findByAlias(TYPE_ALIAS);
        try {
            if (typeProcess == null) {
                throw new ProcessException("Тип процесса AutoNfr не найден");
            }
            Optional<CamundaProcess> existing = camundaProcessRepository.findByProcIdAndBusinessKey(procId, businessKey);
            if (existing.isPresent()) {
                execution.setVariable("process_id", existing.get().getId());
                if (nfrId == null) {
                    throw new ProcessException("nfrId не задан");
                }
                return;
            }
            if (nfrId == null) {
                throw new ProcessException("nfrId не задан");
            }
            CamundaProcess camundaProcess = saveProcess(typeProcess.getId(), procId, businessKey);
            execution.setVariable("process_id", camundaProcess.getId());
            saveContext("nfrId", nfrId.toString(), camundaProcess);
            saveContext("rule", String.join(",", rule), camundaProcess);
            saveAlias(camundaProcess.getId(), "crt", typeProcess);
        } catch (ProcessException e) {
            saveErrStatus(procId, businessKey, typeProcess, nfrId, rule);
            throw e;
        } catch (Exception e) {
            log.error("RegisterAutoNfrProcessDelegate: ошибка регистрации", e);
            saveErrStatus(procId, businessKey, typeProcess, nfrId, rule);
            throw new ProcessException("Ошибка регистрации процесса автоназначения НФТ", e);
        }
    }

    private void saveErrStatus(String procId, String businessKey, TypeProcess typeProcess,
                               Integer nfrId, List<String> rule) {
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
            if (nfrId != null) {
                saveContext("nfrId", nfrId.toString(), failed);
            }
            saveContext("rule", String.join(",", rule), failed);
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

    private static String businessKey(DelegateExecution execution, String fallback) {
        String key = execution.getBusinessKey();
        if (key == null || key.isBlank()) {
            return fallback;
        }
        return key;
    }

    static Integer readInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Integer value) {
            return value;
        }
        if (raw instanceof Number value) {
            return value.intValue();
        }
        String text = raw.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static List<String> readRule(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(item -> item != null && !item.toString().isEmpty())
                .map(Object::toString)
                .toList();
    }
}
