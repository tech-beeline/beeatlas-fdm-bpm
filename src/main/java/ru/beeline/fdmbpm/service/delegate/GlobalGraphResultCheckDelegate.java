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
@Component("GlobalGraphResultCheckDelegate")
public class GlobalGraphResultCheckDelegate extends StatusLogic implements JavaDelegate {
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("Шаг: Проверка результата глобального графа");
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Boolean doneGlobalGraph = (Boolean) delegateExecution.getVariable("doneGlobalGraph");

        if (!doneGlobalGraph) {
            log.info("Ошибка при проверке результата локального графа. Создание записи с ошибкой");
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            log.info("Обработка процесса. processId={}, procId={}, businessKey={}, typeProcessId={}",
                     processId,
                     camundaProcess.getProcId(),
                     camundaProcess.getBusinessKey(),
                     camundaProcess.getTypeProcessId());
            TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TypeProcess finalTypeProcess = typeProcess;
            tt.execute(status -> {
                saveAlias(processId, "errglblgrph", finalTypeProcess);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Проверка результата глобального графа");
        }
    }
}
