package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.service.TechRecalculationService;

@Slf4j
@Component("TechDescriptionQualityRecalculation")
public class TechDescriptionQualityRecalculationDelegate implements JavaDelegate {


    @Autowired
    private TechRecalculationService techRecalculationService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("running process TechDescriptionQualityRecalculation");
        techRecalculationService.getTechRecalculation();
    }
}
