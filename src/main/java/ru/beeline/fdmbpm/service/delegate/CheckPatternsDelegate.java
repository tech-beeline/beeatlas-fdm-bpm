package ru.beeline.fdmbpm.service.delegate;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.TechradarClient;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;
import ru.beeline.fdmbpm.dto.techradar.PatternDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component("CheckPatternsDelegate")
public class CheckPatternsDelegate implements JavaDelegate {

    @Autowired
    private ProductClient productClient;

    @Autowired
    private TechradarClient techradarClient;

    @Autowired
    private GraphClient graphClient;

    @Override
    public void execute(DelegateExecution execution) {
        String cmdb = (String) execution.getVariable("cmdb");
        Integer docId = (Integer) execution.getVariable("docId");
        log.info("cmdb is" + cmdb);
        log.info("docId is" + docId);

        if (cmdb == null) {
            log.error("cmdb is null in process context");
            return;
        }

        List<Integer> patternIds = productClient.getPatternIdsByProductAlias(cmdb);
        log.info("patternIds is" + patternIds);

        if (patternIds == null || patternIds.isEmpty()) {
            log.info("No patterns for product alias {}", cmdb);
            return;
        }

        List<PatternDTO> patternsFromTechradar = techradarClient.getPatterns();
        if (patternsFromTechradar == null || patternsFromTechradar.isEmpty()) {
            log.warn("Techradar GET /api/v1/patterns returned no data, pattern checks skipped");
            return;
        }

        Map<Integer, PatternDTO> patternById = patternsFromTechradar.stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(PatternDTO::getId, Function.identity(), (a, b) -> a));

        List<PatternCheckResultDTO> results = new ArrayList<>();

        for (Integer patternId : patternIds) {
            if (patternId == null) {
                continue;
            }

            PatternDTO pattern = patternById.get(patternId);
            if (pattern == null) {
                log.debug("Pattern id {} not found in techradar list, skip", patternId);
                continue;
            }

            if (hasDeleteDate(pattern)) {
                log.debug("Pattern {} has deleteDate set, skip", pattern.getCode());
                continue;
            }

            if (pattern.getRule() == null || pattern.getRule().isBlank()) {
                log.debug("Pattern {} has empty rule, skip", pattern.getCode());
                continue;
            }

            if (pattern.getCode() == null) {
                continue;
            }

            log.info("pattern is {}", pattern);

            String rule = pattern.getRule().replace("{cmdb}", cmdb);

            List<Object> elements = graphClient.executePatternQuery(rule);
            if (elements == null) {
                log.error("Graph GET /api/v1/elements failed for pattern {}, skip", pattern.getCode());
                continue;
            }

            boolean isCheck = !elements.isEmpty();

            PatternCheckResultDTO result = PatternCheckResultDTO.builder()
                    .code(pattern.getCode())
                    .isCheck(isCheck)
                    .resultDetails(null)
                    .build();
            log.info("results is" + results.toString());

            results.add(result);
        }

        if (results.isEmpty()) {
            log.info("Pattern checks results are empty, nothing to send");
            return;
        }

        productClient.postPatternCheckResults(cmdb, "pipeline", docId, results);
    }

    private static boolean hasDeleteDate(PatternDTO pattern) {
        String deleteDate = pattern.getDeleteDate();
        return deleteDate != null && !deleteDate.isBlank();
    }
}

