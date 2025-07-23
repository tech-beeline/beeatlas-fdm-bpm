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
@Component("PublicToStructurizrDelegate")
public class PublicToStructurizrDelegate extends StatusLogic implements JavaDelegate {

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
        log.info("Шаг: Публикация в structurizr");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        log.info("process_id: {}, docId: {}", processId, docId);
        TypeProcess typeProcess = null;
        try {
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            log.info("camundaProcess : {}", camundaProcess);
            typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            log.info("typeProcess : {}", typeProcess);
            archClient.publicToStructurizr(docId);
            saveAlias(processId, "strct", typeProcess);
            log.info("Шаг: Публикация в structurizr, успешно завершен.");
        } catch (Exception e) {
            log.error("Ошибка при Публикация в structurizr. Создание записи с ошибкой", e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TypeProcess finalTypeProcess = typeProcess;
            tt.execute(status -> {
                saveAlias(processId, "strcterr", finalTypeProcess);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Публикация в structurizr");
        }
    }
}

