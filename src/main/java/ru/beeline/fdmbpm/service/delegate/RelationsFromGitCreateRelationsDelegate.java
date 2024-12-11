package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.RelationsService;

@Slf4j
@Component("RelationsFromGitCreateRelations")
public class RelationsFromGitCreateRelationsDelegate implements JavaDelegate {
    @Autowired
    RelationsService relationsService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("running process Relations From Git");
        relationsService.createRelations();
    }
}
