package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.TechradarClient;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;
import ru.beeline.fdmbpm.dto.techradar.PatternDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckPatternsDelegateTest {

    private CheckPatternsDelegate delegate;
    private ProductClient productClient;
    private TechradarClient techradarClient;
    private GraphClient graphClient;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        delegate = new CheckPatternsDelegate();
        productClient = mock(ProductClient.class);
        techradarClient = mock(TechradarClient.class);
        graphClient = mock(GraphClient.class);
        ReflectionTestUtils.setField(delegate, "productClient", productClient);
        ReflectionTestUtils.setField(delegate, "techradarClient", techradarClient);
        ReflectionTestUtils.setField(delegate, "graphClient", graphClient);

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("cmdb")).thenReturn("BC-1");
        when(execution.getVariable("docId")).thenReturn(77);
    }

    @Test
    void postsV2WithPipelineSourceIdAndMainBranch() {
        when(productClient.getPatternIdsByProductAlias("BC-1")).thenReturn(List.of(10));
        when(techradarClient.getPatterns()).thenReturn(List.of(PatternDTO.builder()
                .id(10)
                .code("P-OK")
                .rule("g.V().has('cmdb','{cmdb}')")
                .build()));
        when(graphClient.executePatternQuery("g.V().has('cmdb','BC-1')")).thenReturn(List.of(new Object()));

        delegate.execute(execution);

        verify(productClient).postPatternCheckResults(eq("BC-1"), eq("pipeline"), eq(77), eq("main"), anyList());
        verify(productClient, never()).getPatternIdsByProductAlias(anyString(), anyString());
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
    }

    @Test
    void branchProcessWithoutPatternsDoesNotPost() {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:1:c0ffee");
        when(execution.getVariable("branch")).thenReturn("design");
        when(productClient.getPatternIdsByProductAlias("BC-1", "design")).thenReturn(List.of());

        delegate.execute(execution);

        verify(productClient, never()).postPatternCheckResults(anyString(), anyString(), any(), anyString(), anyList());
    }
}
