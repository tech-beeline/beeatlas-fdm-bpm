package ru.beeline.fdmbpm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.domain.Application;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.domain.ExecutorRoles;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.camundaProcess.CommentDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.GetContextDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.GetProcessByIdDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.GetProcessDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ProcessDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.ShortContextDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.StatusDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.TypeDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.mapper.ContextDtoMapper;
import ru.beeline.fdmbpm.repository.ApplicationRepository;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.ContextRepository;
import ru.beeline.fdmbpm.repository.ExecutorRolesRepository;
import ru.beeline.fdmbpm.repository.StatusProcessRepository;
import ru.beeline.fdmbpm.repository.TypeProcessRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static ru.beeline.fdmbpm.utils.Constants.USER_ID_HEADER;

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
    ApplicationRepository applicationRepository;

    @Autowired
    ExecutorRolesRepository executorRolesRepository;

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
            for (Context context : contextList) {
                Optional<CamundaProcess> optionalCamundaProcess = camundaProcessRepository.findById(context.getCamundaProcessId());
                if (optionalCamundaProcess.isPresent()) {
                    CamundaProcess camundaProcess = optionalCamundaProcess.get();
                    TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId())
                            .orElseThrow(() -> new NotFoundException(String.format("Type process, для camunda process, с type process id: %s не найден",
                                    camundaProcess.getTypeProcessId())));
                    CamundaProcessStatus camundaProcessStatus =
                            camundaProcessStatusRepository.findFirstByCamundaProcessIdOrderByCreatedDateDesc(camundaProcess.getId())
                                    .orElseThrow(() -> new NotFoundException(String.format("Camunda process status для camunda process с id: %s не найден",
                                            camundaProcess.getId())));
                    StatusProcess statusProcess =
                            statusProcessRepository.findById(camundaProcessStatus.getStatusProcessId())
                                    .orElseThrow(() -> new NotFoundException("Status Process не найден"));
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

    public void patchExecutorProcess(String businessKey, String nextStatus, HttpServletRequest request) {

        Application application = applicationRepository.findByBusinessKey(businessKey).orElseThrow(() ->
                new NotFoundException(String.format("Запись с данным businessKey: %s не найдена", businessKey)));
        List<ExecutorRoles> executorRoles = executorRolesRepository.findByTypeId(application.getTypeId());
        if (executorRoles.isEmpty()) {
            throw new NotFoundException(String.format("Роль с данным Type Id: %s не найдена", application.getTypeId()));
        } else {
            if (application.getExecutorId() != null) {
                throw new ValidationException("Исполнитель уже назначен");
            } else {
                application.setExecutorId(Integer.valueOf(request.getHeader(USER_ID_HEADER)));
                applicationRepository.save(application);
                patchChangeStatus(businessKey,nextStatus,request,null);
            }
        }
    }

    public void patchChangeStatus(String businessKey, String nextStatus, HttpServletRequest request, CommentDTO commentDTO) {


    }
}
