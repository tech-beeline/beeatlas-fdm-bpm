/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.camundaProcess.ProcessTypeDTO;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessTypeServiceTest {

    private TypeProcessRepository typeProcessRepository;
    private ProcessService processService;

    @BeforeEach
    void setUp() {
        typeProcessRepository = mock(TypeProcessRepository.class);
        processService = new ProcessService();
        ReflectionTestUtils.setField(processService, "typeProcessRepository", typeProcessRepository);
    }

    @Test
    void returnsEveryTypeInRepositoryOrder() {
        when(typeProcessRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                new TypeProcess(1, "Structurizr DataPipeLine", "Пайплайн публикации архитектуры в Structurizr", "Datapipe"),
                new TypeProcess(4, "Перерасчёт fitness function", "Расчёт одной fitness function по продуктам со structurizr", "RecalcFf")));

        List<ProcessTypeDTO> types = processService.getProcessTypes();

        assertThat(types).extracting(ProcessTypeDTO::getId).containsExactly(1, 4);
        assertThat(types.get(0).getName()).isEqualTo("Structurizr DataPipeLine");
        assertThat(types.get(0).getDescription()).isEqualTo("Пайплайн публикации архитектуры в Structurizr");
        assertThat(types.get(0).getAlias()).isEqualTo("Datapipe");
        assertThat(types.get(1).getAlias()).isEqualTo("RecalcFf");
        verify(typeProcessRepository).findAllByOrderByIdAsc();
    }

    @Test
    void emptyCatalogReturnsEmptyList() {
        when(typeProcessRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        assertThat(processService.getProcessTypes()).isEmpty();
    }
}
