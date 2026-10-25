package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import ru.beeline.fdmbpm.client.StagingClient;
import ru.beeline.fdmbpm.dto.staging.PipelineRunStatusDTO;
import ru.beeline.fdmbpm.exception.ProcessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.beeline.fdmbpm.service.delegate.StagingDelegatesTestSupport.PROCESS_ID;

class WaitStagingPipelineDelegateTest {

    private StagingDelegatesTestSupport support;
    private StagingClient stagingClient;
    private WaitStagingPipelineDelegate delegate;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        support = new StagingDelegatesTestSupport();
        stagingClient = mock(StagingClient.class);
        delegate = new WaitStagingPipelineDelegate();
        support.wire(delegate);
        delegate.stagingClient = stagingClient;
        delegate.camundaProcessRepository = support.camundaProcessRepository;
        delegate.typeProcessRepository = support.typeProcessRepository;
        delegate.transactionManager = mock(PlatformTransactionManager.class);

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("process_id")).thenReturn(PROCESS_ID);
        when(execution.getVariable("stagingRunId")).thenReturn(501L);
    }

    private void givenArtifact(String artifactType) {
        when(execution.getVariable("stagingArtifactType")).thenReturn(artifactType);
    }

    private void givenStatus(String status) {
        when(stagingClient.waitPipelineRunTerminal(501L))
                .thenReturn(new PipelineRunStatusDTO(501L, null, status, null, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "awaiting_review", "reviewing", "applying"})
    @DisplayName("Нетерминальный статус и таймаут long-poll — stagingStatus записан, статус data-pipe не меняется")
    void keepsWaitingOnNonTerminalStatus(String status) {
        givenArtifact("structurizr-sequence");
        givenStatus(status);

        delegate.execute(execution);

        verify(execution).setVariable("stagingStatus", status);
        assertThat(support.savedAliases()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "structurizr-architecture, completed, stgarchdn",
            "structurizr-architecture, completed_without_publish, stgarchdn",
            "structurizr-architecture, failed, stgarcherr",
            "structurizr-architecture, cancelled, stgarcherr",
            "structurizr-sequence, completed, stgseqdn",
            "structurizr-sequence, completed_without_publish, stgseqwop",
            "structurizr-sequence, failed, stgseqerr",
            "structurizr-sequence, cancelled, stgseqerr"
    })
    @DisplayName("Терминальный статус — stagingStatus для шлюза и статус data-pipe, исключения нет")
    void savesTerminalStatus(String artifactType, String status, String alias) {
        givenArtifact(artifactType);
        givenStatus(status);

        delegate.execute(execution);

        verify(execution).setVariable("stagingStatus", status);
        assertThat(support.savedAliases()).containsExactly(alias);
    }

    @Test
    @DisplayName("Повторный опрос терминального статуса не дублирует запись")
    void doesNotDuplicateTerminalStatus() {
        givenArtifact("structurizr-architecture");
        givenStatus("completed");
        support.givenStatusAlreadySaved("stgarchdn");

        delegate.execute(execution);

        assertThat(support.savedAliases()).isEmpty();
    }

    @Test
    @DisplayName("Ошибка GET — stgseqerr, stagingStatus=failed и исключение")
    void failsWhenStatusRequestFails() {
        givenArtifact("structurizr-sequence");
        when(stagingClient.waitPipelineRunTerminal(501L)).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> delegate.execute(execution)).isInstanceOf(ProcessException.class);

        verify(execution).setVariable("stagingStatus", "failed");
        assertThat(support.savedAliases()).containsExactly("stgseqerr");
    }

    @Test
    @DisplayName("Запуск не удался — staging не опрашивается, статус ошибки уже записан шагом запуска")
    void skipsWhenStartFailed() {
        givenArtifact("structurizr-architecture");
        when(execution.getVariable("stagingRunId")).thenReturn(null);
        when(execution.getVariable("stagingStatus")).thenReturn("failed");

        delegate.execute(execution);

        verifyNoInteractions(stagingClient);
        assertThat(support.savedAliases()).isEmpty();
    }
}
