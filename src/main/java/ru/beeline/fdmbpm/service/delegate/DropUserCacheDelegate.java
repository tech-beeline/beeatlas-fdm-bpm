package ru.beeline.fdmbpm.service.delegate;

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

@Slf4j
@Component("DropUserCacheDelegate")
public class DropUserCacheDelegate implements JavaDelegate {

    @Autowired
    private RabbitService rabbitService;

    @Value("${queue.user-drop-cache.name}")
    private String userDropCacheQueue;

    @Override
    public void execute(DelegateExecution execution) {
        Object dropCacheObj = execution.getVariable("drop-cache");
        if (!(dropCacheObj instanceof Boolean) || !(Boolean) dropCacheObj) {
            log.info("drop-cache is false or not set, skipping cache drop");
            return;
        }

        Object userObj = execution.getVariable("user");
        if (!(userObj instanceof UserShortDTO)) {
            log.error("Variable 'user' is not instance of UserShortDTO: {}", userObj);
            return;
        }

        UserShortDTO user = (UserShortDTO) userObj;
        if (user.getLogin() == null) {
            log.error("UserShortDTO.login is null");
            return;
        }

        UserDropCacheMessage message = new UserDropCacheMessage(user.getLogin());
        rabbitService.sendMessage(userDropCacheQueue, message);
        log.info("Sent user drop cache message for login: {}", user.getLogin());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class UserDropCacheMessage {
        private String userLogin;
    }
}

