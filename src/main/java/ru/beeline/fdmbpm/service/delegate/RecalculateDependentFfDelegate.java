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
import ru.beeline.fdmbpm.dto.ffmanager.FitnessFunctionCatalogDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("RecalculateDependentFfDelegate")
public class RecalculateDependentFfDelegate implements JavaDelegate {

    static final String RECALCULATED_FF_CODES = "recalculatedFfCodes";

    @Autowired
    private FfManagerClient ffManagerClient;

    @Override
    public void execute(DelegateExecution execution) {
        String alias = RegisterFfRecalcProcessDelegate.readCode(execution.getVariable("product"));
        String ffCode = RegisterFfRecalcProcessDelegate.readCode(execution.getVariable("ffCode"));
        Integer docId = RegisterAutoNfrProcessDelegate.readInteger(execution.getVariable("docId"));
        Integer processId = RegisterAutoNfrProcessDelegate.readInteger(execution.getVariable("process_id"));
        if (alias.isEmpty() || ffCode.isEmpty() || docId == null) {
            log.info("RecalculateDependentFfDelegate: пропуск, alias={}, ffCode={}, docId={}", alias, ffCode, docId);
            execution.setVariable(RECALCULATED_FF_CODES, new ArrayList<String>());
            return;
        }
        List<String> dependents = dependentCodes(ffCode, ffManagerClient.getFitnessFunctions());
        log.info("RecalculateDependentFfDelegate: alias={}, ffCode={}, зависимых={}", alias, ffCode, dependents);
        List<String> recalculated = new ArrayList<>();
        for (String code : dependents) {
            if (ffManagerClient.runFitnessFunction(code, alias, docId, processId)) {
                recalculated.add(code);
            } else {
                log.info("RecalculateDependentFfDelegate: {} не пересчитана для {}", code, alias);
            }
        }
        execution.setVariable(RECALCULATED_FF_CODES, recalculated);
        log.info("RecalculateDependentFfDelegate: в контекст {}={}", RECALCULATED_FF_CODES, recalculated);
    }

    static List<String> dependentCodes(String sourceCode, List<FitnessFunctionCatalogDTO> catalog) {
        if (sourceCode == null || sourceCode.isBlank() || catalog == null || catalog.isEmpty()) {
            return List.of();
        }
        String sourceLower = sourceCode.trim().toLowerCase(Locale.ROOT);
        Map<String, String> codeByLower = new LinkedHashMap<>();
        Map<String, List<String>> applicabilityByLower = new LinkedHashMap<>();
        for (FitnessFunctionCatalogDTO ff : catalog) {
            if (ff == null || ff.getCode() == null || ff.getCode().isBlank()) {
                continue;
            }
            String lower = ff.getCode().trim().toLowerCase(Locale.ROOT);
            if (lower.equals(sourceLower) || !containsToken(ff.getApplicability(), sourceCode)) {
                continue;
            }
            codeByLower.putIfAbsent(lower, ff.getCode().trim());
            applicabilityByLower.putIfAbsent(lower, tokens(ff.getApplicability()));
        }
        Map<String, Set<String>> waitingFor = new LinkedHashMap<>();
        for (String lower : codeByLower.keySet()) {
            Set<String> prerequisites = new LinkedHashSet<>();
            for (String token : applicabilityByLower.get(lower)) {
                String prerequisite = token.toLowerCase(Locale.ROOT);
                if (codeByLower.containsKey(prerequisite) && !prerequisite.equals(lower)) {
                    prerequisites.add(prerequisite);
                }
            }
            waitingFor.put(lower, prerequisites);
        }
        List<String> ordered = new ArrayList<>();
        boolean progressed = true;
        while (ordered.size() < codeByLower.size() && progressed) {
            progressed = false;
            for (String lower : codeByLower.keySet()) {
                if (containsCode(ordered, codeByLower.get(lower))) {
                    continue;
                }
                boolean ready = waitingFor.get(lower).stream()
                        .allMatch(prerequisite -> containsCode(ordered, codeByLower.get(prerequisite)));
                if (ready) {
                    ordered.add(codeByLower.get(lower));
                    progressed = true;
                }
            }
        }
        for (String lower : codeByLower.keySet()) {
            if (!containsCode(ordered, codeByLower.get(lower))) {
                ordered.add(codeByLower.get(lower));
            }
        }
        return ordered;
    }

    private static boolean containsToken(String applicability, String code) {
        for (String token : tokens(applicability)) {
            if (token.equalsIgnoreCase(code.trim())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> tokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : raw.split(",", -1)) {
            String token = part.trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static boolean containsCode(List<String> codes, String code) {
        for (String existing : codes) {
            if (existing.equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }
}
