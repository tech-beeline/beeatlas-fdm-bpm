package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.NfrDetailsV2BpmDTO;
import ru.beeline.fdmbpm.dto.product.NfrPatternBpmDTO;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignNfrAndCalcPatternDelegateTest {

    private AssignNfrAndCalcPatternDelegate delegate;
    private ProductClient productClient;
    private GraphClient graphClient;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        delegate = new AssignNfrAndCalcPatternDelegate();
        productClient = mock(ProductClient.class);
        graphClient = mock(GraphClient.class);
        ReflectionTestUtils.setField(delegate, "productClient", productClient);
        ReflectionTestUtils.setField(delegate, "graphClient", graphClient);

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("nfrId")).thenReturn(5);
        when(execution.getVariable("product")).thenReturn("BC-1");
        when(execution.getVariable("cmdb")).thenReturn("BC-1");
    }

    @Test
    void postsV2WithNfrWithoutSourceIdAndMainBranch() {
        when(productClient.getNfrDetailsById(5)).thenReturn(NfrDetailsV2BpmDTO.builder()
                .id(5)
                .patterns(List.of(NfrPatternBpmDTO.builder()
                        .code("P-NFR")
                        .rule("g.V()")
                        .build()))
                .build());
        when(graphClient.executePatternQuery("g.V()")).thenReturn(List.of(new Object()));

        delegate.execute(execution);

        ArgumentCaptor<List<PatternCheckResultDTO>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(productClient).postPatternCheckResults(
                eq("BC-1"), eq("nfr"), isNull(), eq("main"), resultsCaptor.capture());
        assertThat(resultsCaptor.getValue()).extracting(PatternCheckResultDTO::getCode).containsExactly("P-NFR");
    }
}
