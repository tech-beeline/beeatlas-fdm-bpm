/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * Выгрузка в Sparx для Process_datapipeline_branch. Метода выгрузки в ветку ещё нет,
 * поэтому шаг завершается без вызова Sparx, без статуса и без переменной error.
 */
@Slf4j
@Component("PublicToSparxDelegate")
public class PublicToSparxDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Шаг: Выгрузка в Sparx пропущен, метода выгрузки в ветку нет: cmdb={}, process_id={}, docId={}, "
                        + "branch={}", execution.getVariable("cmdb"), execution.getVariable("process_id"),
                execution.getVariable("docId"), DatapipeBranchProcess.branch(execution));
    }
}
