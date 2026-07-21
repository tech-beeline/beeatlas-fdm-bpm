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
        log.info("Продукт (cmdb): {}", cmdb);
        if (cmdb == null || cmdb.isBlank()) {
            log.warn("AssignProductNfrDelegate: переменная cmdb не задана");
            return;
        }
        List<NfrCatalogItemDTO> nfrList = productClient.getAllNfr();
        if (nfrList == null || nfrList.isEmpty()) {
            log.info("Каталог НФТ пуст или недоступен, назначение для {} пропущено", cmdb);
            return;
        }
        log.info("Размер каталога НФТ: {}", nfrList.size());
        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions =
                ffManagerClient.getMergedActualResultsForNfr(cmdb);
        List<Integer> idsToAssign = new ArrayList<>();
        for (NfrCatalogItemDTO nfr : nfrList) {
            log.info("Обработка НФТ: {}", nfr);
            Integer nfrId = nfr.getId();
            if (nfrId == null) {
                log.warn("Пропуск НФТ без id (code={})", nfr.getCode());
                continue;
            }
            String rule = nfr.getRule();
            if (rule == null || rule.isBlank()) {
                log.info("rule пуст, пропуск НФТ с id={} code={}", nfr.getId(), nfr.getCode());
                continue;
            }
            if (!allRuleCodesPassFitness(rule, fitnessFunctions)) {
                log.info("НФТ id={} code={} не прошло проверку rule по ФФ", nfr.getId(), nfr.getCode());
                continue;
            }
            idsToAssign.add(nfrId);
        }
        if (idsToAssign.isEmpty()) {
            log.info("Нет НФТ для назначения на продукт {}", cmdb);
            return;
        }
        productClient.postProductNfr(cmdb, idsToAssign);
        log.info("Назначено {} НФТ на продукт {}", idsToAssign.size(), cmdb);
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
