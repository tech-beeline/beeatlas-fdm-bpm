package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.service.InfrastructureService;

@Slf4j
@Component("GettingAllApplications")
public class GettingAllApplicationsDelegate implements JavaDelegate {

    @Autowired
    InfrastructureService infrastructureService;
    @Autowired
    ProductClient productClient;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("running process GettingAllApplications");
        delegateExecution.setVariable("products", productClient.getProductMnemonics());
    }
}
