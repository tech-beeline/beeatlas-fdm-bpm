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
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;
import ru.beeline.fdmbpm.dto.product.NfrCatalogItemDTO;
import ru.beeline.fdmbpm.dto.product.NfrDetailsV2BpmDTO;
import ru.beeline.fdmbpm.dto.product.NfrPatternBpmDTO;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component("RecalculateTriggeredNfrDelegate")
public class RecalculateTriggeredNfrDelegate implements JavaDelegate {

    @Autowired
    private ProductClient productClient;
    @Autowired
    private FfManagerClient ffManagerClient;
    @Autowired
    private GraphClient graphClient;

    @Override
    public void execute(DelegateExecution execution) {
        String alias = RegisterFfRecalcProcessDelegate.readCode(execution.getVariable("product"));
        String ffCode = RegisterFfRecalcProcessDelegate.readCode(execution.getVariable("ffCode"));
        List<String> recalculated = RegisterAutoNfrProcessDelegate.readRule(
                execution.getVariable(RecalculateDependentFfDelegate.RECALCULATED_FF_CODES));
        if (alias.isEmpty()) {
            log.info("RecalculateTriggeredNfrDelegate: пропуск, продукт не задан");
            return;
        }
        Set<String> ffCodes = new LinkedHashSet<>();
        if (!ffCode.isEmpty()) {
            ffCodes.add(ffCode);
        }
        ffCodes.addAll(recalculated);
        if (ffCodes.isEmpty()) {
            log.info("RecalculateTriggeredNfrDelegate: пропуск, нет кодов ФФ в контексте, alias={}", alias);
            return;
        }
        log.info("RecalculateTriggeredNfrDelegate: alias={}, ffCodes={}", alias, ffCodes);

        List<NfrCatalogItemDTO> catalog = productClient.getAllNfr();
        if (catalog == null || catalog.isEmpty()) {
            log.info("RecalculateTriggeredNfrDelegate: каталог НФТ пуст, alias={}", alias);
            return;
        }
        List<NfrCatalogItemDTO> triggered = new ArrayList<>();
        for (NfrCatalogItemDTO nfr : catalog) {
            if (nfr != null && nfr.getId() != null && ruleContainsAny(nfr.getRule(), ffCodes)) {
                triggered.add(nfr);
            }
        }
        if (triggered.isEmpty()) {
            log.info("RecalculateTriggeredNfrDelegate: нет НФТ с триггером из {}, alias={}", ffCodes, alias);
            return;
        }

        List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions =
                ffManagerClient.getMergedActualResultsForNfr(alias);
        List<Integer> assignedIds = new ArrayList<>();
        for (NfrCatalogItemDTO nfr : triggered) {
            if (!allRuleCodesPassFitness(nfr.getRule(), fitnessFunctions)) {
                log.info("RecalculateTriggeredNfrDelegate: НФТ id={} code={} не назначено, rule={}",
                        nfr.getId(), nfr.getCode(), nfr.getRule());
                continue;
            }
            log.info("RecalculateTriggeredNfrDelegate: НФТ id={} code={} назначается, rule={}",
                    nfr.getId(), nfr.getCode(), nfr.getRule());
            assignedIds.add(nfr.getId());
        }
        if (assignedIds.isEmpty()) {
            log.info("RecalculateTriggeredNfrDelegate: ни одно НФТ не назначено, проверка паттернов пропущена, alias={}",
                    alias);
            return;
        }
        productClient.postProductNfr(alias, assignedIds, null);
        for (Integer nfrId : assignedIds) {
            checkPatterns(alias, nfrId);
        }
    }

    private void checkPatterns(String alias, Integer nfrId) {
        NfrDetailsV2BpmDTO details = productClient.getNfrDetailsById(nfrId);
        if (details == null || details.getPatterns() == null || details.getPatterns().isEmpty()) {
            log.info("RecalculateTriggeredNfrDelegate: у НФТ {} нет паттернов", nfrId);
            return;
        }
        List<PatternCheckResultDTO> results = new ArrayList<>();
        for (NfrPatternBpmDTO pattern : details.getPatterns()) {
            if (pattern.getDeleteDate() != null && !pattern.getDeleteDate().isBlank()) {
                continue;
            }
            if (pattern.getRule() == null || pattern.getRule().isBlank() || pattern.getCode() == null) {
                continue;
            }
            String rule = GraphClient.singleLine(pattern.getRule().replace("{cmdb}", alias));
            List<Object> elements = graphClient.executePatternQuery(rule);
            if (elements == null) {
                log.info("RecalculateTriggeredNfrDelegate: паттерн {} пропущен, граф вернул ошибку", pattern.getCode());
                continue;
            }
            results.add(PatternCheckResultDTO.builder()
                    .code(pattern.getCode())
                    .isCheck(!elements.isEmpty())
                    .resultDetails(null)
                    .build());
        }
        if (results.isEmpty()) {
            log.info("RecalculateTriggeredNfrDelegate: для НФТ {} нечего записать в паттерны", nfrId);
            return;
        }
        productClient.postPatternCheckResults(alias, "nfr", null, null, results);
        log.info("RecalculateTriggeredNfrDelegate: паттерны НФТ {} записаны, count={}", nfrId, results.size());
    }

    static boolean ruleContainsAny(String rule, Set<String> ffCodes) {
        for (String token : ruleTokens(rule)) {
            for (String code : ffCodes) {
                if (token.equalsIgnoreCase(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean allRuleCodesPassFitness(String rule,
                                                   List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions) {
        boolean sawToken = false;
        for (String token : ruleTokens(rule)) {
            sawToken = true;
            if (!isCodeChecked(fitnessFunctions, token)) {
                return false;
            }
        }
        return sawToken;
    }

    private static boolean isCodeChecked(List<AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO> fitnessFunctions,
                                         String code) {
        if (fitnessFunctions == null) {
            return false;
        }
        for (AssessmentFitnessForNfrDTO.FitnessFunctionNfrCheckDTO ff : fitnessFunctions) {
            if (ff.getCode() != null && ff.getCode().equalsIgnoreCase(code) && Boolean.TRUE.equals(ff.getIsCheck())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> ruleTokens(String rule) {
        if (rule == null || rule.isBlank()) {
            return List.of();
        }
        String normalized = rule.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        for (String part : normalized.split(",", -1)) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
