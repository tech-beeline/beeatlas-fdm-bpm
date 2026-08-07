/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.UserClient;
import ru.beeline.fdmbpm.dto.camundaProcess.BeeworksUserProductsDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.BeeworksUserRoleDTO;
import ru.beeline.fdmbpm.dto.camundaProcess.UserShortDTO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component("UpdateUserProductsDelegate")
public class UpdateUserProductsDelegate implements JavaDelegate {

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProductClient productClient;

    @Override
    public void execute(DelegateExecution execution) {
        Object userObj = execution.getVariable("user");
        if (!(userObj instanceof UserShortDTO)) {
            log.error("Переменная 'user' не является экземпляром UserShortDTO: {}", userObj);
            execution.setVariable("drop-cache", false);
            return;
        }
        UserShortDTO user = (UserShortDTO) userObj;
        String login = user.getLogin();
        Integer id = user.getId();
        if (login == null || id == null) {
            log.error("UserShortDTO содержит null в поле login или id: {}", user);
            execution.setVariable("drop-cache", false);
            return;
        }
        log.info("ID пользователя: {}", user.getId());
        try {
            BeeworksUserProductsDTO beeworksResponse = userClient.getBeeworksUserProducts(login);
            if (beeworksResponse == null || beeworksResponse.getBwRoles() == null) {
                log.info("beeworksResponse или getBwRoles() равен null: {}", userObj);
                execution.setVariable("drop-cache", false);
                return;
            }
            List<String> cmdbCodes = beeworksResponse.getBwRoles()
                    .stream()
                    .map(BeeworksUserRoleDTO::getCmdbCode)
                    .filter(Objects::nonNull)
                    .filter(code -> !code.isEmpty())
                    .collect(Collectors.toList());
            log.info("Получено {} кодов продуктов для пользователя {}", cmdbCodes.size(), id);
            productClient.updateUserProducts(id, cmdbCodes);
            execution.setVariable("drop-cache", true);
            log.info("Продукты для пользователя {} успешно обновлены, кэш будет сброшен", id);
        } catch (Exception e) {
            log.error("Ошибка при обновлении продуктов пользователя {}: ", id, e);
            execution.setVariable("drop-cache", false);
        }
    }
}

