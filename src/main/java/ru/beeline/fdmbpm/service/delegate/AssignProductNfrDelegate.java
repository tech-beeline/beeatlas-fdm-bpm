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
import ru.beeline.fdmbpm.client.FfManagerClient;
import ru.beeline.fdmbpm.client.ProductClient;
import ru.beeline.fdmbpm.domain.CamundaProcess;
import ru.beeline.fdmbpm.domain.TypeProcess;
import ru.beeline.fdmbpm.dto.product.AssessmentFitnessForNfrDTO;
import ru.beeline.fdmbpm.dto.product.NfrCatalogItemDTO;
import ru.beeline.fdmbpm.repository.camunda.CamundaProcessRepository;
import ru.beeline.fdmbpm.repository.camunda.TypeProcessRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("AssignProductNfrDelegate")
public class AssignProductNfrDelegate extends StatusLogic implements JavaDelegate {

    static final String STATUS_DONE = "nfrcrt";
    static final String STATUS_ERROR = "nfrerr";

    @Autowired
    private ProductClient productClient;

    @Autowired
    private FfManagerClient ffManagerClient;

    @Autowired
    TypeProcessRepository typeProcessRepository;

    @Autowired
    CamundaProcessRepository camundaProcessRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Override
    public void execute(DelegateExecution execution) {
        if (DatapipeBranchProcess.isBranchProcess(execution)) {
            assign(execution);
            return;
        }
        try {
            assign(execution);
            saveDatapipeStatus(execution, STATUS_DONE);
        } catch (RuntimeException e) {
            saveErrorStatus(execution, STATUS_ERROR);
            throw e;
        }
    }

    private void assign(DelegateExecution execution) {
        String cmdb = (String) execution.getVariable("cmdb");
        Integer docId = (Integer) execution.getVariable("docId");
        Integer processId = (Integer) execution.getVariable("process_id");
        String branch = DatapipeBranchProcess.branch(execution);
        log.info("ℹ️ [pipeline] Шаг: Назначение НФТ на продукт | cmdb={}, docId={}, process_id={}, branch={}",
                cmdb, docId, processId, branch);

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

        log.info("ℹ️ [pipeline/nfr] POST /api/v1/nfr/product alias={} branch={} ids={}", cmdb, branch, idsToAssign);
        productClient.postProductNfr(cmdb, idsToAssign, branch);
        log.info("ℹ️ [pipeline] Шаг завершён: Назначение НФТ | cmdb={}, branch={}, назначено={}",
                cmdb, branch, idsToAssign.size());
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
