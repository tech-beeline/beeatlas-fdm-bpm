package ru.beeline.fdmbpm.service.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.beeline.fdmbpm.service.RabbitService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.beeline.fdmbpm.service.delegate.StagingDelegatesTestSupport.PROCESS_ID;

class GlobalGraphEnrichmentDelegateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StagingDelegatesTestSupport support;
    private RabbitService rabbitService;
    private GlobalGraphEnrichmentDelegate delegate;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        support = new StagingDelegatesTestSupport();
        rabbitService = mock(RabbitService.class);
        delegate = new GlobalGraphEnrichmentDelegate();
        support.wire(delegate);
        delegate.camundaProcessRepository = support.camundaProcessRepository;
        delegate.typeProcessRepository = support.typeProcessRepository;
        delegate.rabbitService = rabbitService;
        delegate.objectMapper = objectMapper;

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("process_id")).thenReturn(PROCESS_ID);
        when(execution.getVariable("docId")).thenReturn(1042);
    }

    private String sentMessage() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitService).sendMessage(eq("create_global_graph"), captor.capture());
        return (String) captor.getValue();
    }

    @Test
    @DisplayName("Process_datapipeline_branch — branch процесса после trim в create_global_graph")
    void sendsBranchOfBranchProcess() throws Exception {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:3:abc");
        when(execution.getVariable("branch")).thenReturn("  design ");

        delegate.execute(execution);

        assertThat(objectMapper.readTree(sentMessage()))
                .isEqualTo(objectMapper.readTree("{\"taskKey\":42,\"docId\":1042,\"branch\":\"design\"}"));
        assertThat(support.savedAliases()).containsExactly("glbltskcrt");
    }

    @Test
    @DisplayName("Process_datapipeline_branch — branch=main без смены регистра")
    void sendsMainBranchAsIs() throws Exception {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:3:abc");
        when(execution.getVariable("branch")).thenReturn("main");

        delegate.execute(execution);

        assertThat(objectMapper.readTree(sentMessage()))
                .isEqualTo(objectMapper.readTree("{\"taskKey\":42,\"docId\":1042,\"branch\":\"main\"}"));
        assertThat(support.savedAliases()).containsExactly("glbltskcrt");
    }

    @Test
    @DisplayName("Process_datapipeline_branch — branch=develop")
    void sendsDevelopBranch() throws Exception {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:3:abc");
        when(execution.getVariable("branch")).thenReturn("develop");

        delegate.execute(execution);

        assertThat(objectMapper.readTree(sentMessage()))
                .isEqualTo(objectMapper.readTree("{\"taskKey\":42,\"docId\":1042,\"branch\":\"develop\"}"));
        assertThat(support.savedAliases()).containsExactly("glbltskcrt");
    }

    @Test
    @DisplayName("Process_08wprkp — прежнее тело без поля branch")
    void keepsDatapipeMessage() throws Exception {
        when(execution.getProcessDefinitionId()).thenReturn("Process_08wprkp:12:def");
        when(execution.getVariable("branch")).thenReturn("design");

        delegate.execute(execution);

        assertThat(objectMapper.readTree(sentMessage()))
                .isEqualTo(objectMapper.readTree("{\"taskKey\":42,\"docId\":1042}"));
        assertThat(support.savedAliases()).containsExactly("glbltskcrt");
    }
}
