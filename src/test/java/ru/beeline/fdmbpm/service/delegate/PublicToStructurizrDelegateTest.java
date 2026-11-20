package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import ru.beeline.fdmbpm.client.ArchClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.exception.ExternalServiceException;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicToStructurizrDelegateTest {

    private static final int PROCESS_ID = 42;
    private static final int DOC_ID = 30162;
    private static final int TYPE_ID = 1;
    private static final int STRCTERR_STATUS_ID = 101;

    private ArchClient archClient;
    private CamundaProcessStatusRepository camundaProcessStatusRepository;
    private PublicToStructurizrDelegate delegate;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        archClient = mock(ArchClient.class);
        CamundaProcessRepository camundaProcessRepository = mock(CamundaProcessRepository.class);
        TypeProcessRepository typeProcessRepository = mock(TypeProcessRepository.class);
        StatusProcessRepository statusProcessRepository = mock(StatusProcessRepository.class);
        camundaProcessStatusRepository = mock(CamundaProcessStatusRepository.class);

        when(camundaProcessRepository.findById(PROCESS_ID)).thenReturn(Optional.of(
                CamundaProcess.builder().id(PROCESS_ID).typeProcessId(TYPE_ID).build()));
        when(typeProcessRepository.findById(TYPE_ID)).thenReturn(Optional.of(
                new TypeProcess(TYPE_ID, "DataPipe", null, "Datapipe")));
        when(statusProcessRepository.findByAliasAndTypeProcessId(eq("strcterr"), eq(TYPE_ID)))
                .thenReturn(new StatusProcess(STRCTERR_STATUS_ID, TYPE_ID, "strcterr", "Ошибка Structurizr",
                        false, true, 5));
        when(statusProcessRepository.findByAliasAndTypeProcessId(eq("strct"), eq(TYPE_ID)))
                .thenReturn(new StatusProcess(100, TYPE_ID, "strct", "Structurizr", true, false, 4));

        delegate = new PublicToStructurizrDelegate();
        delegate.archClient = archClient;
        delegate.camundaProcessRepository = camundaProcessRepository;
        delegate.typeProcessRepository = typeProcessRepository;
        ReflectionTestUtils.setField(delegate, "transactionManager", mock(PlatformTransactionManager.class));
        delegate.statusProcessRepository = statusProcessRepository;
        delegate.camundaProcessStatusRepository = camundaProcessStatusRepository;

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("process_id")).thenReturn(PROCESS_ID);
        when(execution.getVariable("docId")).thenReturn(DOC_ID);
    }

    @Test
    @DisplayName("Успех — статус strct без message")
    void savesSuccessWithoutMessage() {
        delegate.execute(execution);

        ArgumentCaptor<CamundaProcessStatus> captor = ArgumentCaptor.forClass(CamundaProcessStatus.class);
        verify(camundaProcessStatusRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatusProcessId()).isEqualTo(100);
        assertThat(captor.getValue().getMessage()).isNull();
    }

    @Test
    @DisplayName("404 с details — message из details и cause в ProcessException")
    void savesDetailsAndPropagatesCause() {
        String details = "Для продукта mlops не настроен workspace Structurizr (structurizrApiUrl). "
                + "Обратитесь к владельцу продукта.";
        ExternalServiceException cause = new ExternalServiceException("HTTP 404", 404, details,
                new RuntimeException("upstream"));
        doThrow(cause).when(archClient).publicToStructurizr(DOC_ID);

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(ProcessException.class)
                .hasMessageContaining("Публикация в structurizr")
                .hasCause(cause);

        ArgumentCaptor<CamundaProcessStatus> captor = ArgumentCaptor.forClass(CamundaProcessStatus.class);
        verify(camundaProcessStatusRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatusProcessId()).isEqualTo(STRCTERR_STATUS_ID);
        assertThat(captor.getValue().getMessage()).isEqualTo(details);
    }

    @Test
    @DisplayName("Таймаут без details — общий текст в message")
    void savesGenericMessageOnTimeout() {
        ExternalServiceException cause = new ExternalServiceException("Read timed out", null, null,
                new RuntimeException("Read timed out"));
        doThrow(cause).when(archClient).publicToStructurizr(DOC_ID);

        assertThatThrownBy(() -> delegate.execute(execution))
                .isInstanceOf(ProcessException.class)
                .cause()
                .isSameAs(cause);

        ArgumentCaptor<CamundaProcessStatus> captor = ArgumentCaptor.forClass(CamundaProcessStatus.class);
        verify(camundaProcessStatusRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMessage())
                .isEqualTo("Внутренняя ошибка на шаге «Публикация в Structurizr». Обратитесь в поддержку");
    }
}
