package ru.beeline.fdmbpm.service.delegate;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.TypeProcessRepository;
import ru.beeline.fdmbpm.service.RabbitService;

@Slf4j
@Component("LocalGraphEnrichmentDelegate")
public class LocalGraphEnrichmentDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    RabbitService rabbitService;
    @Autowired
    ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Обагащение графа локального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        log.info("Обработка с processId: {}, docId: {}", processId, docId);
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        log.info("Обработка процесса. processId={}, procId={}, businessKey={}, typeProcessId={}",
                 processId,
                 camundaProcess.getProcId(),
                 camundaProcess.getBusinessKey(),
                 camundaProcess.getTypeProcessId());
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        try {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("taskKey", processId);
            item.put("docId", docId);
            log.info("Send to create_loacal_graph");
            rabbitService.sendMessage("create_loacal_graph", objectMapper.writeValueAsString(item));
            log.info("Send to create_loacal_graph completed");
            saveAlias(processId, "lcltskcrt", typeProcess);
            log.info("Обогащение графа успешно. processId={}, docId={}", processId, docId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            saveAlias(processId, "errlcltskcrt", typeProcess);
            throw new RuntimeException(e.getMessage());
        }
    }
}
