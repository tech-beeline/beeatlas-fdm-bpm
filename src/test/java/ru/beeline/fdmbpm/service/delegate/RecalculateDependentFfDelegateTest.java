package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.beeline.fdmbpm.client.FfManagerClient;
import ru.beeline.fdmbpm.dto.ffmanager.FitnessFunctionCatalogDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecalculateDependentFfDelegateTest {

    private RecalculateDependentFfDelegate delegate;
    private FfManagerClient ffManagerClient;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        delegate = new RecalculateDependentFfDelegate();
        ffManagerClient = mock(FfManagerClient.class);
        ReflectionTestUtils.setField(delegate, "ffManagerClient", ffManagerClient);
        execution = mock(DelegateExecution.class);
        when(execution.getVariable("product")).thenReturn("APP");
        when(execution.getVariable("ffCode")).thenReturn("FF-A");
        when(execution.getVariable("docId")).thenReturn(15);
        when(execution.getVariable("process_id")).thenReturn(7);
    }

    @Test
    void recalculatesDependentsInPrerequisiteOrderAndStoresSuccessfulCodes() {
        when(ffManagerClient.getFitnessFunctions()).thenReturn(List.of(
                FitnessFunctionCatalogDTO.builder().code("FF-A").build(),
                FitnessFunctionCatalogDTO.builder().code("FF-C").applicability("FF-A, FF-B").build(),
                FitnessFunctionCatalogDTO.builder().code("FF-B").applicability("ff-a").build(),
                FitnessFunctionCatalogDTO.builder().code("FF-X").applicability("OTHER").build()));
        when(ffManagerClient.runFitnessFunction(eq("FF-B"), eq("APP"), eq(15), eq(7))).thenReturn(true);
        when(ffManagerClient.runFitnessFunction(eq("FF-C"), eq("APP"), eq(15), eq(7))).thenReturn(false);

        delegate.execute(execution);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(ffManagerClient);
        inOrder.verify(ffManagerClient).runFitnessFunction("FF-B", "APP", 15, 7);
        inOrder.verify(ffManagerClient).runFitnessFunction("FF-C", "APP", 15, 7);
        verify(ffManagerClient, never()).runFitnessFunction(eq("FF-A"), any(), any(), any());
        verify(ffManagerClient, never()).runFitnessFunction(eq("FF-X"), any(), any(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> codes = ArgumentCaptor.forClass(List.class);
        verify(execution).setVariable(eq(RecalculateDependentFfDelegate.RECALCULATED_FF_CODES), codes.capture());
        assertThat(codes.getValue()).containsExactly("FF-B");
    }

    @Test
    void storesEmptyListWhenDocumentIsMissing() {
        when(execution.getVariable("docId")).thenReturn(null);

        delegate.execute(execution);

        verify(ffManagerClient, never()).getFitnessFunctions();
        verify(execution).setVariable(RecalculateDependentFfDelegate.RECALCULATED_FF_CODES, List.of());
    }

    @Test
    void ordersDependentThatReferencesAnotherDependentAfterIt() {
        List<String> codes = RecalculateDependentFfDelegate.dependentCodes("FF-A", List.of(
                FitnessFunctionCatalogDTO.builder().code("FF-C").applicability("FF-A,FF-B").build(),
                FitnessFunctionCatalogDTO.builder().code("FF-B").applicability("FF-A").build()));

        assertThat(codes).containsExactly("FF-B", "FF-C");
    }
}
