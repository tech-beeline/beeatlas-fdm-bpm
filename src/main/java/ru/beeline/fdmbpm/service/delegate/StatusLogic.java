package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.StatusProcessRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
public class StatusLogic {

    @Autowired
    StatusProcessRepository statusProcessRepository;
    @Autowired
    CamundaProcessStatusRepository camundaProcessStatusRepository;

    protected void saveAlias(Integer processId, String alias, TypeProcess typeProcess) {
        StatusProcess statusProcess = statusProcessRepository.findByAliasAndTypeProcessId(alias, typeProcess.getId());
        camundaProcessStatusRepository.save(CamundaProcessStatus.builder()
                .statusProcessId(statusProcess.getId())
                .camundaProcessId(processId)
                .createdDate(LocalDateTime.now())
                .build());
    }
}
