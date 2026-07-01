/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.FfManagerClient;

import java.util.List;

@Slf4j
@Component("GetProductsForNfrDelegate")
public class GetProductsForNfrDelegate implements JavaDelegate {

    @Autowired
    private FfManagerClient ffManagerClient;

    @Override
    public void execute(DelegateExecution execution) {
        Integer nfrId = (Integer) execution.getVariable("nfrId");
        @SuppressWarnings("unchecked")
        List<String> rule = (List<String>) execution.getVariable("rule");
        log.info("GetProductsForNfrDelegate: nfrId={}, rule={}", nfrId, rule);

        if (rule == null || rule.isEmpty()) {
            log.warn("GetProductsForNfrDelegate: rule пуст, products=[]");
            execution.setVariable("products", List.of());
            return;
        }

        List<String> products = ffManagerClient.getProductsPassedFf(rule);
        log.info("GetProductsForNfrDelegate: получено {} продуктов для nfrId={}", products.size(), nfrId);
        execution.setVariable("products", products);
    }
}
