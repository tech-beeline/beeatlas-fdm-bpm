/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import ru.beeline.fdmbpm.service.RabbitService;

@Slf4j
@Component("LocalGraphEnrichmentDelegate")
public class LocalGraphEnrichmentDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    RabbitService rabbitService;
    @Autowired
    ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Обагащение локального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        log.info("Обработка с processId: {}, docId: {}", processId, docId);
        TypeProcess typeProcess = null;
        try {
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            log.info("Обработка процесса. processId={}, procId={}, businessKey={}, typeProcessId={}",
                    processId,
                    camundaProcess.getProcId(),
                    camundaProcess.getBusinessKey(),
                    camundaProcess.getTypeProcessId());
            typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            ObjectNode item = objectMapper.createObjectNode();
            item.put("taskKey", processId);
            item.put("docId", docId);
            log.info("Send to create_local_graph");
            rabbitService.sendMessage("create_local_graph", objectMapper.writeValueAsString(item));
            log.info("Send to create_local_graph completed");
            saveAlias(processId, "lcltskcrt", typeProcess);
            log.info("Обогащение графа успешно. processId={}, docId={}", processId, docId);
        } catch (Exception e) {
            log.error("Ошибка при обагащении локального графа. Создание записи с ошибкой", e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TypeProcess finalTypeProcess = typeProcess;
            tt.execute(status -> {
                saveAlias(processId, "errlclgrph", finalTypeProcess);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Обагащение локального графа");
        }
    }
}
