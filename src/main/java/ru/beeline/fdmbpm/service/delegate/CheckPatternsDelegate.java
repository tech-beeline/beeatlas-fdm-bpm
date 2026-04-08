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

        List<PatternCheckResultDTO> results = new ArrayList<>();

        for (Integer patternId : patternIds) {
            if (patternId == null) {
                continue;
            }

            PatternDTO pattern = techradarClient.getPattern(patternId);
            log.info("pattern is" + pattern.toString());

            if (pattern == null || pattern.getRule() == null || pattern.getCode() == null) {
                continue;
            }

            String rule = pattern.getRule().replace("{cmdb}", cmdb);

            List<Object> elements;
            try {
                elements = graphClient.executePatternQuery(rule);
            } catch (RuntimeException e) {
                log.error("Error executing graph query for pattern {}: {}", pattern.getCode(), e.getMessage());
                continue;
            }

            boolean isCheck = elements != null && !elements.isEmpty();

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
}

