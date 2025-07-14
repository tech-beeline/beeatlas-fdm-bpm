package ru.beeline.fdmbpm.service.appservice;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.client.DashboardClient;
import ru.beeline.fdmbpm.client.NotifyServiceClient;
import ru.beeline.fdmbpm.client.UserClient;
import ru.beeline.fdmbpm.domain.*;
import ru.beeline.fdmbpm.dto.camundaProcess.UserProfileDTO;
import ru.beeline.fdmbpm.exception.CustomCamundaException;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.repository.*;
import ru.beeline.fdmlib.dto.capability.BusinessCapabilityOrderDraftResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ApplicationProcessService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private CapabilityClient capabilityClient;

    @Autowired
    private DashboardClient dashboardClient;

    @Autowired
    private NotifyServiceClient notifyServiceClient;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ExecutorRolesRepository executorRolesRepository;

    @Autowired
    private ApplicationTypeEnumRepository applicationTypeEnumRepository;

    @Autowired
    private ApplicationTypeStatusRepository applicationTypeStatusRepository;

    @Autowired
    private RestTemplate restTemplate;

    public void applicationProcess(String processInstanceId,
                                   String businessKey,
                                   Integer authorId,
                                   String type,
                                   String comment,
                                   Integer entityId,
                                   String name) {

        log.info("Запуск процесса обработки заявки. processInstanceId: {}, businessKey: {}",
                 processInstanceId,
                 businessKey);
        ApplicationTypeEnum applicationTypeEnum = applicationTypeEnumRepository.findByAlias(type)
                .orElseThrow(() -> new CustomCamundaException(String.format(
                        "Запись в таблице application_type_enum с alias: %s не найдена.",
                        type)));
        Integer typeId = applicationTypeEnum.getId();
        log.info("Тип заявки найден. typeId: {}", typeId);
        ApplicationTypeStatus applicationTypeStatus = applicationTypeStatusRepository.findByTypeIdAndSerialNumber(typeId,
                                                                                                                  1)
                .orElseThrow(() -> new CustomCamundaException(String.format(
                        "Запись в таблице application_type_status с typeId: %s , и SerialNumber 1 не найдена.",
                        typeId)));
        log.info("Статус для типа заявки найден. statusId: {}", applicationTypeStatus.getId());
        Application saveApplication = applicationRepository.save(Application.builder()
                                                                         .typeId(applicationTypeEnum.getId())
                                                                         .statusId(applicationTypeStatus.getId())
                                                                         .authorId(authorId)
                                                                         .processId(processInstanceId)
                                                                         .businessKey(businessKey)
                                                                         .name(name)
                                                                         .createDate(LocalDateTime.now())
                                                                         .entityId(entityId)
                                                                         .build());
        log.info("Заявка успешно сохранена. applicationId: {}", saveApplication.getId());
        if (comment != null && !comment.isEmpty()) {
            log.info("Добавляем комментарий к заявке. comment: {}", comment);
            saveCommentWithUser(comment, authorId, saveApplication.getId());
        }
        log.info("Устанавливаем переменные в контекст процесса. applicationId: {}, typeId: {}",
                 saveApplication.getId(),
                 applicationTypeEnum.getId());
        runtimeService.setVariable(processInstanceId, "applicationId", saveApplication.getId());
        runtimeService.setVariable(processInstanceId, "typeId", applicationTypeEnum.getId());
    }

    private void saveCommentWithUser(String comment, Integer authorId, Integer applicationId) {
        UserProfileDTO user = userClient.getUserProfile(authorId);
        commentRepository.save(Comment.builder()
                                       .applicationId(applicationId)
                                       .comment(comment)
                                       .createdDate(LocalDateTime.now())
                                       .fullName(user.getFullName())
                                       .build());
    }

    public void sendGroupNotifications(Integer applicationId, String type, Integer typeId, String name) {
        log.info("Рассылаем уведомления ответственным. applicationId: {}, type: {}", applicationId, type);
        List<ExecutorRoles> executorRoles = executorRolesRepository.findByTypeId(typeId);
        for (ExecutorRoles role : executorRoles) {
            log.info("Отправка уведомления для роли. role: {}, type: {}, applicationId: {}",
                     role.getRole(),
                     type,
                     applicationId);
            notifyServiceClient.postBusinessEvent(role.getRole(), type, applicationId, name);
        }
    }

    public void notifyResponsible(Integer applicationId, String type) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Запись в таблице application c id: " + applicationId + " не найдена"));
        log.info("Отправка уведомления ответственному. responsibleId: {}", application.getResponsibleId());
        notifyServiceClient.postExportNotify(applicationId, type, application.getResponsibleId());
    }

    public void performTargetAction(DelegateExecution delegateExecution, Integer typeId, Integer entityId) {
        ApplicationTypeEnum applicationTypeEnum = applicationTypeEnumRepository.findById(typeId)
                .orElseThrow(() -> new NotFoundException("Запись в таблице application_type_enum c id: " + typeId + " не найдена"));
        String targetCall = applicationTypeEnum.getTargetCall();
        log.info("targetCall: {}", targetCall);
        if (targetCall != null && !targetCall.isEmpty() && entityId != null) {
            String url = targetCall.replace("{id}", entityId.toString());
            log.info("url: {}", url);
            sendPostRequest(url);
        }
        if(delegateExecution != null) {
            if (applicationTypeEnum.getAlias().equals("create_business_capability") || applicationTypeEnum.getAlias()
                    .equals("update_business_capability")) {
                Application application = applicationRepository.findByProcessId(delegateExecution.getProcessInstanceId())
                        .get();
                BusinessCapabilityOrderDraftResponseDTO order = capabilityClient.getOrderBc(application.getEntityId());
                dashboardClient.putCapability(order);
            }
        }
    }

    private void sendPostRequest(String url) {
        try {
            restTemplate.postForObject(url, null, String.class);
            log.info("Целевое действие выполнено по URL: {}", url);
        } catch (Exception e) {
            log.error("Ошибка при выполнении целевого действия по URL: {}", url, e);
        }
    }
}
