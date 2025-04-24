package ru.beeline.fdmbpm.service;


import camundajar.impl.scala.App;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.fdmbpm.client.UserClient;
import ru.beeline.fdmbpm.domain.Application;
import ru.beeline.fdmbpm.domain.ApplicationTypeEnum;
import ru.beeline.fdmbpm.domain.ApplicationTypeStatus;
import ru.beeline.fdmbpm.dto.camundaProcess.UserProfileDTO;
import ru.beeline.fdmbpm.exception.CustomCamundaException;
import ru.beeline.fdmbpm.repository.ApplicationRepository;
import ru.beeline.fdmbpm.repository.ApplicationTypeEnumRepository;
import ru.beeline.fdmbpm.repository.ApplicationTypeStatusRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ApplicationProcessService {

    @Autowired
    UserClient userClient;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    ApplicationTypeEnumRepository applicationTypeEnumRepository;

    @Autowired
    ApplicationTypeStatusRepository applicationTypeStatusRepository;


    public void applicationProcess(String processInstanceId, String businessKey, Integer authorId, String type,
                                   String comment, Integer entityId, String name) {

        ApplicationTypeEnum applicationTypeEnum = applicationTypeEnumRepository.findByAlias(type);
        if (applicationTypeEnum == null) {
            throw new CustomCamundaException(String.format("Запись в таблице application_type_enum с alias: {} не найдена.", type));
        }
        ApplicationTypeStatus applicationTypeStatus =
                applicationTypeStatusRepository.findByTypeIdAndSerialNumber(applicationTypeEnum.getId(), 1);

        Application application = applicationRepository.save(Application.builder()
                        .typeId(applicationTypeEnum.getId())
                        .statusId(applicationTypeStatus.getId())
                        .authorId(authorId)
                        .processId(processInstanceId)
                        .businessKey(businessKey)
                        .name(name)
                        .createDate(LocalDateTime.now())
                        .entityId(entityId)
                .build());
        if(comment!=null && !comment.isEmpty()){

            UserProfileDTO user = userClient.getUserProfile()
        }
    }
}
//вызвать метод GET /api/v1/user/{authorId} (https://btask.beeline.ru/browse/SFDM-2523) сервиса user-auth
//из ответа получить full_name
//в таблице comments схемы процесс создать запись
//id - следующее значение сиквенса seq_comments_id
//application_id = id заявки из таблицы application
//comment = значение атрибута comment из тела запроса
//create_date = текущая дата/время
//full_name = full_name из ответа на вызов GET метода
//положить в контекст appliationId созданной заявки
//положить в контекст typeId заявки с которым она создана