/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.beeline.fdmbpm.client.StagingClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.staging.PipelineRunDTO;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;
import ru.beeline.fdmbpm.utils.StepErrorMessage;

import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_ARTIFACT_TYPE_VARIABLE;
import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_FAILED;
import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_RUN_ID_VARIABLE;
import static ru.beeline.fdmbpm.service.delegate.DatapipeBranchProcess.STAGING_STATUS_VARIABLE;

/**
 * Запуск прогона staging. Ошибка запуска не бросает исключение: stagingStatus=failed,
 * и шлюз после ожидания уводит процесс к завершению.
 */
@Slf4j
public abstract class StartStagingPipelineDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    StagingClient stagingClient;

    @Autowired
    ContextRepository contextRepository;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    private final StagingArtifact artifact;

    protected StartStagingPipelineDelegate(StagingArtifact artifact) {
        this.artifact = artifact;
    }

    @Override
    public void execute(DelegateExecution execution) {
        Integer processId = (Integer) execution.getVariable("process_id");
        String cmdb = (String) execution.getVariable("cmdb");
        Integer docId = (Integer) execution.getVariable("docId");
        String branch = DatapipeBranchProcess.branch(execution);
        log.info("Шаг: Запуск пайпа staging {}: cmdb={}, process_id={}, docId={}, branch={}",
                artifact.getType(), cmdb, processId, docId, branch);
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        execution.setVariable(STAGING_ARTIFACT_TYPE_VARIABLE, artifact.getType());

        Long runId;
        try {
            String artifactUid = contextValue(processId, artifact.artifactUidContextName());
            if (artifactUid == null) {
                throw new IllegalStateException("в processes.context нет " + artifact.artifactUidContextName());
            }
            ResponseEntity<PipelineRunDTO> response =
                    stagingClient.startPipelineRun(artifact.getType(), artifactUid, branch, docId);
            if (response.getStatusCode().value() != HttpStatus.ACCEPTED.value()) {
                throw new IllegalStateException("staging не принял запуск: HTTP " + response.getStatusCode().value());
            }
            if (response.getBody() == null || response.getBody().getRunId() == null) {
                throw new IllegalStateException("staging не вернул runId");
            }
            runId = response.getBody().getRunId();
        } catch (Exception e) {
            log.error("❌ Ошибка запуска пайпа staging {} (cmdb={}, process_id={}, docId={}, branch={})",
                    artifact.getType(), cmdb, processId, docId, branch, e);
            execution.removeVariable(STAGING_RUN_ID_VARIABLE);
            execution.setVariable(STAGING_STATUS_VARIABLE, STAGING_FAILED);
            saveAlias(processId, artifact.getErrorAlias(), typeProcess,
                    StepErrorMessage.of("Запуск: " + artifact.getTitle(), e));
            return;
        }

        log.info("Пайп staging {} запущен: runId={}, process_id={}", artifact.getType(), runId, processId);
        execution.setVariable(STAGING_RUN_ID_VARIABLE, runId);
        execution.removeVariable(STAGING_STATUS_VARIABLE);
        contextRepository.save(Context.builder()
                .name(artifact.runIdContextName())
                .value(runId.toString())
                .camundaProcessId(processId)
                .build());
        saveAlias(processId, artifact.getStartedAlias(), typeProcess);
    }

    private String contextValue(Integer processId, String name) {
        return contextRepository.findByCamundaProcessId(processId).stream()
                .filter(context -> name.equals(context.getName()))
                .map(Context::getValue)
                .findFirst()
                .orElse(null);
    }
}
