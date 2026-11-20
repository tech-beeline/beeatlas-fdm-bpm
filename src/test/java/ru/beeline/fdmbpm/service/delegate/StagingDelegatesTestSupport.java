package ru.beeline.fdmbpm.service.delegate;

import org.mockito.ArgumentCaptor;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Репозитории статусов процесса: id статуса равен 1000 + порядковый номер алиаса.
 */
class StagingDelegatesTestSupport {

    static final int PROCESS_ID = 42;
    static final int TYPE_ID = 2;
    static final List<String> ALIASES = List.of("crt", "vldterr", "cmdberr", "errcrt",
            "stgarchcrt", "stgarchdn", "stgarcherr", "stgseqcrt", "stgseqdn", "stgseqwop", "stgseqerr",
            "glbltskcrt", "errglblgrph");

    final CamundaProcessRepository camundaProcessRepository = mock(CamundaProcessRepository.class);
    final TypeProcessRepository typeProcessRepository = mock(TypeProcessRepository.class);
    final StatusProcessRepository statusProcessRepository = mock(StatusProcessRepository.class);
    final CamundaProcessStatusRepository camundaProcessStatusRepository = mock(CamundaProcessStatusRepository.class);

    StagingDelegatesTestSupport() {
        when(camundaProcessRepository.findById(PROCESS_ID)).thenReturn(Optional.of(
                CamundaProcess.builder().id(PROCESS_ID).typeProcessId(TYPE_ID).build()));
        when(typeProcessRepository.findById(TYPE_ID)).thenReturn(Optional.of(
                new TypeProcess(TYPE_ID, "DataPipe ветки архитектуры", null, "DatapipeBranch")));
        when(statusProcessRepository.findByAliasAndTypeProcessId(anyString(), any())).thenAnswer(invocation -> {
            String alias = invocation.getArgument(0);
            return new StatusProcess(statusId(alias), TYPE_ID, alias, alias, false, alias.endsWith("err"), 3);
        });
        when(camundaProcessStatusRepository.findByCamundaProcessIdAndStatusProcessId(anyInt(), anyInt()))
                .thenReturn(Optional.empty());
    }

    void givenStatusAlreadySaved(String alias) {
        when(camundaProcessStatusRepository.findByCamundaProcessIdAndStatusProcessId(PROCESS_ID, statusId(alias)))
                .thenReturn(Optional.of(CamundaProcessStatus.builder().statusProcessId(statusId(alias)).build()));
    }

    void wire(StatusLogic delegate) {
        delegate.statusProcessRepository = statusProcessRepository;
        delegate.camundaProcessStatusRepository = camundaProcessStatusRepository;
    }

    static int statusId(String alias) {
        return 1000 + ALIASES.indexOf(alias);
    }

    List<String> savedAliases() {
        boolean saved = mockingDetails(camundaProcessStatusRepository).getInvocations().stream()
                .anyMatch(invocation -> invocation.getMethod().getName().equals("saveAndFlush"));
        if (!saved) {
            return List.of();
        }
        ArgumentCaptor<CamundaProcessStatus> captor = ArgumentCaptor.forClass(CamundaProcessStatus.class);
        verify(camundaProcessStatusRepository, atLeast(1)).saveAndFlush(captor.capture());
        return captor.getAllValues().stream()
                .map(status -> ALIASES.get(status.getStatusProcessId() - 1000))
                .toList();
    }
}
