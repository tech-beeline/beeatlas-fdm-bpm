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
import ru.beeline.fdmbpm.domain.CamundaProcessStatus;
import ru.beeline.fdmbpm.domain.Context;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessStatusRepository;
import ru.beeline.fdmbpm.repository.camunda.ContextRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component("CalculateProductFfDelegate")
public class CalculateProductFfDelegate implements JavaDelegate {

    @Autowired
    private ContextRepository contextRepository;
    @Autowired
    private CamundaProcessStatusRepository camundaProcessStatusRepository;
    @Autowired
    private FfManagerClient ffManagerClient;

    @Override
    public void execute(DelegateExecution execution) {
        String alias = RegisterFfRecalcProcessDelegate.readCode(execution.getVariable("product"));
        String ffCode = RegisterFfRecalcProcessDelegate.readCode(execution.getVariable("ffCode"));
        Integer parentProcessId = RegisterAutoNfrProcessDelegate.readInteger(execution.getVariable("process_id"));
        if (alias.isEmpty() || ffCode.isEmpty()) {
            log.info("CalculateProductFfDelegate: пропуск, alias={}, ffCode={}", alias, ffCode);
            return;
        }
        Integer docId = findDocId(alias);
        if (docId == null) {
            log.info("CalculateProductFfDelegate: docId не найден, продукт {} пропущен", alias);
            return;
        }
        log.info("CalculateProductFfDelegate: alias={}, ffCode={}, docId={}, process_id={}",
                alias, ffCode, docId, parentProcessId);
        execution.setVariable("docId", docId);
        ffManagerClient.runFitnessFunction(ffCode, alias, docId, parentProcessId);
    }

    private Integer findDocId(String alias) {
        List<Context> cmdbContexts = contextRepository.findByNameAndValueIgnoreCase("cmdb", alias);
        if (cmdbContexts == null || cmdbContexts.isEmpty()) {
            return null;
        }
        Set<Integer> processIds = new LinkedHashSet<>();
        for (Context context : cmdbContexts) {
            if (context.getCamundaProcessId() != null) {
                processIds.add(context.getCamundaProcessId());
            }
        }
        List<CmdbDocIdSelector.Candidate> candidates = new ArrayList<>();
        for (Integer processId : processIds) {
            camundaProcessStatusRepository.findFirstByCamundaProcessIdOrderByCreatedDateDesc(processId)
                    .map(CamundaProcessStatus::getCreatedDate)
                    .ifPresent(date -> candidates.add(new CmdbDocIdSelector.Candidate(processId, date)));
        }
        Integer latestProcessId = CmdbDocIdSelector.latestProcessId(candidates);
        if (latestProcessId == null) {
            return null;
        }
        List<Context> contexts = contextRepository.findByCamundaProcessId(latestProcessId);
        if (contexts == null) {
            return null;
        }
        for (Context context : contexts) {
            if ("docId".equals(context.getName())) {
                return CmdbDocIdSelector.parseDocId(context.getValue());
            }
        }
        return null;
    }
}
