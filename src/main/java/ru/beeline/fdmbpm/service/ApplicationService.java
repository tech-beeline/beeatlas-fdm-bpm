/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.client.UserClient;
import ru.beeline.fdmbpm.domain.Application;
import ru.beeline.fdmbpm.domain.ApplicationTypeStatus;
import ru.beeline.fdmbpm.domain.Comment;
import ru.beeline.fdmbpm.domain.ExecutorRoles;
import ru.beeline.fdmbpm.dto.applicationDTO.*;
import ru.beeline.fdmbpm.dto.camundaProcess.CommentDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.RoleInfoDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.UserProfileDTO;
import ru.beeline.fdmbpm.exception.CustomCamundaException;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.repository.camunda.*;
import ru.beeline.fdmbpm.dto.capability.BusinessCapabilityOrderDraftResponseDTO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


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

    @Autowired
    private CapabilityClient capabilityClient;
    @Autowired
    private CamundaProcessRepository camundaProcessRepository;

    public ResponseEntity patchExecutorProcess(String businessKey, String nextStatus, String userId) {
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException(String.format("Запись с данным business_key: %s не найдена",
                        businessKey)));
        List<ExecutorRoles> executorRoles = executorRolesRepository.findByTypeId(application.getTypeId());
        if (executorRoles.isEmpty()) {
            throw new NotFoundException(String.format("Роль с данным Type Id: %s не найдена", application.getTypeId()));
        }
        if (application.getExecutorId() != null) {
            throw new ValidationException("Исполнитель уже назначен");
        }
        application.setExecutorId(Integer.valueOf(userId));
        ResponseEntity responseEntity = patchChangeStatus(businessKey, nextStatus, userId, null);
        if (responseEntity.getStatusCode().is5xxServerError() || responseEntity.getStatusCode().is4xxClientError()) {
            return responseEntity;
        } else {
            applicationRepository.save(application);
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public ResponseEntity patchChangeStatus(String businessKey, String statusAlias, String userId,
                                            CommentDTO commentDTO) {
        log.info("start ChangeStatus process to statusAlias=" + statusAlias);
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException(String.format("Запись с данным businessKey: %s не найдена",
                        businessKey)));
        Integer userIdInt = Integer.valueOf(userId);
        ApplicationTypeStatus targetStatus =
                applicationTypeStatusRepository.findByTypeIdAndAlias(application.getTypeId(), statusAlias)
                        .orElseThrow(() -> new ValidationException("Данного статуса не существует"));
        log.info(" target status is " + targetStatus.getId());
        ApplicationTypeStatus currentStatus = applicationTypeStatusRepository.findById(application.getStatusId())
                .orElseThrow(() -> new NotFoundException("Статус с данным id не найден"));
        log.info(" current status is " + currentStatus.getId());
        if (currentStatus.getAlias().equals(targetStatus.getAlias())) {
            log.info("id текущего статуса и статуса из пути запроса совпадают");
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        log.info(" currentStatus.getIsEndStatus() is " + currentStatus.getIsEndStatus());
        if (currentStatus.getIsEndStatus()) {
            throw new ValidationException("Заявка завершена");
        }
        Integer currentSerial = currentStatus.getSerialNumber();
        Integer targetSerial = targetStatus.getSerialNumber();
        boolean canChangeStatus = targetSerial.equals(currentSerial) || targetSerial.equals(currentSerial + 1);

        if (!canChangeStatus) {
            throw new ValidationException("Переход к этому статусу невозможен");
        }
        application.setUpdateDate(LocalDateTime.now());
        log.info("syc order");
        String appName = syncOrder(businessKey);
        application.setStatusId(targetStatus.getId());
        log.info("set status");
        if (targetStatus.getIsAuthorResponsible()) {
            application.setResponsibleId(application.getAuthorId());
        } else {
            application.setResponsibleId(application.getExecutorId());
        }
        sendStatusChangeMessageToProcess(application, targetStatus.getMessage(), appName);
        applicationRepository.save(application);
        log.info("save comment");
        saveComment(application, commentDTO, userIdInt);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    public List<ApplicationDTO> getApplicationsByAuthor(Integer userId) {
        List<Application> application = applicationRepository.findAllByAuthorId(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Записи с данным AuthorId: %s не найдены",
                        userId)));
        Set<Integer> participantIds = new HashSet<>();
        application.forEach(app -> {
            participantIds.add(app.getAuthorId());
            participantIds.add(app.getExecutorId());
        });
        List<ApplicationParticipantDTO> participants = userClient.getUsersInfo(participantIds.stream()
                .collect(Collectors.toList()));
        List<AdditionalInfoDTO> additional = capabilityClient.getAdditionalInfoDTO(application.stream()
                .filter(app -> app.getApplicationType()
                        .getEntityType()
                        .equals("BUSINESS_CAPABILITY"))
                .map(Application::getEntityId)
                .collect(Collectors.toList()));
        return buildApplicationDTO(application, participants, additional);
    }

    public List<ApplicationDTO> getApplicationsByExecutor(Integer userId) {
        List<Application> application = applicationRepository.findAllByExecutorId(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Записи с данным AuthorId: %s не найдены",
                        userId)));
        Set<Integer> participantIds = new HashSet<>();
        application.forEach(app -> {
            participantIds.add(app.getAuthorId());
            participantIds.add(app.getExecutorId());
        });
        List<ApplicationParticipantDTO> participants = userClient.getUsersInfo(participantIds.stream()
                .collect(Collectors.toList()));
        List<AdditionalInfoDTO> additional = capabilityClient.getAdditionalInfoDTO(application.stream()
                .filter(app -> app.getApplicationType()
                        .getEntityType()
                        .equals("BUSINESS_CAPABILITY"))
                .map(Application::getEntityId)
                .collect(Collectors.toList()));
        return buildApplicationDTO(application, participants, additional);
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

    private void sendStatusChangeMessageToProcess(Application application, String message, String appName) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("message", message);
            variables.put("name", appName);
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

    public void sendMessageToProcess(String taskKey, Map<String, Object> variables, String messageName) {
        try {
            String processId = camundaProcessRepository.findById(Integer.valueOf(taskKey)).get().getProcId();
            runtimeService.createMessageCorrelation(messageName)
                    .processInstanceId(processId)
                    .setVariables(variables)
                    .correlate();
            log.info("Переданы данные в процесс Camunda: {taskKey, " + taskKey + "}");
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения в Camunda", e);
            throw new CustomCamundaException("Ошибка при отправке сообщения в Camunda");
        }
    }

    public List<ApplicationDTO> getAssignedApplications(String userRolesHeader) {
        List<String> roles = parseRoles(userRolesHeader);
        List<ExecutorRoles> executorRoles;
        if (roles != null && !roles.isEmpty()) {
            executorRoles = executorRolesRepository.findByRoleIn(roles);
        } else {
            throw new ValidationException("Отсутствует роль в заголовках");
        }
        List<Application> applicationList = applicationRepository.findAllByTypeIdInAndExecutorIdNull(executorRoles.stream()
                .map(ExecutorRoles::getTypeId)
                .collect(
                        Collectors.toList()));
        Set<Integer> participantIds = new HashSet<>();
        applicationList.forEach(app -> {
            participantIds.add(app.getAuthorId());
            participantIds.add(app.getExecutorId());
        });
        List<ApplicationParticipantDTO> participants = userClient.getUsersInfo(participantIds.stream()
                .collect(Collectors.toList()));
        List<AdditionalInfoDTO> additional = capabilityClient.getAdditionalInfoDTO(applicationList.stream()
                .filter(app -> app.getApplicationType()
                        .getEntityType()
                        .equals("BUSINESS_CAPABILITY"))
                .map(Application::getEntityId)
                .collect(Collectors.toList()));
        return buildApplicationDTO(applicationList, participants, additional);
    }

    private List<String> parseRoles(String userRolesHeader) {
        if (userRolesHeader == null || userRolesHeader.isBlank()) return Collections.emptyList();
        return Arrays.stream(userRolesHeader.split(","))
                .map(s -> s.replaceAll("[\\[\\]\"]", "").trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<ApplicationDTO> buildApplicationDTO(List<Application> applicationList,
                                                     List<ApplicationParticipantDTO> participants,
                                                     List<AdditionalInfoDTO> additional) {
        Map<Integer, ApplicationParticipantDTO> participantsMap = participants.stream()
                .collect(Collectors.toMap(ApplicationParticipantDTO::getId, Function.identity()));
        return applicationList.stream()
                .map(application -> ApplicationDTO.builder()
                        .id(application.getId())
                        .businessKey(application.getBusinessKey())
                        .type(ApplicationTypeDTO.builder()
                                .id(application.getApplicationType().getId())
                                .name(application.getApplicationType().getName())
                                .description(application.getApplicationType().getDescription())
                                .entityType(application.getApplicationType().getEntityType())
                                .build())
                        .status(ApplicationStatusDTO.builder()
                                .id(application.getStatus().getId())
                                .name(application.getStatus().getName())
                                .alias(application.getStatus().getAlias())
                                .isEndStatus(application.getStatus().getIsEndStatus())
                                .build())
                        .author(ApplicationParticipantDTO.builder()
                                .id(application.getAuthorId())
                                .fullName(participantsMap.get(application.getAuthorId()).getFullName())
                                .email(participantsMap.get(application.getAuthorId()).getEmail())
                                .build())
                        .entityId(application.getEntityId())
                        .executor(getExecutor(application, participantsMap))
                        .name(application.getName())
                        .responsibleId(application.getResponsibleId())
                        .createDate(application.getCreateDate())
                        .updateDate(application.getUpdateDate())
                        .comments(buildComments(application.getId()))
                        .additionalInfo(getAdditionalInfo(application, additional))
                        .build())
                .toList();
    }

    private ApplicationParticipantDTO getExecutor(Application application,
                                                  Map<Integer, ApplicationParticipantDTO> participantsMap) {
        if (application.getExecutorId() == null) {
            return null;
        }
        return ApplicationParticipantDTO.builder()
                .id(application.getExecutorId())
                .fullName(participantsMap.get(application.getExecutorId()).getFullName())
                .email(participantsMap.get(application.getExecutorId()).getEmail())
                .build();
    }

    private List<ApplicationAdditionalInfoDTO> getAdditionalInfo(Application application,
                                                                 List<AdditionalInfoDTO> additional) {
        if (additional == null) {
            return null;
        }
        return additional.stream()
                .filter(additionalInfoDTO -> application.getEntityId().equals(additionalInfoDTO.getOrderBcId()))
                .map(additionalInfoDTO -> ApplicationAdditionalInfoDTO.builder()
                        .name("Домен")
                        .value(additionalInfoDTO.getDomainName().toString())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ApplicationCommentDTO> buildComments(Integer applicationId) {
        List<Comment> commentList = commentRepository.findAllByApplicationId(applicationId);
        if (!commentList.isEmpty()) {
            return commentList.stream()
                    .map(comment -> ApplicationCommentDTO.builder()
                            .id(comment.getId())
                            .comment(comment.getComment())
                            .createdDate(comment.getCreatedDate())
                            .fullName(comment.getFullName())
                            .build())
                    .toList();
        }
        return new ArrayList<>();
    }

    public ApplicationExtendedDTO getApplications(Integer id, String businessKey) {
        if (id == null && businessKey == null) {
            throw new ValidationException("Отсутствует id/business-key");
        }
        Application application;
        if (id != null) {
            application = applicationRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException(String.format("Запись с данным id: %s не найдена", id)));
        } else {
            application = applicationRepository.findByBusinessKey(businessKey)
                    .orElseThrow(() -> new NotFoundException(String.format("Запись с данным businessKey: %s не найдена",
                            businessKey)));
        }
        UserProfileDTO authorProfileDTO = userClient.getUserProfile(application.getAuthorId());
        AuthorDTO author = AuthorDTO.builder()
                .id(authorProfileDTO.getId())
                .fullName(authorProfileDTO.getFullName())
                .email(authorProfileDTO.getEmail())
                .build();
        AuthorDTO executor = null;
        if (application.getExecutorId() != null) {
            UserProfileDTO executorProfileDTO = userClient.getUserProfile(application.getExecutorId());
            executor = AuthorDTO.builder()
                    .id(executorProfileDTO.getId())
                    .fullName(executorProfileDTO.getFullName())
                    .email(executorProfileDTO.getEmail())
                    .build();
        }
        return buildApplicationExtendedDTO(application, author, executor);
    }

    private ApplicationExtendedDTO buildApplicationExtendedDTO(Application application,
                                                               AuthorDTO author,
                                                               AuthorDTO executor) {
        return ApplicationExtendedDTO.builder()
                .id(application.getId())
                .entityId(application.getEntityId())
                .businessKey(application.getBusinessKey())
                .type(ApplicationTypeDTO.builder()
                        .id(application.getApplicationType().getId())
                        .name(application.getApplicationType().getName())
                        .description(application.getApplicationType().getDescription())
                        .entityType(application.getApplicationType().getEntityType())
                        .build())
                .status(ApplicationStatusShortDTO.builder()
                        .id(application.getStatus().getId())
                        .name(application.getStatus().getName())
                        .alias(application.getStatus().getAlias())
                        .isEndStatus(application.getStatus().getIsEndStatus())
                        .build())
                .author(author)
                .executor(executor)
                .name(application.getName())
                .responsibleId(application.getResponsibleId())
                .createDate(application.getCreateDate())
                .updateDate(application.getUpdateDate())
                .comments(buildComments(application.getId()))
                .build();
    }

    public void changeExecutor(String businessKey, Integer newExecutorId, Integer userId) {
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException("Процесс по данному businessKey не найден"));
        validateApplicationStatus(application);
        validateUser(newExecutorId);
        if (application.getExecutorId().equals(application.getResponsibleId())) {
            application.setResponsibleId(newExecutorId);
        }
        application.setExecutorId(newExecutorId);
        application.setUpdateDate(LocalDateTime.now());
        applicationRepository.save(application);
    }

    private ApplicationTypeStatus validateApplicationStatus(Application application) {
        ApplicationTypeStatus applicationTypeStatus = applicationTypeStatusRepository.findByIdAndTypeId(application.getStatusId(),
                        application.getTypeId())
                .orElseThrow(() -> new NotFoundException("Статус процесса не найден"));
        if (applicationTypeStatus.getIsEndStatus()) {
            throw new ValidationException("Заявка завершена");
        }
        return applicationTypeStatus;
    }

    private void validateUser(Integer userId) {
        UserProfileDTO user = userClient.getUserProfile(userId);
        if (user == null) {
            throw new NotFoundException("Указанного пользователя не существует");
        }
        List<String> roles = user.getRoles().stream().map(RoleInfoDTO::getAlias).toList();
        if (!roles.contains("ADMINISTRATOR")) {
            throw new ValidationException("Новый исполнитель по заявке должен иметь роль ADMINISTRATOR");
        }
    }

    public String syncOrder(String businessKey) {
        Application application = applicationRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new NotFoundException("Процесс по данному businessKey не найден"));
        validateApplicationStatus(application);
        if (application.getApplicationType().getEntityType().equals("BUSINESS_CAPABILITY")) {
            BusinessCapabilityOrderDraftResponseDTO bcOrder = capabilityClient.getBusinessCapabilityOrder(application.getEntityId());
            if (!bcOrder.getName().equals(application.getName())) {
                application.setName(bcOrder.getName());
                application.setUpdateDate(LocalDateTime.now());
                applicationRepository.save(application);
            }
        }
        return application.getName();
    }

}

