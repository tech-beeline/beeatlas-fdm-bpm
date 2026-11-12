package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.ProcessEngineServices;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatapipeBranchProcessTest {

    private static DelegateExecution execution(String processDefinitionId) {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getProcessDefinitionId()).thenReturn(processDefinitionId);
        return execution;
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "Process_datapipeline_branch:3:8f1c-11, Process_datapipeline_branch",
            "Process_08wprkp:12:aa01, Process_08wprkp",
            "null, null"
    })
    void extractsDefinitionKey(String processDefinitionId, String key) {
        DelegateExecution execution = execution(processDefinitionId);

        assertThat(DatapipeBranchProcess.processDefinitionKey(execution)).isEqualTo(key);
        verify(execution, never()).getProcessEngineServices();
    }

    @Test
    @DisplayName("processDefinitionId — голый UUID (key:version:id длиннее 64 символов): ключ из определения процесса")
    void resolvesKeyOfUuidDefinitionId() {
        String uuid = "79a8fed2-d077-11f1-8dc2-9eea91d79369";
        DelegateExecution execution = execution(uuid);
        ProcessEngineServices services = mock(ProcessEngineServices.class);
        RepositoryService repositoryService = mock(RepositoryService.class);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(execution.getProcessEngineServices()).thenReturn(services);
        when(services.getRepositoryService()).thenReturn(repositoryService);
        when(repositoryService.getProcessDefinition(uuid)).thenReturn(definition);
        when(definition.getKey()).thenReturn("Process_datapipeline_branch");

        assertThat(DatapipeBranchProcess.processDefinitionKey(execution)).isEqualTo("Process_datapipeline_branch");
        assertThat(DatapipeBranchProcess.isBranchProcess(execution)).isTrue();
    }
}
