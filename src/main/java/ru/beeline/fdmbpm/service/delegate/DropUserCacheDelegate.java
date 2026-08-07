/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.dto.camundaProcess.UserShortDTO;
import ru.beeline.fdmbpm.service.RabbitService;

import java.io.Serializable;

@Slf4j
@Component("DropUserCacheDelegate")
public class DropUserCacheDelegate implements JavaDelegate {

    @Autowired
    private RabbitService rabbitService;

    @Value("${queue.user-drop-cache.name}")
    private String userDropCacheQueue;

    @Autowired
    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(DelegateExecution execution) {
        try {

            log.info("DropUserCacheDelegate start");
            Object dropCacheObj = execution.getVariable("drop-cache");
            if (!(dropCacheObj instanceof Boolean) || !(Boolean) dropCacheObj) {
                log.info("drop-cache is: {}", execution.getVariable("drop-cache"));
                return;
            }

            Object userObj = execution.getVariable("user");
            if (!(userObj instanceof UserShortDTO)) {
                log.info("userObj isn't instanceof UserShortDTO: {}", execution.getVariable("user"));
                return;
            }

            UserShortDTO user = (UserShortDTO) userObj;
            if (user.getLogin() == null) {
                log.info("user.getLogin() == null: {}", user.getLogin());
                return;
            }
            UserDropCacheMessage msg = new UserDropCacheMessage(user.getLogin());
            String message = null;
            message = objectMapper.writeValueAsString(msg);

            rabbitService.sendMessage(userDropCacheQueue, message);
            log.info("Sent user drop cache message for login: {}", user.getLogin());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class UserDropCacheMessage implements Serializable {
        private String userLogin;
    }
}

