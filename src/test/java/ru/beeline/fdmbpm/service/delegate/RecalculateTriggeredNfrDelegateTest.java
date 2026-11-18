package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.beeline.fdmbpm.client.FfManagerClient;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;
import ru.beeline.fdmbpm.dto.product.NfrCatalogItemDTO;
import ru.beeline.fdmbpm.dto.product.NfrDetailsV2BpmDTO;
import ru.beeline.fdmbpm.dto.product.NfrPatternBpmDTO;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecalculateTriggeredNfrDelegateTest {

    private RecalculateTriggeredNfrDelegate delegate;
    private ProductClient productClient;
    private FfManagerClient ffManagerClient;
    private GraphClient graphClient;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        delegate = new RecalculateTriggeredNfrDelegate();
        productClient = mock(ProductClient.class);
        ffManagerClient = mock(FfManagerClient.class);
        graphClient = mock(GraphClient.class);
        ReflectionTestUtils.setField(delegate, "productClient", productClient);
        ReflectionTestUtils.setField(delegate, "ffManagerClient", ffManagerClient);
        ReflectionTestUtils.setField(delegate, "graphClient", graphClient);

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("product")).thenReturn("APP");
        when(execution.getVariable("ffCode")).thenReturn("FF-A");
        when(execution.getVariable(RecalculateDependentFfDelegate.RECALCULATED_FF_CODES))
                .thenReturn(List.of("FF-B"));
    }

    @Test
    void assignsNfrTriggeredBySourceOrRecalculatedFfAndChecksItsPatterns() {
        when(productClient.getAllNfr()).thenReturn(List.of(
                nfr(1, "N1", "FF-A"),
                nfr(2, "N2", "FF-B, FF-Z"),
                nfr(3, "N3", "OTHER"),
                nfr(4, "N4", "ff-b")));
        when(ffManagerClient.getMergedActualResultsForNfr("APP")).thenReturn(List.of(
                check("FF-A", true),
                check("FF-B", true),
                check("FF-Z", false)));
        when(productClient.getNfrDetailsById(1)).thenReturn(NfrDetailsV2BpmDTO.builder()
                .id(1)
                .patterns(List.of(NfrPatternBpmDTO.builder().code("P1").rule("g.V().has('cmdb','{cmdb}')").build()))
                .build());
        when(productClient.getNfrDetailsById(4)).thenReturn(NfrDetailsV2BpmDTO.builder()
                .id(4)
                .patterns(List.of())
                .build());
        when(graphClient.executePatternQuery("g.V().has('cmdb','APP')")).thenReturn(List.of(new Object()));

        delegate.execute(execution);

        verify(productClient).postProductNfr(eq("APP"), eq(List.of(1, 4)), isNull());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PatternCheckResultDTO>> results = ArgumentCaptor.forClass(List.class);
        verify(productClient).postPatternCheckResults(eq("APP"), eq("nfr"), isNull(), isNull(), results.capture());
        assertThat(results.getValue()).singleElement().satisfies(result -> {
            assertThat(result.getCode()).isEqualTo("P1");
            assertThat(result.getIsCheck()).isTrue();
        });
        verify(productClient, never()).postPatternCheckResults(eq("APP"), eq("nfr"), isNull(), isNull(), eq(List.of()));
    }

    @Test
    void doesNotAssignOrCheckPatternsWhenRuleDoesNotPass() {
        when(productClient.getAllNfr()).thenReturn(List.of(nfr(2, "N2", "FF-A, FF-Z")));
        when(ffManagerClient.getMergedActualResultsForNfr("APP")).thenReturn(List.of(
                check("FF-A", true),
                check("FF-Z", false)));

        delegate.execute(execution);

        verify(productClient, never()).postProductNfr(any(), any(), any());
        verify(productClient, never()).getNfrDetailsById(any());
        verify(graphClient, never()).executePatternQuery(any());
    }

    private static NfrCatalogItemDTO nfr(int id, String code, String rule) {
        return NfrCatalogItemDTO.builder().id(id).code(code).rule(rule).build();
    }

    private static AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO check(String code, boolean isCheck) {
        return AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO.builder()
                .code(code)
                .isCheck(isCheck)
                .build();
    }
}
