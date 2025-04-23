package ru.beeline.fdmbpm.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.client.UserClient;
import ru.beeline.fdmbpm.controller.RequestContext;
import ru.beeline.fdmbpm.domain.Application;
import ru.beeline.fdmbpm.domain.ApplicationTypeStatus;
import ru.beeline.fdmbpm.domain.Comment;
import ru.beeline.fdmbpm.domain.ExecutorRoles;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationCommentDTO;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationDTO;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationStatusDTO;
import ru.beeline.fdmbpm.dto.applicationDTO.ApplicationTypeDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.CommentDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.UserProfileDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.repository.ApplicationRepository;
import ru.beeline.fdmbpm.repository.ApplicationTypeStatusRepository;
import ru.beeline.fdmbpm.repository.CommentRepository;
import ru.beeline.fdmbpm.repository.ExecutorRolesRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.beeline.fdmbpm.utils.Constants.USER_ID_HEADER;

@Slf4j
@Service
public class ApplicationService {

    @Autowired
    UserClient userClient;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    ExecutorRolesRepository executorRolesRepository;

    @Autowired
    ApplicationTypeStatusRepository applicationTypeStatusRepository;

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
                patchChangeStatus(businessKey, nextStatus, request, null);
            }
        }
    }

    public void patchChangeStatus(String businessKey, String statusAlias, HttpServletRequest request, CommentDTO commentDTO) {
        Application application = getAuthorizedApplication(businessKey, request);
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        ApplicationTypeStatus targetStatus = applicationTypeStatusRepository
                .findByTypeIdAndAlias(application.getTypeId(), statusAlias);
        if (targetStatus == null) {
            throw new ValidationException("Данного статуса не существует");
        }
        ApplicationTypeStatus currentStatus = applicationTypeStatusRepository.findById(application.getStatusId())
                .orElseThrow(() -> new NotFoundException("Статус с данным id не найден"));
        if (currentStatus.getAlias().equals(targetStatus.getAlias())) {
            return;
        }
        if (currentStatus.getIsEndStatus()) {
            throw new ValidationException("Заявка завершена");
        }
        Integer currentSerial = currentStatus.getSerialNumber();
        Integer targetSerial = targetStatus.getSerialNumber();
        boolean canChangeStatus = targetSerial.equals(currentSerial) || targetSerial.equals(currentSerial - 1);
        if (!canChangeStatus) {
            throw new ValidationException("Переход к этому статусу невозможен");
        }
        application.setStatusId(targetStatus.getId());
        if (targetStatus.getIsAuthorResponsible()) {
            application.setResponsibleId(application.getAuthorId());
        } else {
            application.setResponsibleId(application.getExecutorId());
        }
        application.setUpdateDate(LocalDateTime.now());
        applicationRepository.save(application);
        saveComment(application, commentDTO, userId);
        sendStatusChangeMessageToProcess(application, targetStatus.getMessage());
    }

    private Application getAuthorizedApplication(String businessKey, HttpServletRequest request) {
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException(String.format("Запись с данным businessKey: %s не найдена", businessKey)));
        if (request.getHeader(USER_ID_HEADER) == null || request.getHeader(USER_ID_HEADER).isEmpty()) {
            throw new ForbiddenException("Нет прав доступа");
        }
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        if (!Objects.equals(application.getAuthorId(), userId) || !Objects.equals(application.getExecutorId(), userId)) {
            throw new ForbiddenException("Нет прав доступа");
        }
        return application;
    }

    private void saveComment(Application application, CommentDTO commentDTO, Integer userId) {
        if (commentDTO != null && commentDTO.getComment() != null) {
            UserProfileDTO userProfileDTO = userClient.getUserProfile(userId);
            if (userProfileDTO == null) {
                throw new NotFoundException("Пользователь с данным id не найден");
            }
            commentRepository.save(Comment.builder()
                    .applicationId(application.getId())
                    .comment(commentDTO.getComment())
                    .createdDate(LocalDateTime.now())
                    .fullName(userProfileDTO.getFullName())
                    .build());
        }
    }

    private void sendStatusChangeMessageToProcess(Application application, String message) {
//                Процесса еще не реализован
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("message", message);
//        runtimeService.createMessageCorrelation("change_of_status")
//                .processInstanceId(application.getProcessId())
//                .setVariables(variables)
//                .correlate();
        log.info("переданы данные в процесс камунды: {message, " + message + "}");
        log.info("самого процесса еще нет");
    }

    public List<ApplicationDTO> getAssignedApplications() {
        List<String> roles = RequestContext.getRoles();
        List<ExecutorRoles> executorRoles;
        if (roles != null && !roles.isEmpty()) {
            executorRoles = executorRolesRepository.findByRoleIn(roles);
        } else {
            throw new ValidationException("Отсутствует роль в заголовках");
        }
        List<Application> applicationList = applicationRepository.findAllByTypeIdIn(executorRoles.stream().
                map(ExecutorRoles::getTypeId).collect(Collectors.toList()));
        return buildApplicationDTO(applicationList);
    }

    private List<ApplicationDTO> buildApplicationDTO(List<Application> applicationList) {
        return applicationList.stream().
                map(application -> ApplicationDTO.builder()
                        .id(application.getId())
                        .type(ApplicationTypeDTO.builder()
                                .id(application.getApplicationType().getId())
                                .name(application.getApplicationType().getName())
                                .description(application.getApplicationType().getDescription())
                                .entityType(application.getApplicationType().getEntityType())
                                .build())
                        .status(ApplicationStatusDTO.builder()
                                .id(application.getStatus().getId())
                                .name(application.getStatus().getName())
                                .isEndStatus(application.getStatus().getIsEndStatus())
                                .build())
                        .authorId(application.getAuthorId())
                        .executorId(application.getExecutorId())
                        .name(application.getName())
                        .responsibleId(application.getResponsibleId())
                        .createDate(application.getCreateDate())
                        .updateDate(application.getUpdateDate())
                        .comments(buildComments(application.getId()))
                        .build()).toList();
    }

    private List<ApplicationCommentDTO> buildComments(Integer applicationId) {
        List<Comment> commentList = commentRepository.findAllByApplicationId(applicationId);
        if (!commentList.isEmpty()) {
            return commentList.stream().map(comment -> ApplicationCommentDTO.builder()
                    .id(comment.getId())
                    .comment(comment.getComment())
                    .createdDate(comment.getCreatedDate())
                    .fullName(comment.getFullName())
                    .build()).toList();
        }
        return new ArrayList<>();
    }
}
