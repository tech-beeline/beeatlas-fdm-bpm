package ru.beeline.fdmbpm.service.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.SmartKtsClient;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.ProductDTO;
import ru.beeline.fdmbpm.dto.smartkts.StructurizrObjectDTO;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GenerateKtsPassportDelegateTest {

    private static final int PROCESS_ID = 42;
    private static final String CMDB = "BC-012345";
    private static final String STRUCTURIZR_URL = "https://structurizr.vimpelcom.ru/share/98765";

    private GenerateKtsPassportDelegate delegate;
    private DelegateExecution execution;
    private ProductClient productClient;
    private SmartKtsClient smartKtsClient;
    private CamundaProcessStatusRepository camundaProcessStatusRepository;

    @BeforeEach
    void setUp() {
        productClient = mock(ProductClient.class);
        smartKtsClient = mock(SmartKtsClient.class);
        camundaProcessStatusRepository = mock(CamundaProcessStatusRepository.class);
        TypeProcessRepository typeProcessRepository = mock(TypeProcessRepository.class);
        StatusProcessRepository statusProcessRepository = mock(StatusProcessRepository.class);

        when(typeProcessRepository.findByAlias("Datapipe")).thenReturn(new TypeProcess(1, "Datapipe", null, "Datapipe"));
        when(statusProcessRepository.findByAliasAndTypeProcessId(GenerateKtsPassportDelegate.STATUS_DONE, 1))
                .thenReturn(new StatusProcess(101, 1, "done", GenerateKtsPassportDelegate.STATUS_DONE, false, false, 13));
        when(statusProcessRepository.findByAliasAndTypeProcessId(GenerateKtsPassportDelegate.STATUS_ERROR, 1))
                .thenReturn(new StatusProcess(102, 1, "error", GenerateKtsPassportDelegate.STATUS_ERROR, false, true, 13));

        delegate = new GenerateKtsPassportDelegate();
        delegate.smartKtsEnabled = true;
        delegate.productClient = productClient;
        delegate.smartKtsClient = smartKtsClient;
        delegate.typeProcessRepository = typeProcessRepository;
        delegate.statusProcessRepository = statusProcessRepository;
        delegate.camundaProcessStatusRepository = camundaProcessStatusRepository;

        execution = mock(DelegateExecution.class);
        when(execution.getVariable("process_id")).thenReturn(PROCESS_ID);
        when(execution.getVariable("cmdb")).thenReturn(CMDB);
        when(execution.getVariable("docId")).thenReturn(7);
    }

    @Test
    @DisplayName("Признак integration.smartkts.enabled выключен — шаг пропускается без вызовов и без error")
    void skipsWhenDisabled() {
        delegate.smartKtsEnabled = false;

        delegate.execute(execution);

        verifyNoInteractions(productClient, smartKtsClient, camundaProcessStatusRepository);
        verify(execution, never()).setVariable(anyString(), any());
    }

    @Test
    @DisplayName("Успешная генерация — structurizr_id из URL продукта, bwiki_kts_link из переменной, ktsGenerated=true, статус ktscrt")
    void generatesPassport() {
        when(execution.getVariable("bwiki_kts_link")).thenReturn("https://bwiki.vimpelcom.ru/pages/kts-1");
        givenProduct(STRUCTURIZR_URL);
        when(smartKtsClient.generateKtsPassport("98765", "https://bwiki.vimpelcom.ru/pages/kts-1"))
                .thenReturn(new StructurizrObjectDTO("98765", "https://bwiki.vimpelcom.ru/pages/kts-1"));

        delegate.execute(execution);

        verify(execution).setVariable(GenerateKtsPassportDelegate.KTS_GENERATED_VARIABLE, true);
        verify(execution, never()).setVariable(eq(GenerateKtsPassportDelegate.ERROR_VARIABLE), any());
        assertThat(savedStatusId()).isEqualTo(101);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("bwiki_kts_link не задан — в SmartKTS уходит null")
    void sendsNullLinkWhenVariableMissing(String link) {
        when(execution.getVariable("bwiki_kts_link")).thenReturn(link);
        givenProduct(STRUCTURIZR_URL);

        delegate.execute(execution);

        verify(smartKtsClient).generateKtsPassport(eq("98765"), isNull());
    }

    @Test
    @DisplayName("Ошибка SmartKTS — описание в error, статус ktserr, ktsGenerated не выставляется, исключение не пробрасывается")
    void setsErrorWhenSmartKtsFails() {
        givenProduct(STRUCTURIZR_URL);
        when(smartKtsClient.generateKtsPassport(anyString(), any()))
                .thenThrow(new IllegalStateException("SmartKTS вернул HTTP 422: {\"detail\":\"bad\"}"));

        delegate.execute(execution);

        ArgumentCaptor<Object> error = ArgumentCaptor.forClass(Object.class);
        verify(execution).setVariable(eq(GenerateKtsPassportDelegate.ERROR_VARIABLE), error.capture());
        assertThat((String) error.getValue()).contains("SmartKTS вернул HTTP 422");
        verify(execution, never()).setVariable(eq(GenerateKtsPassportDelegate.KTS_GENERATED_VARIABLE), any());
        assertThat(savedStatusId()).isEqualTo(102);
    }

    @Test
    @DisplayName("У продукта нет structurizr_api_url — error и статус ktserr без вызова SmartKTS")
    void setsErrorWhenProductHasNoStructurizrUrl() {
        givenProduct(null);

        delegate.execute(execution);

        verifyNoInteractions(smartKtsClient);
        verify(execution).setVariable(eq(GenerateKtsPassportDelegate.ERROR_VARIABLE), any());
        assertThat(savedStatusId()).isEqualTo(102);
    }

    @Test
    @DisplayName("Product Service не вернул продукт — error и статус ktserr")
    void setsErrorWhenProductNotFound() {
        when(productClient.getProductByCmdb(CMDB)).thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        delegate.execute(execution);

        verifyNoInteractions(smartKtsClient);
        ArgumentCaptor<Object> error = ArgumentCaptor.forClass(Object.class);
        verify(execution).setVariable(eq(GenerateKtsPassportDelegate.ERROR_VARIABLE), error.capture());
        assertThat((String) error.getValue()).contains(CMDB).contains("404");
    }

    @ParameterizedTest
    @CsvSource({
            "https://structurizr.vimpelcom.ru/share/98765, 98765",
            "https://structurizr.vimpelcom.ru/share/98765/, 98765",
            "https://structurizr.vimpelcom.ru/share/98765?version=2, 98765",
            "' https://structurizr.vimpelcom.ru/share/abc-1#diagrams ', abc-1",
            "https://structurizr.vimpelcom.ru/share/98765/diagrams, 98765",
            "98765, 98765"
    })
    @DisplayName("structurizr_id — последний сегмент пути structurizr_api_url")
    void extractsStructurizrId(String url, String expected) {
        assertThat(GenerateKtsPassportDelegate.extractStructurizrId(url)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "https://structurizr.vimpelcom.ru/share/", "https://structurizr.vimpelcom.ru",
            "https://structurizr.vimpelcom.ru/", "https://structurizr vimpelcom/share/1"})
    @DisplayName("Из пустого или некорректного structurizr_api_url structurizr_id не выделяется")
    void rejectsBadStructurizrUrl(String url) {
        assertThatThrownBy(() -> GenerateKtsPassportDelegate.extractStructurizrId(url))
                .isInstanceOf(IllegalStateException.class);
    }

    private void givenProduct(String structurizrApiUrl) {
        ProductDTO product = ProductDTO.builder().structurizrApiUrl(structurizrApiUrl).build();
        when(productClient.getProductByCmdb(CMDB)).thenReturn(ResponseEntity.ok(product));
    }

    private Integer savedStatusId() {
        ArgumentCaptor<CamundaProcessStatus> status = ArgumentCaptor.forClass(CamundaProcessStatus.class);
        verify(camundaProcessStatusRepository).saveAndFlush(status.capture());
        assertThat(status.getValue().getCamundaProcessId()).isEqualTo(PROCESS_ID);
        return status.getValue().getStatusProcessId();
    }
}
