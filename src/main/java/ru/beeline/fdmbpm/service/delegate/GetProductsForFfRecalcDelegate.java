/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.ProductInfoShortDTO;
import ru.beeline.fdmbpm.exception.ProcessException;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("GetProductsForFfRecalcDelegate")
public class GetProductsForFfRecalcDelegate extends StatusLogic implements JavaDelegate {

    @Autowired
    private ProductClient productClient;
    @Autowired
    private CamundaProcessRepository camundaProcessRepository;
    @Autowired
    private TypeProcessRepository typeProcessRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution execution) {
        Integer processId = RegisterAutoNfrProcessDelegate.readInteger(execution.getVariable("process_id"));
        try {
            List<String> products = aliasesWithStructurizr(productClient.getProductInfo());
            log.info("GetProductsForFfRecalcDelegate: продуктов {}", products.size());
            execution.setVariable("products", products);
        } catch (Exception e) {
            log.error("GetProductsForFfRecalcDelegate: ошибка списка продуктов, process_id={}", processId, e);
            saveError(processId);
            throw new ProcessException("Ошибка получения списка продуктов для перерасчёта fitness function", e);
        }
    }

    static List<String> aliasesWithStructurizr(List<ProductInfoShortDTO> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> aliases = new ArrayList<>();
        for (ProductInfoShortDTO product : products) {
            if (product == null) {
                continue;
            }
            String alias = product.getAlias() == null ? "" : product.getAlias().trim();
            String structurizrApiUrl = product.getStructurizrApiUrl() == null ? "" : product.getStructurizrApiUrl().trim();
            if (!alias.isEmpty() && !structurizrApiUrl.isEmpty()) {
                aliases.add(alias);
            }
        }
        return aliases;
    }

    private void saveError(Integer processId) {
        if (processId == null) {
            return;
        }
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.execute(status -> {
            typeOf(processId).ifPresent(typeProcess -> saveAlias(processId, "fferr", typeProcess));
            return null;
        });
    }

    private java.util.Optional<TypeProcess> typeOf(Integer processId) {
        return camundaProcessRepository.findById(processId)
                .map(CamundaProcess::getTypeProcessId)
                .flatMap(typeProcessRepository::findById);
    }
}
