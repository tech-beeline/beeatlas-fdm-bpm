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
import ru.beeline.fdmbpm.client.GraphClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.client.TechradarClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.PatternCheckResultDTO;
import ru.beeline.fdmbpm.dto.techradar.PatternDTO;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component("CheckPatternsDelegate")
public class CheckPatternsDelegate extends StatusLogic implements JavaDelegate {

    static final String STATUS_DONE = "ptrncrt";
    static final String STATUS_ERROR = "ptrnerr";

    @Autowired
    private ProductClient productClient;

    @Autowired
    private TechradarClient techradarClient;

    @Autowired
    private GraphClient graphClient;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution execution) {
        if (DatapipeBranchProcess.isBranchProcess(execution)) {
            checkPatternsForBranch(execution);
            return;
        }
        try {
            checkPatternsForDatapipe(execution);
            saveDatapipeStatus(execution, STATUS_DONE);
        } catch (RuntimeException e) {
            saveErrorStatus(execution, STATUS_ERROR);
            throw e;
        }
    }

    private void checkPatternsForDatapipe(DelegateExecution execution) {
        String cmdb = (String) execution.getVariable("cmdb");
        Integer docId = (Integer) execution.getVariable("docId");
        String branch = DatapipeBranchProcess.branch(execution);
        log.info("cmdb is" + cmdb);
        log.info("docId is" + docId);
        log.info("branch is" + branch);

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

        List<PatternCheckResultDTO> results = checkPatterns(cmdb, patternIds);

        if (results.isEmpty()) {
            log.info("Pattern checks results are empty, nothing to send");
            return;
        }

        productClient.postPatternCheckResults(cmdb, "pipeline", docId, branch, results);
    }

    private void saveDatapipeStatus(DelegateExecution execution, String alias) {
        Integer processId = (Integer) execution.getVariable("process_id");
        CamundaProcess camundaProcess = camundaProcessRepository.findById(processId).get();
        TypeProcess typeProcess = typeProcessRepository.findById(camundaProcess.getTypeProcessId()).get();
        saveAlias(processId, alias, typeProcess);
    }

    private void saveErrorStatus(DelegateExecution execution, String alias) {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.execute(status -> {
            saveDatapipeStatus(execution, alias);
            return null;
        });
    }

    /**
     * Проверка паттернов Process_datapipeline_branch: кандидаты по НФТ ветки процесса,
     * результат пишется в ту же ветку.
     */
    private void checkPatternsForBranch(DelegateExecution execution) {
        String cmdb = (String) execution.getVariable("cmdb");
        Integer docId = (Integer) execution.getVariable("docId");
        String branch = DatapipeBranchProcess.branch(execution);
        log.info("Pattern checks for branch: cmdb={}, docId={}, branch={}", cmdb, docId, branch);

        if (cmdb == null) {
            log.error("cmdb is null in process context");
            return;
        }

        List<Integer> patternIds = productClient.getPatternIdsByProductAlias(cmdb, branch);
        log.info("patternIds is" + patternIds);

        if (patternIds == null || patternIds.isEmpty()) {
            log.info("No patterns for product alias {} in branch {}", cmdb, branch);
            return;
        }

        List<PatternCheckResultDTO> results = checkPatterns(cmdb, patternIds);

        if (results.isEmpty()) {
            log.info("Pattern checks results are empty, nothing to send");
            return;
        }

        productClient.postPatternCheckResults(cmdb, "pipeline", docId, branch, results);
    }

    private List<PatternCheckResultDTO> checkPatterns(String cmdb, List<Integer> patternIds) {
        List<PatternDTO> patternsFromTechradar = techradarClient.getPatterns();
        if (patternsFromTechradar == null || patternsFromTechradar.isEmpty()) {
            log.warn("Techradar GET /api/v1/patterns returned no data, pattern checks skipped");
            return List.of();
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

            String rule = GraphClient.singleLine(pattern.getRule().replace("{cmdb}", cmdb));

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
        return results;
    }

    private static boolean hasDeleteDate(PatternDTO pattern) {
        String deleteDate = pattern.getDeleteDate();
        return deleteDate != null && !deleteDate.isBlank();
    }
}

