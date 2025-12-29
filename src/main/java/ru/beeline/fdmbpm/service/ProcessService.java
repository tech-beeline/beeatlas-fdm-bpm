package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.camundaProcess.GetContextDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.GetProcessByIdDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.GetProcessDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ProcessDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ShortContextDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.StatusDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.TypeDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.mapper.ContextDtoMapper;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProcessService {

    @Autowired
    ContextDtoMapper contextDtoMapper;

    @Autowired
    ContextRepository contextRepository;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    @Autowired
    StatusProcessRepository statusProcessRepository;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

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
                .isAsync(camundaProcess.getIsAsync())
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

    public List<GetContextDTO> getAllProcessByContext(String name, String value) {
        List<Context> contextList = contextRepository.findByNameAndValue(name, value);
        List<GetContextDTO> result = new ArrayList<>();
        if (!contextList.isEmpty()) {
            List<CamundaProcess> camundaProcessList = camundaProcessRepository
                    .findByIdIn(contextList.stream().map(Context::getCamundaProcessId).distinct().toList());
            Map<Integer, CamundaProcess> camundaProcessMap = camundaProcessList.stream().collect(Collectors.toMap(
                    CamundaProcess::getId, camundaProcess -> camundaProcess));
            Map<Integer, TypeProcess> typeProcessMap = typeProcessRepository.findAll().stream().collect(Collectors.toMap(
                    TypeProcess::getId, typeProcess -> typeProcess));
            Map<Integer, StatusProcess> statusProcessMap = statusProcessRepository.findAll().stream().collect(Collectors.toMap(
                    StatusProcess::getId, statusProcess -> statusProcess));
            for (Context context : contextList) {
                CamundaProcess camundaProcess = camundaProcessMap.get(context.getCamundaProcessId());
                if (camundaProcess != null) {
                    TypeProcess typeProcess = typeProcessMap.get(camundaProcess.getTypeProcessId());
                    if (typeProcess == null) {
                        throw new NotFoundException(String.format("Type process, для camunda process, с type process id: %s не найден",
                                camundaProcess.getTypeProcessId()));
                    }
                    CamundaProcessStatus camundaProcessStatus =
                            camundaProcessStatusRepository.findFirstByCamundaProcessIdOrderByCreatedDateDesc(camundaProcess.getId())
                                    .orElseThrow(() -> new NotFoundException(String.format("Camunda process status для camunda process с id: %s не найден",
                                            camundaProcess.getId())));
                    StatusProcess statusProcess = statusProcessMap.get(camundaProcessStatus.getStatusProcessId());
                    if (statusProcess == null) {
                        throw new NotFoundException("Status Process не найден");
                    }
                    result.add(contextDtoMapper.convert(camundaProcess, typeProcess, statusProcess, camundaProcessStatus));
                }
            }
        }
        return result;
    }

    public GetProcessByIdDTO getProcessById(Integer id) {
        CamundaProcess camundaProcess = camundaProcessRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Запись camunda process, с данным id не найдена"));
        TypeProcess typeProcess = findTypeProcessById(camundaProcess.getTypeProcessId());
        List<CamundaProcessStatus> camundaProcessStatusList = findAllCamundaProcessStatus(camundaProcess.getId());
        List<StatusDTO> statusDTOList = camundaProcessStatusList.stream()
                .map(camundaProcessStatus -> {
                    StatusProcess statusProcess = findStatusProcess(camundaProcessStatus.getStatusProcessId());
                    return StatusDTO.builder()
                            .id(camundaProcessStatus.getId())
                            .name(statusProcess.getName())
                            .alias(statusProcess.getAlias())
                            .createdDate(camundaProcessStatus.getCreatedDate())
                            .build();
                }).toList();
        List<ShortContextDTO> shortContextDTOList = contextRepository.findByCamundaProcessId(camundaProcess.getId())
                .stream()
                .map(context -> contextDtoMapper.shortContextConvert(context))
                .toList();
        return processByIdConvert(camundaProcess, typeProcess, statusDTOList, shortContextDTOList);
    }

    private TypeProcess findTypeProcessById(Integer id) {
        return typeProcessRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Type process с id: %s не найден", id)));
    }

    private GetProcessByIdDTO processByIdConvert(CamundaProcess camundaProcess, TypeProcess typeProcess,
                                                 List<StatusDTO> statusDTOList, List<ShortContextDTO> shortContextDTOList) {
        return GetProcessByIdDTO.builder()
                .id(camundaProcess.getId())
                .procId(camundaProcess.getProcId())
                .businessKey(camundaProcess.getBusinessKey())
                .type(TypeDTO.builder()
                        .id(typeProcess.getId())
                        .name(typeProcess.getName())
                        .description(typeProcess.getDescription())
                        .build())
                .statuses(statusDTOList)
                .context(shortContextDTOList)
                .build();
    }
}
