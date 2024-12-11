package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.CapabilityService;

@Slf4j
@Component("CapabilityUploadSendProduct")
public class CapabilityUploadSendProductDelegate implements JavaDelegate {
    @Autowired
    CapabilityService capabilityService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("running process CapabilityUpload, step: send product");
        capabilityService.sendProduct();
    }
}
