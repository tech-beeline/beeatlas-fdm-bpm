package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import ru.beeline.fdmbpm.client.FfManagerClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;
import ru.beeline.fdmbpm.dto.product.NfrCatalogItemDTO;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssignProductNfrDelegateTest {

    private static final int PROCESS_ID = 42;

    private AssignProductNfrDelegate delegate;
    private ProductClient productClient;
    private FfManagerClient ffManagerClient;
    private CamundaProcessStatusRepository camundaProcessStatusRepository;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        delegate = new AssignProductNfrDelegate();
        productClient = mock(ProductClient.class);
        ffManagerClient = mock(FfManagerClient.class);
        camundaProcessStatusRepository = mock(CamundaProcessStatusRepository.class);
        CamundaProcessRepository camundaProcessRepository = mock(CamundaProcessRepository.class);
        TypeProcessRepository typeProcessRepository = mock(TypeProcessRepository.class);
        StatusProcessRepository statusProcessRepository = mock(StatusProcessRepository.class);
        ReflectionTestUtils.setField(delegate, "productClient", productClient);
        ReflectionTestUtils.setField(delegate, "ffManagerClient", ffManagerClient);
        delegate.camundaProcessRepository = camundaProcessRepository;
        delegate.typeProcessRepository = typeProcessRepository;
        delegate.transactionManager = mock(PlatformTransactionManager.class);
        delegate.statusProcessRepository = statusProcessRepository;
        delegate.camundaProcessStatusRepository = camundaProcessStatusRepository;

        when(camundaProcessRepository.findById(PROCESS_ID)).thenReturn(Optional.of(
                CamundaProcess.builder().id(PROCESS_ID).typeProcessId(1).build()));
        when(typeProcessRepository.findById(1)).thenReturn(Optional.of(new TypeProcess(1, "Datapipe", null, "Datapipe")));
        when(statusProcessRepository.findByAliasAndTypeProcessId(AssignProductNfrDelegate.STATUS_DONE, 1))
                .thenReturn(new StatusProcess(301, 1, "done", AssignProductNfrDelegate.STATUS_DONE, false, false, 13));
        when(statusProcessRepository.findByAliasAndTypeProcessId(AssignProductNfrDelegate.STATUS_ERROR, 1))
                .thenReturn(new StatusProcess(302, 1, "error", AssignProductNfrDelegate.STATUS_ERROR, false, true, 13));

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("cmdb")).thenReturn("BC-1");
        when(execution.getVariable("docId")).thenReturn(77);
        when(execution.getVariable("process_id")).thenReturn(PROCESS_ID);
    }

    @Test
    void blankCmdbWritesNfrcrtWithoutCalls() {
        when(execution.getVariable("cmdb")).thenReturn("  ");

        delegate.execute(execution);

        verifyNoInteractions(productClient, ffManagerClient);
        assertThat(savedStatusId()).isEqualTo(301);
    }

    @Test
    void emptyCatalogWritesNfrcrt() {
        when(productClient.getAllNfr()).thenReturn(List.of());

        delegate.execute(execution);

        verify(productClient).getAllNfr();
        verify(productClient, never()).postProductNfr(anyString(), anyList(), any());
        verifyNoInteractions(ffManagerClient);
        assertThat(savedStatusId()).isEqualTo(301);
    }

    @Test
    void assignsMatchingNfrAndWritesNfrcrt() {
        when(productClient.getAllNfr()).thenReturn(List.of(
                NfrCatalogItemDTO.builder().id(5).code("NFR-1").rule("FF-1").build()));
        when(ffManagerClient.getMergedActualResultsForNfr("BC-1")).thenReturn(List.of(
                AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO.builder().code("FF-1").isCheck(true).build()));

        delegate.execute(execution);

        verify(productClient).postProductNfr(eq("BC-1"), eq(List.of(5)), isNull());
        assertThat(savedStatusId()).isEqualTo(301);
    }

    @Test
    void branchProcessPostsNfrToProcessBranch() {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:1:c0ffee");
        when(execution.getVariable("branch")).thenReturn("develop");
        when(productClient.getAllNfr()).thenReturn(List.of(
                NfrCatalogItemDTO.builder().id(5).code("NFR-1").rule("FF-1").build()));
        when(ffManagerClient.getMergedActualResultsForNfr("BC-1")).thenReturn(List.of(
                AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO.builder().code("FF-1").isCheck(true).build()));

        delegate.execute(execution);

        verify(productClient).postProductNfr(eq("BC-1"), eq(List.of(5)), eq("develop"));
        verifyNoInteractions(camundaProcessStatusRepository);
    }

    @Test
    void postFailureWritesNfrerrAndRethrows() {
        when(productClient.getAllNfr()).thenReturn(List.of(
                NfrCatalogItemDTO.builder().id(5).code("NFR-1").rule("FF-1").build()));
        when(ffManagerClient.getMergedActualResultsForNfr("BC-1")).thenReturn(List.of(
                AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO.builder().code("FF-1").isCheck(true).build()));
        doThrow(new RuntimeException("Failed to assign NFR to product"))
                .when(productClient).postProductNfr(eq("BC-1"), eq(List.of(5)), isNull());

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to assign NFR to product");
        assertThat(savedStatusId()).isEqualTo(302);
    }

    @Test
    void branchProcessDoesNotWriteNfrStatus() {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:1:c0ffee");
        when(execution.getVariable("cmdb")).thenReturn("  ");

        delegate.execute(execution);

        verifyNoInteractions(productClient, ffManagerClient, camundaProcessStatusRepository);
    }

    private Integer savedStatusId() {
        ArgumentCaptor<CamundaProcessStatus> status = ArgumentCaptor.forClass(CamundaProcessStatus.class);
        verify(camundaProcessStatusRepository).saveAndFlush(status.capture());
        assertThat(status.getValue().getCamundaProcessId()).isEqualTo(PROCESS_ID);
        return status.getValue().getStatusProcessId();
    }
}
