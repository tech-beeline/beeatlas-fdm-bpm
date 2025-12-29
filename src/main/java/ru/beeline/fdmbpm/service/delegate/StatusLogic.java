package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.StatusProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.StatusProcessRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
public class StatusLogic {

    @Autowired
    StatusProcessRepository statusProcessRepository;
    @Autowired
    CamundaProcessStatusRepository camundaProcessStatusRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveAlias(Integer processId, String alias, TypeProcess typeProcess) {
        log.info("saveAlias: processId= {}", processId);
        log.info("saveAlias: alias= {}", alias);
        StatusProcess statusProcess = statusProcessRepository.findByAliasAndTypeProcessId(alias, typeProcess.getId());
        camundaProcessStatusRepository.saveAndFlush(CamundaProcessStatus.builder()
                .statusProcessId(statusProcess.getId())
                .camundaProcessId(processId)
                .createdDate(LocalDateTime.now())
                .build());
    }
}
