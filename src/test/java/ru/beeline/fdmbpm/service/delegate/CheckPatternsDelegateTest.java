package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.TechradarClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;
import ru.beeline.fdmbpm.dto.techradar.PatternDTO;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CheckPatternsDelegateTest {

    private static final int PROCESS_ID = 42;

    private CheckPatternsDelegate delegate;
    private ProductClient productClient;
    private TechradarClient techradarClient;
    private GraphClient graphClient;
    private CamundaProcessStatusRepository camundaProcessStatusRepository;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        delegate = new CheckPatternsDelegate();
        productClient = mock(ProductClient.class);
        techradarClient = mock(TechradarClient.class);
        graphClient = mock(GraphClient.class);
        camundaProcessStatusRepository = mock(CamundaProcessStatusRepository.class);
        CamundaProcessRepository camundaProcessRepository = mock(CamundaProcessRepository.class);
        TypeProcessRepository typeProcessRepository = mock(TypeProcessRepository.class);
        StatusProcessRepository statusProcessRepository = mock(StatusProcessRepository.class);
        ReflectionTestUtils.setField(delegate, "productClient", productClient);
        ReflectionTestUtils.setField(delegate, "techradarClient", techradarClient);
        ReflectionTestUtils.setField(delegate, "graphClient", graphClient);
        delegate.camundaProcessRepository = camundaProcessRepository;
        delegate.typeProcessRepository = typeProcessRepository;
        delegate.transactionManager = mock(PlatformTransactionManager.class);
        delegate.statusProcessRepository = statusProcessRepository;
        delegate.camundaProcessStatusRepository = camundaProcessStatusRepository;

        when(camundaProcessRepository.findById(PROCESS_ID)).thenReturn(Optional.of(
                CamundaProcess.builder().id(PROCESS_ID).typeProcessId(1).build()));
        when(typeProcessRepository.findById(1)).thenReturn(Optional.of(new TypeProcess(1, "Datapipe", null, "Datapipe")));
        when(statusProcessRepository.findByAliasAndTypeProcessId(CheckPatternsDelegate.STATUS_DONE, 1))
                .thenReturn(new StatusProcess(401, 1, "done", CheckPatternsDelegate.STATUS_DONE, false, false, 14));
        when(statusProcessRepository.findByAliasAndTypeProcessId(CheckPatternsDelegate.STATUS_ERROR, 1))
                .thenReturn(new StatusProcess(402, 1, "error", CheckPatternsDelegate.STATUS_ERROR, false, true, 14));

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("cmdb")).thenReturn("BC-1");
        when(execution.getVariable("docId")).thenReturn(77);
        when(execution.getVariable("process_id")).thenReturn(PROCESS_ID);
    }

    @Test
    void postsV2WithPipelineSourceIdAndNullBranchDefaultsToMainInClient() {
        when(productClient.getPatternIdsByProductAlias("BC-1")).thenReturn(List.of(10));
        when(techradarClient.getPatterns()).thenReturn(List.of(PatternDTO.builder()
                .id(10)
                .code("P-OK")
                .rule("g.V().has('cmdb','{cmdb}')")
                .build()));
        when(graphClient.executePatternQuery("g.V().has('cmdb','BC-1')")).thenReturn(List.of(new Object()));

        delegate.execute(execution);

        verify(productClient).postPatternCheckResults(eq("BC-1"), eq("pipeline"), eq(77), eq(null), anyList());
        verify(productClient, never()).getPatternIdsByProductAlias(anyString(), anyString());
        assertThat(savedStatusId()).isEqualTo(401);
    }

    @Test
    void datapipeProcessReadsBranchVariableWhenPresent() {
        when(execution.getVariable("branch")).thenReturn("design");
        when(productClient.getPatternIdsByProductAlias("BC-1")).thenReturn(List.of(10));
        when(techradarClient.getPatterns()).thenReturn(List.of(PatternDTO.builder()
                .id(10)
                .code("P-OK")
                .rule("g.V().has('cmdb','{cmdb}')")
                .build()));
        when(graphClient.executePatternQuery("g.V().has('cmdb','BC-1')")).thenReturn(List.of(new Object()));

        delegate.execute(execution);

        verify(productClient).postPatternCheckResults(eq("BC-1"), eq("pipeline"), eq(77), eq("design"), anyList());
        assertThat(savedStatusId()).isEqualTo(401);
    }

    @Test
    void branchProcessReadsAndWritesPatternsOfProcessBranch() {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:1:c0ffee");
        when(execution.getVariable("branch")).thenReturn(" develop ");
        when(productClient.getPatternIdsByProductAlias("BC-1", "develop")).thenReturn(List.of(10, 11));
        when(techradarClient.getPatterns()).thenReturn(List.of(
                PatternDTO.builder().id(10).code("P-OK").rule("g.V().has('cmdb','{cmdb}')").build(),
                PatternDTO.builder().id(11).code("P-NO").rule("g.V().has('x','{cmdb}')").build()));
        when(graphClient.executePatternQuery("g.V().has('cmdb','BC-1')")).thenReturn(List.of(new Object()));
        when(graphClient.executePatternQuery("g.V().has('x','BC-1')")).thenReturn(List.of());

        delegate.execute(execution);

        ArgumentCaptor<List<PatternCheckResultDTO>> results = ArgumentCaptor.forClass(List.class);
        verify(productClient).postPatternCheckResults(eq("BC-1"), eq("pipeline"), eq(77), eq("develop"),
                results.capture());
        assertThat(results.getValue()).extracting(PatternCheckResultDTO::getCode, PatternCheckResultDTO::getIsCheck)
                .containsExactly(tuple("P-OK", true), tuple("P-NO", false));
        verify(productClient, never()).getPatternIdsByProductAlias("BC-1");
        verifyNoInteractions(camundaProcessStatusRepository);
    }

    @Test
    void branchProcessWithoutPatternsDoesNotPost() {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:1:c0ffee");
        when(execution.getVariable("branch")).thenReturn("design");
        when(productClient.getPatternIdsByProductAlias("BC-1", "design")).thenReturn(List.of());

        delegate.execute(execution);

        verify(productClient, never()).postPatternCheckResults(anyString(), anyString(), any(), anyString(), anyList());
        verifyNoInteractions(camundaProcessStatusRepository);
    }

    @Test
    void emptyPatternListWritesPtrncrt() {
        when(productClient.getPatternIdsByProductAlias("BC-1")).thenReturn(List.of());

        delegate.execute(execution);

        verify(productClient, never()).postPatternCheckResults(anyString(), anyString(), any(), anyString(), anyList());
        assertThat(savedStatusId()).isEqualTo(401);
    }

    @Test
    void nullCmdbWritesPtrncrt() {
        when(execution.getVariable("cmdb")).thenReturn(null);

        delegate.execute(execution);

        verifyNoInteractions(productClient);
        assertThat(savedStatusId()).isEqualTo(401);
    }

    @Test
    void unexpectedFailureWritesPtrnerrAndRethrows() {
        when(productClient.getPatternIdsByProductAlias("BC-1")).thenReturn(List.of(10));
        when(techradarClient.getPatterns()).thenThrow(new IllegalStateException("techradar down"));

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("techradar down");

        verify(productClient, never()).postPatternCheckResults(anyString(), anyString(), any(), anyString(), anyList());
        assertThat(savedStatusId()).isEqualTo(402);
    }

    @Test
    void collapsesLineBreaksAfterCmdbReplacement() {
        when(productClient.getPatternIdsByProductAlias("BC-1")).thenReturn(List.of(10));
        when(techradarClient.getPatterns()).thenReturn(List.of(PatternDTO.builder()
                .id(10)
                .code("P-OK")
                .rule("g.V()\n.has('a','{cmdb}')\r\n.has('b','{cmdb}')")
                .build()));
        when(graphClient.executePatternQuery("g.V() .has('a','BC-1') .has('b','BC-1')")).thenReturn(List.of());

        delegate.execute(execution);

        verify(graphClient).executePatternQuery("g.V() .has('a','BC-1') .has('b','BC-1')");
    }

    private Integer savedStatusId() {
        ArgumentCaptor<CamundaProcessStatus> status = ArgumentCaptor.forClass(CamundaProcessStatus.class);
        verify(camundaProcessStatusRepository).saveAndFlush(status.capture());
        assertThat(status.getValue().getCamundaProcessId()).isEqualTo(PROCESS_ID);
        return status.getValue().getStatusProcessId();
    }
}
