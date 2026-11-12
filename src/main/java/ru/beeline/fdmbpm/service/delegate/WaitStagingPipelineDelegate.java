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
import ru.beeline.fdmbpm.client.StagingClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.staging.PipelineRunStatusDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;
import ru.beeline.fdmbpm.utils.StepErrorMessage;

import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_ARTIFACT_TYPE_VARIABLE;
import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_FAILED;
import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_RUN_ID_VARIABLE;
import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_STATUS_VARIABLE;

/**
 * Ожидание терминального статуса прогона staging. Нетерминальный статус и таймаут long-poll
 * статус data-pipe не меняют: шлюз ждёт PT1M и вызывает делегат снова.
 */
@Slf4j
@Component("WaitStagingPipelineDelegate")
public class WaitStagingPipelineDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    StagingClient stagingClient;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution execution) {
        Integer processId = (Integer) execution.getVariable("process_id");
        StagingArtifact artifact =
                StagingArtifact.byType((String) execution.getVariable(STAGING_ARTIFACT_TYPE_VARIABLE));
        Object runIdValue = execution.getVariable(STAGING_RUN_ID_VARIABLE);
        String currentStatus = (String) execution.getVariable(STAGING_STATUS_VARIABLE);
        log.info("Шаг: Ожидание пайпа staging {}: process_id={}, runId={}, stagingStatus={}",
                artifact.getType(), processId, runIdValue, currentStatus);
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();

        if (runIdValue == null && StagingClient.TERMINAL_STATUSES.contains(currentStatus)) {
            log.info("Прогон {} не создан, статус {} уже записан шагом запуска", artifact.getType(), currentStatus);
            return;
        }

        PipelineRunStatusDTO response;
        try {
            if (runIdValue == null) {
                throw new IllegalStateException("не задана переменная " + STAGING_RUN_ID_VARIABLE);
            }
            response = stagingClient.waitPipelineRunTerminal(((Number) runIdValue).longValue());
        } catch (Exception e) {
            log.error("❌ Ошибка запроса статуса пайпа staging {} (process_id={}, runId={})",
                    artifact.getType(), processId, runIdValue, e);
            execution.setVariable(STAGING_STATUS_VARIABLE, STAGING_FAILED);
            String message = StepErrorMessage.of("Ожидание: " + artifact.getTitle(), e);
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tt.execute(status -> {
                saveAliasOnce(processId, artifact.getErrorAlias(), typeProcess, message);
                return null;
            });
            throw new ProcessException("Ошибка процесса на шаге: Ожидание пайпа staging " + artifact.getType(), e);
        }

        String stagingStatus = response.getStatus();
        execution.setVariable(STAGING_STATUS_VARIABLE, stagingStatus);
        String alias = artifact.terminalAlias(stagingStatus);
        if (alias == null) {
            log.info("Прогон {} runId={} ещё не завершён: status={}, stage={}, more={}",
                    artifact.getType(), response.getRunId(), stagingStatus, response.getStage(), response.isMore());
            return;
        }
        log.info("Прогон {} runId={} завершён: status={}, статус data-pipe {}",
                artifact.getType(), response.getRunId(), stagingStatus, alias);
        saveAliasOnce(processId, alias, typeProcess, null);
    }

    private void saveAliasOnce(Integer processId, String alias, TypeProcess typeProcess, String message) {
        StatusProcess statusProcess = statusProcessRepository.findByAliasAndTypeProcessId(alias, typeProcess.getId());
        if (camundaProcessStatusRepository.findByCamundaProcessIdAndStatusProcessId(processId,
                statusProcess.getId()).isEmpty()) {
            saveAlias(processId, alias, typeProcess, message);
        }
    }
}
