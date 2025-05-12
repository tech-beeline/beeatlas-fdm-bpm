package ru.beeline.fdmbpm.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import ru.beeline.fdmbpm.exception.CustomCamundaException;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.repository.ApplicationRepository;
import ru.beeline.fdmbpm.repository.ApplicationTypeStatusRepository;
import ru.beeline.fdmbpm.repository.CommentRepository;
import ru.beeline.fdmbpm.repository.ExecutorRolesRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public ResponseEntity patchExecutorProcess(String businessKey, String nextStatus, HttpServletRequest request) {
        Application application = applicationRepository.findByBusinessKey(businessKey).orElseThrow(() ->
                new NotFoundException(String.format("Запись с данным business_key: %s не найдена", businessKey)));
        List<ExecutorRoles> executorRoles = executorRolesRepository.findByTypeId(application.getTypeId());
        if (executorRoles.isEmpty()) {
            throw new NotFoundException(String.format("Роль с данным Type Id: %s не найдена", application.getTypeId()));
        }
        if (!hasAccessRole(executorRoles.stream().map(ExecutorRoles::getRole).toList(), RequestContext.getRoles())) {
            throw new ForbiddenException("Forbidden");
        }
        if (application.getExecutorId() != null) {
            throw new ValidationException("Исполнитель уже назначен");
        }
        application.setExecutorId(Integer.valueOf(request.getHeader(USER_ID_HEADER)));
        ResponseEntity responseEntity = patchChangeStatus(businessKey, nextStatus, request, null);
        if (responseEntity.getStatusCode().is5xxServerError() || responseEntity.getStatusCode().is4xxClientError()) {
            return responseEntity;
        } else {
            applicationRepository.save(application);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    private boolean hasAccessRole(List<String> executorRoles, List<String> roles) {
        return roles.stream()
                .anyMatch(executorRoles::contains);
    }

    public ResponseEntity patchChangeStatus(String businessKey, String statusAlias, HttpServletRequest request,
                                            CommentDTO commentDTO) {
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
            log.info("id текущего статуса и статуса из пути запроса совпадают");
            return ResponseEntity.status(HttpStatus.OK).build();
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
        sendStatusChangeMessageToProcess(application, targetStatus.getMessage());
        applicationRepository.save(application);
        saveComment(application, commentDTO, userId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public List<ApplicationDTO> getApplicationsByAuthor(Integer userId) {
        List<Application> application = applicationRepository.findAllByAuthorId(userId).orElseThrow(() ->
                new NotFoundException(String.format("Записи с данным AuthorId: %s не найдены", userId)));
        return buildApplicationDTO(application);
    }

    public List<ApplicationDTO> getApplicationsByExecutor(Integer userId) {
        List<Application> application = applicationRepository.findAllByExecutorId(userId).orElseThrow(() ->
                new NotFoundException(String.format("Записи с данным AuthorId: %s не найдены", userId)));
        return buildApplicationDTO(application);
    }

    private Application getAuthorizedApplication(String businessKey, HttpServletRequest request) {
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException(String.format("Запись с данным businessKey: %s не найдена", businessKey)));
        if (request.getHeader(USER_ID_HEADER) == null || request.getHeader(USER_ID_HEADER).isEmpty()) {
            throw new ForbiddenException("Нет прав доступа");
        }
        Integer userId = Integer.valueOf(request.getHeader(USER_ID_HEADER));
        if (!Objects.equals(application.getAuthorId(), userId) && !Objects.equals(application.getExecutorId(), userId)) {
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
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("message", message);
            runtimeService.createMessageCorrelation("change_of_status")
                    .processInstanceId(application.getProcessId())
                    .setVariables(variables)
                    .correlate();
            log.info("Переданы данные в процесс Camunda: {message, " + message + "}");
        } catch (MismatchingMessageCorrelationException e) {
            log.error("Процесс в Camunda с ID " + application.getProcessId() + " не найден или уже завершён");
            throw new CustomCamundaException("Процесс в Camunda с ID " + application.getProcessId() + " не найден или уже завершён");
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения в Camunda", e);
            throw new CustomCamundaException("Ошибка при отправке сообщения в Camunda");
        }
    }

    public List<ApplicationDTO> getAssignedApplications() {
        List<String> roles = RequestContext.getRoles();
        List<ExecutorRoles> executorRoles;
        if (roles != null && !roles.isEmpty()) {
            executorRoles = executorRolesRepository.findByRoleIn(roles);
        } else {
            throw new ValidationException("Отсутствует роль в заголовках");
        }
        List<Application> applicationList = applicationRepository.findAllByTypeIdInAndExecutorIdNull(executorRoles.stream().
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
