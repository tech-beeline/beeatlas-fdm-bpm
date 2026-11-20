/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service;

import jakarta.persistence.Column;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.camundaProcess.ProcessHistoryItemDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ProcessHistoryResponseDTO;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.mapper.ContextDtoMapper;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.ProcessHistoryFilter;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessServiceHistoryTest {

    private static final LocalDateTime STARTED = LocalDateTime.of(2026, 11, 18, 16, 40, 1, 120_000_000);
    private static final LocalDateTime SAME_MOMENT = LocalDateTime.of(2026, 11, 18, 16, 40, 1, 200_000_000);
    private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 11, 18, 10, 0, 0);

    private CamundaProcessRepository camundaProcessRepository;
    private CamundaProcessStatusRepository camundaProcessStatusRepository;
    private StatusProcessRepository statusProcessRepository;
    private ContextRepository contextRepository;
    private TypeProcessRepository typeProcessRepository;
    private ProcessService processService;

    @BeforeEach
    void setUp() {
        camundaProcessRepository = mock(CamundaProcessRepository.class);
        camundaProcessStatusRepository = mock(CamundaProcessStatusRepository.class);
        statusProcessRepository = mock(StatusProcessRepository.class);
        contextRepository = mock(ContextRepository.class);
        typeProcessRepository = mock(TypeProcessRepository.class);
        processService = new ProcessService();
        ReflectionTestUtils.setField(processService, "camundaProcessRepository", camundaProcessRepository);
        ReflectionTestUtils.setField(processService, "camundaProcessStatusRepository", camundaProcessStatusRepository);
        ReflectionTestUtils.setField(processService, "statusProcessRepository", statusProcessRepository);
        ReflectionTestUtils.setField(processService, "contextRepository", contextRepository);
        ReflectionTestUtils.setField(processService, "typeProcessRepository", typeProcessRepository);
        ReflectionTestUtils.setField(processService, "contextDtoMapper", new ContextDtoMapper());
    }

    @Test
    void createDateIsFilledByTheDatabase() throws NoSuchFieldException {
        Column column = CamundaProcess.class.getDeclaredField("createDate").getAnnotation(Column.class);

        assertThat(column.name()).isEqualTo("create_date");
        assertThat(column.insertable()).isFalse();
        assertThat(column.updatable()).isFalse();
    }

    @Test
    void returnsPageNewestFirstWithContextAndLatestStatus() {
        CamundaProcess first = process(501, 4, "8f0c2a", STARTED);
        CamundaProcess second = process(400, 1, "older", EARLIER);
        when(camundaProcessRepository.countHistory(any())).thenReturn(137L);
        when(camundaProcessRepository.findHistory(any(), eq(10), eq(0))).thenReturn(List.of(first, second));
        when(typeProcessRepository.findAllById(List.of(4, 1))).thenReturn(List.of(
                new TypeProcess(4, "Перерасчёт fitness function", "Расчёт одной fitness function по продуктам со structurizr", "RecalcFf"),
                new TypeProcess(1, "Structurizr DataPipeLine", "Пайплайн", "Datapipe")));
        when(contextRepository.findByCamundaProcessIdIn(List.of(501, 400))).thenReturn(List.of(
                Context.builder().id(30).camundaProcessId(501).name("later").value("2").build(),
                Context.builder().id(10).camundaProcessId(501).name("ffCode").value("FF-1").build()));
        when(camundaProcessStatusRepository.findByCamundaProcessIdIn(List.of(501, 400))).thenReturn(List.of(
                status(1, 501, 10, EARLIER, null),
                status(9001, 501, 11, SAME_MOMENT, null),
                status(9000, 501, 11, SAME_MOMENT, "раньше"),
                status(9002, 501, 11, null, "пустая дата")));
        when(statusProcessRepository.findAllById(List.of(11))).thenReturn(List.of(
                new StatusProcess(11, 4, "Процесс перерасчёта fitness function зарегистрирован", "crt", false, false, 1)));

        ProcessHistoryResponseDTO response = history("10", "0", null);

        assertThat(response.getTotal()).isEqualTo(137);
        assertThat(response.getProcesses()).extracting(ProcessHistoryItemDTO::getId).containsExactly(501, 400);
        ProcessHistoryItemDTO item = response.getProcesses().get(0);
        assertThat(item.getProcId()).isEqualTo("8f0c2a");
        assertThat(item.getCreateDate()).isEqualTo(STARTED);
        assertThat(item.getType().getId()).isEqualTo(4);
        assertThat(item.getType().getName()).isEqualTo("Перерасчёт fitness function");
        assertThat(item.getType().getDescription()).isEqualTo("Расчёт одной fitness function по продуктам со structurizr");
        assertThat(item.getContext()).extracting(context -> context.getName()).containsExactly("ffCode", "later");
        assertThat(item.getStatus().getId()).isEqualTo(9001);
        assertThat(item.getStatus().getAlias()).isEqualTo("crt");
        assertThat(item.getStatus().getCreatedDate()).isEqualTo(SAME_MOMENT);
        assertThat(item.getStatus().getIsDone()).isFalse();
        assertThat(item.getStatus().getIsError()).isFalse();
        assertThat(item.getStatus().getMessage()).isNull();
        assertThat(response.getProcesses().get(1).getContext()).isEmpty();
        assertThat(response.getProcesses().get(1).getStatus()).isNull();

        ArgumentCaptor<ProcessHistoryFilter> filter = ArgumentCaptor.forClass(ProcessHistoryFilter.class);
        verify(camundaProcessRepository).findHistory(filter.capture(), eq(10), eq(0));
        assertThat(filter.getValue().typeIds()).isEmpty();
        assertThat(filter.getValue().createDateFrom()).isNull();
        assertThat(filter.getValue().createDateTo()).isNull();
        assertThat(filter.getValue().contextName()).isNull();
        assertThat(filter.getValue().contextValue()).isNull();
    }

    @Test
    void nextPageUsesOffsetAndDoesNotRepeatTheFirstQuery() {
        when(camundaProcessRepository.countHistory(any())).thenReturn(20L);
        when(camundaProcessRepository.findHistory(any(), eq(10), eq(10))).thenReturn(List.of(process(300, 1, "p", STARTED)));
        when(typeProcessRepository.findAllById(List.of(1))).thenReturn(List.of(
                new TypeProcess(1, "Structurizr DataPipeLine", "Пайплайн", "Datapipe")));
        when(contextRepository.findByCamundaProcessIdIn(List.of(300))).thenReturn(List.of());
        when(camundaProcessStatusRepository.findByCamundaProcessIdIn(List.of(300))).thenReturn(List.of());

        history("10", "10", List.of());

        verify(camundaProcessRepository).findHistory(any(), eq(10), eq(10));
    }

    @Test
    void typeFilterLimitsTotalAndPage() {
        when(typeProcessRepository.findAllById(List.of(1, 4))).thenReturn(List.of(
                new TypeProcess(1, "Structurizr DataPipeLine", "Пайплайн", "Datapipe"),
                new TypeProcess(4, "Перерасчёт fitness function", "Расчёт", "RecalcFf")));
        when(camundaProcessRepository.countHistory(any())).thenReturn(2L);
        when(camundaProcessRepository.findHistory(any(), eq(5), eq(0)))
                .thenReturn(List.of(process(8, 4, "ff", STARTED)));
        when(contextRepository.findByCamundaProcessIdIn(List.of(8))).thenReturn(List.of());
        when(camundaProcessStatusRepository.findByCamundaProcessIdIn(List.of(8))).thenReturn(List.of());

        ProcessHistoryResponseDTO response = history(" 5 ", "0", List.of("1", "4", "1", " 4 "));

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getProcesses()).extracting(ProcessHistoryItemDTO::getId).containsExactly(8);
        ArgumentCaptor<ProcessHistoryFilter> filter = ArgumentCaptor.forClass(ProcessHistoryFilter.class);
        verify(camundaProcessRepository).countHistory(filter.capture());
        assertThat(filter.getValue().typeIds()).containsExactly(1, 4);
        assertThat(filter.getValue().contextName()).isNull();
    }

    @Test
    void offsetPastTheEndReturnsEmptyPageAndTotal() {
        when(camundaProcessRepository.countHistory(any())).thenReturn(3L);

        ProcessHistoryResponseDTO response = history("10", "3", null);

        assertThat(response.getTotal()).isEqualTo(3);
        assertThat(response.getProcesses()).isEmpty();
        verify(camundaProcessRepository, never()).findHistory(any(), eq(10), eq(3));
    }

    @Test
    void rejectsBrokenPageParamsAndUnknownType() {
        assertThatThrownBy(() -> history(null, "0", null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("limit должен быть целым числом от 1");
        assertThatThrownBy(() -> history("0", "0", null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("limit должен быть целым числом от 1");
        assertThatThrownBy(() -> history("10", "-1", null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("offset должен быть целым числом от 0");
        assertThatThrownBy(() -> history("10", "0", List.of("x")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("typeProcessId должен быть целым числом");
        when(typeProcessRepository.findAllById(List.of(99))).thenReturn(List.of());
        assertThatThrownBy(() -> history("10", "0", List.of("99")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Тип процесса с id 99 не найден");
        verify(camundaProcessRepository, never()).countHistory(any());
    }

    @Test
    void dateAndContextFiltersArePassedToTheQuery() {
        when(typeProcessRepository.findAllById(List.of(4))).thenReturn(List.of(
                new TypeProcess(4, "Перерасчёт fitness function", "Расчёт", "RecalcFf")));
        when(camundaProcessRepository.countHistory(any())).thenReturn(1L);
        when(camundaProcessRepository.findHistory(any(), eq(25), eq(0)))
                .thenReturn(List.of(process(501, 4, "8f0c2a", STARTED)));
        when(contextRepository.findByCamundaProcessIdIn(List.of(501))).thenReturn(List.of(
                Context.builder().id(1).camundaProcessId(501).name("ffCode").value("FF-1").build(),
                Context.builder().id(2).camundaProcessId(501).name("product").value("fdmshowcaseapp").build()));
        when(camundaProcessStatusRepository.findByCamundaProcessIdIn(List.of(501))).thenReturn(List.of());

        ProcessHistoryResponseDTO response = processService.getProcessHistory(
                "25", "0", List.of("4"),
                "2026-11-18 00:00:00.000", "2026-11-18 23:59:59.999",
                List.of("ffCode"), List.of("FF-1"));

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getProcesses().get(0).getContext()).extracting(context -> context.getName())
                .containsExactly("ffCode", "product");
        ArgumentCaptor<ProcessHistoryFilter> filter = ArgumentCaptor.forClass(ProcessHistoryFilter.class);
        verify(camundaProcessRepository).findHistory(filter.capture(), eq(25), eq(0));
        assertThat(filter.getValue().typeIds()).containsExactly(4);
        assertThat(filter.getValue().createDateFrom()).isEqualTo(LocalDateTime.of(2026, 11, 18, 0, 0, 0));
        assertThat(filter.getValue().createDateTo()).isEqualTo(LocalDateTime.of(2026, 11, 18, 23, 59, 59, 999_000_000));
        assertThat(filter.getValue().contextName()).isEqualTo("ffCode");
        assertThat(filter.getValue().contextValue()).isEqualTo("FF-1");
    }

    @Test
    void contextNameWithoutValueDoesNotSetValue() {
        when(camundaProcessRepository.countHistory(any())).thenReturn(0L);

        ProcessHistoryResponseDTO response = processService.getProcessHistory(
                "10", "0", null, null, null, List.of("ffCode"), List.of("   "));

        assertThat(response.getTotal()).isZero();
        assertThat(response.getProcesses()).isEmpty();
        ArgumentCaptor<ProcessHistoryFilter> filter = ArgumentCaptor.forClass(ProcessHistoryFilter.class);
        verify(camundaProcessRepository).countHistory(filter.capture());
        assertThat(filter.getValue().contextName()).isEqualTo("ffCode");
        assertThat(filter.getValue().contextValue()).isNull();
        verify(camundaProcessRepository, never()).findHistory(any(), eq(10), eq(0));
    }

    @Test
    void rejectsInvalidDateAndContextValueWithoutName() {
        assertThatThrownBy(() -> processService.getProcessHistory(
                "10", "0", null, "2026-11-19 00:00:00.000", "2026-11-18 00:00:00.000", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("createDateFrom не может быть позже createDateTo");
        assertThatThrownBy(() -> processService.getProcessHistory(
                "10", "0", null, "18.11.2026", null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("createDateFrom должен быть датой yyyy-MM-dd HH:mm:ss.SSS");
        assertThatThrownBy(() -> processService.getProcessHistory(
                "10", "0", null, null, "   ", null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("createDateTo должен быть датой yyyy-MM-dd HH:mm:ss.SSS");
        assertThatThrownBy(() -> processService.getProcessHistory(
                "10", "0", null, null, null, null, List.of("FF-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("contextValue нельзя передать без contextName");
        assertThatThrownBy(() -> processService.getProcessHistory(
                "10", "0", null, null, null, List.of("   "), List.of("FF-1")))
                .isInstanceOf(ValidationException.class)
                .hasMessage("contextValue нельзя передать без contextName");
        assertThatThrownBy(() -> processService.getProcessHistory(
                "10", "0", null, null, null, List.of("ffCode", "product"), null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("contextName передаётся один раз");
        verify(camundaProcessRepository, never()).countHistory(any());
    }

    private ProcessHistoryResponseDTO history(String limit, String offset, List<String> typeIds) {
        return processService.getProcessHistory(limit, offset, typeIds, null, null, null, null);
    }

    private static CamundaProcess process(int id, int typeId, String procId, LocalDateTime createDate) {
        return CamundaProcess.builder()
                .id(id)
                .typeProcessId(typeId)
                .procId(procId)
                .businessKey("bk-" + id)
                .isAsync(false)
                .createDate(createDate)
                .build();
    }

    private static CamundaProcessStatus status(int id, int processId, int statusProcessId,
                                                LocalDateTime createdDate, String message) {
        return CamundaProcessStatus.builder()
                .id(id)
                .camundaProcessId(processId)
                .statusProcessId(statusProcessId)
                .createdDate(createdDate)
                .message(message)
                .build();
    }
}
