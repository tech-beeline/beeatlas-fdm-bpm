package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CapabilityClient;

@Slf4j
@Component("СalculatePrivateTechCapabiltiesCount")
public class CalculatePrivateTechCapabilitiesCountDelegate implements JavaDelegate {

    @Autowired
    CapabilityClient capabilityClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("running process СalculatePrivateTechCapabiltiesCount");
        int entityId = (int) delegateExecution.getVariable("entity_id");
        capabilityClient.сalculatePrivateTechCapabiltiesCount(entityId);
    }
}
