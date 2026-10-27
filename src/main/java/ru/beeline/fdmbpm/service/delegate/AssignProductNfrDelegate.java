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
import java.util.stream.Collectors;

@Slf4j
@Component("AssignProductNfrDelegate")
public class AssignProductNfrDelegate implements JavaDelegate {

    @Autowired
    private ProductClient productClient;

    @Autowired
    private FfManagerClient ffManagerClient;

    @Override
    public void execute(DelegateExecution execution) {
        String cmdb = (String) execution.getVariable("cmdb");
        Integer docId = (Integer) execution.getVariable("docId");
        Integer processId = (Integer) execution.getVariable("process_id");
        log.info("ℹ️ [pipeline] Шаг: Назначение НФТ на продукт | cmdb={}, docId={}, process_id={}",
                cmdb, docId, processId);

        if (cmdb == null || cmdb.isBlank()) {
            log.warn("ℹ️ [pipeline/nfr] STOP: переменная cmdb не задана — назначение НФТ пропущено");
            return;
        }

        List<NfrCatalogItemDTO> nfrList = productClient.getAllNfr();
        if (nfrList == null || nfrList.isEmpty()) {
            log.info("ℹ️ [pipeline/nfr] STOP: каталог НФТ пуст или недоступен (GET /api/v1/nfr), назначение для {} пропущено",
                    cmdb);
            return;
        }
        log.info("ℹ️ [pipeline/nfr] Каталог НФТ: {} шт. id={}",
                nfrList.size(),
                nfrList.stream().map(NfrCatalogItemDTO::getId).collect(Collectors.toList()));

        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions =
                ffManagerClient.getMergedActualResultsForNfr(cmdb);
        long passedFf = fitnessFunctions == null ? 0
                : fitnessFunctions.stream().filter(ff -> Boolean.TRUE.equals(ff.getIsCheck())).count();
        log.info("ℹ️ [pipeline/nfr] FF actual-results для {}: всего={}, isCheck=true={}",
                cmdb,
                fitnessFunctions == null ? 0 : fitnessFunctions.size(),
                passedFf);
        if (fitnessFunctions != null && !fitnessFunctions.isEmpty()) {
            log.info("ℹ️ [pipeline/nfr] FF коды: {}",
                    fitnessFunctions.stream()
                            .map(ff -> ff.getCode() + "=" + ff.getIsCheck())
                            .collect(Collectors.joining(", ")));
        } else {
            log.warn("ℹ️ [pipeline/nfr] FF результаты пусты — ни одно НФТ с непустым rule не пройдёт проверку");
        }

        List<Integer> idsToAssign = new ArrayList<>();
        for (NfrCatalogItemDTO nfr : nfrList) {
            Integer nfrId = nfr.getId();
            if (nfrId == null) {
                log.warn("ℹ️ [pipeline/nfr] skip НФТ без id (code={})", nfr.getCode());
                continue;
            }
            String rule = nfr.getRule();
            if (rule == null || rule.isBlank()) {
                log.info("ℹ️ [pipeline/nfr] skip НФТ id={} code={}: rule пуст — авто-назначение невозможно",
                        nfrId, nfr.getCode());
                continue;
            }
            if (!allRuleCodesPassFitness(rule, fitnessFunctions)) {
                log.info("ℹ️ [pipeline/nfr] skip НФТ id={} code={}: rule='{}' — не все коды ФФ с isCheck=true",
                        nfrId, nfr.getCode(), rule);
                continue;
            }
            log.info("ℹ️ [pipeline/nfr] OK НФТ id={} code={}: rule='{}' — будет назначено",
                    nfrId, nfr.getCode(), rule);
            idsToAssign.add(nfrId);
        }

        if (idsToAssign.isEmpty()) {
            log.warn("ℹ️ [pipeline/nfr] STOP: нет НФТ для назначения на {}. "
                            + "Следующий шаг CheckPatterns получит пустой список паттернов (если на продукте ещё нет НФТ)",
                    cmdb);
            return;
        }

        log.info("ℹ️ [pipeline/nfr] POST /api/v1/nfr/product alias={} ids={}", cmdb, idsToAssign);
        productClient.postProductNfr(cmdb, idsToAssign);
        log.info("ℹ️ [pipeline] Шаг завершён: Назначение НФТ | cmdb={}, назначено={}", cmdb, idsToAssign.size());
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
        if (fitnessFunctions == null) {
            return false;
        }
        for (AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO ff : fitnessFunctions) {
            if (ff.getCode() != null && ff.getCode().equalsIgnoreCase(code)
                    && Boolean.TRUE.equals(ff.getIsCheck())) {
                return true;
            }
        }
        return false;
    }
}
