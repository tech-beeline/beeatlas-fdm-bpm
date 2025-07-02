package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ArchClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.TypeProcessRepository;

@Slf4j
@Component("PublicToFdmDelegate")
public class PublicToFdmDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    ArchClient archClient;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Публикация в ФДМ");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        log.info("process_id: {}, docId: {}", processId, docId);

        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        log.info("camundaProcess : {}", camundaProcess);
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        log.info("typeProcess : {}", typeProcess);
        try {
            archClient.publicFdm(docId);
            saveAlias(processId, "btls", typeProcess);
            log.info("Шаг: Публикация в ФДМ, успешно завершен.");
        } catch (Exception e) {
            saveAlias(processId, "btlserr", typeProcess);
            throw new RuntimeException(e.getMessage());
        }
    }
}
