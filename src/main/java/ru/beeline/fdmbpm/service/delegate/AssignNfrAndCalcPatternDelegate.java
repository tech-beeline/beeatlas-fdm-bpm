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
import java.util.stream.Collectors;

@Slf4j
@Component("AssignNfrAndCalcPatternDelegate")
public class AssignNfrAndCalcPatternDelegate implements JavaDelegate {

    private static final String BRANCH = "main";

    @Autowired
    private ProductClient productClient;

    @Autowired
    private GraphClient graphClient;

    @Override
    public void execute(DelegateExecution execution) {
        Integer nfrId = (Integer) execution.getVariable("nfrId");
        String product = (String) execution.getVariable("product");
        String cmdb = (String) execution.getVariable("cmdb");
        log.info("ℹ️ [auto-nfr] Шаг: Назначение НФТ + расчёт паттернов | nfrId={}, product={}, cmdb={}",
                nfrId, product, cmdb);

        productClient.postProductNfr(product, List.of(nfrId));
        log.info("ℹ️ [auto-nfr] НФТ {} назначен на продукт {}", nfrId, product);

        NfrDetailsV2BpmDTO nfrDetails = productClient.getNfrDetailsById(nfrId);
        if (nfrDetails == null || nfrDetails.getPatterns() == null || nfrDetails.getPatterns().isEmpty()) {
            log.warn("ℹ️ [auto-nfr] STOP: у НФТ {} нет связанных паттернов (GET /api/v2/nfr/{}) — запись паттернов пропущена",
                    nfrId, nfrId);
            return;
        }
        log.info("ℹ️ [auto-nfr] Паттерны НФТ {}: {}",
                nfrId,
                nfrDetails.getPatterns().stream()
                        .map(NfrPatternBpmDTO::getCode)
                        .collect(Collectors.toList()));

        String effectiveCmdb = (cmdb != null && !cmdb.isBlank()) ? cmdb : product;
        List<PatternCheckResultDTO> results = new ArrayList<>();
        int skipped = 0;

        for (NfrPatternBpmDTO pattern : nfrDetails.getPatterns()) {
            if (pattern.getDeleteDate() != null && !pattern.getDeleteDate().isBlank()) {
                log.info("ℹ️ [auto-nfr] skip code={}: deleteDate={}", pattern.getCode(), pattern.getDeleteDate());
                skipped++;
                continue;
            }
            if (pattern.getRule() == null || pattern.getRule().isBlank()) {
                log.info("ℹ️ [auto-nfr] skip code={}: rule пуст", pattern.getCode());
                skipped++;
                continue;
            }
            if (pattern.getCode() == null) {
                skipped++;
                continue;
            }

            String rule = pattern.getRule().replace("{cmdb}", effectiveCmdb);
            log.info("ℹ️ [auto-nfr] graph query code={} rule={}", pattern.getCode(), rule);

            List<Object> elements = graphClient.executePatternQuery(rule);
            if (elements == null) {
                log.error("ℹ️ [auto-nfr] skip code={}: Graph вернул ошибку (null)", pattern.getCode());
                skipped++;
                continue;
            }

            boolean isCheck = !elements.isEmpty();
            log.info("ℹ️ [auto-nfr] result code={} elements={} isCheck={}",
                    pattern.getCode(), elements.size(), isCheck);
            results.add(PatternCheckResultDTO.builder()
                    .code(pattern.getCode())
                    .isCheck(isCheck)
                    .resultDetails(null)
                    .build());
        }

        log.info("ℹ️ [auto-nfr] Итого: к записи={}, skip={}", results.size(), skipped);

        if (results.isEmpty()) {
            log.warn("ℹ️ [auto-nfr] STOP: нечего сохранять для НФТ {}", nfrId);
            return;
        }

        log.info("ℹ️ [auto-nfr] POST /api/v2/product/{}/patterns/nfr?branch={} source-id=null body={}",
                product, BRANCH,
                results.stream()
                        .map(r -> r.getCode() + "=" + r.getIsCheck())
                        .collect(Collectors.joining(", ")));
        productClient.postPatternCheckResults(product, "nfr", null, BRANCH, results);
        log.info("ℹ️ [auto-nfr] Шаг завершён | product={}, nfrId={}, записано={}", product, nfrId, results.size());
    }
}
