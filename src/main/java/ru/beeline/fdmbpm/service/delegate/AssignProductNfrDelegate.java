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
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;
import ru.beeline.fdmbpm.dto.product.NfrCatalogItemDTO;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("" +
        "AssignProductNfrDelegate")
public class AssignProductNfrDelegate implements JavaDelegate {

    @Autowired
    private ProductClient productClient;

    @Autowired
    private FfManagerClient ffManagerClient;

    @Override
    public void execute(DelegateExecution execution) {
        String cmdb = (String) execution.getVariable("cmdb");
        log.info("cmdb is {}", cmdb);
        if (cmdb == null || cmdb.isBlank()) {
            log.warn("AssignProductNfrDelegate: cmdb is missing");
            return;
        }
        List<NfrCatalogItemDTO> nfrList = productClient.getAllNfr();
        if (nfrList == null || nfrList.isEmpty()) {
            log.info("NFR catalog is empty or unavailable, skip assignment for {}", cmdb);
            return;
        }
        log.info("NFR catalog size is  {}", nfrList.size());
        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions =
                ffManagerClient.getMergedActualResultsForNfr(cmdb);
        List<Integer> idsToAssign = new ArrayList<>();
        for (NfrCatalogItemDTO nfr : nfrList) {
            log.info("iterate nfr for {}", nfr.toString());
            Integer nfrId = nfr.getId();
            if (nfrId == null) {
                log.warn("Skip NFR with missing id (code={})", nfr.getCode());
                continue;
            }
            String rule = nfr.getRule();
            if (rule == null || rule.isBlank()) {
                log.info("rule is empty, assigning NFR with id={} code={}", nfr.getId(), nfr.getCode());
                idsToAssign.add(nfrId);
                continue;
            }
            if (!allRuleCodesPassFitness(rule, fitnessFunctions)) {
                log.info("no allRuleCodesPassFitness");
                continue;
            }
            idsToAssign.add(nfrId);
        }
        if (idsToAssign.isEmpty()) {
            log.info("No NFR to assign for product {}", cmdb);
            return;
        }
        productClient.postProductNfr(cmdb, idsToAssign);
        log.info("Posted {} NFR ids to product {}", idsToAssign.size(), cmdb);
    }

    private static boolean allRuleCodesPassFitness(String rule,
                                                   List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions) {
        String normalized = rule.replaceAll("\\s+", "");
        String[] parts = normalized.split(",", -1);
        boolean sawNonEmptyToken = false;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            sawNonEmptyToken = true;
            if (!isCodeChecked(fitnessFunctions, part)) {
                return false;
            }
        }
        return sawNonEmptyToken;
    }

    private static boolean isCodeChecked(
            List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions,
            String code) {
        for (AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO ff : fitnessFunctions) {
            if (ff.getCode() != null && ff.getCode().equalsIgnoreCase(code)
                    && Boolean.TRUE.equals(ff.getIsCheck())) {
                return true;
            }
        }
        return false;
    }
}
