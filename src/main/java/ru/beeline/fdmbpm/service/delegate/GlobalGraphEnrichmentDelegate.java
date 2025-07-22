package ru.beeline.fdmbpm.service.delegate;


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

@Slf4j
@Component("GlobalGraphEnrichmentDelegate")
public class GlobalGraphEnrichmentDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    GraphClient graphClient;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

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
            graphClient.postGlobalGraph(docId);
            saveAlias(processId, "glblgrph", typeProcess);
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
