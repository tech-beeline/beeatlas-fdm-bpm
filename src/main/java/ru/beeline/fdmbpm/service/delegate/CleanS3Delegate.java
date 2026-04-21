package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.DocumentClient;

@Slf4j
@Component("CleanS3Delegate")
public class CleanS3Delegate implements JavaDelegate {

    @Autowired
    DocumentClient documentClient;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("CleanS3Delegate start");
        documentClient.deleteOldDocuments();
        log.info("CleanS3Delegate completed");
    }
}
