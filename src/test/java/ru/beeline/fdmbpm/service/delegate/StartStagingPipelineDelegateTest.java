package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.beeline.fdmbpm.client.StagingClient;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.dto.staging.PipelineRunDTO;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.beeline.fdmbpm.service.delegate.StagingDelegatesTestSupport.PROCESS_ID;

class StartStagingPipelineDelegateTest {

    private StagingDelegatesTestSupport support;
    private StagingClient stagingClient;
    private ContextRepository contextRepository;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        support = new StagingDelegatesTestSupport();
        stagingClient = mock(StagingClient.class);
        contextRepository = mock(ContextRepository.class);
        when(contextRepository.findByCamundaProcessId(PROCESS_ID)).thenReturn(List.of(
                context("cmdb", "BC-1"),
                context("artifactUid.structurizr-architecture", "BC-1"),
                context("artifactUid.structurizr-sequence", "BC-1")));

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("process_id")).thenReturn(PROCESS_ID);
        when(execution.getVariable("cmdb")).thenReturn("BC-1");
        when(execution.getVariable("docId")).thenReturn(77);
        when(execution.getVariable("branch")).thenReturn("  develop ");
    }

    private <T extends StartStagingPipelineDelegate> T wire(T delegate) {
        support.wire(delegate);
        delegate.stagingClient = stagingClient;
        delegate.contextRepository = contextRepository;
        delegate.camundaProcessRepository = support.camundaProcessRepository;
        delegate.typeProcessRepository = support.typeProcessRepository;
        return delegate;
    }

    @Test
    @DisplayName("Архитектура 202 — stagingRunId, stagingStatus удалён, runId в context, статус stgarchcrt")
    void startsArchitecture() {
        when(stagingClient.startPipelineRun("structurizr-architecture", "BC-1", "develop", 77))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(new PipelineRunDTO(501L)));

        wire(new StartStructurizrArchitectureDelegate()).execute(execution);

        verify(execution).setVariable("stagingArtifactType", "structurizr-architecture");
        verify(execution).setVariable("stagingRunId", 501L);
        verify(execution).removeVariable("stagingStatus");
        verify(execution, never()).setVariable("stagingStatus", "failed");
        assertThat(savedContext()).extracting(Context::getName, Context::getValue)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("runId.structurizr-architecture", "501"));
        assertThat(support.savedAliases()).containsExactly("stgarchcrt");
    }

    @Test
    @DisplayName("Dynamic view 202 — свой artifactType, runId.structurizr-sequence, статус stgseqcrt")
    void startsSequence() {
        when(stagingClient.startPipelineRun("structurizr-sequence", "BC-1", "develop", 77))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(new PipelineRunDTO(502L)));

        wire(new StartStructurizrSequenceDelegate()).execute(execution);

        verify(execution).setVariable("stagingArtifactType", "structurizr-sequence");
        verify(execution).setVariable("stagingRunId", 502L);
        assertThat(savedContext()).extracting(Context::getName).containsExactly("runId.structurizr-sequence");
        assertThat(support.savedAliases()).containsExactly("stgseqcrt");
    }

    @Test
    @DisplayName("Ответ не 202 — stagingStatus=failed, stgarcherr, context не пишется, исключения нет")
    void failsWithoutExceptionWhenNotAccepted() {
        when(stagingClient.startPipelineRun(anyString(), anyString(), anyString(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        wire(new StartStructurizrArchitectureDelegate()).execute(execution);

        verify(execution).setVariable("stagingStatus", "failed");
        verify(execution).removeVariable("stagingRunId");
        verify(contextRepository, never()).save(any());
        assertThat(support.savedAliases()).containsExactly("stgarcherr");
    }

    @Test
    @DisplayName("202 без runId — ошибка запуска stgseqerr")
    void failsWhenRunIdMissing() {
        when(stagingClient.startPipelineRun(anyString(), anyString(), anyString(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(new PipelineRunDTO(null)));

        wire(new StartStructurizrSequenceDelegate()).execute(execution);

        verify(execution).setVariable("stagingStatus", "failed");
        verify(contextRepository, never()).save(any());
        assertThat(support.savedAliases()).containsExactly("stgseqerr");
    }

    @Test
    @DisplayName("Нет artifactUid в context — staging не вызывается, ошибка запуска")
    void failsWhenArtifactUidMissing() {
        when(contextRepository.findByCamundaProcessId(PROCESS_ID)).thenReturn(List.of(context("cmdb", "BC-1")));

        wire(new StartStructurizrArchitectureDelegate()).execute(execution);

        verifyNoInteractions(stagingClient);
        verify(execution).setVariable("stagingStatus", "failed");
        assertThat(support.savedAliases()).containsExactly("stgarcherr");
    }

    private List<Context> savedContext() {
        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(contextRepository).save(captor.capture());
        return captor.getAllValues();
    }

    private static Context context(String name, String value) {
        return Context.builder().camundaProcessId(PROCESS_ID).name(name).value(value).build();
    }
}
