package ru.beeline.fdmbpm.service.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import ru.beeline.fdmbpm.client.DocumentClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.beeline.fdmbpm.service.delegate.StagingDelegatesTestSupport.PROCESS_ID;

class RegisterProcessDelegateTest {

    private static final String CMDB = "BC-1";

    private StagingDelegatesTestSupport support;
    private ContextRepository contextRepository;
    private DocumentClient documentClient;
    private RegisterProcessDelegate delegate;
    private DelegateExecution execution;

    @BeforeEach
    void setUp() {
        support = new StagingDelegatesTestSupport();
        contextRepository = mock(ContextRepository.class);
        documentClient = mock(DocumentClient.class);
        ProductClient productClient = mock(ProductClient.class);
        when(productClient.getProductByCmdb(CMDB)).thenReturn(ResponseEntity.ok(ProductDTO.builder().build()));
        when(support.camundaProcessRepository.save(any()))
                .thenReturn(CamundaProcess.builder().id(PROCESS_ID).build());
        when(support.typeProcessRepository.findByAlias("Datapipe"))
                .thenReturn(new TypeProcess(1, "Structurizr DataPipeLine", null, "Datapipe"));
        when(support.typeProcessRepository.findByAlias("DatapipeBranch"))
                .thenReturn(new TypeProcess(2, "DataPipe ветки архитектуры", null, "DatapipeBranch"));

        delegate = new RegisterProcessDelegate();
        support.wire(delegate);
        delegate.contextRepository = contextRepository;
        delegate.typeProcessRepository = support.typeProcessRepository;
        delegate.camundaProcessRepository = support.camundaProcessRepository;
        delegate.documentClient = documentClient;
        delegate.productClient = productClient;
        delegate.objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(delegate, "transactionManager", mock(PlatformTransactionManager.class));

        execution = mock(DelegateExecution.class);
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getBusinessKey()).thenReturn("bk-1");
        when(execution.getVariable("cmdb")).thenReturn(CMDB);
        when(execution.getVariable("docId")).thenReturn(77);
        givenWorkspace("bc-1");
    }

    private void givenBranchProcess(String branch) {
        when(execution.getProcessDefinitionId()).thenReturn("Process_datapipeline_branch:3:8f1c");
        when(execution.getVariable("branch")).thenReturn(branch);
    }

    private void givenWorkspace(String workspaceCmdb) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("workspace.json").build());
        byte[] body = ("{\"model\":{\"properties\":{\"workspace_cmdb\":\"" + workspaceCmdb + "\"}}}")
                .getBytes(StandardCharsets.UTF_8);
        when(documentClient.getDocument(77)).thenReturn(ResponseEntity.ok().headers(headers).body(body));
    }

    @Test
    @DisplayName("Процесс ветки — тип DatapipeBranch, в context branch после trim и artifactUid обоих пайпов, статус crt")
    void registersBranchProcess() {
        givenBranchProcess("  Develop ");

        delegate.execute(execution);

        assertThat(savedContext()).extracting(Context::getName, Context::getValue).containsExactly(
                tuple("cmdb", CMDB),
                tuple("branch", "Develop"),
                tuple("artifactUid.structurizr-architecture", CMDB),
                tuple("artifactUid.structurizr-sequence", CMDB),
                tuple("docId", "77"));
        verify(support.statusProcessRepository).findByAliasAndTypeProcessId("crt", 2);
        assertThat(support.savedAliases()).containsExactly("crt");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Процесс ветки без ветки — errcrt и исключение, main не подставляется")
    void rejectsInvalidBranch(String branch) {
        givenBranchProcess(branch);

        assertThatThrownBy(() -> delegate.execute(execution)).isInstanceOf(ProcessException.class);

        assertThat(support.savedAliases()).containsExactly("errcrt");
    }

    @Test
    @DisplayName("Ветка длиннее 64 символов — errcrt и исключение")
    void rejectsTooLongBranch() {
        givenBranchProcess("b".repeat(65));

        assertThatThrownBy(() -> delegate.execute(execution)).isInstanceOf(ProcessException.class);

        assertThat(support.savedAliases()).containsExactly("errcrt");
    }

    @Test
    @DisplayName("Ветка ровно 64 символа допустима")
    void acceptsBranchOfMaxLength() {
        givenBranchProcess("b".repeat(64));

        delegate.execute(execution);

        assertThat(support.savedAliases()).containsExactly("crt");
    }

    @Test
    @DisplayName("Процесс ветки без docId — vldterr, branch всё равно в context")
    void requiresDocIdForBranchProcess() {
        givenBranchProcess("design");
        when(execution.getVariable("docId")).thenReturn(null);

        assertThatThrownBy(() -> delegate.execute(execution)).isInstanceOf(ProcessException.class);

        assertThat(support.savedAliases()).containsExactly("vldterr");
        assertThat(savedContext()).extracting(Context::getName)
                .contains("branch", "artifactUid.structurizr-architecture", "artifactUid.structurizr-sequence")
                .doesNotContain("docId");
    }

    @Test
    @DisplayName("Процесс ветки, workspace_cmdb другого продукта — cmdberr")
    void rejectsForeignWorkspace() {
        givenBranchProcess("main");
        givenWorkspace("BC-2");

        assertThatThrownBy(() -> delegate.execute(execution)).isInstanceOf(ProcessException.class);

        assertThat(support.savedAliases()).containsExactly("cmdberr");
    }

    @Test
    @DisplayName("Process_08wprkp — тип Datapipe, в context только cmdb и docId, переменная branch игнорируется")
    void keepsDatapipeBehaviour() {
        when(execution.getProcessDefinitionId()).thenReturn("Process_08wprkp:12:aa01");
        when(execution.getVariable("branch")).thenReturn("develop");

        delegate.execute(execution);

        assertThat(savedContext()).extracting(Context::getName).containsExactly("cmdb", "docId");
        verify(support.statusProcessRepository).findByAliasAndTypeProcessId("crt", 1);
    }

    @Test
    @DisplayName("Process_08wprkp без docId — по-прежнему crt")
    void datapipeWithoutDocId() {
        when(execution.getProcessDefinitionId()).thenReturn("Process_08wprkp:12:aa01");
        when(execution.getVariable("docId")).thenReturn(null);

        delegate.execute(execution);

        assertThat(support.savedAliases()).containsExactly("crt");
    }

    private List<Context> savedContext() {
        ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
        verify(contextRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }
}
