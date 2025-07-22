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
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.TypeProcessRepository;
import ru.beeline.fdmbpm.service.RabbitService;

@Slf4j
@Component("GlobalGraphEnrichmentDelegate")
public class GlobalGraphEnrichmentDelegate extends StatusLogic implements JavaDelegate {

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
        log.info("Таск : Обогащение глобального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        TypeProcess typeProcess = null;
        try {
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        log.info("camundaProcess : {}", camundaProcess);
        typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        log.info("typeProcess : {}", typeProcess);
            ObjectNode item = objectMapper.createObjectNode();
            item.put("taskKey", processId);
            item.put("docId", docId);
            log.info("Send to create_global_graph");
            rabbitService.sendMessage("create_global_graph", objectMapper.writeValueAsString(item));
            log.info("Send to create_global_graph completed");

            saveAlias(processId, "glbltskcrt", typeProcess);
            log.info("Таск : Обогащение глобального графа, зевершен");
        } catch (Exception e) {
            log.error("Ошибка при Обагащение глобального графа. Создание записи с ошибкой", e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TypeProcess finalTypeProcess = typeProcess;
            tt.execute(status -> {
                saveAlias(processId, "errglblgrph", finalTypeProcess);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Обагащение глобального графа");
        }
    }
}
