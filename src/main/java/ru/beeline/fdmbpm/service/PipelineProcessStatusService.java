package ru.beeline.fdmbpm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.dto.pipeline.ProcessStatusItemDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.List;

@Service
public class PipelineProcessStatusService {

    private static final String TYPE_PROCESS_NOT_FOUND_MESSAGE = "Тип процесса с указанным идентификатором не найден.";

    @Autowired
    private TypeProcessRepository typeProcessRepository;

    @Autowired
    private StatusProcessRepository statusProcessRepository;

    public List<ProcessStatusItemDTO> getPipelineStatuses(Integer typeProcessId) {
        if (!typeProcessRepository.existsById(typeProcessId)) {
            throw new NotFoundException(TYPE_PROCESS_NOT_FOUND_MESSAGE);
        }

        return statusProcessRepository.findAllByTypeProcessIdOrderBySequenceAsc(typeProcessId)
                .stream()
                .map(status -> ProcessStatusItemDTO.builder()
                        .id(status.getId())
                        .typeProcessId(status.getTypeProcessId())
                        .name(status.getName())
                        .alias(status.getAlias())
                        .isDone(status.getIsDone())
                        .isError(status.getIsError())
                        .sequence(status.getSequence())
                        .build())
                .toList();
    }
}

