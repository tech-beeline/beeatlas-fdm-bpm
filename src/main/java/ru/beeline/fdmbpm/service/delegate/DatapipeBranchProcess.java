/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.repository.ProcessDefinition;

/**
 * Процесс Process_datapipeline_branch: сохранение архитектуры продукта в ветке через staging.
 * Делегаты, общие с Process_08wprkp, различают процессы по ключу definition.
 */
public final class DatapipeBranchProcess {

    public static final String PROCESS_KEY = "Process_datapipeline_branch";
    public static final String TYPE_ALIAS = "DatapipeBranch";
    public static final String DATAPIPE_TYPE_ALIAS = "Datapipe";
    public static final int MAX_BRANCH_LENGTH = 64;

    public static final String BRANCH_VARIABLE = "branch";
    public static final String STAGING_RUN_ID_VARIABLE = "stagingRunId";
    public static final String STAGING_STATUS_VARIABLE = "stagingStatus";
    public static final String STAGING_ARTIFACT_TYPE_VARIABLE = "stagingArtifactType";
    public static final String STAGING_FAILED = "failed";

    private DatapipeBranchProcess() {
    }

    public static boolean isBranchProcess(DelegateExecution execution) {
        return PROCESS_KEY.equals(processDefinitionKey(execution));
    }

    /**
     * Ключ definition процесса. Camunda пишет processDefinitionId как {@code key:version:id}, только пока
     * он не длиннее 64 символов, иначе id — голый UUID. У Process_datapipeline_branch id всегда UUID,
     * поэтому ключ без двоеточия берётся из определения процесса.
     */
    static String processDefinitionKey(DelegateExecution execution) {
        String processDefinitionId = execution.getProcessDefinitionId();
        if (processDefinitionId == null) {
            return null;
        }
        int colon = processDefinitionId.indexOf(':');
        if (colon > 0) {
            return processDefinitionId.substring(0, colon);
        }
        ProcessDefinition definition = execution.getProcessEngineServices().getRepositoryService()
                .getProcessDefinition(processDefinitionId);
        return definition == null ? null : definition.getKey();
    }

    /**
     * Переменная старта branch после обрезки пробелов по краям, регистр не меняется.
     */
    public static String branch(DelegateExecution execution) {
        Object value = execution.getVariable(BRANCH_VARIABLE);
        return value == null ? null : value.toString().trim();
    }
}
