package ru.beeline.fdmbpm.service.delegate;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.ArchClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.repository.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.TypeProcessRepository;

@Slf4j
@Component("CalculateGlobalFFDelegate")
public class CalculateGlobalFFDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    ArchClient archClient;
    @Autowired
    TypeProcessRepository typeProcessRepository;
    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        //TODO: to be continue
    }
}
