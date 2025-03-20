package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.dto.camundaProcess.GetProcessDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ProcessDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.StatusProcessRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ProcessService {

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    StatusProcessRepository statusProcessRepository;

    @Autowired
    CamundaProcessStatusRepository camundaProcessStatusRepository;

    public ProcessDTO getProcess(String idEnum, String id) {
        CamundaProcess camundaProcess = findCamundaProcess(idEnum, id);
        CamundaProcessStatus camundaProcessStatus = findLatestCamundaProcessStatus(camundaProcess.getId());
        StatusProcess statusProcess = findStatusProcess(camundaProcessStatus.getStatusProcessId());

        return ProcessDTO.builder()
                .id(camundaProcessStatus.getStatusProcessId())
                .name(statusProcess.getName())
                .processId(camundaProcess.getId())
                .isDone(statusProcess.getIsDone())
                .isError(statusProcess.getIsError())
                .createdDate(camundaProcessStatus.getCreatedDate())
                .build();
    }

    private CamundaProcess findCamundaProcess(String idEnum, String id) {
        return switch (idEnum) {
            case "process-id" -> camundaProcessRepository.findByProcId(id)
                    .orElseThrow(() -> new NotFoundException("Запись с таким process-id не найдена"));
            case "business-key" -> camundaProcessRepository.findByBusinessKey(id)
                    .orElseThrow(() -> new NotFoundException("Запись с таким business-key не найдена"));
            default -> throw new NotFoundException("Запись не найдена");
        };
    }

    private CamundaProcessStatus findLatestCamundaProcessStatus(Integer camundaProcessId) {
        return camundaProcessStatusRepository.findFirstByCamundaProcessIdOrderByCreatedDateDesc(camundaProcessId)
                .orElseThrow(() -> new NotFoundException("Статус процесса не найден"));
    }

    private StatusProcess findStatusProcess(Integer statusProcessId) {
        return statusProcessRepository.findById(statusProcessId)
                .orElseThrow(() -> new NotFoundException("Информация о статусе процесса не найдена"));
    }

    private List<CamundaProcessStatus> findAllCamundaProcessStatus(Integer camundaProcessId) {
        return camundaProcessStatusRepository.findByCamundaProcessId(camundaProcessId);
    }

    public List<GetProcessDTO> getAllStatusProcess(String idEnum, String id) {
        CamundaProcess camundaProcess = findCamundaProcess(idEnum, id);
        List<CamundaProcessStatus> camundaProcessStatusList = findAllCamundaProcessStatus(camundaProcess.getId());
        List<GetProcessDTO> result = new ArrayList<>();
        if (!camundaProcessStatusList.isEmpty()) {
            result.addAll(camundaProcessStatusList.stream()
                    .map(camundaProcessStatus -> {
                        StatusProcess statusProcess = findStatusProcess(camundaProcessStatus.getStatusProcessId());
                        return GetProcessDTO.builder()
                                .id(camundaProcessStatus.getStatusProcessId())
                                .name(statusProcess.getName())
                                .isDone(statusProcess.getIsDone())
                                .isError(statusProcess.getIsError())
                                .createdDate(camundaProcessStatus.getCreatedDate())
                                .build();
                    })
                    .sorted(Comparator.comparing(GetProcessDTO::getCreatedDate))
                    .toList()
            );
        }
        return result;
    }
}
