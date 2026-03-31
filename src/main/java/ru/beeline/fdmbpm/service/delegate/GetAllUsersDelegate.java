package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.UserClient;
import ru.beeline.fdmbpm.dto.camundaProcess.UserShortDTO;

import java.util.List;

@Slf4j
@Component("GetAllUsersDelegate")
public class GetAllUsersDelegate implements JavaDelegate {

    @Autowired
    private UserClient userClient;

    @Override
    public void execute(DelegateExecution execution) {
        log.info("GetAllUsersDelegate start");
        List<UserShortDTO> users = userClient.getAllUsers();
        log.info("GetAllUsersDelegate users size: {}", users.size());
        execution.setVariable("users", users);
    }
}

