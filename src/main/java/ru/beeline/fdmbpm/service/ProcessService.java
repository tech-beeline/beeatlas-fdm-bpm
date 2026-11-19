/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.domain.*;
import ru.beeline.fdmbpm.dto.camundaProcess.*;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.mapper.ContextDtoMapper;
import ru.beeline.fdmbpm.repository.camunda.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProcessService {

    private static final DateTimeFormatter CREATE_DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss.SSS")
            .withResolverStyle(ResolverStyle.STRICT);

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
        List<Context> contextList = contextRepository.findByNameAndValueIgnoreCase(name, value);
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
                    log.info("StatusProcess: " + statusProcess.toString());
                    result.add(contextDtoMapper.convert(camundaProcess, typeProcess, statusProcess, camundaProcessStatus));
                }
            }
        }
        return result;
    }

    public List<ProcessTypeDTO> getProcessTypes() {
        return typeProcessRepository.findAllByOrderByIdAsc().stream()
                .map(typeProcess -> ProcessTypeDTO.builder()
                        .id(typeProcess.getId())
                        .name(typeProcess.getName())
                        .description(typeProcess.getDescription())
                        .alias(typeProcess.getAlias())
                        .build())
                .toList();
    }

    public ProcessHistoryResponseDTO getProcessHistory(String limitRaw, String offsetRaw, List<String> typeProcessIds,
                                                       String createDateFromRaw, String createDateToRaw,
                                                       List<String> contextNames, List<String> contextValues) {
        int limit = parsePageParam(limitRaw, 1, "limit должен быть целым числом от 1");
        int offset = parsePageParam(offsetRaw, 0, "offset должен быть целым числом от 0");
        List<Integer> typeIds = parseTypeIds(typeProcessIds);
        Map<Integer, TypeProcess> knownTypes = Map.of();
        if (!typeIds.isEmpty()) {
            knownTypes = typeProcessRepository.findAllById(typeIds).stream()
                    .collect(Collectors.toMap(TypeProcess::getId, typeProcess -> typeProcess));
            for (Integer typeId : typeIds) {
                if (!knownTypes.containsKey(typeId)) {
                    throw new ValidationException("Тип процесса с id " + typeId + " не найден");
                }
            }
        }
        LocalDateTime createDateFrom = parseCreateDate(createDateFromRaw,
                "createDateFrom должен быть датой yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime createDateTo = parseCreateDate(createDateToRaw,
                "createDateTo должен быть датой yyyy-MM-dd HH:mm:ss.SSS");
        if (createDateFrom != null && createDateTo != null && createDateFrom.isAfter(createDateTo)) {
            throw new ValidationException("createDateFrom не может быть позже createDateTo");
        }
        String contextName = singleOptional(contextNames, "contextName передаётся один раз");
        String contextValue = singleOptional(contextValues, "contextValue передаётся один раз");
        if (contextName == null && contextValue != null) {
            throw new ValidationException("contextValue нельзя передать без contextName");
        }

        ProcessHistoryFilter filter = new ProcessHistoryFilter(
                typeIds, createDateFrom, createDateTo, contextName, contextValue);
        long total = camundaProcessRepository.countHistory(filter);
        if (offset >= total) {
            return ProcessHistoryResponseDTO.builder().total(total).processes(List.of()).build();
        }
        List<CamundaProcess> page = camundaProcessRepository.findHistory(filter, limit, offset);
        return ProcessHistoryResponseDTO.builder()
                .total(total)
                .processes(toHistoryItems(page, knownTypes))
                .build();
    }

    private List<ProcessHistoryItemDTO> toHistoryItems(List<CamundaProcess> page, Map<Integer, TypeProcess> knownTypes) {
        if (page.isEmpty()) {
            return List.of();
        }
        List<Integer> processIds = page.stream().map(CamundaProcess::getId).toList();
        Map<Integer, TypeProcess> types = knownTypes.isEmpty()
                ? typeProcessRepository.findAllById(page.stream().map(CamundaProcess::getTypeProcessId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(TypeProcess::getId, typeProcess -> typeProcess))
                : knownTypes;
        Map<Integer, List<ShortContextDTO>> contextByProcess = contextRepository.findByCamundaProcessIdIn(processIds)
                .stream()
                .sorted(Comparator.comparing(Context::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(
                        Context::getCamundaProcessId,
                        LinkedHashMap::new,
                        Collectors.mapping(contextDtoMapper::shortContextConvert, Collectors.toList())));
        Map<Integer, CamundaProcessStatus> latestStatusByProcess = latestStatusByProcess(processIds);
        Map<Integer, StatusProcess> statusDefinitions = statusDefinitions(latestStatusByProcess.values());

        List<ProcessHistoryItemDTO> items = new ArrayList<>();
        for (CamundaProcess process : page) {
            TypeProcess typeProcess = types.get(process.getTypeProcessId());
            if (typeProcess == null) {
                throw new NotFoundException(String.format("Type process с id: %s не найден", process.getTypeProcessId()));
            }
            CamundaProcessStatus latestStatus = latestStatusByProcess.get(process.getId());
            items.add(ProcessHistoryItemDTO.builder()
                    .id(process.getId())
                    .procId(process.getProcId())
                    .createDate(process.getCreateDate())
                    .type(TypeDTO.builder()
                            .id(typeProcess.getId())
                            .name(typeProcess.getName())
                            .description(typeProcess.getDescription())
                            .build())
                    .context(contextByProcess.getOrDefault(process.getId(), List.of()))
                    .status(toStatusDto(latestStatus, statusDefinitions))
                    .build());
        }
        return items;
    }

    private Map<Integer, CamundaProcessStatus> latestStatusByProcess(Collection<Integer> processIds) {
        Map<Integer, CamundaProcessStatus> latest = new LinkedHashMap<>();
        for (CamundaProcessStatus status : camundaProcessStatusRepository.findByCamundaProcessIdIn(processIds)) {
            CamundaProcessStatus current = latest.get(status.getCamundaProcessId());
            if (current == null || statusComesAfter(status, current)) {
                latest.put(status.getCamundaProcessId(), status);
            }
        }
        return latest;
    }

    private boolean statusComesAfter(CamundaProcessStatus candidate, CamundaProcessStatus current) {
        Comparator<CamundaProcessStatus> byDateThenId = Comparator
                .comparing(CamundaProcessStatus::getCreatedDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(CamundaProcessStatus::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
        return byDateThenId.compare(candidate, current) > 0;
    }

    private Map<Integer, StatusProcess> statusDefinitions(Collection<CamundaProcessStatus> statuses) {
        List<Integer> statusIds = statuses.stream()
                .map(CamundaProcessStatus::getStatusProcessId)
                .distinct()
                .toList();
        if (statusIds.isEmpty()) {
            return Map.of();
        }
        return statusProcessRepository.findAllById(statusIds).stream()
                .collect(Collectors.toMap(StatusProcess::getId, statusProcess -> statusProcess));
    }

    private StatusDTO toStatusDto(CamundaProcessStatus status, Map<Integer, StatusProcess> statusDefinitions) {
        if (status == null) {
            return null;
        }
        StatusProcess statusProcess = statusDefinitions.get(status.getStatusProcessId());
        if (statusProcess == null) {
            throw new NotFoundException("Информация о статусе процесса не найдена");
        }
        return StatusDTO.builder()
                .id(status.getId())
                .name(statusProcess.getName())
                .alias(statusProcess.getAlias())
                .createdDate(status.getCreatedDate())
                .isDone(statusProcess.getIsDone())
                .isError(statusProcess.getIsError())
                .message(status.getMessage())
                .build();
    }

    private List<Integer> parseTypeIds(List<String> typeProcessIds) {
        if (typeProcessIds == null || typeProcessIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (String raw : typeProcessIds) {
            unique.add(parseTypeId(raw));
        }
        return List.copyOf(unique);
    }

    private Integer parseTypeId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("typeProcessId должен быть целым числом");
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new ValidationException("typeProcessId должен быть целым числом");
            }
            return (int) value;
        } catch (NumberFormatException e) {
            throw new ValidationException("typeProcessId должен быть целым числом");
        }
    }

    private LocalDateTime parseCreateDate(String raw, String errorMessage) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            throw new ValidationException(errorMessage);
        }
        try {
            return LocalDateTime.parse(text, CREATE_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException(errorMessage);
        }
    }

    private String singleOptional(List<String> raw, String repeatMessage) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (raw.size() > 1) {
            throw new ValidationException(repeatMessage);
        }
        String value = raw.get(0);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private int parsePageParam(String raw, int min, String errorMessage) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException(errorMessage);
        }
        try {
            long value = Long.parseLong(raw.trim());
            if (value < min || value > Integer.MAX_VALUE) {
                throw new ValidationException(errorMessage);
            }
            return (int) value;
        } catch (NumberFormatException e) {
            throw new ValidationException(errorMessage);
        }
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
                            .isDone(statusProcess.getIsDone())
                            .isError(statusProcess.getIsError())
                            .message(camundaProcessStatus.getMessage())
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
