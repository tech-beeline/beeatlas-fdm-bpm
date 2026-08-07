/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.NfrDetailsV2BpmDTO;
import ru.beeline.fdmbpm.dto.product.NfrPatternBpmDTO;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("AssignNfrAndCalcPatternDelegate")
public class AssignNfrAndCalcPatternDelegate implements JavaDelegate {

    @Autowired
    private ProductClient productClient;

    @Autowired
    private GraphClient graphClient;

    @Override
    public void execute(DelegateExecution execution) {
        Integer nfrId = (Integer) execution.getVariable("nfrId");
        String product = (String) execution.getVariable("product");
        String cmdb = (String) execution.getVariable("cmdb");
        log.info("AssignNfrAndCalcPatternDelegate: nfrId={}, product={}, cmdb={}", nfrId, product, cmdb);

        // 1. Назначить НФТ на продукт
        productClient.postProductNfr(product, List.of(nfrId));
        log.info("NFR {} assigned to product {}", nfrId, product);

        // 2. Получить детали НФТ с паттернами
        NfrDetailsV2BpmDTO nfrDetails = productClient.getNfrDetailsById(nfrId);
        if (nfrDetails == null || nfrDetails.getPatterns() == null || nfrDetails.getPatterns().isEmpty()) {
            log.info("NFR {} has no patterns, pattern check skipped", nfrId);
            return;
        }

        // cmdb для подстановки в правило: берём из контекста, при отсутствии — alias продукта
        String effectiveCmdb = (cmdb != null && !cmdb.isBlank()) ? cmdb : product;

        List<PatternCheckResultDTO> results = new ArrayList<>();

        for (NfrPatternBpmDTO pattern : nfrDetails.getPatterns()) {
            if (pattern.getDeleteDate() != null && !pattern.getDeleteDate().isBlank()) {
                log.debug("Pattern {} has deleteDate, skip", pattern.getCode());
                continue;
            }
            if (pattern.getRule() == null || pattern.getRule().isBlank()) {
                log.debug("Pattern {} has empty rule, skip", pattern.getCode());
                continue;
            }
            if (pattern.getCode() == null) {
                continue;
            }

            String rule = pattern.getRule().replace("{cmdb}", effectiveCmdb);

            List<Object> elements = graphClient.executePatternQuery(rule);
            if (elements == null) {
                log.error("Graph query failed for pattern {}, skip", pattern.getCode());
                continue;
            }

            results.add(PatternCheckResultDTO.builder()
                    .code(pattern.getCode())
                    .isCheck(!elements.isEmpty())
                    .resultDetails(null)
                    .build());
        }

        if (results.isEmpty()) {
            log.info("Pattern check results empty for NFR {}, nothing to save", nfrId);
            return;
        }

        log.info("Saving {} pattern check results for product {} (NFR {})", results.size(), product, nfrId);
        productClient.postPatternCheckResults(product, "nfr", null, results);
    }
}
