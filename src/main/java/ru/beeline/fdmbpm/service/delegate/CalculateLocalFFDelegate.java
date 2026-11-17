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
import ru.beeline.fdmbpm.client.FfManagerClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.ffmanager.PostFfManagerDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;
import ru.beeline.fdmbpm.utils.StepErrorMessage;

@Slf4j
@Component("CalculateLocalFFDelegate")
public class CalculateLocalFFDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private FfManagerClient ffManagerClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        Integer processId = (Integer) delegateExecution.getVariable("process_id");
        Integer docId = (Integer) delegateExecution.getVariable("docId");
        String cmdb = (String) delegateExecution.getVariable("cmdb");
        String branch = DatapipeBranchProcess.branch(delegateExecution);
        log.info("ℹ️ [pipeline] Шаг: Расчёт локальных FF | cmdb={}, process_id={}, docId={}, branch={} "
                + "(результаты FF нужны для назначения НФТ)", cmdb, processId, docId, branch);
        TypeProcess typeProcess = null;
        try {
            log.info("ℹ️ [pipeline/ff] POST FF Manager run-all | app={}, docId={}, source_id={}, branch={}",
                    cmdb, docId, processId, branch);
            ffManagerClient.postFfManager(PostFfManagerDTO.builder().app(cmdb).build(), docId, processId, branch);
            CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
            typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
            saveAlias(processId, "ffdn", typeProcess);
            log.info("ℹ️ [pipeline] Шаг завершён: Расчёт локальных FF | cmdb={}, process_id={}, statusAlias=ffdn. "
                            + "Внимание: FfManagerClient глотает HTTP-ошибки — смотри логи [ff-manager]",
                    cmdb, processId);
        } catch (Exception e) {
            log.error("ℹ️ [pipeline] Ошибка на шаге Расчёт локальных FF | cmdb={}, process_id={}",
                    cmdb, processId, e);
            String message = StepErrorMessage.of("Расчет Фитнес функции", e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TypeProcess finalTypeProcess = typeProcess;
            tt.execute(status -> {
                saveAlias(processId, "fferrcd", finalTypeProcess, message);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Расчет Фитнес функции", e);
        }
    }
}
